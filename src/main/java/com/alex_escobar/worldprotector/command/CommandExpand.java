package com.alex_escobar.worldprotector.command;

import com.alex_escobar.worldprotector.utils.ExpandUtils;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CommandExpand {

    private CommandExpand() {
    }

    public static final int MAX_Y_LEVEL = 255;
    public static final int MIN_Y_LEVEL = 0;

    public static final LiteralArgumentBuilder<CommandSourceStack> EXPAND_COMMAND = register();

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal(Command.EXPAND.toString())
                .executes(ctx -> giveHelp(ctx.getSource()))
                .then(Commands.literal(Command.HELP.toString())
                        .executes(ctx -> giveHelp(ctx.getSource())))
                .then(Commands.literal(Command.VERT.toString())
                        .executes(ctx -> giveHelp(ctx.getSource()))
                        .then(Commands.argument(Command.Y1.toString(), IntegerArgumentType.integer(MIN_Y_LEVEL, MAX_Y_LEVEL))
                                .then(Commands.argument(Command.Y2.toString(), IntegerArgumentType.integer(MIN_Y_LEVEL, MAX_Y_LEVEL))
                                        .executes(ctx -> vert(ctx.getSource(), ctx.getArgument(Command.Y1.toString(), Integer.class), ctx.getArgument(Command.Y2.toString(), Integer.class))))))
                .then(Commands.literal(Command.DEFAULT_Y.toString())
                        .then(Commands.argument(Command.Y1.toString(), IntegerArgumentType.integer(MIN_Y_LEVEL, MAX_Y_LEVEL))
                                .then(Commands.argument(Command.Y2.toString(), IntegerArgumentType.integer(MIN_Y_LEVEL, MAX_Y_LEVEL))
                                        .executes(ctx -> setDefaultYExpansion(ctx.getSource(), ctx.getArgument(Command.Y1.toString(), Integer.class), ctx.getArgument(Command.Y2.toString(), Integer.class))))))
                .then(Commands.literal(Command.VERT.toString())
                        .executes(ctx -> vert(ctx.getSource(), MIN_Y_LEVEL, MAX_Y_LEVEL)));
    }

    public static int giveHelp(CommandSourceStack source) {
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpHeader("help.expand.header"));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.expand.1", Command.EXPAND, Command.VERT));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.expand.2", Command.EXPAND, Command.DEFAULT_Y));
        return 0;
    }

    public static int vert(CommandSourceStack source, int y1, int y2) {
        try {
            ItemStack item = source.getPlayerOrException().getMainHandItem();
            int yLow = Integer.min(y1, y2);
            int yHigh = Integer.max(y1, y2);
            ExpandUtils.expandVert(source.getPlayerOrException(), item, yLow, yHigh);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int setDefaultYExpansion(CommandSourceStack source, int y1, int y2) {
        try {
            Player player = source.getPlayerOrException();
            int yLow = Integer.min(y1, y2);
            int yHigh = Integer.max(y1, y2);
            ExpandUtils.setDefaultYLevels(player, yLow, yHigh);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        return 0;
    }

}
