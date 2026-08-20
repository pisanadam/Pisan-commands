package dev.khaoscube.pisancommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;

public final class PisanCommandsMod implements ModInitializer {
    public static final String MOD_ID = "pisan_commands";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final PisanConfig CONFIG = new PisanConfig();

    @Override
    public void onInitialize() {
        CONFIG.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> register(dispatcher, buildContext));
        LOGGER.info("Pisan Commands Fabric loaded. Minecart max speed: {} blocks/s", CONFIG.getMinecartSpeed());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        registerEnchant(dispatcher, buildContext);
        registerRename(dispatcher);
        registerGive(dispatcher);
        registerSummon(dispatcher);
        registerMinecartSpeed(dispatcher);
        registerManagement(dispatcher);
    }

    private static void registerEnchant(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        var root = Commands.literal("pisanenchant").requires(PisanCommandsMod::isAdmin);
        for (String action : new String[]{"add", "edit", "auto"}) {
            root.then(Commands.literal(action)
                    .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                            .then(Commands.argument("level", IntegerArgumentType.integer(0, 32767))
                                    .executes(ctx -> enchant(ctx, action, true)))));
        }
        root.then(Commands.literal("remove")
                .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                        .executes(ctx -> enchant(ctx, "remove", false))));
        var node = dispatcher.register(root);
        dispatcher.register(Commands.literal("pe").requires(PisanCommandsMod::isAdmin).redirect(node));
        dispatcher.register(Commands.literal("penchant").requires(PisanCommandsMod::isAdmin).redirect(node));
    }

    @SuppressWarnings("unchecked")
    private static int enchant(CommandContext<CommandSourceStack> ctx, String action, boolean hasLevel) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            failure(ctx.getSource(), "Elinde bir item olmali.");
            return 0;
        }

        Holder.Reference<Enchantment> enchantment = (Holder.Reference<Enchantment>) ctx.getArgument("enchantment", Holder.Reference.class);
        int current = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        int level = hasLevel ? IntegerArgumentType.getInteger(ctx, "level") : current;

        switch (action) {
            case "add" -> {
                if (current > 0) {
                    failure(ctx.getSource(), "Bu buyu zaten var. /pisanenchant edit kullan.");
                    return 0;
                }
                EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(enchantment, level));
            }
            case "edit" -> {
                if (current <= 0) {
                    failure(ctx.getSource(), "Bu buyu itemde yok. /pisanenchant add kullan.");
                    return 0;
                }
                EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(enchantment, level));
            }
            case "auto" -> EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(enchantment, level));
            case "remove" -> EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(enchantment::equals));
            default -> {
                return 0;
            }
        }

        success(ctx.getSource(), "Enchant islemi tamamlandi: " + action + " (seviye " + level + ")");
        return 1;
    }

    private static void registerRename(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(Commands.literal("pisanrename").requires(PisanCommandsMod::isAdmin)
                .then(Commands.literal("reset").executes(PisanCommandsMod::resetName))
                .then(Commands.literal("edit")
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(PisanCommandsMod::rename))));
        dispatcher.register(Commands.literal("pr").requires(PisanCommandsMod::isAdmin).redirect(root));
        dispatcher.register(Commands.literal("pren").requires(PisanCommandsMod::isAdmin).redirect(root));
    }

    private static int rename(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            failure(ctx.getSource(), "Elinde bir item olmali.");
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").trim();
        if (name.length() > 256) name = name.substring(0, 256);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        success(ctx.getSource(), "Item adi degistirildi: " + name);
        return 1;
    }

    private static int resetName(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            failure(ctx.getSource(), "Elinde bir item olmali.");
            return 0;
        }
        stack.remove(DataComponents.CUSTOM_NAME);
        success(ctx.getSource(), "Item adi sifirlandi.");
        return 1;
    }

    private static void registerGive(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(Commands.literal("pisangive").requires(PisanCommandsMod::isAdmin)
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> forward(ctx, "give " + StringArgumentType.getString(ctx, "args")))));
        dispatcher.register(Commands.literal("pg").requires(PisanCommandsMod::isAdmin).redirect(root));
        dispatcher.register(Commands.literal("pgive").requires(PisanCommandsMod::isAdmin).redirect(root));
    }

    private static void registerSummon(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(Commands.literal("pisansummon").requires(PisanCommandsMod::isAdmin)
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> forward(ctx, "summon " + StringArgumentType.getString(ctx, "args")))));
        dispatcher.register(Commands.literal("ps").requires(PisanCommandsMod::isAdmin).redirect(root));
        dispatcher.register(Commands.literal("psummon").requires(PisanCommandsMod::isAdmin).redirect(root));
    }

    private static void registerMinecartSpeed(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(Commands.literal("pisanmaxminecartspeed").requires(PisanCommandsMod::isAdmin)
                .executes(ctx -> {
                    success(ctx.getSource(), "Minecart max hizi: " + format(CONFIG.getMinecartSpeed()) + " blok/s");
                    return 1;
                })
                .then(Commands.argument("blocks_per_second", DoubleArgumentType.doubleArg(0.05, 1000.0))
                        .executes(ctx -> {
                            double value = DoubleArgumentType.getDouble(ctx, "blocks_per_second");
                            CONFIG.setMinecartSpeed(value);
                            success(ctx.getSource(), "Minecart max hizi " + format(value) + " blok/s olarak kaydedildi.");
                            return 1;
                        })));
        dispatcher.register(Commands.literal("pmcs").requires(PisanCommandsMod::isAdmin).redirect(root));
        dispatcher.register(Commands.literal("pminecartspeed").requires(PisanCommandsMod::isAdmin).redirect(root));
    }

    private static void registerManagement(CommandDispatcher<CommandSourceStack> dispatcher) {
        var pisan = Commands.literal("pisan").requires(PisanCommandsMod::isAdmin)
                .executes(PisanCommandsMod::help)
                .then(Commands.literal("help").executes(PisanCommandsMod::help))
                .then(Commands.literal("save").executes(ctx -> forward(ctx, "save-all flush")))
                .then(Commands.literal("list").executes(ctx -> forward(ctx, "list")))
                .then(Commands.literal("day").executes(ctx -> forward(ctx, "time set day")))
                .then(Commands.literal("night").executes(ctx -> forward(ctx, "time set night")))
                .then(Commands.literal("clear").executes(ctx -> forward(ctx, "weather clear")))
                .then(Commands.literal("rain").executes(ctx -> forward(ctx, "weather rain")))
                .then(Commands.literal("thunder").executes(ctx -> forward(ctx, "weather thunder")))
                .then(forwardingLiteral("gm", "gamemode"))
                .then(forwardingLiteral("tp", "tp"))
                .then(forwardingLiteral("kick", "kick"))
                .then(forwardingLiteral("ban", "ban"))
                .then(forwardingLiteral("pardon", "pardon"))
                .then(forwardingLiteral("op", "op"))
                .then(forwardingLiteral("deop", "deop"))
                .then(forwardingLiteral("whitelist", "whitelist"))
                .then(forwardingLiteral("difficulty", "difficulty"))
                .then(forwardingLiteral("gamerule", "gamerule"))
                .then(forwardingLiteral("time", "time"))
                .then(forwardingLiteral("weather", "weather"))
                .then(Commands.literal("run")
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> forward(ctx, stripSlash(StringArgumentType.getString(ctx, "command"))))))
                .then(Commands.literal("heal")
                        .executes(ctx -> forward(ctx, "effect give @s minecraft:instant_health 1 4 true"))
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .executes(ctx -> forward(ctx, "effect give " + StringArgumentType.getString(ctx, "target") + " minecraft:instant_health 1 4 true"))))
                .then(Commands.literal("feed")
                        .executes(ctx -> forward(ctx, "effect give @s minecraft:saturation 1 10 true"))
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .executes(ctx -> forward(ctx, "effect give " + StringArgumentType.getString(ctx, "target") + " minecraft:saturation 1 10 true"))));
        dispatcher.register(pisan);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> forwardingLiteral(String literal, String vanilla) {
        return Commands.literal(literal)
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> forward(ctx, vanilla + " " + StringArgumentType.getString(ctx, "args"))));
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        String[] lines = {
                "Pisan Commands Fabric",
                "/pisanenchant <add|edit|auto|remove> <enchant> [level]",
                "/pisanrename <edit <isim>|reset>",
                "/pisangive <vanilla /give argumanlari>",
                "/pisansummon <vanilla /summon argumanlari>",
                "/pisanmaxminecartspeed [blok/s]",
                "/pisan day|night|clear|rain|thunder|save|list",
                "/pisan gm|tp|kick|ban|pardon|op|deop|whitelist|difficulty|gamerule ...",
                "/pisan heal [hedef] | /pisan feed [hedef] | /pisan run <komut>"
        };
        Arrays.stream(lines).forEach(line -> ctx.getSource().sendSuccess(() -> Component.literal(line), false));
        return 1;
    }

    private static int forward(CommandContext<CommandSourceStack> ctx, String command) throws CommandSyntaxException {
        String normalized = stripSlash(command.trim());
        if (normalized.isEmpty()) return 0;
        return ctx.getSource().getServer().getCommands().getDispatcher().execute(normalized, ctx.getSource());
    }

    private static boolean isAdmin(CommandSourceStack source) {
        try {
            CommandNode<CommandSourceStack> op = source.getServer().getCommands().getDispatcher().getRoot().getChild("op");
            return op != null && op.canUse(source);
        } catch (Throwable t) {
            LOGGER.warn("Yetki kontrolu yapilamadi", t);
            return false;
        }
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[Pisan] " + message), false);
    }

    private static void failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal("[Pisan] " + message));
    }

    private static String stripSlash(String command) {
        while (command.startsWith("/")) command = command.substring(1);
        return command;
    }

    private static String format(double value) {
        if (Math.rint(value) == value) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
