package historicalpassports.test;

import historicalpassports.HistoricalPassports;
import historicalpassports.block.InspectionDeskEntity;
import historicalpassports.data.*;
import historicalpassports.passport.*;
import historicalpassports.permission.OfficerPermissions;
import historicalpassports.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder(HistoricalPassports.ID)
@PrefixGameTestTemplate(false)
public final class PassportGameTests {
    private static UUID nonce(UUID player) {
        try {
            var field=PassportService.class.getDeclaredField("SESSIONS");field.setAccessible(true);
            Object session=((Map<?,?>)field.get(null)).get(player);
            var nonce=session.getClass().getDeclaredField("nonce");nonce.setAccessible(true);return (UUID)nonce.get(session);
        } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    @GameTest(template="empty", timeoutTicks=100) public static void sessionSecurityAndStamping(GameTestHelper h) {
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"StampTester"));
        var data=PassportSavedData.get(p.server);var stack=PassportService.issue(p.server,p.getUUID(),"StampTester",Countries.get("TR"),"Test");
        p.getInventory().setItem(0,stack);var r=PassportService.verify(p.server,stack);
        PassportService.openItem(p,stack);UUID first=nonce(p.getUUID());
        PassportService.action(p,first,1,true,true,"Test gate","",0);
        h.assertTrue(r.history().isEmpty(),"Unauthorized session stamped");
        data.officer(p.getUUID(),"StampTester",true);
        h.runAtTickTime(5,()->PassportService.action(p,first,3,false,true,"TR","",0));
        h.runAtTickTime(10,()->{
            // Moving the document to another inventory slot must retain valid access.
            p.getInventory().setItem(0,ItemStack.EMPTY);p.getInventory().setItem(8,stack);
            PassportService.action(p,first,1,true,true,"Test gate","",0);
            h.assertTrue(r.history().size()==1 && r.history().get(0).approved(),"Authorized approval failed after slot move");
            PassportService.action(p,first,1,true,true,"Replay","",0);
            h.assertTrue(r.history().size()==1,"Replayed nonce added a stamp");
        });
        h.runAtTickTime(20,()->{
            PassportService.action(p,nonce(p.getUUID()),1,false,false,"Test gate","Visa refused",0);
            h.assertTrue(r.history().size()==2 && !r.history().get(1).approved() && !r.history().get(1).entry(),"Decision and crossing type mixed");
            h.assertTrue(r.status(PassportService.today(),true).equals("valid"),"Denial revoked passport");
            data.officer(p.getUUID(),"StampTester",false);
        });
        h.runAtTickTime(30,()->{
            PassportService.action(p,nonce(p.getUUID()),1,true,true,"Test gate","",0);
            h.assertTrue(r.history().size()==2,"Role revoked mid-session still stamps");
            data.officer(p.getUUID(),"StampTester",true);p.getInventory().setItem(8,ItemStack.EMPTY);
        });
        h.runAtTickTime(40,()->{
            PassportService.action(p,nonce(p.getUUID()),1,true,true,"Test gate","",0);
            h.assertTrue(r.history().size()==2,"Transferred document remains stampable");PassportService.close(p.getUUID());h.succeed();
        });
    }
    @GameTest(template = "empty") public static void countryRegistry(GameTestHelper h) {
        h.assertTrue(Countries.ALL.size() == 195, "Exactly 195 baseline countries");
        for (String code : Countries.ALL.keySet()) h.assertTrue(ModRegistry.PASSPORTS.get(code).isPresent(), "Missing item: " + code);
        h.succeed();
    }
    @GameTest(template = "empty") public static void forgeryAndClones(GameTestHelper h) {
        var server=h.getLevel().getServer(); UUID owner=UUID.randomUUID();
        ItemStack sample=new ItemStack(ModRegistry.PASSPORTS.get("TR").get());
        h.assertTrue(PassportService.verify(server,sample)==null,"Creative sample must be unverified");
        ItemStack official=PassportService.issue(server,owner,"Tester",Countries.get("TR"),"Test authority");
        var record=PassportService.verify(server,official);
        h.assertTrue(record!=null,"Issued identity must resolve");
        ItemStack clone=official.copy();
        h.assertTrue(PassportService.verify(server,clone)==record,"Clone must resolve to SAME canonical identity");
        clone.getTagElement("Passport").putString("holderName","Forged name");
        h.assertTrue(PassportService.verify(server,clone).holderName.equals("Tester"),"NBT name must not change registry");
        clone.getTagElement("Passport").putUUID("seal",UUID.randomUUID());
        h.assertTrue(PassportService.verify(server,clone)==null,"Forged seal accepted");
        ItemStack wrongCountry=new ItemStack(ModRegistry.PASSPORTS.get("DE").get()); wrongCountry.setTag(official.getTag().copy());
        h.assertTrue(PassportService.verify(server,wrongCountry)==null,"Country substitution accepted");
        record.revoke();
        h.assertTrue(PassportService.verify(server,official).revoked(),"Revocation is canonical");
        var viewer=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TooltipTester"));
        viewer.tickCount=20; official.getTagElement("Passport").putString("statusHint","valid");
        official.getItem().inventoryTick(official,h.getLevel(),viewer,0,true);
        h.assertTrue(official.getTagElement("Passport").getString("statusHint").equals("revoked"),"Server did not correct forged tooltip status");
        h.succeed();
    }
    @GameTest(template = "empty") public static void diskPersistence(GameTestHelper h) throws Exception {
        var d=new PassportSavedData();UUID owner=UUID.randomUUID(),officer=UUID.randomUUID();
        PassportRecord r=new PassportRecord("TEST-1","TR",owner,"Holder","Authority",1,3651,UUID.randomUUID());
        r.append(new Inspection(officer,"Officer",123456789,true,true,"Checkpoint",""),100);
        r.append(new Inspection(officer,"Officer",123456790,false,false,"Checkpoint","Missing visa"),100);
        d.add(r); d.officer(officer,"Officer",true);
        d.mail(owner,new ItemStack(ModRegistry.PASSPORTS.get("TR").get()));
        Path path=Path.of("test-results","registry-roundtrip.nbt");Files.createDirectories(path.getParent());
        NbtIo.writeCompressed(d.save(new CompoundTag()),path.toFile());
        PassportSavedData loaded=PassportSavedData.load(NbtIo.readCompressed(path.toFile()));
        h.assertTrue(loaded.isOfficer(officer),"Officer role lost after disk reload");
        h.assertTrue(loaded.find("TEST-1").history().size()==2,"History lost after disk reload");
        h.assertTrue(loaded.mailbox(owner).size()==1,"Offline return lost after disk reload");
        h.assertTrue(loaded.find("TEST-1").status(100,true).equals("valid"),"Prior denial must not invalidate document");
        h.assertTrue(loaded.find("TEST-1").status(3651,true).equals("expired"),"Expiry boundary");
        h.assertTrue(loaded.find("TEST-1").status(3651,false).equals("valid"),"Expiration setting");
        loaded.find("TEST-1").revoke();
        h.assertTrue(PassportSavedData.load(loaded.save(new CompoundTag())).find("TEST-1").revoked(),"Revocation lost");
        h.succeed();
    }
    @GameTest(template = "empty") public static void rolesAndUnsolicitedPackets(GameTestHelper h) {
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TestOfficer"));var data=PassportSavedData.get(p.server);
        data.officer(p.getUUID(),"Test",false);
        h.assertFalse(OfficerPermissions.canStamp(p),"Normal player can stamp");
        data.officer(p.getUUID(),"Test",true);h.assertTrue(OfficerPermissions.canStamp(p),"Officer cannot stamp");
        data.officer(p.getUUID(),"Test",false);h.assertFalse(OfficerPermissions.canStamp(p),"Revoke not immediate");
        PassportService.action(p,UUID.randomUUID(),1,true,true,"Forgery","",0);
        h.succeed();
    }
    @GameTest(template = "empty") public static void deskEscrowAndOfflineReturn(GameTestHelper h) {
        BlockPos pos=new BlockPos(1,1,1);h.setBlock(pos,ModRegistry.DESK.get());
        var desk=(InspectionDeskEntity)h.getBlockEntity(pos);UUID owner=UUID.randomUUID();
        var server=h.getLevel().getServer();ItemStack stack=PassportService.issue(server,owner,"Offline",Countries.get("JP"),"Test");
        h.assertTrue(desk.submit(stack,owner),"Submission failed");h.assertTrue(stack.isEmpty(),"Original item not consumed");
        h.assertFalse(desk.submit(new ItemStack(ModRegistry.PASSPORTS.get("DE").get()),owner),"Occupied desk accepted second document");
        var serialized=desk.saveWithFullMetadata();var restored=new InspectionDeskEntity(desk.getBlockPos(),desk.getBlockState());restored.load(serialized);
        h.assertTrue(PassportService.verify(server,restored.document())!=null,"Desk save/load lost identity");
        desk.returnToOwner();desk.returnToOwner();
        h.assertTrue(PassportSavedData.get(server).mailbox(owner).size()==1,"Double return duplicated document");
        h.assertTrue(desk.document().isEmpty(),"Escrow not cleared");h.succeed();
    }
    @GameTest(template = "empty") public static void historyLimitPreservesRecords(GameTestHelper h) {
        var r=new PassportRecord("TEST-LIMIT","US",UUID.randomUUID(),"Test","Test",0,100,UUID.randomUUID());
        var i=new Inspection(UUID.randomUUID(),"Officer",1,false,true,"Checkpoint","Reason");
        h.assertTrue(r.append(i,1),"First record rejected");h.assertFalse(r.append(i,1),"Limit not enforced");
        h.assertTrue(r.history().size()==1 && r.history().get(0)==i,"Existing history altered");h.succeed();
    }
    @GameTest(template = "empty") public static void deskOwnershipAndBlockRemoval(GameTestHelper h) {
        var owner=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DeskOwner"));
        var intruder=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Intruder"));
        BlockPos relative=new BlockPos(1,1,1);h.setBlock(relative,ModRegistry.DESK.get());BlockPos pos=h.absolutePos(relative);
        var desk=(InspectionDeskEntity)h.getBlockEntity(relative);var block=ModRegistry.DESK.get();var state=desk.getBlockState();
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
        var stack=PassportService.issue(owner.server,owner.getUUID(),"DeskOwner",Countries.get("FR"),"Test");
        intruder.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack.copy());
        block.use(state,h.getLevel(),pos,intruder,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(desk.document().isEmpty(),"Non-holder submitted another player's document");
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        block.use(state,h.getLevel(),pos,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertFalse(desk.document().isEmpty(),"Owner could not submit");
        intruder.setShiftKeyDown(true);block.use(state,h.getLevel(),pos,intruder,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertFalse(desk.document().isEmpty(),"Intruder removed escrow");
        h.setBlock(relative,net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(PassportSavedData.get(owner.server).mailbox(owner.getUUID()).size()==1,"Broken desk failed to return offline owner's document");
        var source=intruder.createCommandSourceStack().withPermission(0);
        var root=owner.server.getCommands().getDispatcher().getRoot().getChild("passport");
        h.assertFalse(root.getChild("issue").canUse(source),"Normal player allowed to issue");
        h.assertFalse(root.getChild("officer").canUse(source),"Normal player allowed to grant officer");
        h.assertFalse(root.getChild("revoke").canUse(source),"Normal player allowed to revoke");
        h.assertTrue(root.getChild("issue").canUse(source.withPermission(2)),"Operator cannot issue");
        PassportService.close(owner.getUUID());h.succeed();
    }
}
