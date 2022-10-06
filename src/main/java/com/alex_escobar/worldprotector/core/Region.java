package com.alex_escobar.worldprotector.core;

import com.alex_escobar.worldprotector.utils.PlayerUtils;
import com.alex_escobar.worldprotector.utils.RegionPlayerUtils;
import joptsimple.internal.Strings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Collectors;

public class Region implements IRegion {

	private String name;
	private ResourceKey<Level> dimension;
	private AABB area;
	private final Set<String> flags;
	private final Map<UUID, String> players;

	private final Set<String> teams;

	private boolean isActive;
	private int priority = 2;
	private boolean isMuted;
	private int tpTargetX;
	private int tpTargetY;
	private int tpTargetZ;
	private String enterMessage = Strings.EMPTY;
	private String exitMessage = Strings.EMPTY;
	private String enterMessageSmall = Strings.EMPTY;
	private String exitMessageSmall = Strings.EMPTY;
	// nbt keys
	public static final String TP_X = "tp_x";
	public static final String TP_Y = "tp_y";
	public static final String TP_Z = "tp_z";
	public static final String NAME = "name";
	public static final String UUID = "uuid";
	public static final String DIM = "dimension";
	public static final String MIN_X = "minX";
	public static final String MIN_Y = "minY";
	public static final String MIN_Z = "minZ";
	public static final String MAX_X = "maxX";
	public static final String MAX_Y = "maxY";
	public static final String MAX_Z = "maxZ";

	public static final String PRIORITY = "priority";
	public static final String ACTIVE = "active";
	public static final String PLAYERS = "players";
	public static final String FLAGS = "flags";

	public static final String TEAMS = "teams";
	public static final String ENTER_MSG_1 = "enter_msg";
	public static final String ENTER_MSG_2 = "enter_msg_small";
	public static final String EXIT_MSG_1 = "exit_msg";
	public static final String EXIT_MSG_2 = "exit_msg_small";
	public static final String MUTED = "muted";

	public static final String VERSION = "version";
	public static final String DATA_VERSION = "2.1.4.0";

	public Region(CompoundTag nbt) {
		this.flags = new HashSet<>();
		this.players = new HashMap<>();
		this.teams = new HashSet<>();
		deserializeNBT(nbt);
	}

	public Region(String name, AABB area, ResourceKey<Level> dimension) {
		this.name = name;
		this.area = area;
		this.tpTargetX = (int) this.area.getCenter().x;
		this.tpTargetY = (int) this.area.getCenter().y;
		this.tpTargetZ = (int) this.area.getCenter().z;
		this.dimension = dimension;
		this.isActive = true;
		this.isMuted = false;
		this.players = new HashMap<>();
		this.flags = new HashSet<>();
		this.teams = new HashSet<>();
	}

	public Region(String name, AABB area, BlockPos tpPos, ResourceKey<Level> dimension) {
		this.tpTargetX = tpPos.getX();
		this.tpTargetY = tpPos.getY();
		this.tpTargetZ = tpPos.getZ();
		this.name = name;
		this.area = area;
		this.dimension = dimension;
		this.isActive = true;
		this.isMuted = false;
		this.players = new HashMap<>();
		this.teams = new HashSet<>();
		this.flags = new HashSet<>();
	}

	public Region(Region copy) {
		this.tpTargetX = copy.getTpTarget().getX();
		this.tpTargetY = copy.getTpTarget().getY();
		this.tpTargetZ = copy.getTpTarget().getZ();
		this.name = copy.getName();
		this.area = copy.getArea();
		this.dimension = copy.getDimension();
		this.isActive = copy.isActive();
		this.isMuted = copy.isMuted();
		this.players = copy.getPlayers();
		this.teams = copy.getTeams();
		this.flags = copy.getFlags();
		this.priority = copy.getPriority();
		this.enterMessage = copy.getEnterMessage();
		this.exitMessage = copy.getExitMessage();
		this.enterMessageSmall = copy.getEnterMessageSmall();
		this.exitMessageSmall = copy.getExitMessageSmall();
	}

	public void addTeam(String team){
		this.teams.add(team);
	}

	public Set<String> getTeams() {
		return teams;
	}

	public void removeTeam(String team){
		teams.remove(team);
	}

	public Region(IRegion copy) {
		this.tpTargetX = copy.getTpTarget().getX();
		this.tpTargetY = copy.getTpTarget().getY();
		this.tpTargetZ = copy.getTpTarget().getZ();
		this.name = copy.getName();
		this.area = copy.getArea();
		this.dimension = copy.getDimension();
		this.isActive = copy.isActive();
		this.players = copy.getPlayers();
		this.teams = copy.getTeams();
		this.flags = copy.getFlags();
		this.priority = copy.getPriority();
	}

	@Override
	public AABB getArea() {
		return area;
	}

	public void setArea(AABB area) {
		this.area = area;
	}

