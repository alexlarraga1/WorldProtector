package com.alex_escobar.worldprotector.events;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.alex_escobar.worldprotector.utils.RegionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = WorldProtector.MODID)
public class EventTeleport {

    private EventTeleport() {
    }

    @SubscribeEvent
    public static void onEnderTeleportTo(EntityTeleportEvent event) {
        Level world = event.getEntity().level;
        if (!world.isClientSide) {
            BlockPos teleportTarget = new BlockPos(event.getTargetX(), event.getTargetY(), event.getTargetZ());
            BlockPos teleportSource = new BlockPos(event.getEntity().position());
            List<IRegion> regionsTo = RegionUtils.getHandlingRegionsFor(teleportTarget, world);
            List<IRegion> regionsFrom = RegionUtils.getHandlingRegionsFor(teleportSource, world);
            // handle player teleportation using ender pearls
            if (event instanceof EntityTeleportEvent.EnderPearl) {
                EntityTeleportEvent.EnderPearl enderPearlEvent = (EntityTeleportEvent.EnderPearl) event;
                handlePlayerEnderPearlUse(enderPearlEvent, regionsFrom, regionsTo);
            }
            if (event instanceof EntityTeleportEvent.EnderEntity) {
                EntityTeleportEvent.EnderEntity enderEntityEvent = (EntityTeleportEvent.EnderEntity) event;
                // handle enderman teleportation
                if (enderEntityEvent.getEntityLiving() instanceof EnderMan) {
                    handleEndermanTp(enderEntityEvent, regionsFrom, regionsTo);
                }
                // handle shulker teleportation
                if (enderEntityEvent.getEntityLiving() instanceof Shulker) {
                    handleShulkerTp(enderEntityEvent, regionsFrom, regionsTo);
                }
            }
        }
    }

    private static void handlePlayerEnderPearlUse(EntityTeleportEvent.EnderPearl event, List<IRegion> regionsFrom, List<IRegion> regionsTo) {
        ServerPlayer player = event.getPlayer();
        for (IRegion region : regionsFrom) {
            if (region.containsFlag(RegionFlag.USE_ENDERPEARL_FROM_REGION) && region.forbids(player)) {
                event.setCanceled(true);
                if (!region.isMuted()) {
                    MessageUtils.sendStatusMessage(player, "message.event.teleport.ender_pearl.from_region");
                }
                // refund pearl
                int count = player.getItemInHand(player.getUsedItemHand()).getCount();
                player.getItemInHand(player.getUsedItemHand()).setCount(count + 1);
                return;
            }
        }
        for (IRegion region : regionsFrom) {
            if (region.containsFlag(RegionFlag.USE_ENDERPEARL_TO_REGION) && region.forbids(player)) {
                event.setCanceled(true);
                if (!region.isMuted()) {
                    MessageUtils.sendStatusMessage(player, "message.event.teleport.ender_pearl.to_region");
                }
                // refund pearl
                int count = player.getItemInHand(player.getUsedItemHand()).getCount();
                player.getItemInHand(player.getUsedItemHand()).setCount(count + 1);
                return;
            }
        }

    }

    private static void handleShulkerTp(EntityTeleportEvent.EnderEntity event, List<IRegion> regionsFrom, List<IRegion> regionsTo) {
        boolean shulkerTpFromProhibited = regionsFrom.stream()
                .anyMatch(region -> region.containsFlag(RegionFlag.SHULKER_TELEPORT_FROM_REGION));
        if (shulkerTpFromProhibited) {
            event.setCanceled(true);
        }
        boolean shulkerTpToProhibited = regionsTo.stream()
                .anyMatch(region -> region.containsFlag(RegionFlag.SHULKER_TELEPORT_TO_REGION));
        if (shulkerTpToProhibited) {
            event.setCanceled(true);
        }
    }

    private static void handleEndermanTp(EntityTeleportEvent.EnderEntity event, List<IRegion> regionsFrom, List<IRegion> regionsTo) {
        boolean endermanTpFromProhibited = regionsFrom.stream()
                .anyMatch(region -> region.containsFlag(RegionFlag.ENDERMAN_TELEPORT_FROM_REGION));
        if (endermanTpFromProhibited) {
            event.setCanceled(true);
        }
        boolean endermanTpToProhibited = regionsTo.stream()
                .anyMatch(region -> region.containsFlag(RegionFlag.ENDERMAN_TELEPORT_TO_REGION));
        if (endermanTpToProhibited) {
            event.setCanceled(true);
        }
    }
}
