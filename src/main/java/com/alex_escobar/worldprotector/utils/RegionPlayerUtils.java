package com.alex_escobar.worldprotector.utils;

import com.alex_escobar.worldprotector.command.Command;
import com.alex_escobar.worldprotector.config.ServerConfigBuilder;
import com.alex_escobar.worldprotector.data.RegionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.stream.Collectors;

import static com.alex_escobar.worldprotector.config.ServerConfigBuilder.WP_CMD;
import static com.alex_escobar.worldprotector.utils.MessageUtils.buildRunCommandLink;
import static com.alex_escobar.worldprotector.utils.MessageUtils.sendMessage;

public final class RegionPlayerUtils {

    private RegionPlayerUtils() {
    }

    public static void addPlayers(String regionName, Player sourcePlayer, List<Player> playersToAdd) {
        if (RegionManager.get().containsRegion(regionName)) {
            List<Player> addedPlayers = RegionManager.get().addPlayers(regionName, playersToAdd);
            List<String> playerNames = addedPlayers.stream()
                    .map(player -> player.getName().getString())
                    .collect(Collectors.toList());
            String playerString = String.join(", ", playerNames);
            if (!addedPlayers.isEmpty()) {
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.players.add.multiple", playerString, regionName));
                addedPlayers.forEach(playerEntity -> sendMessage((ServerPlayer) playerEntity, new TranslatableComponent( "message.player.regionadded", regionName)));
            } else {
                // TODO: rework lang key
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent( "message.players.add.none", playerString, regionName));
            }
        } else {
            sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.unknown", regionName));
        }
    }

    public static void addOfflinePlayer(String regionName, Player sourcePlayer, PlayerUtils.MCPlayerInfo playerInfo) {
        if (RegionManager.get().containsRegion(regionName)) {
            String playerToAddName = playerInfo.playerName;
            if (RegionManager.get().addPlayer(regionName, playerInfo)) {
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.addplayer", playerToAddName, regionName));
            } else {
                // Player already defined in this region -> Message needed or silent acknowledgement?
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.errorplayer", regionName, playerToAddName));
            }
        } else {
            sendMessage((ServerPlayer) sourcePlayer,  new TranslatableComponent("message.region.unknown", regionName));
        }
    }

    public static void addPlayer(String regionName, Player sourcePlayer, Player playerToAdd) {
        if (RegionManager.get().containsRegion(regionName)) {
            String playerToAddName = playerToAdd.getName().getString();
            if (RegionManager.get().addPlayer(regionName, playerToAdd)) {
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.addplayer", playerToAddName, regionName));
                sendMessage((ServerPlayer) playerToAdd, new TranslatableComponent("message.player.regionadded", regionName));
            } else {
                // Player already defined in this region -> Message needed or silent acknowledgement?
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.errorplayer", regionName, playerToAddName));
            }
        } else {
            sendMessage((ServerPlayer) sourcePlayer,  new TranslatableComponent("message.region.unknown", regionName));
        }
    }

    public static void removeOfflinePlayer(String regionName, Player sourcePlayer, String playerToRemoveName) {
        if (RegionManager.get().containsRegion(regionName)) {
            if (RegionManager.get().removePlayer(regionName, playerToRemoveName)) {
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.removeplayer", playerToRemoveName, regionName));
            } else {
                // Player was not present in this region -> Message needed or silent acknowledgement?
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.unknownplayer", regionName, playerToRemoveName));
            }
        } else {
            sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.unknown", regionName));
        }
    }

    public static void removePlayer(String regionName, Player sourcePlayer, Player playerToRemove) {
        if (RegionManager.get().containsRegion(regionName)) {
            String playerToRemoveName = playerToRemove.getName().getString();
            if (RegionManager.get().removePlayer(regionName, playerToRemove)) {
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.removeplayer", playerToRemoveName, regionName));
                sendMessage((ServerPlayer) playerToRemove, new TranslatableComponent("message.player.regionremoved", regionName));
            } else {
                // Player was not present in this region -> Message needed or silent acknowledgement?
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.unknownplayer", regionName, playerToRemoveName));
            }
        } else {
            sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.unknown", regionName));
        }
    }

    public static void removePlayers(String regionName, Player sourcePlayer, List<Player> playersToRemove){
        if (RegionManager.get().containsRegion(regionName)) {
            List<Player> removedPlayers = RegionManager.get().removePlayers(regionName, playersToRemove);
            List<String> playerNames = removedPlayers.stream()
                    .map(player -> player.getName().getString())
                    .collect(Collectors.toList());
            String playerString = String.join(", ", playerNames);
            if (!removedPlayers.isEmpty()) {
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.players.remove.multiple", playerString, regionName));
                removedPlayers.forEach(playerEntity -> sendMessage((ServerPlayer) playerEntity, new TranslatableComponent( "message.player.regionremoved", regionName)));
            } else {
                sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent( "message.players.remove.none", playerString));
            }
        } else {
            sendMessage((ServerPlayer) sourcePlayer, new TranslatableComponent("message.region.unknown", regionName));
        }
    }

    public static boolean hasNeededOpLevel(Player player) {
        ServerOpListEntry opPlayerEntry = ServerLifecycleHooks.getCurrentServer()
                .getPlayerList()
                .getOps()
                .get(player.getGameProfile());
        if (opPlayerEntry != null) {
            return opPlayerEntry.getLevel() >= ServerConfigBuilder.OP_COMMAND_PERMISSION_LEVEL.get();
        }
        return false;
    }

    public static void listPlayersInRegion(String regionName, Player player) {
        RegionManager.get().getRegion(regionName).ifPresent(region -> {
            // TODO: lang-key   "chat.header.region":"Players in Region '%s'"
            sendMessage((ServerPlayer) player, new TranslatableComponent("== Players in Region '" + regionName + "' ==").withStyle(ChatFormatting.BOLD));
            if (region.getPlayers().isEmpty()) {
                sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.info.noplayers"));
                return;
            }
            region.getPlayers().values().forEach(playerName -> {
                sendMessage((ServerPlayer) player, buildRemovePlayerLink(playerName, regionName));
            });
            sendMessage((ServerPlayer) player, new TextComponent(""));
        });
    }

    public static void listTeamsInRegion(String regionName, Player player) {
        RegionManager.get().getRegion(regionName).ifPresent(region -> {
            // TODO: lang-key   "chat.header.region":"Players in Region '%s'"
            sendMessage((ServerPlayer) player, new TranslatableComponent(ChatFormatting.BOLD + "== Teams in Region '" + regionName + "' =="));
            if (region.getTeams().isEmpty()) {
                sendMessage((ServerPlayer) player, new TranslatableComponent("message.region.info.noteams"));
                return;
            }
            region.getTeams().forEach(teamName -> {
                sendMessage((ServerPlayer) player, buildRemoveTeamLink(teamName, regionName));
            });
            sendMessage((ServerPlayer) player, new TextComponent(""));
        });
    }

    public static BaseComponent buildRemovePlayerLink(String playerName, String region) {
        String command =  "/" + WP_CMD + " " + Command.PLAYER + " " + Command.REMOVE_OFFLINE + " " + region + " " + playerName;
        return (BaseComponent) new TextComponent(" - ")
                // TODO: Langkey and overload method with translatableComponent
                .append(buildRunCommandLink("x", command, ChatFormatting.RED, "Remove player '" + playerName + "' from region " + "'" + region + "'"))
                .append(new TextComponent(" '" + playerName + "'"));
    }

    public static BaseComponent buildRemoveTeamLink(String teamName, String region) {
        String command =  "/" + WP_CMD + " " + Command.TEAM + " " + Command.REMOVE + " " + region + " " + teamName;
        return (BaseComponent) new TextComponent(" - ")
                // TODO: Langkey and overload method with translatableComponent
                .append(buildRunCommandLink("x", command, ChatFormatting.RED, "Remove team '" + teamName + "' from region " + "'" + region + "'"))
                .append(new TextComponent(" '" + teamName + "'"));
    }


    public static void removeTeam(String regionName, ServerPlayer sourcePlayer, Team team) {
        if (RegionManager.get().containsRegion(regionName)) {
            String teamToRemove = team.getName();
            if (RegionManager.get().removeTeam(regionName, teamToRemove)) {
                sendMessage(sourcePlayer, new TranslatableComponent("message.region.removeteam", teamToRemove, regionName));
            }
        } else {
            sendMessage(sourcePlayer, new TranslatableComponent("message.region.unknown", regionName));
        }
    }

    public static void addTeam(String regionName, ServerPlayer sourcePlayer, Team team) {
        if (RegionManager.get().containsRegion(regionName)) {
            String teamToAdd = team.getName();
            if (RegionManager.get().addTeam(regionName, teamToAdd)) {
                sendMessage(sourcePlayer, new TranslatableComponent("message.region.addteam", teamToAdd, regionName));
            }
        } else {
            sendMessage(sourcePlayer,  new TranslatableComponent("message.region.unknown", regionName));
        }
    }
}
