package dev.smto.moremechanics.item;

import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.util.RegistryHelper;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

public class SolidifiedExperienceItem extends Item implements PolymerItem, MoreMechanicsContent {
    private final Identifier id;

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.ITEM;
    }

    public SolidifiedExperienceItem(Identifier id) {
        super(new Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)).rarity(Rarity.COMMON));
        this.id = id;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.STICK;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user instanceof ServerPlayerEntity player) {
            player.addExperience(10);
            if (!player.isCreative()) player.getStackInHand(hand).decrement(1);
            player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                    RegistryHelper.getRegistryEntry(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, RegistryKeys.SOUND_EVENT),
                    SoundCategory.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    0.1F, 1.0F + world.random.nextFloat(), 0
            ));
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }
}
