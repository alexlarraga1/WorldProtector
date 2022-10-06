package com.alex_escobar.worldprotector.events;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

import static com.alex_escobar.worldprotector.events.EventMobs.isAnimal;
import static com.alex_escobar.worldprotector.events.EventMobs.isMonster;
import static com.alex_escobar.worldprotector.utils.RegionUtils.getHandlingRegionsFor;

@Mod.EventBusSubscriber(modid = WorldProtector.MODID)
public class EventWorld {

    private EventWorld() {
    }

    @SubscribeEvent
    public static void onFarmLandTrampled(BlockEvent.FarmlandTrampleEvent event) {
        if (!event.getWorld().isClientSide()) {
            List<IRegion> regions = getHandlingRegionsFor(event.getPos(), (Level) event.getWorld());
            for (IRegion r : regions) {
                // cancel all trampling
                if (r.containsFlag(RegionFlag.TRAMPLE_FARMLAND.toString())) {
                    event.setCanceled(true);
                    return;
                }
                // cancel only player trampling
                if (event.getEntity() instanceof Player) {
                    Player player = (Player) event.getEntity();
                    if (r.containsFlag(RegionFlag.TRAMPLE_FARMLAND_PLAYER.toString())) {
                        event.setCanceled(true);
                        if (!r.isMuted()) {
                            MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.world.trample_farmland");
                        }
                    }
                } else {
                    // cancel trampling by other entities
                    if (r.containsFlag(RegionFlag.TRAMPLE_FARMLAND_OTHER.toString())) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLightningStrikeOccur(EntityStruckByLightningEvent event){
        Entity poorBastard = event.getEntity();
        if (!poorBastard.level.isClientSide) {
            boolean isLightningProhibited = getHandlingRegionsFor(new BlockPos(poorBastard.position()), poorBastard.level).stream()
                    .anyMatch(region -> region.containsFlag(RegionFlag.LIGHTNING_PROT));
            if (isLightningProhibited) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBonemealUse(BonemealEvent event){
        if (!event.getWorld().isClientSide) {
            Player player = (Player) event.getEntity();
            List<IRegion> regions = getHandlingRegionsFor(event.getPos(), player.level);
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.USE_BONEMEAL) && region.forbids(player)) {
                    event.setCanceled(true);
                    if (!region.isMuted()) {
                        MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.world.use_bone_meal");
                    }
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityXpDrop(LivingExperienceDropEvent event){
        if (!event.getEntityLiving().level.isClientSide) {
            Player player = event.getAttackingPlayer();
            Entity entity = event.getEntity();
            List<IRegion> regions = getHandlingRegionsFor(entity, entity.level);
            boolean entityDroppingXpIsPlayer = event.getEntityLiving() instanceof Player;
            for (IRegion region : regions) {
                // prevent all xp drops
                if (region.containsFlag(RegionFlag.XP_DROP_ALL)) {
                    if (entityDroppingXpIsPlayer) {
                        event.setCanceled(true);
                        return;
                    }
                    if (region.forbids(player)) {
                        event.setCanceled(true);
                        if (!region.isMuted()) {
                            MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.world.exp_drop.all");
                        }
                        return;
                    }
                }
                // prevent monster xp drop
                if (region.containsFlag(RegionFlag.XP_DROP_MONSTER) && isMonster(entity) && region.forbids(player)) {
                    event.setCanceled(true);
                    if (!region.isMuted()) {
                        MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.world.exp_drop.monsters");
                    }
                    return;
                }
                // prevent other entity xp drop (villagers, animals, ..)
                if (region.containsFlag(RegionFlag.XP_DROP_OTHER) && !isMonster(entity) && !entityDroppingXpIsPlayer) {
                    if (region.forbids(player)) {
                        event.setCanceled(true);
                        if (!region.isMuted()) {
                            MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.world.exp_drop.non_hostile");
                        }
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDropLoot(LivingDropsEvent event){
        LivingEntity lootEntity = event.getEntityLiving();
        if (!lootEntity.level.isClientSide) {
            boolean isLootDropProhibited = getHandlingRegionsFor(new BlockPos(lootEntity.position()), lootEntity.level).stream()
                    .anyMatch(region -> region.containsFlag(RegionFlag.LOOT_DROP));
            if (isLootDropProhibited) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDestroyBlock(LivingDestroyBlockEvent event){
        if (!event.getEntityLiving().level.isClientSide) {
            LivingEntity destroyer = event.getEntityLiving();
            List<IRegion> regions = getHandlingRegionsFor(destroyer, destroyer.level);
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.DRAGON_BLOCK_PROT) && destroyer instanceof EnderDragon) {
                    event.setCanceled(true);
                    return;
                }

                if (region.containsFlag(RegionFlag.WITHER_BLOCK_PROT) && destroyer instanceof WitherBoss) {
                    event.setCanceled(true);
                    return;
                }
                if (region.containsFlag(RegionFlag.ZOMBIE_DOOR_PROT) && destroyer instanceof Zombie) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onNetherPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        List<IRegion> regions = getHandlingRegionsFor(event.getPos(), (Level) event.getWorld());
        for (IRegion region : regions) {
            if (region.containsFlag(RegionFlag.SPAWN_PORTAL)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    // This event is only fired for PlayerEntities, See mixins for other entities
    @SubscribeEvent
    public static void onChangeDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        List<IRegion> regions = getHandlingRegionsFor(new BlockPos(entity.position()), entity.level);
        for (IRegion region : regions) {
            if (region.containsFlag(RegionFlag.USE_PORTAL.toString())) {
                event.setCanceled(true);
                return;
            }

            if (region.containsFlag(RegionFlag.USE_PORTAL_PLAYERS) && entity instanceof Player && !region.permits((Player) entity)) {
                event.setCanceled(true);
                if (!region.isMuted()) {
                    MessageUtils.sendStatusMessage((ServerPlayer) entity, "message.event.player.change_dim");
                }
                return;
            }
            if (region.containsFlag(RegionFlag.USE_PORTAL_ITEMS) && entity instanceof ItemEntity
                    || region.containsFlag(RegionFlag.USE_PORTAL_ANIMALS) && isAnimal(entity)
                    || region.containsFlag(RegionFlag.USE_PORTAL_MONSTERS) && isMonster(entity)
                    || region.containsFlag(RegionFlag.USE_PORTAL_VILLAGERS) && entity instanceof AbstractVillager
                    || region.containsFlag(RegionFlag.USE_PORTAL_MINECARTS) && entity instanceof AbstractMinecart) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
