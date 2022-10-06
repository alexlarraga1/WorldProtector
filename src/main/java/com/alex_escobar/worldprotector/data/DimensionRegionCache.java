package com.alex_escobar.worldprotector.data;

import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.Region;
import com.alex_escobar.worldprotector.utils.PlayerUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.stream.Collectors;

public class DimensionRegionCache extends HashMap<String, IRegion> implements INBTSerializable<CompoundTag> {

    public static final String WHITELIST = "whitelist"; // boolean
    public static final String FLAGS = "flags"; // list
    public static final String REGIONS = "regions";  //compound
    public static final String PROTECTORS = "protectors"; // list of uuid?
    public Collection<String> dimensionFlags;
    public Collection<String> protectors;
    public boolean hasWhitelist;

    public DimensionRegionCache(IRegion region) {
        this();
        addRegion(region);
    }

    public DimensionRegionCache() {
        super();
        this.dimensionFlags = new ArrayList<>(0);
        this.protectors = new ArrayList<>(0);
        this.hasWhitelist = true;
    }

    public DimensionRegionCache(CompoundTag nbt) {
        this();
        deserializeNBT(nbt);
    }

    public boolean isActive(String regionName) {
        if (this.containsKey(regionName)) {
            return getRegion(regionName).isActive();
        }
        return false;
    }

    public boolean setIsActive(String regionName, boolean active) {
        if (this.containsKey(regionName)) {
            getRegion(regionName).setIsActive(active);
            return true;
        }
        return false;
    }

    public boolean setIsMuted(String regionName, boolean isMuted) {
        if (this.containsKey(regionName)) {
            getRegion(regionName).setIsMuted(isMuted);
            return true;
        }
        return false;
    }

    public Collection<IRegion> getRegions() {
        return Collections.unmodifiableCollection(this.values());
    }

    public Collection<String> getRegionNames() {
        return Collections.unmodifiableCollection(this.keySet());
    }

    public IRegion removeRegion(String regionName) {
        return this.remove(regionName);
    }

    public void clearRegions() {
        this.clear();
    }

    // TODO: rework to only update area?
    public void updateRegion(IRegion newRegion) {
        if (this.containsKey(newRegion.getName())) {
            this.put(newRegion.getName(), newRegion);
        }
    }

    public void setActiveStateForRegions(boolean activeState) {
        values().forEach(region -> region.setIsActive(activeState));
    }

    /**
     * Make sure region exists with RegionManager.get().containsRegion() before
     *
     * @param regionName regionName to get corresponding region object for
     * @return region object corresponding to region name
     */
    public IRegion getRegion(String regionName) {
        return this.get(regionName);
    }

    public void addRegion(IRegion region) {
        this.put(region.getName(), region);
    }

    /* Flag related methods */
    public Set<String> getFlags(String regionName) {
        if (this.containsKey(regionName)) {
            return this.get(regionName).getFlags();
        }
        return new HashSet<>();
    }

    public boolean removeFlag(IRegion region, String flag) {
        if (this.containsKey(region.getName())) {
            return this.get(region.getName()).removeFlag(flag);
        }
        return false;
    }

    public boolean addFlag(IRegion region, String flag) {
        if (this.containsKey(region.getName())) {
            return this.get(region.getName()).addFlag(flag);
        }
        return false;
    }

