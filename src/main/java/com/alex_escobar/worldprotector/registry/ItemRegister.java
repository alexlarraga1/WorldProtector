package com.alex_escobar.worldprotector.registry;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.item.ItemEmblem;
import com.alex_escobar.worldprotector.item.ItemFlagStick;
import com.alex_escobar.worldprotector.item.ItemRegionMarker;
import com.alex_escobar.worldprotector.item.ItemRegionStick;

public class ItemRegister {

    private ItemRegister() {}
	
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister(WorldProtector.MODID);

    public static final ItemRegistryObject<ItemRegionStick> REGION_STICK = ITEMS.register("region_stick", ItemRegionStick::new);
    public static final ItemRegistryObject<ItemRegionMarker> REGION_MARKER = ITEMS.register("region_marker", ItemRegionMarker::new);
    public static final ItemRegistryObject<ItemFlagStick> FLAG_STICK = ITEMS.register("flag_stick", ItemFlagStick::new);
    public static final ItemRegistryObject<ItemEmblem> EMBLEM = ITEMS.register("emblem", ItemEmblem::new);

}
