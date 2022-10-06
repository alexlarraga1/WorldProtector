package com.alex_escobar.worldprotector.registry;

import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class WrappedRegistryObject<T extends IForgeRegistryEntry<? super T>> implements Supplier<T>, INamedEntry {

    protected RegistryObject<T> registryObject;

    public WrappedRegistryObject(RegistryObject<T> registryObject) {
        this.registryObject = registryObject;
    }

    @Nonnull
    @Override
    public T get() {
        return registryObject.get();
    }

    @Override
    public String getInternalRegistryName() {
        return registryObject.getId().getPath();
    }
}