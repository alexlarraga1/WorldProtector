package com.alex_escobar.worldprotector.events;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.alex_escobar.worldprotector.utils.RegionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = WorldProtector.MODID)
public class EventInteract {

	private EventInteract(){}

	@SubscribeEvent
	public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		List<IRegion> regions = RegionUtils.getHandlingRegionsFor(event.getPos(), event.getWorld());
		if (!event.getWorld().isClientSide) {
			for (IRegion region : regions) {
				Player player = event.getPlayer();
				BlockEntity targetEntity = event.getWorld().getBlockEntity(event.getPos());
				boolean isLockableTileEntity = targetEntity instanceof BaseContainerBlockEntity;
				boolean isEnderChest = targetEntity instanceof EnderChestBlockEntity;
				boolean isContainer = targetEntity instanceof LecternBlockEntity || isLockableTileEntity;
				boolean isPlayerProhibited = region.forbids(player);

				// used to allow player to place blocks when shift clicking container or usable bock
				boolean playerHasNoBlocksToPlaceInHands = player.getItemInHand(InteractionHand.MAIN_HAND).getItem().equals(Items.AIR)
								&& player.getItemInHand(InteractionHand.OFF_HAND).getItem().equals(Items.AIR);


				BlockHitResult pos = event.getHitVec();
				if (pos != null && pos.getType() == HitResult.Type.BLOCK) {
					BlockPos bPos = pos.getBlockPos();
					Block target = event.getWorld().getBlockState(bPos).getBlock();
					boolean isUsableBlock = target instanceof ButtonBlock ||
							target instanceof DoorBlock ||
							target instanceof TrapDoorBlock ||
							target instanceof LeverBlock ||
							target instanceof NoteBlock ||
							target instanceof FenceGateBlock ||
							target instanceof DaylightDetectorBlock ||
							target instanceof DiodeBlock ||
							target instanceof LecternBlock ||
							target instanceof BeaconBlock ||
							target instanceof BrewingStandBlock;

					if (region.containsFlag(RegionFlag.USE) && isPlayerProhibited && isUsableBlock) {
						if (player.isCrouching() && playerHasNoBlocksToPlaceInHands || !player.isCrouching()) {
							event.setCanceled(true);
							if (!region.isMuted()) {
								MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.interact.use");
							}
							return;
						}
					}
				}
				// TODO: player can still activate pressure plates, trip wires and observers (by placing blocks: prohibit this with place flag)

				// check for ender chest access
				if (region.containsFlag(RegionFlag.ENDER_CHEST_ACCESS) && isEnderChest && isPlayerProhibited) {
					if (player.isCrouching() && playerHasNoBlocksToPlaceInHands || !player.isCrouching()) {
						event.setCanceled(true);
						if (!region.isMuted()) {
							MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.interact.access_ender_chest");
						}
						return;
					}
				}
				// check for container access
				if (region.containsFlag(RegionFlag.CONTAINER_ACCESS) && isContainer && isPlayerProhibited) {
					if (player.isCrouching() && playerHasNoBlocksToPlaceInHands || !player.isCrouching()) {
						event.setCanceled(true);
						if (!region.isMuted()) {
							MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.interact.access_container");
						}
						return;
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
		List<IRegion> regions = RegionUtils.getHandlingRegionsFor(event.getPos(), event.getWorld());
		if (!event.getWorld().isClientSide) {
			for (IRegion region : regions) {
				Player player = event.getPlayer();
				boolean containsChestAccess = region.containsFlag(RegionFlag.CONTAINER_ACCESS);
				boolean playerHasPermission = region.permits(player);
				boolean isMinecartContainer = event.getTarget() instanceof AbstractMinecartContainer;

				if (containsChestAccess && !playerHasPermission && isMinecartContainer) {
					event.setCanceled(true);
					if (!region.isMuted()) {
						MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.interact.access_container");
					}
					return;
				}
			}
		}
	}

	@SubscribeEvent
	public static void onSteppedOnActivator(BlockEvent.NeighborNotifyEvent event) {
		Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
		BlockPos pos = event.getPos();
		boolean cancelEvent = false;
		// tripwire does not work yet
		if (block instanceof BasePressurePlateBlock AbstractPressurePlateBlock
			/*	|| block instanceof TripWireHookBlock
				|| block instanceof TripWireBlock*/) {
			// TODO: check for tripwire blocks in a row and surpress updates
			List<IRegion> regions = RegionUtils.getHandlingRegionsFor(pos, (Level) event.getWorld());
			for (IRegion region : regions) {
				if (region.containsFlag(RegionFlag.USE)) {
					AABB areaAbovePressurePlate = new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
					List<Player> players = event.getWorld().getEntities(EntityType.PLAYER, areaAbovePressurePlate, (player) -> true);
					for (Player player : players) {
						cancelEvent = cancelEvent || region.forbids(player);
						if (cancelEvent && !region.isMuted()) {
							MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.interact.use");
						}
						event.setCanceled(cancelEvent);
					}
				}
			}
		}
	}
}