	/**
	 * TODO: Does not check for harmful blocks though
	 *
	 * @param world
	 * @return
	 */
	public BlockPos getCenterSaveTpPos(Level world){
		Vec3 center = area.getCenter();
		int highestNonBlockingY =  world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) area.minX, (int) area.minZ);
		return new BlockPos(center.x, highestNonBlockingY + 1, center.z);
	}

	/**
	 * @param world
	 * @return
	 */
	@Override
	public BlockPos getTpPos(Level world) {
		int highestNonBlockingY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) area.minX, (int) area.minZ);
		return new BlockPos(area.minX, highestNonBlockingY + 1, area.minZ);
	}

	@Override
	public BlockPos getTpTarget() {
		return new BlockPos(this.tpTargetX, this.tpTargetY, this.tpTargetZ);
	}

	@Override
	public void setTpTarget(BlockPos tpPos) {
		this.tpTargetX = tpPos.getX();
		this.tpTargetY = tpPos.getY();
		this.tpTargetZ = tpPos.getZ();
	}

	@Override
	public Set<String> getFlags() {
		return flags;
	}

	/**
	 * True if added
	 *
	 * @param flag
	 * @return
	 */
	@Override
	public boolean addFlag(String flag) {
		return this.flags.add(flag);
	}

	/**
	 * true if removed
	 *
	 * @param flag
	 * @return
	 */
	@Override
	public boolean removeFlag(String flag) {
		return this.flags.remove(flag);
	}

	@Override
	public String getName() {
		return name;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	public String getDimensionString() {
		return dimension.location().toString();
	}

	public String getEnterMessage() {
		return enterMessage;
	}

	public String getExitMessage() {
		return exitMessage;
	}

	public String getEnterMessageSmall() {
		return enterMessageSmall;
	}

	public String getExitMessageSmall() {
		return exitMessageSmall;
	}

	@Override
	public ResourceKey<Level> getDimension() {
		return dimension;
	}

	@Override
	public Map<UUID, String> getPlayers() {
		return Collections.unmodifiableMap(this.players);
	}

	public Set<UUID> getPlayerUUIDs() {
		return Set.copyOf(this.players.keySet());
	}

	@Override
	public Set<String> getPlayerNames() {
		return Set.copyOf(this.players.values());
	}

	/**
	 * Checks if the player is defined in the regions player list OR whether the player is an operator.
	 * Usually this check is needed when an event occurs and it needs to be checked whether
	 * the player has a specific permission to perform an action in the region.
	 *
	 * @param player to be checked
	 * @return true if player is in region list (or the player's team is in the region list) or is an operator, false otherwise
	 */
	@Override
	public boolean permits(Player player) {
		if (RegionPlayerUtils.hasNeededOpLevel(player)) {
			return true;
		}
		return players.containsKey(player.getUUID()) ||
				(player.getTeam() != null && teams.contains(player.getTeam().getName()));
	}

	@Override
	public boolean forbids(Player player) {
		return !this.permits(player);
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public boolean isMuted() {
		return this.isMuted;
	}

	@Override
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Override
	public void setIsMuted(boolean isMuted) {
		this.isMuted = isMuted;
	}

	public void activate() {
		this.isActive = true;
	}

	public void deactivate() {
		this.isActive = false;
	}

	@Override
	public boolean addPlayer(Player player) {
		String oldPlayer = this.players.put(player.getUUID(), player.getName().getString());
		return !player.getName().getString().equals(oldPlayer);
	}

	@Override
	public boolean addPlayer(PlayerUtils.MCPlayerInfo playerInfo) {
		String oldPlayer = this.players.put(java.util.UUID.fromString(playerInfo.playerUUID), playerInfo.playerName);
		return !playerInfo.playerName.equals(oldPlayer);
	}

	@Override
	public boolean removePlayer(String playerName) {
		Optional<UUID> playerUUID = this.players.entrySet().stream()
				.filter((entry) -> entry.getValue().equals(playerName))
				.findFirst().map(Map.Entry::getKey);
		if (playerUUID.isPresent()) {
			String oldPlayer = this.players.remove(playerUUID.get());
			return oldPlayer != null;
		}
		return false;
	}


	@Override
	public boolean removePlayer(Player player) {
		if (this.players.containsKey(player.getUUID())) {
			String oldPlayer = this.players.remove(player.getUUID());
			return oldPlayer != null;
		}
		return false;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putString(VERSION, DATA_VERSION);
		nbt.putString(NAME, name);
		nbt.putInt(MIN_X, (int) area.minX);
		nbt.putInt(MIN_Y, (int) area.minY);
		nbt.putInt(MIN_Z, (int) area.minZ);
		nbt.putInt(MAX_X, (int) area.maxX);
		nbt.putInt(MAX_Y, (int) area.maxY);
		nbt.putInt(MAX_Z, (int) area.maxZ);
		nbt.putInt(TP_X, this.tpTargetX);
		nbt.putInt(TP_Y, this.tpTargetY);
		nbt.putInt(TP_Z, this.tpTargetZ);
		nbt.putInt(PRIORITY, priority);
		nbt.putString(DIM, dimension.location().toString());
		nbt.putBoolean(ACTIVE, isActive);
		nbt.putBoolean(MUTED, isMuted);
		nbt.putString(ENTER_MSG_1, enterMessage);
		nbt.putString(ENTER_MSG_2, enterMessageSmall);
		nbt.putString(EXIT_MSG_1, exitMessage);
		nbt.putString(EXIT_MSG_2, exitMessageSmall);
		ListTag flagsNBT = new ListTag();
		flagsNBT.addAll(flags.stream()
				.map(StringTag::valueOf)
				.collect(Collectors.toSet()));
		nbt.put(FLAGS, flagsNBT);

		// serialize player data
		ListTag playerList = nbt.getList(PLAYERS, Tag.TAG_COMPOUND);
		players.forEach( (uuid, name) -> {
			CompoundTag playerNBT = new CompoundTag();
			playerNBT.putUUID(UUID, uuid);
			playerNBT.putString(NAME, name);
			playerList.add(playerNBT);
		});
		nbt.put(PLAYERS, playerList);

		ListTag teamList = new ListTag();
		teamList.addAll(teams.stream()
				.map(StringTag::valueOf)
				.collect(Collectors.toSet()));
		nbt.put(TEAMS, teamList);
		return nbt;
	}

	private AABB areaFromNBT(CompoundTag nbt){
		return new AABB(
				nbt.getInt(MIN_X), nbt.getInt(MIN_Y), nbt.getInt(MIN_Z),
				nbt.getInt(MAX_X), nbt.getInt(MAX_Y), nbt.getInt(MAX_Z)
		);
	}

	private CompoundTag migrateRegionData(CompoundTag nbt){
		// TODO:

		nbt.putString(VERSION, DATA_VERSION);

		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		nbt = migrateRegionData(nbt);
		this.name = nbt.getString(NAME);
		this.area = areaFromNBT(nbt);
		this.tpTargetX = nbt.getInt(TP_X);
		this.tpTargetY = nbt.getInt(TP_Y);
		this.tpTargetZ = nbt.getInt(TP_Z);
		this.priority = nbt.getInt(PRIORITY);
		this.dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(nbt.getString(DIM)));
		this.isActive = nbt.getBoolean(ACTIVE);
		this.isMuted = nbt.getBoolean(MUTED);
		this.enterMessage = nbt.getString(ENTER_MSG_2);
		this.enterMessageSmall = nbt.getString(ENTER_MSG_2);
		this.exitMessage = nbt.getString(EXIT_MSG_1);
		this.exitMessageSmall = nbt.getString(EXIT_MSG_2);
		this.flags.clear();
		ListTag flagsList = nbt.getList(FLAGS, Tag.TAG_STRING);
		for (int i = 0; i < flagsList.size(); i++) {
			flags.add(flagsList.getString(i));
		}
		// deserialize player data
		this.players.clear();
		ListTag playerLists = nbt.getList(PLAYERS, Tag.TAG_COMPOUND);
		for (int i = 0; i < playerLists.size(); i++) {
			CompoundTag playerMapping = playerLists.getCompound(i);
			players.put(playerMapping.getUUID(UUID), playerMapping.getString(NAME));
		}
		// deserialize teams data
		this.teams.clear();
		ListTag teamList = nbt.getList(TEAMS, Tag.TAG_STRING);
		for (int i = 0; i < teamList.size(); i++) {
			teams.add(teamList.getString(i));
		}
	}

	@Override
	public boolean containsFlag(String flag) {
		return flags.contains(flag);
	}

	public boolean containsFlag(RegionFlag flag) {
		return flags.contains(flag.toString());
	}

	@Override
	public boolean containsPosition(BlockPos position) {
		int x = position.getX();
		int y = position.getY();
		int z = position.getZ();
		// INFO: this.area.contains(x,y,z); does not work. See implementation. Forge-Version 36.1.25
		return x >= this.area.minX && x <= this.area.maxX
				&& y >= this.area.minY && y <= this.area.maxY
				&& z >= this.area.minZ && z <= this.area.maxZ;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", Region.class.getSimpleName() + "[", "]")
				.add("name='" + name + "'")
				.add("dimension=" + dimension.location())
				.add("area=" + area.toString())
				.add("flags=" + flags)
				.add("teams=" + teams)
				.add("players=" + players)
				.add("isActive=" + isActive)
				.add("isMuted=" + isMuted)
				.add("priority=" + priority)
				.add("enterMessage='" + enterMessage + "'")
				.add("exitMessage='" + exitMessage + "'")
				.add("enterMessageSmall='" + enterMessageSmall + "'")
				.add("exitMessageSmall='" + exitMessageSmall + "'")
				.add("tpTarget=[" + tpTargetX + ", " + tpTargetY + ", " + tpTargetZ + "]")
				.toString();
	}
}
