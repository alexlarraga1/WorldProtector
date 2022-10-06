package com.alex_escobar.worldprotector.item;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.utils.RegionFlagUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
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
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.alex_escobar.worldprotector.utils.MessageUtils.sendStatusMessage;

public class ItemFlagStick extends Item {

	public ItemFlagStick() {
		super(new Properties()
				.stacksTo(1)
				.tab(WorldProtector.WORLD_PROTECTOR_TAB));
	}

	private static final List<String> flags;

	// nbt keys
	public static final String FLAG_IDX = "flag_idx";
	public static final String FLAG = "flag";
	public static final String MODE = "mode";

	public static final String MODE_ADD = "add";
	public static final String MODE_REMOVE = "remove";

	static {
		// init flag list
		flags = RegionFlag.getFlags();
		Collections.sort(flags);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		if(Screen.hasShiftDown()) {
			tooltip.add(new TranslatableComponent("help.tooltip.flag-stick.detail.1"));
			tooltip.add(new TranslatableComponent("help.tooltip.flag-stick.detail.2"));
			tooltip.add(new TranslatableComponent("help.tooltip.flag-stick.detail.3"));
			tooltip.add((TranslatableComponent) new TranslatableComponent("help.tooltip.flag-stick.detail.4")
					.withStyle(ChatFormatting.GRAY));
			tooltip.add((TranslatableComponent) new TranslatableComponent("help.tooltip.flag-stick.detail.5")
					.withStyle(ChatFormatting.RED));
		} else {
			tooltip.add(new TranslatableComponent("help.tooltip.flag-stick.simple.1"));
			tooltip.add(new TranslatableComponent("help.tooltip.flag-stick.simple.2"));
			tooltip.add((TranslatableComponent)new TranslatableComponent("help.tooltip.details.shift")
					.withStyle(ChatFormatting.DARK_BLUE)
					.withStyle(ChatFormatting.ITALIC));
		}
	}



