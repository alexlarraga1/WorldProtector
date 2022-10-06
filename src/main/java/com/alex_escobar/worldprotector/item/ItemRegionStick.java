package com.alex_escobar.worldprotector.item;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.data.RegionManager;
import com.alex_escobar.worldprotector.utils.RegionPlayerUtils;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.alex_escobar.worldprotector.utils.MessageUtils.sendStatusMessage;

public class ItemRegionStick extends Item {

	public ItemRegionStick() {
		super(new Properties()
				.stacksTo(1)
				.tab(WorldProtector.WORLD_PROTECTOR_TAB));
	}

	// nbt keys
	public static final String REGION_IDX = "region_idx";
	private static final String LAST_DIM = "last_dim";
	public static final String MODE = "mode";
	public static final String REGION = "region";

	public static final String MODE_ADD = "add";
	public static final String MODE_REMOVE = "remove";

	private static List<String> cachedRegions;
	private static int regionCount = -1;


	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		if (Screen.hasShiftDown()) {
			tooltip.add(new TranslatableComponent("help.tooltip.region-stick.detail.1"));
			tooltip.add(new TranslatableComponent("help.tooltip.region-stick.detail.2"));
			tooltip.add(new TranslatableComponent("help.tooltip.region-stick.detail.3"));
			tooltip.add(new TranslatableComponent("help.tooltip.region-stick.detail.4")
					.withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(new TranslatableComponent("help.tooltip.region-stick.simple.1"));
			tooltip.add(new TranslatableComponent("help.tooltip.details.shift")
					.withStyle(ChatFormatting.DARK_BLUE)
					.withStyle(ChatFormatting.ITALIC));
		}
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		if (!player.level.isClientSide) {
			if ((entity instanceof Player)) {
				String mode = stack.getTag().getString(MODE);
				Player hitPlayer = (Player) entity;
				String regionName = stack.getTag().getString(REGION);
				switch (mode) {
					case MODE_ADD:
						RegionPlayerUtils.addPlayer(regionName, player, hitPlayer);
						break;
					case MODE_REMOVE:
						RegionPlayerUtils.removePlayer(regionName, player, hitPlayer);
						break;
					default:
						/* should not happen */
						break;
				}
			}
		}
		return true; // false will damage entity
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 25;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack p_41452_) {
		return UseAnim.BOW;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
		if (!worldIn.isClientSide) {
			// No functionality yet
		}
		return stack;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (!worldIn.isClientSide) {
			ItemStack regionStick = playerIn.getItemInHand(handIn);
			if (!playerIn.hasPermissions(4) || !playerIn.isCreative()) {
				sendStatusMessage((ServerPlayer) playerIn, new TranslatableComponent("item.usage.permission")
						.withStyle(ChatFormatting.RED));
				return InteractionResultHolder.fail(regionStick);
			}
			if (playerIn.getOffhandItem().getItem() instanceof ItemRegionStick) {
				return InteractionResultHolder.fail(regionStick);
			}
			if (playerIn.getUsedItemHand() == InteractionHand.MAIN_HAND) {
				if (playerIn.isCrouching()) {
					switchMode(regionStick);
					return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
				} else {
					if (cycleRegion(regionStick)) {
						return new InteractionResultHolder<>(InteractionResult.SUCCESS, regionStick);
					}
					sendStatusMessage((ServerPlayer) playerIn, new TranslatableComponent("message.region.info.no_regions")
							.withStyle(ChatFormatting.RED));
					new InteractionResultHolder<>(InteractionResult.FAIL, playerIn.getItemInHand(handIn));
				}
			}
		}
		return new InteractionResultHolder<>(InteractionResult.FAIL, playerIn.getItemInHand(handIn));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return InteractionResult.FAIL;
	}


	@Override
	public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
		if (!worldIn.isClientSide) {
			if (stack.hasTag()) {
				// update region name list
				String dim = worldIn.dimension().location().toString();
				String nbtDim = stack.getTag().getString(LAST_DIM);
				stack.getTag().putString(LAST_DIM, dim);
				List<String> regionNames = new ArrayList<>(RegionManager.get().getRegionNames(worldIn.dimension()));
				if (regionCount != regionNames.size() || !dim.equals(nbtDim)) {
					cachedRegions = regionNames;
					regionCount = cachedRegions.size();
					if (stack.getTag().contains(REGION_IDX)) {
						int regionIndex = stack.getTag().getInt(REGION_IDX);
						regionIndex = Math.max(0, Math.min(regionIndex, regionCount - 1));
						stack.getTag().putInt(REGION_IDX, regionIndex);
					} else {
						stack.getTag().putInt(REGION_IDX, 0);
					}

				}
				setDisplayName(stack, stack.getTag().getString(REGION), stack.getTag().getString(MODE));
			} else {
				// init nbt tag of RegionStick
				CompoundTag nbt = new CompoundTag();
				nbt.putString(MODE, MODE_ADD);
				nbt.putInt(REGION_IDX, 0);
				nbt.putString(LAST_DIM, worldIn.dimension().location().toString());
				if (regionCount > 0) {
					nbt.putString(REGION, cachedRegions.get(0));
				} else {
					nbt.putString(REGION, "N/A");
				}
				stack.setTag(nbt);
			}
		}
	}

	private void setDisplayName(ItemStack regionStick, String region, String mode){
		regionStick.setHoverName(new TextComponent( "Region Stick [" + region + ", " + mode + "]").withStyle(ChatFormatting.AQUA));
	}

	public String getMode(ItemStack regionStick){
		return regionStick.getTag().getString(MODE);
	}

	private void setMode(ItemStack regionStick, String mode){
		regionStick.getTag().putString(MODE, mode);
	}

	public String getRegion(ItemStack regionStick) {
		return regionStick.getTag().getString(REGION);
	}

	private void setRegion(ItemStack regionStick, String region){
		regionStick.getTag().putString(REGION, region);
	}

	private boolean cycleRegion(ItemStack regionStick){
		if (regionCount > 0) {
			int regionIndex = regionStick.getTag().getInt(REGION_IDX);
			// get region and set display name
			String selectedRegion = cachedRegions.get(regionIndex);
			setDisplayName(regionStick, selectedRegion, getMode(regionStick));
			// write region nbt
			setRegion(regionStick, selectedRegion);
			// increase region index and write nbt
			regionIndex = (regionIndex + 1) % (regionCount);
			regionStick.getTag().putInt(REGION_IDX, regionIndex);
			return true;
		} else {
			return false;
		}
	}

	private void switchMode(ItemStack regionStick) {
		String mode = getMode(regionStick);
		String region = getRegion(regionStick);
		switch(mode){
			case MODE_ADD:
				setMode(regionStick, MODE_REMOVE);
				setDisplayName(regionStick, region, MODE_REMOVE);
				break;
			case MODE_REMOVE:
				setMode(regionStick, MODE_ADD);
				setDisplayName(regionStick, region, MODE_ADD);
				break;
			default:
				/* should not happen */
				break;
		}
	}
}
