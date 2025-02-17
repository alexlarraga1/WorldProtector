package com.alex_escobar.worldprotector.events;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.utils.RegionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.stream.Collectors;

import static com.alex_escobar.worldprotector.utils.MessageUtils.sendStatusMessage;


@Mod.EventBusSubscriber(modid = WorldProtector.MODID)
public class EventProtection {

	private EventProtection() {
	}

	@SubscribeEvent
	public static void onPlayerBreakBlock(BreakEvent event) {
		if (!event.getWorld().isClientSide()) {
			Player player = event.getPlayer();
			List<IRegion> regions = RegionUtils.getHandlingRegionsFor(event.getPos(), (Level) event.getWorld());
			for (IRegion region : regions) {
				if (region.containsFlag(RegionFlag.BREAK) && region.forbids(player)) {
					event.setCanceled(true);
					if (!region.isMuted()) {
						sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.event.protection.break_block"));
					}
					return;
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerPlaceBlock(EntityPlaceEvent event) {
		if (!event.getWorld().isClientSide()) {
			List<IRegion> regions = RegionUtils.getHandlingRegionsFor(event.getPos(), (Level) event.getWorld());
			if (event.getEntity() instanceof Player) {
				Player player = (Player) event.getEntity();
				for (IRegion region : regions) {
					if (region.containsFlag(RegionFlag.PLACE) && region.forbids(player)) {
						event.setCanceled(true);
						if (!region.isMuted()) {
							sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.event.protection.place_block"));
						}
					}
				}
			}
			// TODO: Test
			if (event.getEntity() instanceof EnderMan) {
				boolean endermanPlacingProhibited = regions.stream()
						.anyMatch(region -> region.containsFlag(RegionFlag.ENTITY_PLACE));
				if (endermanPlacingProhibited) {
					event.setCanceled(true);
					WorldProtector.LOGGER.debug("Block placed by enderman denied!");
				}
			}
		}
	}


	@SubscribeEvent
	public static void onExplosionStarted(ExplosionEvent.Start event) {
		if (!event.getWorld().isClientSide) {
			List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(event.getExplosion().getPosition()), event.getWorld());
			if (event.getExplosion().getExploder() instanceof Player) {
				Player player = (Player) event.getExplosion().getExploder();
				for (IRegion region : regions) {
					boolean cancelEvent = region.containsFlag(RegionFlag.IGNITE_EXPLOSIVES) && !region.permits(player);
					event.setCanceled(cancelEvent);
					if (cancelEvent) {
						boolean isPlayerExploder = event.getExplosion().getExploder() instanceof Player;
						if (!region.isMuted() && isPlayerExploder) {
							sendStatusMessage((ServerPlayer) player, "message.event.protection.ignite_tnt");
						}
					}
				}
			} else {
				// TODO: Explosion triggered by projectile or other TNT, or [.?.]
			}
		}
	}


	/**
	 * Removes affected entities and/or blocks from the event list to protect them
	 * @param event -
	 */
	@SubscribeEvent
	public static void onExplosion(ExplosionEvent.Detonate event) {
		if (!event.getWorld().isClientSide) {
			event.getAffectedBlocks().removeAll(filterExplosionAffectedBlocks(event, RegionFlag.EXPLOSION_BLOCK.toString()));
			event.getAffectedEntities().removeAll(filterAffectedEntities(event.getAffectedEntities(), RegionFlag.EXPLOSION_ENTITY.toString()));

			boolean explosionTriggeredByCreeper = (event.getExplosion().getExploder() instanceof Creeper);
			if (!explosionTriggeredByCreeper) {
				event.getAffectedBlocks().removeAll(filterExplosionAffectedBlocks(event, RegionFlag.EXPLOSION_OTHER_BLOCKS.toString()));
				event.getAffectedEntities().removeAll(filterAffectedEntities(event.getAffectedEntities(), RegionFlag.EXPLOSION_OTHER_ENTITY.toString()));
			}
			if (explosionTriggeredByCreeper) {
				event.getAffectedBlocks().removeAll(filterExplosionAffectedBlocks(event, RegionFlag.EXPLOSION_CREEPER_BLOCK.toString()));
				event.getAffectedEntities().removeAll(filterAffectedEntities(event.getAffectedEntities(), RegionFlag.EXPLOSION_OTHER_ENTITY.toString()));
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerUseToolSecondary(BlockEvent.BlockToolModificationEvent event) {
		if (!event.getWorld().isClientSide()) {
			Player player = event.getPlayer();
			List<IRegion> regions = RegionUtils.getHandlingRegionsFor(event.getPos(), player.level);
			for (IRegion region : regions){ // iterate through regions, if a region contains a specified flag, cancel event
				boolean playerNotPermitted = !region.permits(player);
				if (region.containsFlag(RegionFlag.TOOL_SECONDARY_USE) && playerNotPermitted) {
					event.setCanceled(true);
					if (!region.isMuted()) {
						sendStatusMessage((ServerPlayer) player, "message.event.protection.tool_secondary_use");
					}
					return;
				}
				if (event.getToolAction() == ToolAction.get("axe") && region.containsFlag(RegionFlag.AXE_STRIP) && playerNotPermitted) {
					event.setCanceled(true);
					if (!region.isMuted()) {
						sendStatusMessage((ServerPlayer) player, "message.event.protection.strip_wood");
					}
					return;
				}
				if (event.getToolAction() == ToolAction.get("hoe") && region.containsFlag(RegionFlag.HOE_TILL) && playerNotPermitted) {
					event.setCanceled(true);
					if (!region.isMuted()) {
						sendStatusMessage((ServerPlayer) player, "message.event.protection.till_farmland");
					}
					return;
				}
				if (event.getToolAction() == ToolAction.get("shovel") && region.containsFlag(RegionFlag.SHOVEL_PATH) && playerNotPermitted) {
					event.setCanceled(true);
					if (!region.isMuted()) {
						sendStatusMessage((ServerPlayer) player, "message.event.protection.shovel_path");
					}
					return;
				}
			}
		}
	}

	@SubscribeEvent

	// TODO: USe FillBucketEvent for buckets

	// Note: Does not prevent from fluids generate additional blocks (cobble generator). Use BlockEvent.FluidPlaceBlockEvent for this
	public static void onBucketFill(FillBucketEvent event) {
		// Note: FilledBucket seems to always be null. use maxStackSize to determine bucket state (empty or filled)
		Player player = event.getPlayer();
		if (!event.getWorld().isClientSide && event.getTarget() != null) {
			List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(event.getTarget().getLocation()), event.getWorld());
			for (IRegion region : regions) {
				// MaxStackSize: 1 -> full bucket so only placeable; >1 -> empty bucket, only fillable
				int bucketItemMaxStackCount = event.getEmptyBucket().getMaxStackSize();

				// placing fluid
				if (bucketItemMaxStackCount == 1) {
					if (region.containsFlag(RegionFlag.PLACE.toString()) && !region.permits(player)) {
						if (!region.isMuted()) {
							sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.event.protection.place_fluid"));
						}
						event.setCanceled(true);
						return;
					}
				}

				// scooping fluid (breaking fluid)
				if (bucketItemMaxStackCount > 1) {
					boolean isWaterlogged = false;
					boolean isFluid = false;
					HitResult pos = event.getTarget();
					if (pos != null && pos.getType() == HitResult.Type.BLOCK) {
						Vec3 absPos = pos.getLocation();
						BlockState blockState = event.getWorld().getBlockState(new BlockPos(absPos));
						// check for waterlogged block
						if (blockState.getBlock() instanceof SimpleWaterloggedBlock) {
							isWaterlogged = blockState.getValue(BlockStateProperties.WATERLOGGED);
						}
						// check if target has a fluid tag
						isFluid = blockState.getFluidState().is(FluidTags.LAVA) || blockState.getFluidState().is(FluidTags.WATER);

						if (isWaterlogged || isFluid) {
							if (region.containsFlag(RegionFlag.BREAK.toString()) && !region.permits(player)) {
								if (!region.isMuted()) {
									sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.event.protection.scoop_fluid"));
								}
								event.setCanceled(true);
								return;
							}
						}
					}
				}

			}
		}
	}

	/*
	@SubscribeEvent
	public static void onPistonPushBlock(PistonEvent.Pre event) {
		Direction dir = event.getDirection();
		BlockPos pistonPos = event.getPos();
		if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND && event.getStructureHelper().canMove()) {
			WorldProtector.LOGGER.debug(event.getDirection());
			WorldProtector.LOGGER.debug(pistonPos);
			WorldProtector.LOGGER.debug(event.getFaceOffsetPos());
			List<BlockPos> blockToMove = event.getStructureHelper().getBlocksToMove();
			blockToMove.forEach( pos -> WorldProtector.LOGGER.debug(pos.toString()));

			List<IRegion> regionsPushedIn = blockToMove.stream()
					.map(pos -> RegionUtils.getHandlingRegionsFor(pos, (Level) event.getWorld()))
					.flatMap(Collection::stream)
					.distinct()
					.collect(Collectors.toList());

			for (IRegion region : regionsPushedIn) {
				if (region.containsFlag(PISTON_PUSH)) {
					event.setCanceled(true);
					return;
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPistonPullBlock(PistonEvent.Pre event){
		Direction dir = event.getDirection();
		BlockPos pistonPos = event.getPos();
		// TODO: check for sticky piston /blocks
		if (event.getPistonMoveType() == PistonEvent.PistonMoveType.RETRACT){
			
		}


	}
    */

	/**
	 * Checks is any region contains the specified flag
	 * @param regions regions to check for
	 * @param flag flag to be checked for
	 * @return true if any region contains the specified flag, false otherwise
	 */
	private static boolean anyRegionContainsFlag(List<IRegion> regions, String flag){
		return regions.stream()
				.anyMatch(region -> region.containsFlag(flag));
	}

	/**
	 * Filters affected blocks from explosion event which are in a region with the specified flag.
	 * @param event detonation event
	 * @param flag flag to be filtered for
	 * @return list of block positions which are in a region with the specified flag
	 */
	private static List<BlockPos> filterExplosionAffectedBlocks(ExplosionEvent.Detonate event, String flag){
		return event.getAffectedBlocks().stream()
				.filter(blockPos -> anyRegionContainsFlag(
						RegionUtils.getHandlingRegionsFor(blockPos, event.getWorld()),
						flag))
				.collect(Collectors.toList());
	}

	private static List<Entity> filterAffectedEntities(List<Entity> entities, String flag) {
		return entities.stream()
				.filter(entity -> anyRegionContainsFlag(
						RegionUtils.getHandlingRegionsFor(new BlockPos(entity.position()), entity.level), flag))
				.collect(Collectors.toList());
	}


	@SubscribeEvent
	public static void onBlockToolInteraction(BlockEvent.BlockToolModificationEvent event) {
		// prevent NBT change?
	}
}
