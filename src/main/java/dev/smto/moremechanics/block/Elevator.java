package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.api.PlayerMovementListener;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.SimplePolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import xyz.nucleoid.packettweaker.PacketContext;

public class Elevator extends SimplePolymerBlock implements PlayerMovementListener, PolymerTexturedBlock, MoreMechanicsContent {
    private final BlockState polymerBlockState;
    private final Identifier id;

    public Elevator(Identifier id) {
        super(Settings.copy(Blocks.SEA_LANTERN).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)), Blocks.SEA_LANTERN);
        this.polymerBlockState = PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(Identifier.of(MoreMechanics.MOD_ID, "block/elevator")));
        this.id = id;
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.BLOCK;
    }

    private Pair<Boolean, Float> checkBlock(BlockState state) {
        if (state.isAir()) {
            return new Pair<>(true, 0.0f);
        }
        if (state.isIn(BlockTags.SNOW) || state.isIn(BlockTags.PRESSURE_PLATES)) {
            return new Pair<>(true, 0.0f);
        }
        if (state.isIn(BlockTags.SLABS)) {
            return new Pair<>(true, 0.5f);
        }
        if (state.isIn(BlockTags.WOOL_CARPETS)) {
            return new Pair<>(true, 0.075f);
        }
        return new Pair<>(false, 0.0f);
    }

    @Override
    public boolean onJump(BlockPos position, ServerPlayerEntity player)
    {
        position = position.up();
        ServerWorld world = (ServerWorld) player.getWorld();
        int count = 0;

        while(count < 128 && position.getY() < 316)
        {
            Block blk = world.getBlockState(position).getBlock();

            if(blk instanceof Elevator)
            {
                BlockPos tpPosition = position.up();

                var check = this.checkBlock(world.getBlockState(tpPosition));
                if(check.getLeft() && world.getBlockState(tpPosition.up()).isAir())
                {
                    player.teleport(tpPosition.getX() + 0.5f, tpPosition.getY() + check.getRight(), tpPosition.getZ() + 0.5f, true);

                    world.playSound(null, tpPosition, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 1.5f );
                    world.spawnParticles(ParticleTypes.POOF, tpPosition.getX() + 0.5f, tpPosition.getY(), tpPosition.getZ() + 0.5f, 5, 0, 0, 0, 0.25f );

                    return true;
                }
            }
            position = position.up();
            count++;
        }
        return false;
    }

    @Override
    public void onCrouch(BlockPos position, ServerPlayerEntity player)
    {
        position = position.down();
        ServerWorld world = (ServerWorld) player.getWorld();
        int count = 0;

        while(count < 128 && position.getY() >= -64)
        {
            Block blk = world.getBlockState(position).getBlock();

            if(blk instanceof Elevator)
            {
                BlockPos tpPosition = position.up();

                var check = this.checkBlock(world.getBlockState(tpPosition));
                if(check.getLeft() && world.getBlockState(tpPosition.up()).isAir())
                {
                    player.teleport( tpPosition.getX() + 0.5f, tpPosition.getY() + check.getRight(), tpPosition.getZ() + 0.5f, true );

                    world.playSound( null, tpPosition, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 1.5f );
                    world.spawnParticles( ParticleTypes.POOF, tpPosition.getX() + 0.5f, tpPosition.getY(), tpPosition.getZ() + 0.5f, 5, 0, 0, 0, 0.25f );

                    return;
                }
            }
            position = position.down();
            count++;
        }
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(MoreMechanics.Blocks.ELEVATOR);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.polymerBlockState;
    }
}