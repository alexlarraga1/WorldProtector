package com.alex_escobar.worldprotector.core;

import com.alex_escobar.worldprotector.utils.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RegionProxy implements IRegion{

    private final String regionName;
    private Region backingRegion;

    public RegionProxy(String regionName){
        this.regionName = regionName;
    }

    @Override
    public AABB getArea() {
        return null;
    }

    @Override
    public Set<String> getFlags() {
        return null;
    }

    @Override
    public boolean addFlag(String flag) {
        return false;
    }

    @Override
    public boolean removeFlag(String flag) {
        return false;
    }

    @Override
    public String getName() {
        return regionName;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public ResourceKey<Level> getDimension() {
        return null;
    }

    @Override
    public BlockPos getTpPos(Level world) {
        return null;
    }

    @Override
    public Map<UUID, String> getPlayers() {
        return null;
    }

    @Override
    public void addTeam(String team) {

    }

    @Override
    public Set<String> getTeams() {
        return null;
    }

    @Override
    public void removeTeam(String team) {

    }

    @Override
    public Set<String> getPlayerNames() {
        return null;
    }

    @Override
    public boolean permits(Player player) {
        return false;
    }

    @Override
    public boolean forbids(Player player) {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isMuted() {
        return false;
    }

    @Override
    public void setIsActive(boolean isActive) {

    }

    @Override
    public void setIsMuted(boolean isMuted) {

    }

    @Override
    public boolean addPlayer(Player player) {
        return false;
    }

    @Override
    public boolean addPlayer(PlayerUtils.MCPlayerInfo playerInfo) {
        return false;
    }

    @Override
    public boolean removePlayer(Player player) {
        return false;
    }

    @Override
    public boolean removePlayer(String playerName) {
        return false;
    }

    @Override
    public boolean containsFlag(String flag) {
        return false;
    }

    @Override
    public boolean containsFlag(RegionFlag flag) {
        return false;
    }

    @Override
    public boolean containsPosition(BlockPos position) {
        return false;
    }

    @Override
    public void setPriority(int priority) {

    }

    @Override
    public void setArea(AABB areaFromNBT) {

    }

    @Override
    public BlockPos getTpTarget() {
        return null;
    }

    @Override
    public void setTpTarget(BlockPos pos) {

    }

    @Override
    public CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

    }
}
