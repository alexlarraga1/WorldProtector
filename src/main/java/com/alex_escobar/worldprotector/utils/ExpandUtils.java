package com.alex_escobar.worldprotector.utils;

import com.alex_escobar.worldprotector.item.ItemRegionMarker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static com.alex_escobar.worldprotector.item.ItemRegionMarker.*;
import static com.alex_escobar.worldprotector.utils.MessageUtils.sendMessage;

public final class ExpandUtils {

	private ExpandUtils() {
	}

	public static void expandVert(Player player, ItemStack item, int y1, int y2) {
		if (isValidRegionMarker(item)) {
			CompoundTag itemTag = item.getTag();
			if (itemTag.getBoolean(VALID)) {
				itemTag.putDouble(Y1, y1);
				itemTag.putDouble(Y2, y2);
				sendMessage((ServerPlayer) player, new TranslatableComponent("message.item-hand.expand", y1, y2));
			} else {
				sendMessage((ServerPlayer) player, "message.item-hand.choose");
			}
		} else {
			sendMessage((ServerPlayer) player, "message.item-hand.take");
		}
	}

	private static boolean isValidRegionMarker(ItemStack itemStack) {
		return itemStack.getItem() instanceof ItemRegionMarker && itemStack.hasTag();
	}

	public static void setDefaultYLevels(Player player, int yLow, int yHigh) {
		ItemStack itemInMainHand = player.getMainHandItem();
		if (isValidRegionMarker(itemInMainHand)) {
			ItemRegionMarker regionMarker = (ItemRegionMarker) itemInMainHand.getItem();
			regionMarker.setDefaultYValues(itemInMainHand, yLow, yHigh);
			sendMessage((ServerPlayer) player, new TranslatableComponent("message.region-marker.set-y", yLow, yHigh));
		} else {
			sendMessage((ServerPlayer) player, "message.item-hand.take");
		}
	}
}
