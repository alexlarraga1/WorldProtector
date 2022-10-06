package com.alex_escobar.worldprotector.utils;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.command.Command;
import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.data.RegionManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import static com.alex_escobar.worldprotector.config.ServerConfigBuilder.DEFAULT_REGION_PRIORITY_INC;
import static com.alex_escobar.worldprotector.config.ServerConfigBuilder.WP_CMD;
import static net.minecraft.network.chat.ComponentUtils.wrapInSquareBrackets;

public final class MessageUtils {

    private MessageUtils() {
    }

    public static void sendCmdFeedback(CommandSourceStack src, MutableComponent text) {
        try {
            if (src.getEntity() == null) {
                src.sendSuccess(text, true);
            } else {
                MessageUtils.sendMessage(src.getPlayerOrException(), text);
            }
        } catch (CommandSyntaxException e) {
            WorldProtector.LOGGER.error(e);
        }
    }

    public static void sendCmdFeedback(CommandSourceStack src, String langKey) {
        try {
            TranslatableComponent text = new TranslatableComponent(langKey);
            if (src.getEntity() == null) {
                src.sendSuccess(text, true);
            } else {
                MessageUtils.sendMessage(src.getPlayerOrException(), text);
            }
        } catch (CommandSyntaxException e) {
            WorldProtector.LOGGER.error(e);
        }
    }

    public static void sendMessage(ServerPlayer player, MutableComponent textComponent) {
        player.sendMessage(textComponent, ChatType.SYSTEM, Util.NIL_UUID);
    }

    public static void sendMessage(ServerPlayer player, String translationKey) {
        player.sendMessage(new TranslatableComponent(translationKey), ChatType.SYSTEM, Util.NIL_UUID);
    }

    public static void sendStatusMessage(ServerPlayer player, String translationKey) {
        player.sendMessage(new TranslatableComponent(translationKey), ChatType.SYSTEM, Util.NIL_UUID);
    }

    public static void sendStatusMessage(ServerPlayer player, MutableComponent textComponent) {
        player.sendMessage(textComponent, ChatType.SYSTEM, Util.NIL_UUID);
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }

    public static void sendRegionInfoCommand(CommandSourceStack src, String regionName) {
        RegionManager.get().getRegion(regionName).ifPresent(region -> {
            BlockPos target = region.getTpTarget();
            String regionInfoCommand = "/" + WP_CMD + " " + Command.REGION + " " + Command.INFO + " " + regionName;
            String regionTeleportCommand = "/tp @s " + target.getX() + " " + target.getY() + " " + target.getZ();

            MutableComponent regionMsg = new TextComponent("Region '")
                    .append(buildRunCommandLink(regionName, regionInfoCommand,
                            ChatFormatting.GREEN, "chat.link.hover.region.info"))
                    .append(new TextComponent("': ").setStyle(Style.EMPTY.withColor(ChatFormatting.RESET)))
                    .append(buildRunCommandLink(target.getX() + ", " + target.getY() + ", " + target.getZ(), regionTeleportCommand,
                            ChatFormatting.GREEN, "chat.link.hover.region.tp"));
            MessageUtils.sendCmdFeedback(src, regionMsg);
        });
    }