	@Override
    public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
	    if (!worldIn.isClientSide && entityLiving instanceof ServerPlayer) {
	    	ServerPlayer player = (ServerPlayer) entityLiving;
	    	String selectedRegion = player.getOffhandItem().getTag().getString(ItemRegionStick.REGION);
	    	String selectedFlag = stack.getTag().getString(FLAG);
			int finishAction = stack.getTag().getInt("finish_action");
			switch (finishAction) {
				case 1:
					RegionFlagUtils.addAllFlags(selectedRegion, player);
					break;
				case 2:
					RegionFlagUtils.addFlag(selectedRegion, player, selectedFlag);
					break;
				case 3:
					RegionFlagUtils.removeAllFlags(selectedRegion, player);
					break;
				case 4:
					RegionFlagUtils.removeFlag(selectedRegion, player, selectedFlag);
					break;
				default:
					WorldProtector.LOGGER.error("Oh oh");
					break;
			}
		}
        return stack;
    }

	@Override
	public int getUseDuration(ItemStack stack) {
		return 32;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level p_41413_, LivingEntity livingEntity, int timeToLeft) {
		//super.releaseUsing(stack, p_41413_, livingEntity, timeToLeft);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack p_41452_) {
		return UseAnim.BOW;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (!worldIn.isClientSide) {
			ItemStack flagStick = playerIn.getItemInHand(handIn);
			// check player permission
			if (!playerIn.hasPermissions(4) || !playerIn.isCreative()) {
				sendStatusMessage((ServerPlayer) playerIn, new TranslatableComponent("item.usage.permission")
						.withStyle(ChatFormatting.RED));
				return InteractionResultHolder.fail(flagStick);
			}
			if (playerIn.getOffhandItem().getItem() instanceof ItemFlagStick) {
				return InteractionResultHolder.fail(flagStick);
			}
			ItemStack offHand = playerIn.getOffhandItem();
			ItemStack mainHand = playerIn.getMainHandItem();
			if (offHand.getItem() instanceof ItemRegionStick && mainHand.getItem() instanceof ItemFlagStick) {
				String selectedRegion = offHand.getTag().getString(ItemRegionStick.REGION);
				String selectedFlag = getSelectedFlag(playerIn.getItemInHand(handIn));
				String flagMode = getMode(flagStick);
				if (selectedFlag.isEmpty() || selectedRegion.isEmpty()) {
					return InteractionResultHolder.fail(flagStick);
				}
				boolean allFlagsSelected = selectedFlag.equals(RegionFlag.ALL.toString());
				switch (flagMode) {
					case MODE_ADD:
						if (allFlagsSelected) {
							flagStick.getTag().putInt("finish_action", 1);
						} else {
							flagStick.getTag().putInt("finish_action", 2);
						}
						break;
					case MODE_REMOVE:
						if (allFlagsSelected) {
							flagStick.getTag().putInt("finish_action", 3);
						} else {
							flagStick.getTag().putInt("finish_action", 4);
						}
						break;
					default:
						break;
				}
				playerIn.startUsingItem(handIn);
				return super.use(worldIn, playerIn, handIn);
			} else {
				if (playerIn.getUsedItemHand() == InteractionHand.MAIN_HAND) {
					if (playerIn.isCrouching()) {
						switchMode(flagStick);
					} else {
						cycleFlags(flagStick);
					}
					return InteractionResultHolder.success(flagStick);
				}
			}
			// check for region stick and add/remove flags
		} else {
			return InteractionResultHolder.fail(playerIn.getItemInHand(handIn));
		}
		return null;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!context.getLevel().isClientSide) {
			BlockEntity target = context.getLevel().getBlockEntity(context.getClickedPos());
			Player player = context.getPlayer();
			ItemStack mainHand = player.getMainHandItem();
			ItemStack offHand = player.getOffhandItem();
			if (offHand.getItem() instanceof ItemRegionStick && mainHand.getItem() instanceof ItemFlagStick) {
				ItemRegionStick regionStick = (ItemRegionStick) offHand.getItem();
				ItemFlagStick flagStick = (ItemFlagStick) mainHand.getItem();
				String flagMode = flagStick.getMode(mainHand);
				String selectedRegion = regionStick.getRegion(offHand);
				if (target instanceof RandomizableContainerBlockEntity) {
					RandomizableContainerBlockEntity container = (RandomizableContainerBlockEntity) target;
					if (container.isEmpty()) {
						sendStatusMessage((ServerPlayer) player, "message.flags.container.noflags");
						return InteractionResult.FAIL;
					}
					List<String> nameTags = new ArrayList<>();
					for (int i = 0; i < container.getContainerSize(); i++) {
						ItemStack stack = container.getItem(i);
						if (stack.getItem() instanceof NameTagItem) {
							nameTags.add(stack.getDisplayName().getString());
						}
					}
					if (nameTags.isEmpty()) {
						sendStatusMessage((ServerPlayer) player, "message.flags.container.noflags");
						return InteractionResult.FAIL;
					}
					List<String> validFlags = nameTags.stream().filter(RegionFlag::contains).collect(Collectors.toList());
					if (validFlags.isEmpty()) {
						sendStatusMessage((ServerPlayer) player, "message.flags.container.novalidflags");
						return InteractionResult.FAIL;
					}
					switch (flagMode) {
						case MODE_ADD:
							RegionFlagUtils.addFlags(selectedRegion, player, validFlags);
							break;
						case MODE_REMOVE:
							RegionFlagUtils.removeFlags(selectedRegion, player, validFlags);
							break;
						default:
							/* should never happen */
							return InteractionResult.FAIL;
					}
					return InteractionResult.SUCCESS;
				}
			} else {
				return InteractionResult.FAIL;
			}
		}
		return InteractionResult.FAIL;
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		return true; // false will damage entity
	}

	@Override
	public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
		if (!worldIn.isClientSide) {
			// ensure flag stick has a nbt tag and is initialized as needed
			if (!stack.hasTag()) {
				CompoundTag nbt = new CompoundTag();
				nbt.putString(MODE, MODE_ADD);
				nbt.putString(FLAG, RegionFlag.ALL.toString());
				nbt.putInt(FLAG_IDX, 0);
				nbt.putInt("finish_action", 0);
				stack.setTag(nbt);
				setDisplayName(stack, RegionFlag.ALL, MODE_ADD);
			} else {
				String flag = stack.getTag().getString(FLAG);
				String mode = stack.getTag().getString(MODE);
				setDisplayName(stack, flag, mode);
			}

		}

	}

	private void switchMode(ItemStack flagStick){
		String mode = getMode(flagStick);
		String flag = getSelectedFlag(flagStick);
		switch(mode){
			case MODE_ADD:
				setMode(flagStick, MODE_REMOVE);
				setDisplayName(flagStick, flag, MODE_REMOVE);
				break;
			case MODE_REMOVE:
				setMode(flagStick, MODE_ADD);
				setDisplayName(flagStick, flag, MODE_ADD);
				break;
			default:
				/* should not happen */
				break;
		}
	}

	private void cycleFlags(ItemStack flagStick){
		int flagIndex = flagStick.getTag().getInt(FLAG_IDX);
		// get flag and set display name
		String selectedFlag = flags.get(flagIndex);
		setDisplayName(flagStick, selectedFlag, getMode(flagStick));
		// write flag nbt
		flagStick.getTag().putString(FLAG, selectedFlag);
		// increase flag index and write nbt
		flagIndex = (flagIndex + 1) % (flags.size());
		flagStick.getTag().putInt(FLAG_IDX, flagIndex);
	}

	private String getMode(ItemStack flagStick) {
		return flagStick.getTag().getString(MODE);
	}

	private void setMode(ItemStack flagStick, String mode){
		flagStick.getTag().putString(MODE, mode);
	}

	private String getSelectedFlag(ItemStack flagStick){
		return flagStick.getTag().getString(FLAG);
	}

	private void setDisplayName(ItemStack flagStick, String flag, String mode){
		flagStick.setHoverName(new TextComponent("Flag Stick [" + flag + ", " + mode + "]").withStyle(ChatFormatting.GREEN));
	}

	private void setDisplayName(ItemStack flagStick, RegionFlag flag, String mode){
		setDisplayName(flagStick, flag.toString(), mode);
	}
}
