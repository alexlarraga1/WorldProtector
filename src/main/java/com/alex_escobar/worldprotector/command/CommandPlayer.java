package com.alex_escobar.worldprotector.command;

import com.alex_escobar.worldprotector.data.RegionManager;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.alex_escobar.worldprotector.utils.PlayerUtils;
import com.alex_escobar.worldprotector.utils.RegionPlayerUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.alex_escobar.worldprotector.utils.MessageUtils.sendCmdFeedback;
import static com.alex_escobar.worldprotector.utils.MessageUtils.sendMessage;

public class CommandPlayer {

    public static final LiteralArgumentBuilder<CommandSourceStack> PLAYER_COMMAND = register();

    // TODO: test adding/removing multiple players
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal(Command.PLAYER.toString())
                .executes(ctx -> giveHelp(ctx.getSource()))
                .then(Commands.literal(Command.HELP.toString())
                        .executes(ctx -> giveHelp(ctx.getSource())))
                .then(Commands.literal(Command.ADD.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.PLAYER.toString(), EntityArgument.players())
                                        .executes(ctx -> addPlayers(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), EntityArgument.getPlayers(ctx, Command.PLAYER.toString()))))))
                .then(Commands.literal(Command.ADD.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.PLAYER.toString(), EntityArgument.player())
                                        .executes(ctx -> addPlayer(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), EntityArgument.getPlayer(ctx, Command.PLAYER.toString()))))))
                // add offline player
                .then(Commands.literal(Command.ADD_OFFLINE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.PLAYER.toString(), StringArgumentType.word())
                                        .executes(ctx -> addOfflinePlayer(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), StringArgumentType.getString(ctx, Command.PLAYER.toString()))))))
                .then(Commands.literal(Command.REMOVE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.PLAYER.toString(), EntityArgument.players())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getRegionPlayers(ctx.getArgument(Command.REGION.toString(), String.class)), builder))
                                        .executes(ctx -> removePlayers(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), EntityArgument.getPlayers(ctx, Command.PLAYER.toString()))))))
                .then(Commands.literal(Command.REMOVE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.PLAYER.toString(), EntityArgument.player())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getRegionPlayers(ctx.getArgument(Command.REGION.toString(), String.class)), builder))
                                        .executes(ctx -> removePlayer(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), EntityArgument.getPlayer(ctx, Command.PLAYER.toString()))))))
                // remove offline player
                .then(Commands.literal(Command.REMOVE_OFFLINE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.PLAYER.toString(), StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getRegionPlayers(ctx.getArgument(Command.REGION.toString(), String.class)), builder))
                                        .executes(ctx -> removeOfflinePlayer(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), StringArgumentType.getString(ctx, Command.PLAYER.toString()))))))
                .then(Commands.literal(Command.LIST.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))));
    }

    private static int removePlayer(CommandSourceStack src, String regionName, ServerPlayer player) {
        if (RegionManager.get().containsRegion(regionName)) {
            String playerToRemoveName = player.getName().getString();
            if (RegionManager.get().removePlayer(regionName, player)) {
                sendMessage(player, new TranslatableComponent("message.region.removeplayer", playerToRemoveName, regionName));
                src.sendSuccess(new TranslatableComponent("message.player.regionremoved", regionName), true);
            } else {
                // Player was not present in this region -> Message needed or silent acknowledgement?
                src.sendFailure(new TranslatableComponent("message.region.unknownplayer", playerToRemoveName, regionName));
                return -2;
            }
        } else {
            src.sendFailure(new TranslatableComponent("message.region.unknown", regionName));
            return -1;
        }
        return 0;
    }

    private static int removeOfflinePlayer(CommandSourceStack source, String regionName, String playerName) {
        try {
            RegionPlayerUtils.removeOfflinePlayer(regionName, source.getPlayerOrException(), playerName);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int removePlayers(CommandSourceStack source, String regionName, Collection<ServerPlayer> players) {
        try {
            List<Player> playerList = players.stream().map(player -> (Player) player).collect(Collectors.toList());
            RegionPlayerUtils.removePlayers(regionName, source.getPlayerOrException(), playerList);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int addPlayer(CommandSourceStack src, String regionName, ServerPlayer player) {
        if (RegionManager.get().containsRegion(regionName)) {
            String playerToAddName = player.getName().getString();
            if (RegionManager.get().addPlayer(regionName, player)) {
                sendMessage(player, new TranslatableComponent("message.player.regionadded", regionName));
                sendCmdFeedback(src, new TranslatableComponent("message.region.addplayer", playerToAddName, regionName));
            } else {
                sendCmdFeedback(src, new TranslatableComponent("message.region.errorplayer", playerToAddName, regionName));
                return -2;
            }
        } else {
            sendCmdFeedback(src, new TranslatableComponent("message.region.unknown", regionName));
            return -1;
        }
        return 0;
    }

    private static int addOfflinePlayer(CommandSourceStack source, String regionName, String playerName) {
        try {
            PlayerUtils.MCPlayerInfo playerInfo = PlayerUtils.queryPlayerUUIDByName(playerName);
            if (playerInfo == null) {
                sendMessage(source.getPlayerOrException(), new TranslatableComponent("message.region.playernotexisting", playerName));
                return -1;
            }
            RegionPlayerUtils.addOfflinePlayer(regionName, source.getPlayerOrException(), playerInfo);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int addPlayers(CommandSourceStack source, String regionName, Collection<ServerPlayer> players) {
        try {
            List<Player> playerList = players.stream().map(player -> (Player) player).collect(Collectors.toList());
            RegionPlayerUtils.addPlayers(regionName, source.getPlayerOrException(), playerList);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int list(CommandSourceStack source, String regionName) {
        RegionManager.get().getRegion(regionName).ifPresent(region -> {
            // TODO: lang-key   "chat.header.region":"Players in Region '%s'"
            sendCmdFeedback(source, new TranslatableComponent("== Players in Region '" + regionName + "' ==").withStyle(ChatFormatting.BOLD));
            if (region.getPlayers().isEmpty()) {
                sendCmdFeedback(source, new TranslatableComponent("message.region.info.noplayers"));
                return;
            }
            region.getPlayers().values().forEach(playerName -> {
                sendCmdFeedback(source, RegionPlayerUtils.buildRemovePlayerLink(playerName, regionName));
            });
        });
        return 0;
    }

    private static int giveHelp(CommandSourceStack source) {
        sendCmdFeedback(source, MessageUtils.buildHelpHeader("help.players.header"));
        sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.players.1", Command.PLAYER, Command.ADD));
        sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.players.2", Command.PLAYER, Command.REMOVE));
        sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.players.3", Command.PLAYER, Command.ADD_OFFLINE));
        sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.players.4", Command.PLAYER, Command.REMOVE_OFFLINE));
        sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.players.5", Command.PLAYER, Command.LIST));
        return 0;
    }

}