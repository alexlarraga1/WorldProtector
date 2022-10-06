package com.alex_escobar.worldprotector.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class CommandsRegister {

	private CommandsRegister(){}

	public static void init(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(CommandWorldProtector.register());
		commandDispatcher.register(CommandWorldProtector.registerAlternate1());
		commandDispatcher.register(CommandWorldProtector.registerAlternate2());
	}

}
