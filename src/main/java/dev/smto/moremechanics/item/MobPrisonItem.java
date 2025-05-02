package dev.smto.moremechanics.item;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.function.Consumer;

public class MobPrisonItem extends Item implements PolymerItem, MoreMechanicsContent {
    private final Identifier id;
    private final Identifier model = MoreMechanics.id("mob_prison");
    public MobPrisonItem(Identifier id) {
        super(new Settings().maxDamage(8).rarity(Rarity.COMMON).registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        this.id = id;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        if (itemStack.contains(MoreMechanics.DataComponentTypes.STORED_ENTITY)) {
            return MoreMechanics.id(this.model.getPath() + "_full");
        }
        return this.model;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("item.moremechanics.mob_prison");
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        return ActionResult.PASS;
    }

    private final List<EntityType<?>> DISALLOWED_ENTITIES = List.of(
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.ELDER_GUARDIAN
    );

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (hand == Hand.OFF_HAND) return ActionResult.PASS;
        if (stack.contains(MoreMechanics.DataComponentTypes.STORED_ENTITY)) {
            user.sendMessage(Text.translatable("item.moremechanics.mob_prison.use.full").formatted(Formatting.RED), true);
            return ActionResult.PASS;
        }
        if (this.DISALLOWED_ENTITIES.contains(entity.getType()) || !(entity instanceof MobEntity)) {
            user.sendMessage(Text.translatable("item.moremechanics.mob_prison.use.disallowed").formatted(Formatting.RED), true);
            return ActionResult.PASS;
        }
        var entityData = new NbtCompound();
        entity.writeNbt(entityData);
        entityData.putString("mobId", EntityType.getId(entity.getType()).toString());
        var nStack = stack.copy();
        nStack.set(MoreMechanics.DataComponentTypes.STORED_ENTITY, NbtComponent.of(entityData));
        entity.discard();
        // stupid workaround around possible polymer bug, the stack does not update properly otherwise (or maybe i am doing something wrong)
        user.getInventory().setStack(user.getInventory().getSelectedSlot(), nStack);
        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null) return ActionResult.PASS;
        var user = context.getPlayer();
        if (!context.getStack().contains(MoreMechanics.DataComponentTypes.STORED_ENTITY)) {
             context.getPlayer().sendMessage(Text.translatable("item.moremechanics.mob_prison.use.empty").formatted(Formatting.RED), true);
            return ActionResult.PASS;
        }
        var stack = context.getStack().copy();
        var entityData = stack.get(MoreMechanics.DataComponentTypes.STORED_ENTITY).copyNbt();
        if (!entityData.contains("mobId")) {
            stack.remove(MoreMechanics.DataComponentTypes.STORED_ENTITY);
            user.getInventory().setStack(user.getInventory().getSelectedSlot(), stack);
            return ActionResult.PASS;
        }
        var ent = EntityType.get(entityData.getString("mobId").get()).orElseThrow().create(context.getWorld(), SpawnReason.MOB_SUMMONED);
        if (ent == null) {
            stack.remove(MoreMechanics.DataComponentTypes.STORED_ENTITY);
            user.getInventory().setStack(user.getInventory().getSelectedSlot(), stack);
            return ActionResult.PASS;
        }
        // read data first
        ent.readNbt(entityData);
        // then modify it
        ent.setPosition(Vec3d.of(context.getBlockPos().offset(context.getSide())).add(0.5, 0, 0.5));
        // and spawn it
        context.getWorld().spawnEntity(ent);
        stack.remove(MoreMechanics.DataComponentTypes.STORED_ENTITY);
        stack.damage(1, (ServerWorld) context.getWorld(), (ServerPlayerEntity) context.getPlayer(), item -> user.getInventory().setStack(user.getInventory().getSelectedSlot(), ItemStack.EMPTY));
        user.getInventory().setStack(user.getInventory().getSelectedSlot(), stack);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.translatable("item.moremechanics.mob_prison.description").formatted(MoreMechanics.getTooltipFormatting()));
        if (stack.contains(MoreMechanics.DataComponentTypes.STORED_ENTITY)) {
            try {
                textConsumer.accept(Text.translatable("item.moremechanics.mob_prison.description.full").formatted(Formatting.BLUE).append(Text.literal(" ")).append(EntityType.get(stack.get(MoreMechanics.DataComponentTypes.STORED_ENTITY).copyNbt().getString("mobId").get()).orElseThrow().getName()).formatted(Formatting.BLUE));
            } catch (Throwable ignored) {
                textConsumer.accept(Text.literal("Possibly invalid data!").formatted(Formatting.ITALIC, Formatting.RED));
            }
        } else textConsumer.accept(Text.translatable("item.moremechanics.mob_prison.description.empty").formatted(Formatting.BLUE));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.contains(MoreMechanics.DataComponentTypes.STORED_ENTITY);
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.ITEM;
    }
}
