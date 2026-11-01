package historicalpassports.test;

import historicalpassports.HistoricalPassports;
import historicalpassports.data.*;
import historicalpassports.passport.*;
import historicalpassports.visa.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(HistoricalPassports.ID)
@PrefixGameTestTemplate(false)
public final class VisaGameTests {
    private static PassportRecord passport(GameTestHelper h){var s=h.getLevel().getServer();return PassportService.verify(s,PassportService.issue(s,UUID.randomUUID(),"VisaTest",Countries.get("TR"),"Test"));}
    @GameTest(template="empty") public static void visaStatusesAndPersistence(GameTestHelper h){
        var d=new VisaData();var r=passport(h);long today=PassportService.today();var v=d.issue(r,"DE","tourist",today+1,30,1,15,"Officer","Travel");
        h.assertTrue(d.status(v,today).equals("not_yet_valid"),"Future visa valid early");h.assertTrue(d.status(v,today+1).equals("valid"),"Start date invalid");h.assertTrue(d.status(v,today+31).equals("expired"),"Expired visa valid");v.putInt("used",1);h.assertTrue(d.status(v,today+1).equals("exhausted"),"Entries not exhausted");v.putBoolean("revoked",true);h.assertTrue(d.status(v,today+1).equals("revoked"),"Revocation lost");
        UUID role=UUID.randomUUID();d.officers.put(role,"Officer");d.policies.put("TR:DE","required");var a=new CompoundTag();a.putString("id","A-test");a.putString("status","denied");d.applications.put("A-test",a);
        var loaded=VisaData.load(d.save(new CompoundTag()));h.assertTrue(loaded.officers.containsKey(role)&&loaded.visas.get(v.getString("id")).getInt("used")==1&&loaded.applications.get("A-test").getString("status").equals("denied")&&loaded.policy("TR","DE").equals("required"),"Save roundtrip lost records");h.succeed();
    }
    @GameTest(template="empty") public static void policyAndPassportInvalidation(GameTestHelper h){
        var d=new VisaData();var r=passport(h);h.assertTrue(d.check(r,"TR").equals("free"),"Own country not free by default");d.policies.put("TR:DE","required");h.assertTrue(d.check(r,"DE").equals("visa_invalid"),"Missing visa accepted");var v=d.issue(r,"DE","tourist",PassportService.today(),90,-1,30,"Officer","");v.putInt("used",123);h.assertTrue(d.check(r,"DE").equals("valid"),"Unlimited visa exhausted");d.policies.put("TR:DE","special");h.assertTrue(d.check(r,"DE").equals("visa_invalid"),"Tourist visa grants special permit");d.issue(r,"DE","special",PassportService.today(),30,1,10,"Officer","");h.assertTrue(d.check(r,"DE").equals("valid"),"Special permit rejected");d.policies.put("TR:DE","prohibited");h.assertTrue(d.check(r,"DE").equals("prohibited"),"Prohibited country allowed");r.revoke();h.assertTrue(d.check(r,"DE").equals("passport_invalid"),"Revoked passport usable");h.succeed();
    }
    private static UUID nonce(String cls,UUID player){try{var f=Class.forName(cls).getDeclaredField(cls.endsWith("VisaDesk")?"sessions":"SESSIONS");f.setAccessible(true);Object session=((Map<?,?>)f.get(null)).get(player);var n=session.getClass().getDeclaredField("nonce");n.setAccessible(true);return (UUID)n.get(session);}catch(Exception e){throw new IllegalStateException(e);}}
    @GameTest(template="empty",timeoutTicks=100) public static void entryConsumptionAndReplay(GameTestHelper h){
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VisaBorder"));var s=p.server;var stack=PassportService.issue(s,p.getUUID(),"VisaBorder",Countries.get("TR"),"Test");p.getInventory().setItem(0,stack);var r=PassportService.verify(s,stack);PassportSavedData.get(s).officer(p.getUUID(),"VisaBorder",true);var d=VisaData.get(s);d.policies.put("TR:DE","required");var v=d.issue(r,"DE","tourist",PassportService.today(),30,1,20,"Officer","");PassportService.openItem(p,stack);
        h.runAtTickTime(10,()->PassportService.action(p,nonce("historicalpassports.passport.PassportService",p.getUUID()),3,false,true,"DE","",0));
        h.runAtTickTime(20,()->{var n=nonce("historicalpassports.passport.PassportService",p.getUUID());PassportService.action(p,n,1,true,true,"Gate","",0);PassportService.action(p,n,1,true,true,"Gate","",0);h.assertTrue(v.getInt("used")==1&&r.history().size()==1,"Replay consumed twice or failed entry");});
        h.runAtTickTime(30,()->{PassportService.action(p,nonce("historicalpassports.passport.PassportService",p.getUUID()),1,true,true,"Gate","",0);h.assertTrue(v.getInt("used")==1&&r.history().size()==1,"Exhausted visa allowed");d.policies.remove("TR:DE");d.setDirty();h.succeed();});
    }
    @GameTest(template="empty") public static void applicationsAuthorizationAndReplay(GameTestHelper h){
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VisaApplicant"));var stack=PassportService.issue(p.server,p.getUUID(),"VisaApplicant",Countries.get("TR"),"Test");p.getInventory().setItem(0,stack);p.getInventory().selected=0;var r=PassportService.verify(p.server,stack);var d=VisaData.get(p.server);
        VisaDesk.open(p,false);var n=nonce("historicalpassports.visa.VisaDesk",p.getUUID());VisaDesk.action(p,n,1,"DE","student","Study",365,1,90,0,"",0);VisaDesk.action(p,n,1,"DE","student","Study",365,1,90,0,"",0);
        var all=d.applications.values().stream().filter(a->a.getString("passport").equals(r.id)).toList();h.assertTrue(all.size()==1,"Application replay duplicated");var a=all.get(0);
        VisaDesk.action(p,nonce("historicalpassports.visa.VisaDesk",p.getUUID()),2,"DE","student","",365,1,90,0,a.getString("id"),0);h.assertTrue(d.forPassport(r.id).isEmpty(),"Applicant self-issued visa");
        d.officers.put(p.getUUID(),"Officer");VisaDesk.open(p,true);n=nonce("historicalpassports.visa.VisaDesk",p.getUUID());VisaDesk.action(p,n,2,"DE","student","",365,1,90,0,a.getString("id"),0);VisaDesk.action(p,n,2,"DE","student","",365,1,90,0,a.getString("id"),0);h.assertTrue(d.forPassport(r.id).size()==1&&a.getString("status").equals("approved"),"Approval not exactly once");h.succeed();
    }
}
