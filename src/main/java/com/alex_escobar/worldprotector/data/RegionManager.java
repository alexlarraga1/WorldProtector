package com.alex_escobar.worldprotector.data;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.api.event.RegionEvent;
import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.utils.PlayerUtils;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.stream.Collectors;

public class RegionManager extends SavedData {

    public static final String TAG_REGIONS = "regions";
    private static final String DATA_NAME = WorldProtector.MODID;
    private static final Map<ResourceKey<Level>, DimensionRegionCache> regionMap = new HashMap<>();
    private static Map<Level, RegionManager> dataMap = new HashMap<>();
    private ServerLevel level;
    
    public RegionManager() {
        super();
    }

    private static RegionManager clientRegionCopy = new RegionManager();

    public static RegionManager get() {
        if(clientRegionCopy == null) {
            Level world = ServerLifecycleHooks.getCurrentServer().overworld();
            ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
            RegionManager fromMap = dataMap.get(overworld);
            if(fromMap == null){
                DimensionDataStorage storage = overworld.getDataStorage();
                RegionManager data = storage.computeIfAbsent(RegionManager::load, RegionManager::new, DATA_NAME);
                if(data != null) {
                    clientRegionCopy = data;
                    data.level = overworld;
                    data.setDirty();
                }
                dataMap.put(world, data);
                return data;
            }
            return fromMap;
        }
        return clientRegionCopy;
    }

//    public static RegionManager get() {
//        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//        return get(server.overworld());
//    }
    
