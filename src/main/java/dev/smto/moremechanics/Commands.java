package dev.smto.moremechanics;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static CommandDispatcher<ServerCommandSource> CACHED_DISPATCHER = new CommandDispatcher<ServerCommandSource>();
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        Commands.CACHED_DISPATCHER = dispatcher;
        dispatcher.register(literal(MoreMechanics.MOD_ID)
            .executes(context -> {
                context.getSource().sendFeedback(() -> Text.translatable("command.moremechanics.generic_no_argument").formatted(Formatting.GOLD), false);
                return 1;
            })
            .then(literal("reload")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        MoreMechanics.CONFIG_MANAGER.read();
                        context.getSource().sendFeedback(() -> Text.literal("Config reloaded.").formatted(Formatting.GOLD), false);
                        return 1;
                    })
            )
            .then(literal("config")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.translatable("command.moremechanics.config.no_argument"), false);
                        return 0;
                    })
                    .then(argument("key", string()).executes(context -> {
                        var key = getString(context, "key");
                        var value = MoreMechanics.CONFIG_MANAGER.toMap().getOrDefault(key, "");
                        context.getSource().sendFeedback(() -> Text.translatable("command.moremechanics.config.get").append(Text.literal(key)).append(Text.literal(" -> ").append(Text.literal(value))), false);
                        return 0;
                    }).suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(MoreMechanics.CONFIG_MANAGER.getKeys(), suggestionsBuilder))
                    .then(argument("value", string()).executes(context -> {
                        var key = getString(context, "key");
                        var value = getString(context, "value");
                        if (MoreMechanics.CONFIG_MANAGER.trySet(key, value)) {
                            MoreMechanics.CONFIG_MANAGER.write();
                            context.getSource().sendFeedback(() -> Text.translatable("command.moremechanics.config.set").append(Text.literal(key)).append(Text.literal(" -> ").append(Text.literal(value))), false);
                            return 0;
                        }
                        else
                            context.getSource().sendFeedback(() -> Text.translatable("command.moremechanics.config.error").formatted(Formatting.RED), false);
                        return 1;
                    }).suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(List.of(MoreMechanics.CONFIG_MANAGER.toMap().getOrDefault(commandContext.getArgument("key", String.class), "")), suggestionsBuilder))
                    ))
            )
        );
    }
}
