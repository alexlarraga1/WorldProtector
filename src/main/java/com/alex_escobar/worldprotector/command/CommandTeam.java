package com.alex_escobar.worldprotector.command;

import com.alex_escobar.worldprotector.data.RegionManager;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.alex_escobar.worldprotector.utils.RegionPlayerUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.scores.Team;

public class CommandTeam {

    public static final LiteralArgumentBuilder<CommandSourceStack> TEAM_COMMAND = register();

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal(Command.TEAM.toString())
                .executes(ctx -> giveHelp(ctx.getSource()))
                .then(Commands.literal(Command.HELP.toString())
                        .executes(ctx -> giveHelp(ctx.getSource())))
                .then(Commands.literal(Command.ADD.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.TEAM.toString(), TeamArgument.team())
                                        .executes(ctx -> addTeam(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), TeamArgument.getTeam(ctx, Command.TEAM.toString()))))))

                .then(Commands.literal(Command.REMOVE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.TEAM.toString(), TeamArgument.team())
                                        .executes(ctx -> removeTeam(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), TeamArgument.getTeam(ctx, Command.TEAM.toString()))))))

                .then(Commands.literal(Command.LIST.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))));
    }

    private static int removeTeam(CommandSourceStack source, String regionName, Team team) {
        if (RegionManager.get().containsRegion(regionName)) {
            String teamToRemove = team.getName();
            if (RegionManager.get().removeTeam(regionName, teamToRemove)) {
                MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.removeteam", teamToRemove, regionName));
            }
        } else {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
        }
        return 0;
    }

    private static int addTeam(CommandSourceStack source, String regionName, Team team) {
        if (RegionManager.get().containsRegion(regionName)) {
            String teamToAdd = team.getName();
            if (RegionManager.get().addTeam(regionName, teamToAdd)) {
                MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.addteam", teamToAdd, regionName));
            }
        } else {
            MessageUtils.sendCmdFeedback(source,  new TranslatableComponent("message.region.unknown", regionName));
        }
        return 0;
    }

    private static int list(CommandSourceStack source, String regionName) {
        RegionManager.get().getRegion(regionName).ifPresent(region -> {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("== Teams in Region '" + regionName + "' ==").withStyle(ChatFormatting.BOLD));
            if (region.getTeams().isEmpty()) {
                MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.info.noteams"));
                return;
            }
            region.getTeams().forEach(teamName -> {
                MessageUtils.sendCmdFeedback(source, RegionPlayerUtils.buildRemoveTeamLink(teamName, regionName));
            });
        });
        return 0;
    }

    private static int giveHelp(CommandSourceStack source) {
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpHeader("help.teams.header"));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.teams.1", Command.PLAYER, Command.ADD));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.teams.2", Command.PLAYER, Command.REMOVE));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.teams.3", Command.PLAYER, Command.LIST));
        return 0;
    }
}
