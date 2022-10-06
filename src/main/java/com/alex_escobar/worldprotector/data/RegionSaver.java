package com.alex_escobar.worldprotector.data;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.core.Region;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.server.ServerStartingEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegionSaver extends SavedData {

    private static final Map<String, Region> Regions = new HashMap<>();
    private static final String DATA_NAME = WorldProtector.MODID;
    private static final Map<Level, RegionSaver> dataMap = new HashMap<>();
    private ServerLevel level;

    public RegionSaver() {
        super();
    }

    public static RegionSaver get(Level world) {
        if (world instanceof ServerLevel) {
            ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
            RegionSaver fromMap = dataMap.get(overworld);
            if (fromMap == null) {
                DimensionDataStorage storage = overworld.getDataStorage();
                RegionSaver data = storage.computeIfAbsent(RegionSaver::load, RegionSaver::new, DATA_NAME);
                if (data != null) {
                    data.level = overworld;
                    data.setDirty();
                }
                dataMap.put(world, data);
            }
            return fromMap;
        }
        return null;
    }

    public static RegionSaver load(CompoundTag compound) {
        RegionSaver data = new RegionSaver();

        Regions.clear();
        ListTag regionsList = compound.getList("regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < regionsList.size(); i++) {
            Region area = new Region(regionsList.getCompound(i));
            Regions.put(area.getName(), area);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag regionsList = new ListTag();
        for (Region area : Regions.values()) {
            regionsList.add(area.serializeNBT());
        }
        compound.put("regions", regionsList);
        return compound;
    }


    public static Set<String> getRegionFlags(String regionName) {
        return Regions.get(regionName).getFlags();
    }

    public static Collection<Region> getRegions() {
        return Regions.values();
    }

    public static Collection<String> getRegionNames() {
        return Regions.keySet();
    }

    public static Region removeRegion(String regionName) {
        return Regions.remove(regionName);
    }

    public static void clearRegions() {
        Regions.clear();
    }

    public static void replaceRegion(Region newRegion) {
        Region oldRegion = Regions.get(newRegion.getName());
        oldRegion.getFlags().forEach(newRegion::addFlag);
        removeRegion(oldRegion.getName());
        addRegion(newRegion);
    }

    public static Region getRegion(String regionName) {
        return Regions.get(regionName);
    }

    public static boolean containsRegion(String regionName) {
        return Regions.containsKey(regionName);
    }

    public static void addRegion(Region region) {
        Regions.put(region.getName(), region);
    }

    public static void onServerStarting(ServerStartingEvent event) {
        try {
			get(event.getServer().overworld());
            WorldProtector.LOGGER.debug("Loaded dimension regions successfully");

        } catch (NullPointerException npe) {
            WorldProtector.LOGGER.error("Loading dimension regions failed");
        }
    }
}
