package com.alex_escobar.worldprotector.events;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.item.ItemRegionMarker;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.alex_escobar.worldprotector.utils.RegionUtils;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

import static com.alex_escobar.worldprotector.utils.MessageUtils.sendStatusMessage;

@Mod.EventBusSubscriber(modid = WorldProtector.MODID)
public class EventPlayers {

    private EventPlayers() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!event.getPlayer().level.isClientSide) {
            if (event.getTarget() instanceof Player) {
                Player target = (Player) event.getTarget();
                List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(target.position()), target.level);
                for (IRegion region : regions) {
                    if (region.containsFlag(RegionFlag.ATTACK_PLAYERS.toString()) && region.forbids(event.getPlayer())) {
                        if (!region.isMuted()) {
                            sendStatusMessage((ServerPlayer) event.getPlayer(), "message.event.player.pvp");
                        }
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    // unrelated: mobs pickup logic => MobEntity#livingTick
    @SubscribeEvent
    public static void onPickupItem(EntityItemPickupEvent event) {
        if (!event.getPlayer().level.isClientSide) {
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(event.getPlayer().position()), event.getPlayer().level);
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.ITEM_PICKUP.toString()) && region.forbids(event.getPlayer())) {
                    if (!region.isMuted()) {
                        sendStatusMessage((ServerPlayer) event.getPlayer(), "message.event.player.pickup_item");
                    }
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLevelChange(PlayerXpEvent.LevelChange event) {
        if (!event.getPlayer().level.isClientSide) {
            Player player = event.getPlayer();
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(player.position()), player.level);
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.LEVEL_FREEZE) && region.forbids(player)) {
                    if (!region.isMuted()) {
                        sendStatusMessage((ServerPlayer) player, "message.event.player.level_freeze");
                    }
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

	@SubscribeEvent
    public static void onPlayerXPChange(PlayerXpEvent.XpChange event) {
        if (!event.getPlayer().level.isClientSide) {
            Player player = event.getPlayer();
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(player.position()), player.level);
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.XP_FREEZE.toString()) && region.forbids(player)) {
                    if (!region.isMuted()) {
                        MessageUtils.sendStatusMessage((ServerPlayer) player, "message.protection.player.xp_freeze");
                    }
                    event.setCanceled(true);
                    event.setAmount(0);
                    return;
                }

            }
        }
    }



	@SubscribeEvent
	public static void onPlayerXpPickup(PlayerXpEvent.PickupXp event){
		if (!event.getPlayer().level.isClientSide) {
            Player player = event.getPlayer();
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(player.position()), player.level);
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.XP_PICKUP.toString()) && region.forbids(player)) {
                    if (!region.isMuted()) {
                        MessageUtils.sendStatusMessage((ServerPlayer) player, "message.protection.player.xp_pickup");
                    }
                    event.setCanceled(true);
                    event.getOrb().remove(Entity.RemovalReason.DISCARDED);
                    return;
                }

            }
        }
	}

    // TODO: handle flags for Villagers, Animals, Monsters, Player separate
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity dmgSourceEntity = event.getSource().getEntity();
        if (event.getEntityLiving() instanceof Player) {
            Player dmgTargetEntity = (Player) event.getEntityLiving();
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(dmgTargetEntity.position()), dmgTargetEntity.level);
            for (IRegion region : regions) {
                // another check for PVP - this does not prevent knochback? but prevents dmg
                if (dmgSourceEntity instanceof Player && region.containsFlag(RegionFlag.ATTACK_PLAYERS)){
                    event.setCanceled(true);
                    return;
                }
                if (region.containsFlag(RegionFlag.INVINCIBLE.toString())) {
                    event.setCanceled(true);
                    return;
                }

            }
        }
    }

    @SubscribeEvent
    public static void onReceiveDmg(LivingDamageEvent event) {
        Entity dmgSourceEntity = event.getSource().getEntity();
        if (event.getEntityLiving() instanceof Player) {
            Player player = (Player) event.getEntityLiving();
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(player.position()), player.level);
            for (IRegion region : regions) {
                // another check for PVP - this does not prevent knochback? but prevents dmg
                if (dmgSourceEntity instanceof Player && region.containsFlag(RegionFlag.ATTACK_PLAYERS)){
                    event.setCanceled(true);
                    return;
                }
                if (region.containsFlag(RegionFlag.INVINCIBLE.toString())) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingKnockBackEvent event) {
        if (event.getEntityLiving() instanceof Player) {
            Player dmgTargetEntity = (Player) event.getEntityLiving();
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(dmgTargetEntity.position()), dmgTargetEntity.level);
            for (IRegion region : regions) {
                // another check for PVP - Prevents knockback
                if (region.containsFlag(RegionFlag.ATTACK_PLAYERS.toString())) {
                    event.setCanceled(true);
                    return;
                }
            }
            // TODO: Flag for knockback?
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntityLiving();
        List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(entity.position()), entity.level);
        for (IRegion region : regions) {
            // prevent fall damage for all entities
            if (region.containsFlag(RegionFlag.FALL_DAMAGE.toString())) {
                event.setCanceled(true); // same result as event.setDamageMultiplier(0.0f);
                return;
            }
            if (entity instanceof Player && region.containsFlag(RegionFlag.FALL_DAMAGE_PLAYERS)) {
                event.setDamageMultiplier(0.0f);
                return;
            }
            if (entity instanceof AbstractVillager && region.containsFlag(RegionFlag.FALL_DAMAGE_VILLAGERS)) {
                event.setDamageMultiplier(0.0f);
                return;
            }
            if (EventMobs.isAnimal(entity) && region.containsFlag(RegionFlag.FALL_DAMAGE_ANIMALS)) {
                event.setDamageMultiplier(0.0f);
                return;
            }
            if (EventMobs.isMonster(entity)) {
                event.setDamageMultiplier(0.0f);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (!event.getPlayer().level.isClientSide) {
            ItemStack rightIn = event.getIngredientInput();
            ItemStack leftIn = event.getItemInput();
            Player player = event.getPlayer();
            if (leftIn.getItem() instanceof ItemRegionMarker && rightIn.getItem() instanceof AirItem) {
                String regionName = event.getItemResult().getDisplayName().getString();
                if (!player.hasPermissions(4) || !player.isCreative()) {
                    sendStatusMessage((ServerPlayer) player, "message.event.players.anvil_region_defined");
                } else {
                    RegionUtils.createRegion(regionName, player, event.getItemResult());
                }
            }
        }
    }

    @SubscribeEvent
    // message send to server but not distributed to all clients
    public static void onSendChat(ServerChatEvent event) {
        if (event.getPlayer() != null) {
            ServerPlayer player = event.getPlayer();
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(player.position()), player.level);
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.SEND_MESSAGE.toString()) && region.forbids(player)) {
                    event.setCanceled(true);
                    if (!region.isMuted()) {
                        sendStatusMessage(player, new TranslatableComponent("message.event.player.speak"));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChatSend(ClientChatEvent event) {
        // can only prevent sending commands/chat for all/global
        // Possible place for a profanity filter
    }

    @SubscribeEvent
    public static void onCommandSend(CommandEvent event) {
        try {
            event.getParseResults().getContext().getSource().getEntityOrException();
            ServerPlayer player = event.getParseResults().getContext().getSource().getPlayerOrException();
            BlockPos playerPos = new BlockPos(player.position());
            List<IRegion> regions = RegionUtils.getHandlingRegionsFor(playerPos, player.level);
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.EXECUTE_COMMAND.toString()) && region.forbids(player)) {
                    event.setCanceled(true);
                    if (!region.isMuted()) {
                        MessageUtils.sendStatusMessage(player, "message.event.player.execute-commands");
                    }
                    return;
                }
            }
            // TODO: add command list to block only specific commands, regardless of mod and permission of command
            // event.getParseResults().getContext().getNodes().forEach(node -> WorldProtector.LOGGER.debug(node.getNode().getName()));
        } catch (CommandSyntaxException e) {
            // Most likely thrown because command was not send by a player.
            // This is fine because we don't want this flag to be triggered from non-players
        }
    }

    @SubscribeEvent
    public static void onPlayerSleep(SleepingTimeCheckEvent event) {
        Player player = event.getPlayer();
        List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(player.position()), player.level);
        for (IRegion region : regions) {
            if (region.containsFlag(RegionFlag.SLEEP.toString()) && region.forbids(player)) {
                if (!region.isMuted()) {
                    MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.player.sleep");
                }
                event.setResult(Event.Result.DENY);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        BlockPos newSpawn = event.getNewSpawn();
        Player player = event.getPlayer();
        List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(player.position()), player.level);
        if (newSpawn != null) {
            // attempt to set spawn
            for (IRegion region : regions) {
                if (region.containsFlag(RegionFlag.SET_SPAWN.toString()) && region.forbids(player)) {
                    event.setCanceled(true);
                    if (!region.isMuted()) {
                        MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.player.set_spawn");
                    }
                    return;
                }
            }
        } /*
        else {
            // attempt to reset spawn
            for (IRegion region : regions) {
                // TODO: not working?
                if (region.containsFlag(RegionFlag.RESET_SPAWN.toString()) && region.forbids(player)) {
                    event.setCanceled(true);
                    MessageUtils.sendStatusMessage(player, "message.event.player.reset_spawn");
                    return;
                }
            }

        }
        */
    }

    @SubscribeEvent
    public static void onPlayerDropItem(ItemTossEvent event) {
        Player player = event.getPlayer();
        List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(player.position()), player.level);
        for (IRegion region : regions) {
            if (region.containsFlag(RegionFlag.ITEM_DROP.toString()) && region.forbids(player)) {
                event.setCanceled(true);
                player.getInventory().add(event.getEntityItem().getItem());
                if (!region.isMuted()) {
                    MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.player.drop_item");
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onEntityMountAttempt(EntityMountEvent event) {
        if (!event.getWorldObj().isClientSide) {
            Entity entityBeingMounted = event.getEntityBeingMounted();
            // could be mob that dismounts because entity being mounted dies?
            boolean playerAttemptsMounting = event.getEntityMounting() instanceof Player;
            if (playerAttemptsMounting) {
                Player player = (Player) event.getEntityMounting();
                List<IRegion> regions = RegionUtils.getHandlingRegionsFor(entityBeingMounted, event.getWorldObj());
                for (IRegion region : regions) {
					/*
					TODO: Wait for 1.17: https://bugs.mojang.com/browse/MC-202202
					if (event.isDismounting() && region.containsFlag(RegionFlag.ANIMAL_UNMOUNTING) && region.forbids(player)) {
						event.setCanceled(true); // Does not correctly unmount player
						if (!region.isMuted()) {
						    sendStatusMessage(player, "message.event.player.unmount");
						}
					}
					*/
                    if (event.isMounting() && region.containsFlag(RegionFlag.ANIMAL_MOUNTING) && region.forbids(player)) {
                        event.setCanceled(true);
                        if (!region.isMuted()) {
                            sendStatusMessage((ServerPlayer) player, "message.event.player.mount");
                        }
                    }
                }
            }
        }
    }
}