    public static RegionManager load(CompoundTag compound) {
        RegionManager data = new RegionManager();
        clearRegions(data);
        CompoundTag dimensionRegions = compound.getCompound(TAG_REGIONS);
        for (String dimKey : dimensionRegions.getAllKeys()) {
            CompoundTag dimRegionMap = dimensionRegions.getCompound(dimKey);
            DimensionRegionCache dimCache = new DimensionRegionCache(dimRegionMap);
            ResourceKey<Level> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimKey));
            regionMap.put(dimension, dimCache);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        CompoundTag dimRegionNbtData = new CompoundTag();
        for (Map.Entry<ResourceKey<Level>, DimensionRegionCache> entry : regionMap.entrySet()) {
            String dim = entry.getKey().location().toString();
            CompoundTag dimCompound = entry.getValue().serializeNBT();
            dimRegionNbtData.put(dim, dimCompound);
        }
        compound.put(TAG_REGIONS, dimRegionNbtData);
        return compound;
    }


    public static void onServerStarting(ServerStartingEvent event) {
        try {
            ServerLevel world = Objects.requireNonNull(event.getServer().getLevel(Level.OVERWORLD));
            if (!world.isClientSide) {
                RegionManager data = get();
                WorldProtector.LOGGER.info("Loaded " + data.getAllRegionNames().size() + " regions for " + data.getDimensionList().size() + " different dimensions");
            }
        } catch (NullPointerException npe) {
            WorldProtector.LOGGER.error("Loading dimension regions failed");
        }
    }

    public static void clearRegions(RegionManager data) {
        regionMap.forEach((dim, cache) -> cache.clearRegions());
        data.setDirty();
    }

    public Optional<DimensionRegionCache> getRegionsForDim(ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim)) {
            return Optional.of(regionMap.get(dim));
        }
        return Optional.empty();
    }

    private Optional<ResourceKey<Level>> getDimensionOfRegion(String regionName) {
        return regionMap.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(regionName))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Optional<IRegion> getRegionInDim(ResourceKey<Level> dim, String regionName) {
        if (regionMap.containsKey(dim) && regionMap.get(dim).containsKey(regionName)) {
            return Optional.of(regionMap.get(dim).get(regionName));
        }
        return Optional.empty();
    }

    public void addRegionToDim(IRegion region) {
        ResourceKey<Level> dim = region.getDimension();
        if (regionMap.containsKey(dim)) {
            regionMap.get(dim).put(region.getName(), region);
        } else {
            DimensionRegionCache initMapForDim = new DimensionRegionCache(region);
            regionMap.put(dim, initMapForDim);
        }
        setDirty();
    }

    public boolean isActive(String regionName) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            DimensionRegionCache cache = maybeCache.get();
            return cache.isActive(regionName);
        }
        return false;
    }

    public boolean setActiveState(String regionName, boolean active) {
        Optional<IRegion> maybeRegion = getRegion(regionName);
        if (maybeRegion.isPresent()) {
            IRegion region = maybeRegion.get();
            boolean wasUpdated = regionMap.get(region.getDimension()).setIsActive(regionName, active);
            if (wasUpdated) {
                setDirty();
            }
            return wasUpdated;
        } else {
            return false;
        }
    }

    public boolean setMutedState(String regionName, boolean isMuted) {
        Optional<IRegion> maybeRegion = getRegion(regionName);
        if (maybeRegion.isPresent()) {
            IRegion region = maybeRegion.get();
            boolean wasUpdated = regionMap.get(region.getDimension()).setIsMuted(regionName, isMuted);
            if (wasUpdated) {
                setDirty();
            }
            return wasUpdated;
        } else {
            return false;
        }
    }

    public Collection<IRegion> getAllRegionsFor(ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim)) {
            return regionMap.get(dim).getRegions();
        }
        return new ArrayList<>(0);
    }

    public Collection<IRegion> getAllRegions() {
        return regionMap.values().stream()
                .flatMap(regionCache -> regionCache.getRegions().stream())
                .collect(Collectors.toList());
    }

    public Collection<IRegion> getAllRegionsSorted() {
        return regionMap.values().stream()
                .flatMap(regionCache -> regionCache.getRegions()
                        .stream()
                        .sorted(Comparator.comparing(IRegion::getName)))
                .collect(Collectors.toList());
    }

    public Collection<String> getAllRegionNames() {
        return regionMap.values().stream()
                .flatMap(regionCache -> regionCache.getRegionNames().stream())
                .collect(Collectors.toList());
    }

    public Collection<String> getRegionNames(ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim)) {
            return regionMap.get(dim).getRegionNames();
        }
        return Collections.emptySet();
    }

    public Collection<IRegion> getRegions(ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim)) {
            return regionMap.get(dim).getRegions();
        }
        return Collections.emptySet();
    }

    public boolean removeRegion(String regionName, ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim)) {
            IRegion removed = regionMap.get(dim).remove(regionName);
            setDirty();
            return removed != null;
        }
        return false;
    }

    public Collection<String> getDimensionList() {
        return regionMap.keySet().stream()
                .map(entry -> entry.location().toString())
                .collect(Collectors.toList());
    }

    // TODO: Clear regions in specific dim
    public void clearRegions() {
        regionMap.forEach((dim, cache) -> cache.clearRegions());
        setDirty();
    }

    public IRegion removeRegion(String regionName) {
        Optional<IRegion> maybeRegion = getRegion(regionName);
        if (maybeRegion.isPresent()) {
            IRegion region = maybeRegion.get();
            IRegion removed = regionMap.get(region.getDimension()).removeRegion(regionName);
            setDirty();
            MinecraftForge.EVENT_BUS.post(new RegionEvent.RemoveRegionEvent(region, null));
            return removed;
        } else {
            return null;
        }
    }

    public Optional<IRegion> getRegion(String regionName) {
        return regionMap.values().stream()
                .filter(regionCache -> regionCache.containsKey(regionName)) // one remaining
                .map(regionCache -> regionCache.getRegion(regionName))
                .findFirst();
    }

    public Optional<DimensionRegionCache> getRegionDimCache(String regionName) {
        return regionMap.values().stream()
                .filter(regionCache -> regionCache.containsKey(regionName))
                .findFirst();
    }

    public boolean containsRegion(String regionName, ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim)) {
            return regionMap.get(dim).containsKey(regionName);
        }
        return false;
    }

    public boolean containsRegion(String regionName) {
        return regionMap.values().stream()
                .anyMatch(regionCache -> regionCache.containsKey(regionName));
    }

    public void setActiveStateForRegionsInDim(ResourceKey<Level> dim, boolean activeState) {
        if (regionMap.containsKey(dim)) {
            regionMap.get(dim).setActiveStateForRegions(activeState);
            setDirty();
        }
    }

    /**
     * Prefer this getRegion
     *
     * @param regionName
     * @param dim
     * @return
     */
    public Optional<IRegion> getRegion(String regionName, ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim) && regionMap.get(dim).containsKey(regionName)) {
            return Optional.of(regionMap.get(dim).getRegion(regionName));
        }
        return Optional.empty();
    }

    public boolean containsDimensionFor(IRegion region) {
        return regionMap.containsKey(region.getDimension());
    }

    // Flag methods
    public Set<String> getRegionFlags(String regionName, ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim)) {
            return regionMap.get(dim).getFlags(regionName);
        }
        return new HashSet<>();
    }

    public void updateRegion(IRegion newRegion, Player player) {
        ResourceKey<Level> dim = newRegion.getDimension();
        if (regionMap.containsKey(dim)) {
            regionMap.get(dim).updateRegion(newRegion);
            MinecraftForge.EVENT_BUS.post(new RegionEvent.UpdateRegionEvent(newRegion, player));
            setDirty();
        }
    }

    /**
     * Always check contains first!
     *
     * @param region
     * @return
     */
    private DimensionRegionCache getCache(IRegion region) {
        return regionMap.get(region.getDimension());
    }

    public boolean removeFlag(IRegion region, String flag) {
        if (containsDimensionFor(region)) {
            boolean wasRemoved = getCache(region).removeFlag(region, flag);
            if (wasRemoved) {
                setDirty();
            }
            return wasRemoved;
        }
        return false;
    }

    public List<String> removeFlags(String regionName, List<String> flags) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            List<String> removed = maybeCache.get().removeFlags(regionName, flags);
            setDirty();
            return removed;
        } else {
            return new ArrayList<>();
        }
    }

    public List<String> addFlags(String regionName, List<String> flags) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            List<String> added = maybeCache.get().addFlags(regionName, flags);
            setDirty();
            return added;
        } else {
            return new ArrayList<>();
        }
    }

    /* Player related methods */

    public boolean addFlag(String regionName, String flag) {
        if (RegionManager.get().containsRegion(regionName)) {
            RegionManager.get().getRegion(regionName).ifPresent(region -> {
                addFlag(region, flag);
            });
            return true;
        }
        return false;
    }

    public boolean addFlag(IRegion region, String flag) {
        if (containsDimensionFor(region)) {
            boolean wasAdded = getCache(region).addFlag(region, flag);
            if (wasAdded) {
                setDirty();
            }
            return wasAdded;
        }
        return false;
    }

    public Set<String> getRegionPlayers(String regionName, ResourceKey<Level> dim) {
        if (regionMap.containsKey(dim) && regionMap.get(dim).containsKey(regionName)) {
            return regionMap.get(dim).getPlayers(regionName);
        }
        return new HashSet<>();
    }

    public boolean addPlayer(String regionName, Player player) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            boolean wasAdded = maybeCache.get().addPlayer(regionName, player);
            if (wasAdded) {
                setDirty();
            }
            return wasAdded;
        }
        return false;
    }

    public boolean addPlayer(String regionName, PlayerUtils.MCPlayerInfo playerInfo) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            boolean wasAdded = maybeCache.get().addPlayer(regionName, playerInfo);
            if (wasAdded) {
                setDirty();
            }
            return wasAdded;
        }
        return false;
    }

    public List<Player> addPlayers(String regionName, List<Player> playersToAdd) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            List<Player> added = maybeCache.get().addPlayers(regionName, playersToAdd);
            setDirty();
            return added;
        } else {
            return new ArrayList<>();
        }
    }

    public boolean removePlayer(String regionName, Player player) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            boolean wasRemoved = maybeCache.get().removePlayer(regionName, player);
            if (wasRemoved) {
                setDirty();
            }
            return wasRemoved;
        }
        return false;
    }

    public boolean removePlayer(String regionName, String playerName) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            boolean wasRemoved = maybeCache.get().removePlayer(regionName, playerName);
            if (wasRemoved) {
                setDirty();
            }
            return wasRemoved;
        }
        return false;
    }


    public List<Player> removePlayers(String regionName, List<Player> playersToRemove) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            List<Player> removed = maybeCache.get().removePlayers(regionName, playersToRemove);
            setDirty();
            return removed;
        } else {
            return new ArrayList<>();
        }
    }

    public boolean forbidsPlayer(String regionName, Player player) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        return maybeCache
                .map(dimensionRegionCache -> dimensionRegionCache.forbidsPlayer(regionName, player))
                .orElse(true);
    }

    // Data

    public Set<String> getRegionPlayers(String regionName) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            DimensionRegionCache regionCache = maybeCache.get();
            if (regionCache.containsKey(regionName)) {
                return new HashSet<>(regionCache.getRegion(regionName).getPlayers().values());
            }
        }
        return new HashSet<>();
    }

    public void addRegion(IRegion region) {
        if (regionMap.containsKey(region.getDimension())) {
            regionMap.get(region.getDimension()).addRegion(region);
        } else {
            DimensionRegionCache newCache = new DimensionRegionCache(region);
            regionMap.put(region.getDimension(), newCache);
        }
        MinecraftForge.EVENT_BUS.post(new RegionEvent.CreateRegionEvent(region, null));
        setDirty();
    }


    public boolean addTeam(String regionName, String teamToAdd) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            boolean wasAdded = maybeCache.get().addTeam(regionName, teamToAdd);
            if (wasAdded) {
                setDirty();
            }
            return wasAdded;
        }
        return false;
    }

    public boolean removeTeam(String regionName, String teamToRemove) {
        Optional<DimensionRegionCache> maybeCache = getRegionDimCache(regionName);
        if (maybeCache.isPresent()) {
            boolean wasRemoved = maybeCache.get().removeTeam(regionName, teamToRemove);
            if (wasRemoved) {
                setDirty();
            }
            return wasRemoved;
        }
        return false;
    }
    
}