    public List<String> addFlags(String regionName, List<String> flags) {
        if (this.containsKey(regionName)) {
            return flags.stream()
                    .filter(flag -> this.get(regionName).addFlag(flag))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<String> removeFlags(String regionName, List<String> flags) {
        if (this.containsKey(regionName)) {
            return flags.stream()
                    .filter(flag -> this.get(regionName).removeFlag(flag))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /* Player related methods */

    public boolean addPlayer(String regionName, Player player){
        if (this.containsKey(regionName)) {
            return this.get(regionName).addPlayer(player);
        }
        return false;
    }

    public boolean addPlayer(String regionName, PlayerUtils.MCPlayerInfo playerInfo){
        if (this.containsKey(regionName)) {
            return this.get(regionName).addPlayer(playerInfo);
        }
        return false;
    }


    public List<Player> addPlayers(String regionName, List<Player> players){
        if (this.containsKey(regionName)) {
            return players.stream()
                    .filter(player -> this.get(regionName).addPlayer(player))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


    public boolean removePlayer(String regionName, Player player){
        if (this.containsKey(regionName)) {
            return this.get(regionName).removePlayer(player);
        }
        return false;
    }

    public boolean removePlayer(String regionName, String playerName){
        if (this.containsKey(regionName)) {
            return this.get(regionName).removePlayer(playerName);
        }
        return false;
    }

    public List<Player> removePlayers(String regionName, List<Player> players){
        if (this.containsKey(regionName)) {
            return players.stream()
                    .filter(player -> this.get(regionName).removePlayer(player))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public boolean forbidsPlayer(String regionName, Player player){
        if (this.containsKey(regionName)) {
            return this.get(regionName).forbids(player);
        }
        return true;
    }

    public Set<String> getPlayers(String regionName) {
        if (this.containsKey(regionName)) {
            return new HashSet<>(getRegion(regionName).getPlayers().values());
        }
        return new HashSet<>();
    }

    public static CompoundTag serializeCache(DimensionRegionCache dimensionRegionCache) {
        CompoundTag dimCache = new CompoundTag();
        for (Entry<String, IRegion> regionEntry : dimensionRegionCache.entrySet()) {
            dimCache.put(regionEntry.getKey(), regionEntry.getValue().serializeNBT());
        }
        return dimCache;
    }

    public static DimensionRegionCache deserialize(CompoundTag nbt) {
        DimensionRegionCache dimCache = new DimensionRegionCache();
        for (String regionKey : nbt.getAllKeys()) {
            CompoundTag regionNbt = nbt.getCompound(regionKey);
            Region region = new Region(regionNbt);
            dimCache.addRegion(region);
        }
        return dimCache;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        CompoundTag regions = new CompoundTag();
        for (Entry<String, IRegion> regionEntry : this.entrySet()) {
            regions.put(regionEntry.getKey(), regionEntry.getValue().serializeNBT());
        }
        nbt.put(REGIONS, regions);
        nbt.put(FLAGS, toNBTList(this.dimensionFlags));
        nbt.put(PROTECTORS, toNBTList(this.dimensionFlags));
        nbt.putBoolean(WHITELIST, this.hasWhitelist);
        return nbt;
    }

    private ListTag toNBTList(Collection<String> list) {
        ListTag nbtList = new ListTag();
        nbtList.addAll(list.stream()
                .map(StringTag::valueOf)
                .toList());
        return nbtList;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        CompoundTag regions = nbt.getCompound(REGIONS);
        for (String regionKey : regions.getAllKeys()) {
            CompoundTag regionNbt = regions.getCompound(regionKey);
            Region region = new Region(regionNbt);
            this.addRegion(region);
        }

        this.dimensionFlags.clear();
        ListTag flagsNBT = nbt.getList(FLAGS, Tag.TAG_STRING);
        for (int i = 0; i < flagsNBT.size(); i++) {
            this.dimensionFlags.add(flagsNBT.getString(i));
        }

        this.protectors.clear();
        ListTag protectorsNBT = nbt.getList(PROTECTORS, Tag.TAG_STRING);
        for (int i = 0; i < protectorsNBT.size(); i++) {
            this.dimensionFlags.add(protectorsNBT.getString(i));
        }

        this.hasWhitelist = nbt.getBoolean(WHITELIST);
    }

    public boolean removeTeam(String regionName, String teamToRemove) {
        if (this.containsKey(regionName)) {
            this.get(regionName).removeTeam(teamToRemove);
            return true;
        }
        return false;
    }

    public boolean addTeam(String regionName, String teamToAdd) {
        if (this.containsKey(regionName)) {
            this.get(regionName).addTeam(teamToAdd);
            return true;
        }
        return false;
    }
}
