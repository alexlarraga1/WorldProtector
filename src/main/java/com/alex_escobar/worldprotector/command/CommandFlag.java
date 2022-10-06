package com.alex_escobar.worldprotector.command;

import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.data.RegionManager;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.alex_escobar.worldprotector.utils.RegionFlagUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;

import static com.alex_escobar.worldprotector.utils.MessageUtils.sendMessage;

public class CommandFlag {

	private CommandFlag(){}

	public static final LiteralArgumentBuilder<CommandSourceStack> FLAG_COMMAND = register();

	public static LiteralArgumentBuilder<CommandSourceStack> register() {
		return Commands.literal(Command.FLAG.toString())
				.executes(ctx -> promptHelp(ctx.getSource()))
				.then(Commands.literal(Command.HELP.toString())
						.executes(ctx -> promptHelp(ctx.getSource())))
				.then(Commands.literal(Command.LIST.toString())
						.then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
								.executes(ctx -> promptFlagListForRegion(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString())))))
				.then(Commands.literal(Command.ADD.toString())
						.then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
								.then(Commands.argument(Command.FLAG.toString(), StringArgumentType.greedyString())
										.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionFlag.getFlags(), builder))
										.executes(ctx -> addFlags(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), StringArgumentType.getString(ctx, Command.FLAG.toString()))))))
				.then(Commands.literal(Command.REMOVE.toString())
						.then(Commands.argument(Command.REGION.toString(), StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getAllRegionNames(), builder))
								.then(Commands.argument(Command.FLAG.toString(), StringArgumentType.greedyString())
										.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RegionManager.get().getRegionFlags(ctx.getArgument(Command.REGION.toString(), String.class), ctx.getSource().getLevel().dimension()), builder))
										.executes(ctx -> removeFlags(ctx.getSource(), StringArgumentType.getString(ctx, Command.REGION.toString()), StringArgumentType.getString(ctx, Command.FLAG.toString()))))));
	}

	private static int promptHelp(CommandSourceStack source) {
		MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpHeader("help.flags.header"));
		MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.flags.1", Command.FLAG, Command.ADD));
		MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.flags.2", Command.FLAG, Command.REMOVE));
		MessageUtils.sendCmdFeedback(source, MessageUtils.buildHelpSuggestionLink("help.flags.3", Command.FLAG, Command.LIST));
		return 0;
	}

	private static int promptFlagListForRegion(CommandSourceStack source, String regionName) {
		if(RegionManager.get().containsRegion(regionName)) {
			RegionManager.get().getRegion(regionName).ifPresent(region -> {
				MessageUtils.sendCmdFeedback(source, new TranslatableComponent("== Flags in Region '" + regionName + "' ==").withStyle(ChatFormatting.BOLD));
				if (region.getFlags().isEmpty()) {
					MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.info.noflags"));
					return;
				}
				region.getFlags().forEach(flag -> {
				    MessageUtils.sendCmdFeedback(source, MessageUtils.buildRemoveFlagLink(flag, regionName));
				});
			});
		} else {
			MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
		}
		return 0;
	}

	private static int addFlag(CommandSourceStack source, String regionName, String flag) {
		if (RegionManager.get().containsRegion(regionName)) {
			RegionManager.get().getRegion(regionName).ifPresent((region) -> {
				if (RegionFlag.contains(flag)) {
					RegionFlag regionFlag = RegionFlag.fromString(flag)
							.orElseThrow(() -> new IllegalArgumentException("Flag could not be converted to enum counterpart"));
					switch (regionFlag) {
						case ALL:
							if (RegionManager.get().containsRegion(regionName)) {
								RegionManager.get().addFlags(regionName, RegionFlag.getFlags());
								MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.flags.add.all", regionName));
							} else {
								MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
							}
							break;
						case ANIMAL_UNMOUNTING:
							MessageUtils.sendCmdFeedback(source, new TextComponent("Unmounting flag is currently not working due to a minecraft vanilla bug. This bug is fixed in 1.17. See: https://bugs.mojang.com/browse/MC-202202."));
							break;
						default:
							if (RegionManager.get().addFlag(region, flag)) {
								MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.flags.add", flag, region.getName()));
							} else {
								MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.flags.add.duplicate", flag, region.getName()));
							}
							break;
					}
				} else {
					MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.flags.unknown", flag));
				}
			});
		} else {
			MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
		}
		return 0;
	}

	private static int addFlags(CommandSourceStack source, String regionName, String flags) {
		try {
			List<String> flagsList = Arrays.asList(flags.split(" "));
			if (flagsList.size() == 1) {
				return addFlag(source, regionName, flagsList.get(0));
			}
			ServerPlayer player = source.getPlayerOrException();
			if (RegionManager.get().containsRegion(regionName)) {
				RegionManager.get().getRegion(regionName).ifPresent((region) -> {
					RegionFlagUtils.addFlags(regionName, player, flagsList);
				});
			} else {
				sendMessage(player, new TranslatableComponent("message.region.unknown", regionName));
			}
		} catch (CommandSyntaxException | IllegalArgumentException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private static int removeFlags(CommandSourceStack source, String regionName, String flags) {
		try {
			List<String> flagsList = Arrays.asList(flags.split(" "));
			if (flagsList.size() == 1) {
				return removeFlag(source, regionName, flagsList.get(0));
			}
			ServerPlayer player = source.getPlayerOrException();
			if (RegionManager.get().containsRegion(regionName)) {
				RegionManager.get().getRegion(regionName).ifPresent((region) -> {
					RegionFlagUtils.removeFlags(regionName, player, flagsList);
				});
			} else {
				sendMessage(player, new TranslatableComponent("message.region.unknown", regionName));
			}
		} catch (CommandSyntaxException | IllegalArgumentException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private static int removeFlag(CommandSourceStack source, String regionName, String flag) {
		if (RegionManager.get().containsRegion(regionName)) {
			RegionManager.get().getRegion(regionName).ifPresent(region -> {
				if (RegionFlag.contains(flag)) {
					RegionFlag regionFlag = RegionFlag.fromString(flag)
							.orElseThrow(() -> new IllegalArgumentException("Flag could not be converted to enum counterpart"));
					switch (regionFlag) {
						case ALL:
							if (RegionManager.get().containsRegion(regionName)) {
								RegionManager.get().removeFlags(regionName, RegionFlag.getFlags());
								MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.flags.remove.all", regionName));
							} else {
								MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
							}
							break;
						case ANIMAL_UNMOUNTING:
							MessageUtils.sendCmdFeedback(source, new TextComponent("Unmounting flag is currently not working due to a minecraft vanilla bug. This bug is fixed in 1.17. See: https://bugs.mojang.com/browse/MC-202202."));
							break;
						default:
							if (RegionManager.get().removeFlag(region, flag)) {
								MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.flags.remove", flag, region.getName()));
							} else {
								MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.flags.remove.not-defined", flag, region.getName()));
							}
							break;
					}
				} else {
					MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.flags.unknown", flag));
				}
			});
		} else {
			MessageUtils.sendCmdFeedback(source, new TranslatableComponent("message.region.unknown", regionName));
		}
		return 0;
	}
}
