package com.alex_escobar.worldprotector.registry;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

public class WrappedDeferredRegister<T extends IForgeRegistryEntry<T>> {

    protected final DeferredRegister<T> internal;

    public WrappedDeferredRegister(String modid, IForgeRegistry<T> registry) {
        internal = DeferredRegister.create(registry, modid);
    }

    public <I extends T, W extends WrappedRegistryObject<I>> W register(String name, Supplier<? extends I> sup, Function<RegistryObject<I>, W> objectWrapper) {
        return objectWrapper.apply(internal.register(name, sup));
    }

    public void register(IEventBus bus) {
        internal.register(bus);
    }
}