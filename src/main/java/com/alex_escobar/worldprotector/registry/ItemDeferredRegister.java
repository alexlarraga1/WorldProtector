package com.alex_escobar.worldprotector.registry;

import com.alex_escobar.worldprotector.WorldProtector;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemDeferredRegister extends WrappedDeferredRegister<Item> {

    private final List<ItemLike> allItems = new ArrayList<>();

    public ItemDeferredRegister(String modid) {
        super(modid, ForgeRegistries.ITEMS);
    }

    public static Item.Properties getWPBaseProperties() {
        return new Item.Properties().tab(WorldProtector.WORLD_PROTECTOR_TAB);
    }

    public ItemRegistryObject<Item> register(String name) {
        return register(name, () -> new Item(getWPBaseProperties()));
    }

    public <ITEM extends Item> ItemRegistryObject<ITEM> register(String name, Function<Item.Properties, ITEM> sup) {
        return register(name, () -> sup.apply(getWPBaseProperties()));
    }

    public <ITEM extends Item> ItemRegistryObject<ITEM> register(String name, Supplier<? extends ITEM> sup) {
        ItemRegistryObject<ITEM> registeredItem = register(name, sup, ItemRegistryObject::new);
        allItems.add(registeredItem);
        return registeredItem;
    }

    public List<ItemLike> getAllItems() {
        return allItems;
    }
}