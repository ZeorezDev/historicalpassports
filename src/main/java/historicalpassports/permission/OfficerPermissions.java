package historicalpassports.permission;

import historicalpassports.config.PassportConfig;
import historicalpassports.data.PassportSavedData;
import net.minecraft.server.level.ServerPlayer;

public final class OfficerPermissions {
    public static boolean canStamp(ServerPlayer player) {
        return PassportSavedData.get(player.server).isOfficer(player.getUUID()) || (!PassportConfig.OFFICER_ONLY.get() && player.hasPermissions(2));
    }
    public static boolean canReadRegistry(ServerPlayer player) { return player.hasPermissions(2) || canStamp(player); }
    private OfficerPermissions() {}
}
