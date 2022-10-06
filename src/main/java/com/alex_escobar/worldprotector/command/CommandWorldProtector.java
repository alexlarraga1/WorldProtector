package com.alex_escobar.worldprotector.command;

import com.alex_escobar.worldprotector.config.ServerConfigBuilder;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;

public class CommandWorldProtector {

    private CommandWorldProtector() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return withSubCommands(Commands.literal(Command.WP.toString()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerAlternate1() {
        return withSubCommands(Commands.literal(Command.W_P.toString()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerAlternate2() {
        return withSubCommands(Commands.literal(Command.WP_LONG.toString()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> withSubCommands(LiteralArgumentBuilder<CommandSourceStack> baseCommand) {
        return baseCommand
                .requires(cs -> cs.hasPermission(ServerConfigBuilder.OP_COMMAND_PERMISSION_LEVEL.get()))
                .executes(ctx -> giveHelp(ctx.getSource()))
                .then(Commands.literal(Command.HELP.toString())
                        .executes(ctx -> giveHelp(ctx.getSource())))
                .then(CommandRegion.REGION_COMMAND)
                .then(CommandExpand.EXPAND_COMMAND)
                .then(CommandFlag.FLAG_COMMAND)
                .then(CommandTeam.TEAM_COMMAND)
                .then(CommandPlayer.PLAYER_COMMAND);
    }

    private static int giveHelp(CommandSourceStack source) {
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpHeader("help.wp.header"));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpLink("help.wp.1", Command.EXPAND));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpLink("help.wp.4", Command.REGION));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpLink("help.wp.2", Command.FLAG));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpLink("help.wp.3", Command.PLAYER));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpLink("help.wp.6", Command.TEAM));
        MessageUtils.sendCmdFeedback(source, new TranslatableComponent("help.wp.5"));
        return 0;
    }
}
