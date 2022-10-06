package com.alex_escobar.worldprotector.utils;

import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.Region;
import com.alex_escobar.worldprotector.data.RegionManager;
import com.alex_escobar.worldprotector.item.ItemRegionMarker;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.alex_escobar.worldprotector.utils.MessageUtils.sendMessage;
import static com.alex_escobar.worldprotector.utils.MessageUtils.sendStatusMessage;

;

public final class RegionUtils {

	private RegionUtils() {
	}

	public static void createRegion(String regionName, Player player, ItemStack item) {
		if (regionName.contains(" ")) { // region contains whitespace
			sendStatusMessage((ServerPlayer) player, "message.region.define.error");
			return;
		}
		if (item.getItem() instanceof ItemRegionMarker) {
			if (item.getTag() != null) {
				if (item.getTag().getBoolean(ItemRegionMarker.VALID)) {
					AABB regionArea = getAreaFromNBT(item.getTag());
					BlockPos tpPos = getTpTargetFromNBT(item.getTag());
					Region region = new Region(regionName, regionArea, tpPos, player.level.dimension());
					RegionManager.get().addRegion(region);
					item.getTag().putBoolean(ItemRegionMarker.VALID, false); // reset flag for consistent command behaviour
					sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.define", regionName));
				} else {
					sendStatusMessage((ServerPlayer) player, "message.item-hand.choose");
				}
			}
		} else {
			sendMessage((ServerPlayer) player, "message.item-hand.take");
		}
	}

	public static void redefineRegion(String regionName, Player player, ItemStack item) {
		if (item.getItem() instanceof ItemRegionMarker) {
			if (item.getTag() != null) {
				if (item.getTag().getBoolean(ItemRegionMarker.VALID)) {
					if (RegionManager.get().containsRegion(regionName)) {
						RegionManager.get().getRegion(regionName).ifPresent(region -> {
							region.setArea(getAreaFromNBT(item.getTag()));
							region.setTpTarget(getTpTargetFromNBT(item.getTag()));
							RegionManager.get().updateRegion(new Region(region), player);
							item.getTag().putBoolean(ItemRegionMarker.VALID, false); // reset flag for consistent command behaviour
							sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.redefine", regionName));
						});

					}
					else {
						sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.region.unknown", regionName));
					}
				} else {
					sendStatusMessage((ServerPlayer) player, "message.item-hand.choose");
				}
			}
		} else {
			sendStatusMessage((ServerPlayer) player, "message.item-hand.take");
		}
	}

	public static Collection<String> getRegionsAroundPos(BlockPos pos, Level world, ResourceKey<Level> dim) {
		return RegionManager.get().getRegions(dim).stream()
				.filter(region -> region.containsPosition(pos))
				.map(IRegion::getName)
				.collect(Collectors.toList());
	}

	public static void teleportToRegion(String regionName, Player player, CommandSourceStack source) {
		if (RegionManager.get().containsRegion(regionName)) {
			RegionManager.get().getRegion(regionName).ifPresent(region -> {
				try {
					BlockPos target = region.getTpTarget();
					String command = "execute in " + region.getDimension().location().toString() + " run tp " + player.getName().getString() + " " + target.getX() + " " + target.getY() + " " + target.getZ();
					sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.region.teleport", region.getName()));
					source.getServer().getCommands().getDispatcher().execute(command, source);
				} catch (CommandSyntaxException e) {
					e.printStackTrace();
				}
			});
		} else {
			sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.region.unknown", regionName));
		}
	}

	public static Collection<String> getDimensionList(Level world) {
		return RegionManager.get().getDimensionList();
	}

	public static List<IRegion> getHandlingRegionsFor(Entity entity, LevelAccessor world) {
		return getHandlingRegionsFor(new BlockPos(entity.position()), ((Level) world).dimension());
	}

	public static List<IRegion> getHandlingRegionsFor(BlockPos position, Level world) {
		return getHandlingRegionsFor(position, world.dimension());
	}

	public static List<IRegion> getHandlingRegionsFor(BlockPos position, ResourceKey<Level> dimension) {
		int maxPriority = 1;
		List<IRegion> handlingRegions = new ArrayList<>();
		List<IRegion> filteredRegions = RegionManager.get().getRegions(dimension).stream()
				.filter(IRegion::isActive)
				.filter(region -> region.containsPosition(position))
				.collect(Collectors.toList());
		for (IRegion region : filteredRegions) {
			if (region.getPriority() == maxPriority) {
				handlingRegions.add(region);
			} else if (region.getPriority() > maxPriority) {
				handlingRegions.clear();
				maxPriority = region.getPriority();
				handlingRegions.add(region);
			}
		}
		return handlingRegions;
	}

	private static AABB getAreaFromNBT(CompoundTag nbtTag) {
		return new AABB(
				nbtTag.getInt(ItemRegionMarker.X1), nbtTag.getInt(ItemRegionMarker.Y1), nbtTag.getInt(ItemRegionMarker.Z1),
				nbtTag.getInt(ItemRegionMarker.X2), nbtTag.getInt(ItemRegionMarker.Y2), nbtTag.getInt(ItemRegionMarker.Z2));
	}

	private static BlockPos getTpTargetFromNBT(CompoundTag nbtTag) {
		if (nbtTag.getBoolean(ItemRegionMarker.TP_TARGET_SET)) {
			return new BlockPos(nbtTag.getInt(ItemRegionMarker.TP_X), nbtTag.getInt(ItemRegionMarker.TP_Y), nbtTag.getInt(ItemRegionMarker.TP_Z));
		} else {
			AABB area = getAreaFromNBT(nbtTag);
			int centerX = (int) area.getCenter().x;
			int centerY = (int) area.getCenter().y;
			int centerZ = (int) area.getCenter().z;
			return new BlockPos(centerX, centerY, centerZ);
		}
	}
}
