package com.alex_escobar.worldprotector.item;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.utils.ExpandUtils;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemRegionMarker extends Item {

	public ItemRegionMarker() {
		super(new Item.Properties()
				.stacksTo(1)
				.tab(WorldProtector.WORLD_PROTECTOR_TAB));
	}

	// nbt keys
	public static final String VALID = "valid";
	public static final String Y_DEFAULT_LOW = "y_low_default";
	public static final String Y_DEFAULT_HIGH = "y_high_default";
	public static final String CYCLE_POINT_ID = "id";
	public static final String X1 = "x1";
	public static final String Y1 = "y1";
	public static final String Z1 = "z1";
	public static final String X2 = "x2";
	public static final String Y2 = "y2";
	public static final String Z2 = "z2";
	public static final String TP_X = "tp_x";
	public static final String TP_Y = "tp_y";
	public static final String TP_Z = "tp_z";
	public static final String TP_TARGET_SET = "tp_target_set";

	public static final int FIRST = 0;
	public static final int SECOND = 1;

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		if (Screen.hasShiftDown()) {
			tooltip.add(new TranslatableComponent("help.tooltip.region-marker.detail.1"));
			tooltip.add(new TranslatableComponent("help.tooltip.region-marker.detail.2"));
			tooltip.add(new TranslatableComponent("help.tooltip.region-marker.optional.1")
					.withStyle(ChatFormatting.GRAY));
			tooltip.add(new TranslatableComponent("help.tooltip.region-marker.optional.2")
					.withStyle(ChatFormatting.GRAY));
			tooltip.add(new TranslatableComponent("help.tooltip.region-marker.detail.3"));
			tooltip.add(new TranslatableComponent("help.tooltip.region-marker.detail.4")
					.withStyle(ChatFormatting.RED));
		} else {
			tooltip.add(new TranslatableComponent("help.tooltip.region-marker.simple.1"));
			tooltip.add(new TranslatableComponent("help.tooltip.region-marker.simple.2"));
			tooltip.add(new TranslatableComponent("help.tooltip.details.shift")
					.withStyle(ChatFormatting.DARK_BLUE)
					.withStyle(ChatFormatting.ITALIC));
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
		WorldProtector.LOGGER.debug(entityLiving);
		if (!worldIn.isClientSide){
			if(entityLiving instanceof ServerPlayer) {
				ServerPlayer player = (ServerPlayer) entityLiving;
				int yLow = (int) stack.getTag().getDouble(Y_DEFAULT_LOW);
				int yHigh = (int) stack.getTag().getDouble(Y_DEFAULT_HIGH);
				ExpandUtils.expandVert(player, stack, yLow, yHigh);
				player.getCooldowns().addCooldown(this, 20);
			}
		}
		return stack;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 32;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (!worldIn.isClientSide) {
			ItemStack markStick = playerIn.getItemInHand(handIn);
			if (!playerIn.hasPermissions(4) || !playerIn.isCreative()) {
				MessageUtils.sendStatusMessage((ServerPlayer) playerIn, new TranslatableComponent("item.usage.permission")
						.withStyle(ChatFormatting.RED));
				return InteractionResultHolder.fail(markStick);
			}
			if (handIn == InteractionHand.MAIN_HAND && isValidRegion(markStick)) {
				playerIn.swing(handIn);
				return super.use(worldIn, playerIn, handIn);
			}
			return InteractionResultHolder.fail(markStick);
		}
		return InteractionResultHolder.fail(playerIn.getItemInHand(handIn));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		Player player = context.getPlayer();
		InteractionHand hand = context.getHand();
		BlockPos pos = context.getClickedPos();
		if (!world.isClientSide) {
			ItemStack playerHeldItem = player.getItemInHand(hand);
			if (player.isCrouching()) {
				CompoundTag playerItemTag = playerHeldItem.getTag();
				playerItemTag.putInt(TP_X, pos.getX());
				playerItemTag.putInt(TP_Y, pos.getY() + 1);
				playerItemTag.putInt(TP_Z, pos.getZ());
				playerItemTag.putBoolean(TP_TARGET_SET, true);
				MessageUtils.sendStatusMessage((ServerPlayer) player, new TextComponent("Teleport target: [" +
						player.getItemInHand(hand).getTag().getInt(X1) + ", " +
						player.getItemInHand(hand).getTag().getInt(Y1) + ", " +
						player.getItemInHand(hand).getTag().getInt(Z1) + "]")
						.withStyle(ChatFormatting.WHITE));
			} else {
				if (playerHeldItem.hasTag()) {
					CompoundTag playerItemTag = playerHeldItem.getTag();
					switch (playerItemTag.getInt(CYCLE_POINT_ID)) {
						case FIRST:
							playerItemTag.putInt(X1, pos.getX());
							playerItemTag.putInt(Y1, pos.getY());
							playerItemTag.putInt(Z1, pos.getZ());
							playerItemTag.putInt(CYCLE_POINT_ID, SECOND);
							playerItemTag.putBoolean(VALID, false);
							MessageUtils.sendStatusMessage((ServerPlayer) player, new TextComponent("Position 1: [" +
									player.getItemInHand(hand).getTag().getInt(X1) + ", " +
									player.getItemInHand(hand).getTag().getInt(Y1) + ", " +
									player.getItemInHand(hand).getTag().getInt(Z1) + "]")
									.withStyle(ChatFormatting.WHITE));
							break;
						case SECOND:
							playerItemTag.putInt(X2, pos.getX());
							playerItemTag.putInt(Y2, pos.getY());
							playerItemTag.putInt(Z2, pos.getZ());
							playerItemTag.putInt(CYCLE_POINT_ID, 0);
							playerItemTag.putBoolean(VALID, true);
							MessageUtils.sendStatusMessage((ServerPlayer) player, new TextComponent("Position 2: [" +
									player.getItemInHand(hand).getTag().getInt(X2) + ", " +
									player.getItemInHand(hand).getTag().getInt(Y2) + ", " +
									player.getItemInHand(hand).getTag().getInt(Z2) + "]")
									.withStyle(ChatFormatting.WHITE));
							break;
						default:
							// Never reached
							break;
					}
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		return true;
	}



	@Override
	public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
		if (!worldIn.isClientSide && !stack.hasTag()) {
			WorldProtector.LOGGER.info("Region Marker nbt initialized");
			CompoundTag nbt = new CompoundTag();
			nbt.putInt(CYCLE_POINT_ID, 0);
			nbt.putBoolean(VALID, false);
			nbt.putDouble(Y_DEFAULT_LOW, 0);
			nbt.putDouble(Y_DEFAULT_HIGH, 255);
			nbt.putBoolean(TP_TARGET_SET, false);
			stack.setTag(nbt);
		}
	}

	private boolean isValidRegion(ItemStack markStick){
		return markStick.getTag().getBoolean(VALID);
	}

	public void setDefaultYValues(ItemStack regionMarker, int yLow, int yHigh) {
		CompoundTag itemTag = regionMarker.getTag();
		itemTag.putDouble(Y_DEFAULT_LOW, yLow);
		itemTag.putDouble(Y_DEFAULT_HIGH, yHigh);
	}
}
