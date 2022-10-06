package com.alex_escobar.worldprotector.utils;

import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.data.RegionManager;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

import static com.alex_escobar.worldprotector.utils.MessageUtils.sendMessage;

public final class RegionFlagUtils {

	private RegionFlagUtils() {
	}

	public static void addAllFlags(String regionName, Player player) {
		if (RegionManager.get().containsRegion(regionName)) {
			RegionManager.get().addFlags(regionName, RegionFlag.getFlags());
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.flags.add.all", regionName));
		} else {
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.unknown", regionName));
		}
	}

	public static void addFlags(String regionName, Player player, List<String> flags) {
		if (RegionManager.get().containsRegion(regionName)) {
			List<String> validFlags = flags.stream()
					.filter(RegionFlag::contains)
					.collect(Collectors.toList());
			List<String> addedFlags = RegionManager.get().addFlags(regionName, validFlags);
			String flagString = String.join(", ", addedFlags);
			if (!addedFlags.isEmpty()) {
				sendMessage((ServerPlayer) player, new TranslatableComponent("message.flags.add.multiple", flagString, regionName));
			} else {
				sendMessage((ServerPlayer) player, new TranslatableComponent( "message.flags.add.none", flagString));
			}
		} else {
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.unknown", regionName));
		}
	}

	public static void removeAllFlags(String regionName, Player player) {
		if (RegionManager.get().containsRegion(regionName)) {
			RegionManager.get().removeFlags(regionName, RegionFlag.getFlags());
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.flags.remove.all", regionName));
		} else {
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.unknown", regionName));
		}
	}

	public static void removeFlags(String regionName, Player player, List<String> flags) {
		if (RegionManager.get().containsRegion(regionName)) {
			List<String> validFlags = flags.stream()
					.filter(RegionFlag::contains)
					.collect(Collectors.toList());
			List<String> removedFlags = RegionManager.get().removeFlags(regionName, validFlags);
			String flagString = String.join(", ", removedFlags);
			if (!removedFlags.isEmpty()) {
				sendMessage((ServerPlayer) player, new TranslatableComponent("message.flags.remove.multiple", flagString, regionName));
			} else {
				sendMessage((ServerPlayer) player, new TranslatableComponent( "message.flags.remove.none", flagString));
			}
		} else {
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.unknown", regionName));
		}
	}

	public static void addFlag(String region, Player player, String flag) {
		if (RegionManager.get().addFlag(region, flag)) {
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.flags.add", flag, region));
		} else {
			sendMessage((ServerPlayer) player, new TranslatableComponent(  "message.flags.add.duplicate", flag, region));
		}
	}

	public static void removeFlag(IRegion region, Player player, String flag){
			if (RegionManager.get().removeFlag(region, flag)) {
				sendMessage((ServerPlayer) player, new TranslatableComponent("message.flags.remove", flag, region.getName()));
			} else {
				sendMessage((ServerPlayer) player, new TranslatableComponent("message.flags.remove.not-defined"));
			}
	}

	public static void removeFlag(String regionName, Player player, String flag) {
		if (RegionManager.get().containsRegion(regionName)) {
			RegionManager.get().getRegion(regionName).ifPresent(region -> removeFlag(region, player, flag));
		} else {
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.unknown", regionName));
		}
	}
}
