package com.uraneptus.sullysmod.common.levelgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PetrifiedTreeFeature extends Feature<PetrifiedTreeConfig> {

    public PetrifiedTreeFeature(Codec<PetrifiedTreeConfig> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<PetrifiedTreeConfig> context) {
        //TODO add sus gravel loot table
        //TODO add amber entities in nbt
        //TODO variants don't seem to work yet
        PetrifiedTreeConfig config = context.config();
        RandomSource random = context.random();
        WorldGenLevel level = context.level();

        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos blockPos = origin.mutable();

        int amount = config.structures().size();
        ResourceLocation structure = config.structures().get(amount <= 1 ? 0 : random.nextInt(amount - 1));
        StructureTemplateManager templateManager = level.getLevel().getServer().getStructureManager();
        StructureTemplate template = templateManager.getOrCreate(structure);
        ChunkPos chunkPos = new ChunkPos(blockPos);
        BoundingBox boundingbox = new BoundingBox(chunkPos.getMinBlockX() - 16, level.getMinBuildHeight(), chunkPos.getMinBlockZ() - 16, chunkPos.getMaxBlockX() + 16, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 16);

        StructurePlaceSettings placeSettings = new StructurePlaceSettings().setBoundingBox(boundingbox).setRandom(random);
        Vec3i size = template.getSize();
        BlockPos centerPos = blockPos.offset(-size.getX() / 2, 0, -size.getZ() / 2);
        BlockPos offsetPos = template.getZeroPositionWithTransform(centerPos.atY(blockPos.getY()), Mirror.NONE, Rotation.NONE);

        return template.placeInWorld(level, offsetPos, offsetPos, placeSettings, random, Block.UPDATE_ALL);
    }
}
