package com.alex_escobar.worldprotector.command;

import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.Region;
import com.alex_escobar.worldprotector.data.RegionManager;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.alex_escobar.worldprotector.utils.RegionUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.alex_escobar.worldprotector.utils.MessageUtils.*;

public class CommandRegion {

    public static final LiteralArgumentBuilder<CommandSourceStack> REGION_COMMAND = register();

    private CommandRegion() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> dimensionArgument =
                Commands.argument(Command.DIMENSION.toString(), DimensionArgument.dimension());

        return Commands.literal(Command.REGION.toString())
                .executes(ctx -> promptHelp(ctx.getSource()))
                .then(Commands.literal(Command.HELP.toString())
                        .executes(ctx -> promptHelp(ctx.getSource())))
                .then(Commands.literal(Command.LIST.toString())
                        .executes(ctx -> promptRegionList(ctx.getSource()))
                        .then(Commands.argument(Command.DIMENSION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(getQuotedDimensionList(ctx.getSource().getLevel()), builder))
                                .executes(ctx -> promptRegionListForDim(ctx.getSource(), StringArgumentType.getString(ctx, Command.DIMENSION.toString())))))
                .then(Commands.literal(Command.INFO.toString())
                        .executes(ctx -> listRegionsAround(ctx.getSource()))
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> promptInfo(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
                .then(Commands.literal(Command.DEFINE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .executes(ctx -> define(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))
                                .then(Commands.argument(Command.START_POS.toString(), BlockPosArgument.blockPos())
                                        .then(Commands.argument(Command.END_POS.toString(), BlockPosArgument.blockPos())
                                                .executes(ctx ->
                                                        defineManually(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, Command.REGION.toString()),
                                                                BlockPosArgument.getLoadedBlockPos(ctx, Command.START_POS.toString()),
                                                                BlockPosArgument.getLoadedBlockPos(ctx, Command.END_POS.toString()),
                                                                ctx.getSource().getLevel().dimension()))
                                                .then(dimensionArgument
                                                        .executes(ctx ->
                                                                defineManually(ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, Command.REGION.toString()),
                                                                        BlockPosArgument.getLoadedBlockPos(ctx, Command.START_POS.toString()),
                                                                        BlockPosArgument.getLoadedBlockPos(ctx, Command.END_POS.toString()),
                                                                        getDimensionFromArgument(ctx))))))))
                .then(Commands.literal(Command.REDEFINE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> redefine(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
                .then(Commands.literal(Command.REMOVE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()))))
                        .then(Commands.literal(Command.ALL.toString()).executes(ctx -> removeAll(ctx.getSource()))))
                .then(Commands.literal(Command.TELEPORT.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> teleport(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
                .then(Commands.literal(Command.TELEPORT_SHORT.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> teleport(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
                .then(Commands.literal(Command.ACTIVATE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> activeRegion(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()))))
                        .then(Commands.literal(Command.ALL.toString()).executes(ctx -> activateAll(ctx.getSource()))))
                .then(Commands.literal(Command.ACTIVATE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> activeRegion(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
                .then(Commands.literal(Command.DEACTIVATE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> deactivateRegion(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
                .then(Commands.literal(Command.DEACTIVATE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> deactivateRegion(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()))))
                        .then(Commands.literal(Command.ALL.toString()).executes(ctx -> deactivateAll(ctx.getSource()))))
                .then(Commands.literal(Command.MUTE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> muteRegion(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
                .then(Commands.literal(Command.UNMUTE.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .executes(ctx -> unmuteRegion(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
                .then(Commands.literal(Command.SET_PRIORITY.toString())
                        .then(Commands.argument(Command.REGION.toString(), StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
                                .then(Commands.argument(Command.PRIORITY.toString(), IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                        .executes(ctx -> setPriority(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), IntegerArgumentType.getInteger(ctx, Command.PRIORITY.toString()))))));
    }

    private static ResourceKey<Level> getDimensionFromArgument(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return DimensionArgument.getDimension(ctx, Command.DIMENSION.toString()).dimension();
    }

    private static int promptInfo(CommandSourceStack source, String regionName) {
        if (RegionManager.get().containsRegion(regionName)) {
            RegionManager.get().getRegion(regionName).ifPresent(region -> {

                BaseComponent regionInfoHeader = (BaseComponent) new TextComponent("== Region ").withStyle(ChatFormatting.BOLD)
                        .append(buildRegionInfoLink(regionName))
                        .append(new TextComponent(" information ==").withStyle(ChatFormatting.BOLD));
                MessageUtils.sendCmdFeedback(source, regionInfoHeader);

                BaseComponent regionTeleportMessage = (BaseComponent) new TranslatableComponent("message.region.info.teleport")
                        .append(buildDimensionTeleportLink(region));
                MessageUtils.sendCmdFeedback(source, regionTeleportMessage);

                // Region area: ...
                MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.info.area", region.getArea().toString().substring(4)));

                // Region priority: n [#] [+] [-]
                int regionPriority = region.getPriority();
                MessageUtils.sendCmdFeedback(source, buildRegionPriorityInfoLink(regionName, regionPriority));

                // TODO: Link for clear players
                // Region players: [n player(s)] [+]
                BaseComponent regionPlayerMessage = new TranslatableComponent("message.region.info.players",
                        region.getPlayers().isEmpty()
                                ? new TranslatableComponent("message.region.info.noplayers").getString()
                                : buildPlayerListLink(region));
                regionPlayerMessage.append(buildAddPlayerLink(regionName));
                MessageUtils.sendCmdFeedback(source, regionPlayerMessage);

                // Region teams: [n teams(s)] [+]
                BaseComponent regionTeamMessage = new TranslatableComponent("message.region.info.teams",
                        region.getTeams().isEmpty()
                                ? new TranslatableComponent("message.region.info.noteams").getString()
                                : buildTeamsListLink(region));
                regionTeamMessage.append(buildAddTeamLink(regionName));
                MessageUtils.sendCmdFeedback(source, regionTeamMessage);


                // TODO: Link for clear flags
                // Region flags: [n flag(s)] [+]
                BaseComponent regionFlagMessage = new TranslatableComponent("message.region.info.flags",
                        region.getFlags().isEmpty()
                                ? new TranslatableComponent("message.region.info.noflags").getString()
                                : buildFlagListLink(region));
                regionFlagMessage.append(buildAddFlagLink(regionName));
                MessageUtils.sendCmdFeedback(source, regionFlagMessage);

                // Region state: [activated], [unmuted]
                MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.info.active", buildRegionActiveLink(region), buildRegionMuteLink(region)));
                MessageUtils.sendCmdFeedback(source, new TextComponent(""));
            });
        } else {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
        }
        return 0;
    }

    private static int activeRegion(CommandSourceStack source, String regionName) {
        if (RegionManager.get().setActiveState(regionName, true)) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent( "message.region.activate", regionName));
        } else {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
        }
        return 0;
    }

    // TODO: deactivate and activate all in specified dimension
    private static int activateAll(CommandSourceStack source) {
        List<IRegion> deactiveRegions = RegionManager.get().getAllRegions().stream()
                .filter(region -> !region.isActive())
                .collect(Collectors.toList());
        deactiveRegions.forEach(region -> region.setIsActive(true));
        RegionManager.get().setDirty();
        List<String> activatedRegions = deactiveRegions.stream()
                .map(IRegion::getName)
                .collect(Collectors.toList());
        String regionString = String.join(", ", activatedRegions);
        if (!activatedRegions.isEmpty()) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.activate.multiple", regionString));
        } else {
            MessageUtils.sendCmdFeedback(source,"message.region.activate.none");
        }
        return 0;
    }

    private static int deactivateRegion(CommandSourceStack source, String regionName) {
        if (RegionManager.get().setActiveState(regionName, false)) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent( "message.region.deactivate", regionName));
        } else {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
        }
        return 0;
    }

    // TODO: deactivate and activate all in specified dimension
    private static int deactivateAll(CommandSourceStack source) {
        List<IRegion> activeRegions = RegionManager.get().getAllRegions().stream()
                .filter(IRegion::isActive)
                .collect(Collectors.toList());
        activeRegions.forEach(region -> region.setIsActive(false));
        RegionManager.get().setDirty();
        List<String> deactivatedRegions = activeRegions.stream()
                .map(IRegion::getName)
                .collect(Collectors.toList());
        String regionString = String.join(", ", deactivatedRegions);
        if (!deactivatedRegions.isEmpty()) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.deactivate.multiple", regionString));
        } else {
            MessageUtils.sendCmdFeedback(source, "message.region.deactivate.none");
        }
        return 0;
    }

    private static int muteRegion(CommandSourceStack source, String regionName) {
        if (RegionManager.get().setMutedState(regionName, true)) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.mute", regionName));
        } else {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
        }
        return 0;
    }

    private static int unmuteRegion(CommandSourceStack source, String regionName) {
        if (RegionManager.get().setMutedState(regionName, false)) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unmute", regionName));
        } else {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
        }
        return 0;
    }

    private static int promptHelp(CommandSourceStack source) {
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpHeader("help.region.header"));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.1", Command.REGION, Command.DEFINE));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.2", Command.REGION, Command.REDEFINE));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.3", Command.REGION, Command.REMOVE));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.4", Command.REGION, Command.LIST));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.5", Command.REGION, Command.INFO));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.6", Command.REGION, Command.SET_PRIORITY));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.7", Command.REGION, Command.TELEPORT_SHORT));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.8", Command.REGION, Command.DEACTIVATE));
        MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.region.9", Command.REGION,  Command.MUTE));
        return 0;
    }

    private static int promptRegionList(CommandSourceStack source) {
        Collection<IRegion> regions = RegionManager.get().getAllRegionsSorted();
        if (regions.isEmpty()) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent( "message.region.info.no_regions"));
            return 0;
        }
        regions.forEach(region -> {
            BaseComponent regionTeleportMessage = (BaseComponent) new TranslatableComponent("message.region.list.entry", region.getName())
                    .append(buildDimensionTeleportLink(region));
            MessageUtils.sendCmdFeedback(source, regionTeleportMessage);
        });
        return 0;
    }

    private static int promptRegionListForDim(CommandSourceStack source, String dim) {
        List<IRegion> regionsForDim = RegionManager.get().getAllRegions()
                .stream()
                .filter(region -> region.getDimension().location().toString().equals(dim))
                .sorted(Comparator.comparing(IRegion::getName))
                .collect(Collectors.toList());
        if (regionsForDim.isEmpty()) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.info.regions_for_dim", dim));
            return 0;
        }
        regionsForDim.forEach(region -> {
            BaseComponent regionTeleportMessage = (BaseComponent) new TranslatableComponent("message.region.list.entry", region.getName())
                    .append(buildDimensionTeleportLink(region));
            MessageUtils.sendCmdFeedback(source, regionTeleportMessage);
        });
        return 0;
    }

    private static int define(CommandSourceStack source, String regionName) {
        try {
            RegionUtils.createRegion(regionName, source.getPlayerOrException(), source.getPlayerOrException().getMainHandItem());
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int defineManually(CommandSourceStack source, String regionName, BlockPos start, BlockPos end, ResourceKey<Level> dim) {
        if (regionName.contains(" ")) { // region contains whitespace
            MessageUtils.sendCmdFeedback(source, "message.region.define.error");
            return -1;
        }
        // TODO: How does this work with server console? which dim?
        Region region = new Region(regionName, new AABB(start, end), dim);
        RegionManager.get().addRegion(region);
        MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.define", regionName));
        return 0;
    }

    private static int redefine(CommandSourceStack source, String regionName) {
        try {
            RegionUtils.redefineRegion(regionName, source.getPlayerOrException(), source.getPlayerOrException().getMainHandItem());
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int remove(CommandSourceStack source, String regionName) {
        if (RegionManager.get().removeRegion(regionName) != null) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.remove", regionName));
        } else {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
        }
        return 0;
    }

    private static int removeAll(CommandSourceStack source) {
        RegionManager.get().clearRegions();
        MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.removeall"));
        return 0;
    }

    private static int listRegionsAround(CommandSourceStack source) {
        BlockPos sourcePos = new BlockPos(source.getPosition().x, source.getPosition().y, source.getPosition().z);
        Collection<String> regions = RegionUtils.getRegionsAroundPos(sourcePos, source.getLevel() ,source.getLevel().dimension());
        if (regions.isEmpty()) {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.list.no-found"));
            return 0;
        }
        MessageUtils.sendCmdFeedback(source, new TranslatableComponent("== Regions around [" + sourcePos.getX() + ", " + sourcePos.getY() + ", " + sourcePos.getZ() + "] ==").withStyle(ChatFormatting.BOLD));
        regions.forEach(regionName -> sendRegionInfoCommand(source, regionName));
        return 0;
    }

    private static int teleport(CommandSourceStack source, String regionName) {
        try {
            // TODO: change tp to require a player argument
            RegionUtils.teleportToRegion(regionName, source.getPlayerOrException(), source);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int setPriority(CommandSourceStack source, String regionName, int priority) {
        if (RegionManager.get().containsRegion(regionName)) {
            RegionManager.get().getRegion(regionName).ifPresent(region -> {
                region.setPriority(priority);
                MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.setpriority", priority, regionName));
            });
            // TODO: make marking private in RegionManager
            RegionManager.get().setDirty();
        } else {
            MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown"));
        }
        return 0;
    }

    private static Collection<String> getQuotedDimensionList(Level world) {
        return RegionUtils.getDimensionList(world).stream()
                .map(dim -> "'" + dim + "'")
                .collect(Collectors.toList());
    }
}
