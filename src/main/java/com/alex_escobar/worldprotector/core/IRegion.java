package com.alex_escobar.worldprotector.core;

import com.alex_escobar.worldprotector.utils.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IRegion extends INBTSerializable<CompoundTag> {
    AABB getArea();

    Set<String> getFlags();

    boolean addFlag(String flag);

    boolean removeFlag(String flag);

    String getName();

    int getPriority();

    ResourceKey<Level> getDimension();

    BlockPos getTpPos(Level world);

    Map<UUID, String> getPlayers();

    public void addTeam(String team);

    public Set<String> getTeams();

    public void removeTeam(String team);

    Set<String> getPlayerNames();

    boolean permits(Player player);

    boolean forbids(Player player);

    boolean isActive();

    boolean isMuted();

    void setIsActive(boolean isActive);

    void setIsMuted(boolean isMuted);

    boolean addPlayer(Player player);

    boolean addPlayer(PlayerUtils.MCPlayerInfo playerInfo);

    boolean removePlayer(Player player);

    boolean removePlayer(String playerName);

    boolean containsFlag(String flag);

    boolean containsFlag(RegionFlag flag);

    boolean containsPosition(BlockPos position);

    void setPriority(int priority);

    void setArea(AABB areaFromNBT);

    BlockPos getTpTarget();

    void setTpTarget(BlockPos pos);
}