    public static MutableComponent buildRunCommandLink(String linkText, String command, ChatFormatting color, String hoverText) {
        return wrapInSquareBrackets(new TextComponent(linkText))
                .setStyle(Style.EMPTY.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent(hoverText))));
    }

    public static MutableComponent buildSuggestCommandLink(String linkText, String command, ChatFormatting color, String hoverText) {
        return wrapInSquareBrackets(new TextComponent(linkText))
                .setStyle(Style.EMPTY.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent(hoverText))));
    }

    public static MutableComponent buildDimensionTeleportLink(IRegion region) {
        BlockPos target = region.getTpTarget();
        String dim = region.getDimension().location().toString();
        String commandText = dim + "@ [" + target.getX() + ", " + target.getY() + ", " + target.getZ() + "]";
        String teleportCommand = "/execute in " + dim + " run tp @s " + target.getX() + " " + target.getY() + " " + target.getZ();
        return buildRunCommandLink(commandText, teleportCommand, ChatFormatting.GREEN, "chat.link.hover.region.tp");
    }

    public static MutableComponent buildRemoveFlagLink(String flag, String region) {
        String removeFlagCommand = "/" + WP_CMD + " " + Command.FLAG + " " + Command.REMOVE + " " + region + " " + flag;
        return new TextComponent(" - ")
                // TODO: lang-key
                .append(buildRunCommandLink("x", removeFlagCommand, ChatFormatting.RED, "Remove flag '" + flag + "'"))
                .append(new TextComponent(" '" + flag + "'"));
    }

    public static MutableComponent buildHelpSuggestionLink(String translationKey, Command baseCmd, Command cmd) {
        String command = "/" + WP_CMD + " " + baseCmd + " " + cmd + " ";
        return new TextComponent(" ")
                .append(buildSuggestCommandLink("=>", command, ChatFormatting.GREEN, "chat.link.hover.command.copy"))
                .append(new TextComponent(" "))
                .append(new TranslatableComponent(translationKey));
    }

    public static MutableComponent buildHelpLink(String translationKey, Command cmd) {
        String command = "/" + WP_CMD + " " + cmd.toString() + " " + Command.HELP;
        return new TextComponent(" ")
                .append(buildRunCommandLink("=>", command, ChatFormatting.GREEN, "Show detailed help for the " + cmd.toString() + " commands"))
                .append(new TextComponent(" "))
                .append(new TranslatableComponent(translationKey));
    }

    public static MutableComponent buildHelpHeader(String translationKey) {
        return new TextComponent(ChatFormatting.BOLD + " == ")
                .append(new TranslatableComponent(translationKey).setStyle(Style.EMPTY.withBold(true)))
                .append(new TextComponent(ChatFormatting.BOLD + " == "));
    }

    public static MutableComponent buildHelpHeader(TranslatableComponent TranslatableComponent) {
        return new TextComponent(ChatFormatting.BOLD + " == ")
                .append(TranslatableComponent)
                .append(new TextComponent(ChatFormatting.BOLD + " == "));
    }

    // TODO: add overloading with lang-key
    public static MutableComponent buildFlagListLink(IRegion region) {
        String command = "/" + WP_CMD + " " + Command.FLAG + " " + Command.LIST + " " + region.getName();
        return new TextComponent(" ")
                .append(buildRunCommandLink(region.getFlags().size() + " flag(s)", command,
                        ChatFormatting.AQUA, "List flags in region '" + region.getName() + "'"));
    }

    // TODO: add overloading with lang-key
    public static MutableComponent buildAddFlagLink(String regionName) {
        String command = "/" + WP_CMD + " " + Command.FLAG + " " + Command.ADD + " " + regionName + " ";
        return new TextComponent(" ").append(buildSuggestCommandLink("+", command,
                ChatFormatting.GREEN, "Add new flag to region '" + regionName + "'"));
    }

    // TODO: add overloading with lang-key
    public static MutableComponent buildPlayerListLink(IRegion region) {
        String command = "/" + WP_CMD + " " + Command.PLAYER + " " + Command.LIST + " " + region.getName();
        return new TextComponent(" ")
                .append(buildRunCommandLink(region.getPlayers().size() + " player(s)", command,
                        ChatFormatting.AQUA, "List players in region '" + region.getName() + "'"));
    }

    public static MutableComponent buildTeamsListLink(IRegion region) {
        String command = "/" + WP_CMD + " " + Command.TEAM + " " + Command.LIST + " " + region.getName();
        return new TextComponent(" ")
                .append(buildRunCommandLink(region.getTeams().size() + " team(s)", command,
                        ChatFormatting.AQUA, "List teams in region '" + region.getName() + "'"));
    }

    // TODO: add overloading with lang-key
    public static MutableComponent buildAddPlayerLink(String regionName) {
        String command = "/" + WP_CMD + " " + Command.PLAYER + " " + Command.ADD + " " + regionName + " ";
        return new TextComponent(" ").append(buildSuggestCommandLink("+", command,
                ChatFormatting.GREEN, "Add new player to region '" + regionName + "'"));
    }

    public static MutableComponent buildAddTeamLink(String regionName) {
        String command = "/" + WP_CMD + " " + Command.TEAM + " " + Command.ADD + " " + regionName + " ";
        return new TextComponent(" ").append(buildSuggestCommandLink("+", command,
                ChatFormatting.GREEN, "Add new team to region '" + regionName + "'"));
    }

    // TODO: lang-keys
    public static MutableComponent buildRegionPriorityInfoLink(String regionName, int regionPriority) {
        String baseCommand = "/" + WP_CMD + " " + Command.REGION + " " + Command.SET_PRIORITY + " " + regionName + " ";
        String setCommand = baseCommand + regionPriority;
        String incrementCommand = baseCommand + (regionPriority + DEFAULT_REGION_PRIORITY_INC.get());
        String decrementCommand = baseCommand + (regionPriority - DEFAULT_REGION_PRIORITY_INC.get());
        return new TranslatableComponent("message.region.info.priority", regionPriority)
                .append(new TextComponent(" "))
                .append(buildSuggestCommandLink("#", setCommand,
                        ChatFormatting.GREEN, "Set new priority for region '" + regionName + "'"))
                .append(new TextComponent(" "))
                .append(buildRunCommandLink("+", incrementCommand,
                        ChatFormatting.GREEN, "Increment region priority by " + DEFAULT_REGION_PRIORITY_INC.get()))
                .append(new TextComponent(" "))
                .append(buildRunCommandLink("-", decrementCommand,
                        ChatFormatting.RED, "Decrement region priority by " + DEFAULT_REGION_PRIORITY_INC.get()));
    }

    // TODO: lang key
    public static MutableComponent buildRegionInfoLink(String regionName) {
        String command = "/" + WP_CMD + " " + Command.REGION + " " + Command.INFO + " " + regionName;
        return buildRunCommandLink(regionName, command, ChatFormatting.GREEN,
                "Show region info for region '" + regionName + "'");
    }

    public static MutableComponent buildRegionMuteLink(IRegion region) {
        boolean isMuted = region.isMuted();
        TranslatableComponent linkText = isMuted
                ? new TranslatableComponent("message.region.info.muted.true")
                : new TranslatableComponent("message.region.info.muted.false");
        ChatFormatting color = isMuted
                ? ChatFormatting.RED
                : ChatFormatting.GREEN;
        String onClickAction = isMuted ? "unmute" : "mute";
        String command = "/" + WP_CMD + " " + Command.REGION + " " + onClickAction + " " + region.getName();
        // TODO: translatable overload (linkText) ?
        return buildRunCommandLink(linkText.getString(), command, color, onClickAction + " " + Command.REGION + " '" + region.getName() + "'");
    }

    public static MutableComponent buildRegionActiveLink(IRegion region) {
        boolean isActive = region.isActive();
        TranslatableComponent activeText = isActive
                ? new TranslatableComponent("message.region.info.active.true")
                : new TranslatableComponent("message.region.info.active.false");
        ChatFormatting color = isActive
                ? ChatFormatting.GREEN
                : ChatFormatting.RED;
        String onClickAction = isActive ? "deactivate" : "activate";
        String command = "/" + WP_CMD + " " + Command.REGION + " " + onClickAction + " " + region.getName();
        // TODO: translatable overload (linkText) ?
        return buildRunCommandLink(activeText.getString(), command, color, onClickAction + " " + Command.REGION + " '" + region.getName() + "'");
    }
}
