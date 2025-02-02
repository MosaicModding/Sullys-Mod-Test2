package com.uraneptus.sullysmod.common.blocks;

import com.uraneptus.sullysmod.common.blockentities.AmberBE;
import com.uraneptus.sullysmod.common.caps.SMEntityCap;
import com.uraneptus.sullysmod.common.networking.MsgEntityAmberStuck;
import com.uraneptus.sullysmod.common.networking.SMPacketHandler;
import com.uraneptus.sullysmod.core.other.tags.SMBlockTags;
import com.uraneptus.sullysmod.core.registry.SMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class AmberBlock extends BaseEntityBlock {

    private static final VoxelShape MELTING_COLLISION_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.0F, 1.0D);

    public AmberBlock(Properties pProperties) {
        super(pProperties);
    }


    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);

        if (blockEntity instanceof AmberBE amber) {
            if (pContext instanceof EntityCollisionContext entitycollisioncontext) {
                Entity entity = entitycollisioncontext.getEntity();
                Level level = blockEntity.getLevel();
                if (!amber.hasStuckEntity()) {
                    if (entity != null) {
                        boolean shouldMeltFlag = false;
                        amber.setBlockMelted(false);

                        for (BlockPos pos : BlockPos.betweenClosed(pPos.offset(-1, -1, -1), pPos.offset(1, 1, 1))) {
                            BlockState state = pLevel.getBlockState(pos);
                            BlockEntity be = pLevel.getBlockEntity(pos);
                            if (state.is(SMBlockTags.MELTS_AMBER)) {
                                shouldMeltFlag = true;
                            }
                            if (state.getBlock() instanceof AmberBlock && be instanceof AmberBE amberBE) {
                                if (amberBE.isBlockMelted() && level != null && level.getBrightness(LightLayer.BLOCK, pPos.above()) >= 9) {
                                    shouldMeltFlag = true;
                                }
                            }
                        }
                        for (BlockPos pos : BlockPos.betweenClosed(pPos.offset(0, -1, 0), pPos.offset(0, -2, 0))) {
                            BlockState state = pLevel.getBlockState(pos);
                            BlockEntity be = pLevel.getBlockEntity(pos);
                            if (state.is(SMBlocks.AMBER.get())) {
                                if (be instanceof AmberBE amberBE && amberBE.hasStuckEntity()) {
                                    CompoundTag compoundtag = amberBE.getEntityStuck();
                                    if (level != null) {
                                        LivingEntity livingEntity = (LivingEntity) EntityType.loadEntityRecursive(compoundtag, level, entityStuck -> entityStuck);
                                        if (livingEntity != null) {
                                            if (livingEntity.getBoundingBox().getYsize() > 1.5F && livingEntity.getBoundingBox().getYsize() < 2F && pos.equals(pPos.offset(0, -1, 0))) {
                                                shouldMeltFlag = false;
                                            }
                                            else if (livingEntity.getBoundingBox().getYsize() >= 2F && livingEntity.getBoundingBox().getYsize() < 3.5F && pos.equals(pPos.offset(0, -2, 0))) {
                                                shouldMeltFlag = false;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (shouldMeltFlag) {
                            if (entity instanceof Player player) {
                                if (player.jumping) {
                                    amber.setBlockMelted(false);
                                    return Shapes.block();
                                }
                            }

                            amber.setBlockMelted(true);
                            amber.update();
                            return MELTING_COLLISION_SHAPE;
                        }
                    }
                } else {
                    amber.setBlockMelted(false);
                }
            }
        }
        return Shapes.block();
    }

    public void onRemove(BlockState blockState, Level pLevel, BlockPos blockPos, BlockState pNewState, boolean pIsMoving) {
        if (blockState.getBlock() == SMBlocks.AMBER.get()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(blockPos);
            if (blockEntity instanceof AmberBE amberBlockEntity) {
                if (amberBlockEntity.hasStuckEntity()) {
                    CompoundTag compoundtag = amberBlockEntity.getEntityStuck();
                    AmberBE.removeIgnoredNBT(compoundtag);
                    LivingEntity livingEntity = (LivingEntity) EntityType.loadEntityRecursive(compoundtag, pLevel, entity -> entity);
                    if (livingEntity != null) {
                        livingEntity.moveTo(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                        pLevel.addFreshEntity(livingEntity);
                    }
                }
                for (BlockPos pos : BlockPos.betweenClosed(blockPos.offset(0, -1, 0), blockPos.offset(0, -2, 0))) {
                    BlockState state = pLevel.getBlockState(pos);
                    BlockEntity be = pLevel.getBlockEntity(pos);
                    if (state.is(SMBlocks.AMBER.get())) {
                        if (be instanceof AmberBE amberBE && amberBE.hasStuckEntity()) {
                            CompoundTag compoundtag = amberBE.getEntityStuck();
                            AmberBE.removeIgnoredNBT(compoundtag);
                            LivingEntity livingEntity = (LivingEntity) EntityType.loadEntityRecursive(compoundtag, pLevel, entityStuck -> entityStuck);
                            if (livingEntity != null) {
                                if (livingEntity.getBoundingBox().getYsize() > 1.5F && livingEntity.getBoundingBox().getYsize() < 2F && pos.equals(blockPos.offset(0, -1, 0))) {
                                    livingEntity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                    pLevel.addFreshEntity(livingEntity);
                                    amberBE.setStuckEntityData(null);
                                    amberBE.setBlockMelted(false);
                                }
                                else if (livingEntity.getBoundingBox().getYsize() >= 2F && livingEntity.getBoundingBox().getYsize() < 3.5F) {
                                    livingEntity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                    pLevel.addFreshEntity(livingEntity);
                                    amberBE.setStuckEntityData(null);
                                    amberBE.setBlockMelted(false);
                                }
                                SMPacketHandler.sendMsg(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> livingEntity), new MsgEntityAmberStuck(livingEntity, false));
                                SMEntityCap.getCapOptional(livingEntity).ifPresent(cap -> {
                                    cap.stuckInAmber = false;
                                });
                            }
                        }
                    }
                }
            }
        }
        super.onRemove(blockState, pLevel, blockPos, pNewState, pIsMoving);
    }

    @Override
    public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof AmberBE amber) {
            if (!amber.hasStuckEntity() && amber.isBlockMelted()) {
                if (!(pEntity instanceof LivingEntity) || pEntity.getFeetBlockState().is(this)) {
                    if (pEntity instanceof Player) {
                        pEntity.makeStuckInBlock(pState, new Vec3(0.8F, 0.1D, 0.8F));
                    } else if (pEntity instanceof Mob mob) {
                        if (mob.getBoundingBox().getYsize() < 1.5F) {
                            mob.makeStuckInBlock(pState, new Vec3(0F, 0.1D, 0F));
                            if (mob.onGround()) {
                                amber.makeEntityStuck(mob);
                            }
                        }
                        else if (mob.getBoundingBox().getYsize() < 2F) {
                            if (pLevel.getBlockState(new BlockPos(mob.getBlockX(), mob.getBlockY() + 1, mob.getBlockZ())).is(SMBlocks.AMBER.get())) {
                                mob.makeStuckInBlock(pState, new Vec3(0F, 0.1D, 0F));
                                if (mob.onGround()) {
                                    amber.makeEntityStuck(mob);
                                }
                            } else {
                                mob.makeStuckInBlock(pState, new Vec3(0.8F, 0.1D, 0.8F));
                            }
                        }
                        else if (mob.getBoundingBox().getYsize() < 3.5F) {
                            if (pLevel.getBlockState(new BlockPos(mob.getBlockX(), mob.getBlockY() + 1, mob.getBlockZ())).is(SMBlocks.AMBER.get()) && pLevel.getBlockState(new BlockPos(mob.getBlockX(), mob.getBlockY() + 2, mob.getBlockZ())).is(SMBlocks.AMBER.get())) {
                                mob.makeStuckInBlock(pState, new Vec3(0F, 0.1D, 0F));
                                if (mob.onGround()) {
                                    amber.makeEntityStuck(mob);
                                }
                            } else {
                                mob.makeStuckInBlock(pState, new Vec3(0.8F, 0.1D, 0.8F));
                            }
                        }
                        else {
                            mob.makeStuckInBlock(pState, new Vec3(0.8F, 0.1D, 0.8F));
                        }
                    }
                    if (pLevel.isClientSide) {
                        RandomSource randomsource = pLevel.getRandom();
                        boolean flag = pEntity.xOld != pEntity.getX() || pEntity.zOld != pEntity.getZ();
                        if (flag && randomsource.nextBoolean()) {
                            pLevel.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pEntity.getX(), pPos.getY() + 1, pEntity.getZ(), Mth.randomBetween(randomsource, -1.0F, 1.0F) * 0.083333336F, 0.05F, Mth.randomBetween(randomsource, -1.0F, 1.0F) * 0.083333336F);
                        }
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return (level1, pos, state1, tile) -> ((AmberBE) tile).tick();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new AmberBE(pPos, pState);
    }
}
