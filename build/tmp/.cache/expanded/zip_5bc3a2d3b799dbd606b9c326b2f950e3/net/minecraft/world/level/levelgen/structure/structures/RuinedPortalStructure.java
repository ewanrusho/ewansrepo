package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class RuinedPortalStructure extends Structure {
    private static final String[] STRUCTURE_LOCATION_PORTALS = new String[]{
        "ruined_portal/portal_1",
        "ruined_portal/portal_2",
        "ruined_portal/portal_3",
        "ruined_portal/portal_4",
        "ruined_portal/portal_5",
        "ruined_portal/portal_6",
        "ruined_portal/portal_7",
        "ruined_portal/portal_8",
        "ruined_portal/portal_9",
        "ruined_portal/portal_10"
    };
    private static final String[] STRUCTURE_LOCATION_GIANT_PORTALS = new String[]{"ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"};
    private static final float PROBABILITY_OF_GIANT_PORTAL = 0.05F;
    private static final int MIN_Y_INDEX = 15;
    private final List<RuinedPortalStructure.Setup> setups;
    public static final MapCodec<RuinedPortalStructure> CODEC = RecordCodecBuilder.mapCodec(
        p_229304_ -> p_229304_.group(
                    settingsCodec(p_229304_),
                    ExtraCodecs.nonEmptyList(RuinedPortalStructure.Setup.CODEC.listOf()).fieldOf("setups").forGetter(p_229299_ -> p_229299_.setups)
                )
                .apply(p_229304_, RuinedPortalStructure::new)
    );

    public RuinedPortalStructure(Structure.StructureSettings pSettings, List<RuinedPortalStructure.Setup> pSetups) {
        super(pSettings);
        this.setups = pSetups;
    }

    public RuinedPortalStructure(Structure.StructureSettings pSettings, RuinedPortalStructure.Setup pSetup) {
        this(pSettings, List.of(pSetup));
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229285_) {
        RuinedPortalPiece.Properties ruinedportalpiece$properties = new RuinedPortalPiece.Properties();
        WorldgenRandom worldgenrandom = p_229285_.random();
        RuinedPortalStructure.Setup ruinedportalstructure$setup = null;
        if (this.setups.size() > 1) {
            float f = 0.0F;

            for (RuinedPortalStructure.Setup ruinedportalstructure$setup1 : this.setups) {
                f += ruinedportalstructure$setup1.weight();
            }

            float f1 = worldgenrandom.nextFloat();

            for (RuinedPortalStructure.Setup ruinedportalstructure$setup2 : this.setups) {
                f1 -= ruinedportalstructure$setup2.weight() / f;
                if (f1 < 0.0F) {
                    ruinedportalstructure$setup = ruinedportalstructure$setup2;
                    break;
                }
            }
        } else {
            ruinedportalstructure$setup = this.setups.get(0);
        }

        if (ruinedportalstructure$setup == null) {
            throw new IllegalStateException();
        } else {
            RuinedPortalStructure.Setup ruinedportalstructure$setup3 = ruinedportalstructure$setup;
            ruinedportalpiece$properties.airPocket = sample(worldgenrandom, ruinedportalstructure$setup3.airPocketProbability());
            ruinedportalpiece$properties.mossiness = ruinedportalstructure$setup3.mossiness();
            ruinedportalpiece$properties.overgrown = ruinedportalstructure$setup3.overgrown();
            ruinedportalpiece$properties.vines = ruinedportalstructure$setup3.vines();
            ruinedportalpiece$properties.replaceWithBlackstone = ruinedportalstructure$setup3.replaceWithBlackstone();
            ResourceLocation resourcelocation;
            if (worldgenrandom.nextFloat() < 0.05F) {
                resourcelocation = ResourceLocation.withDefaultNamespace(STRUCTURE_LOCATION_GIANT_PORTALS[worldgenrandom.nextInt(STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
            } else {
                resourcelocation = ResourceLocation.withDefaultNamespace(STRUCTURE_LOCATION_PORTALS[worldgenrandom.nextInt(STRUCTURE_LOCATION_PORTALS.length)]);
            }

            StructureTemplate structuretemplate = p_229285_.structureTemplateManager().getOrCreate(resourcelocation);
            Rotation rotation = Util.getRandom(Rotation.values(), worldgenrandom);
            Mirror mirror = worldgenrandom.nextFloat() < 0.5F ? Mirror.NONE : Mirror.FRONT_BACK;
            BlockPos blockpos = new BlockPos(structuretemplate.getSize().getX() / 2, 0, structuretemplate.getSize().getZ() / 2);
            ChunkGenerator chunkgenerator = p_229285_.chunkGenerator();
            LevelHeightAccessor levelheightaccessor = p_229285_.heightAccessor();
            RandomState randomstate = p_229285_.randomState();
            BlockPos blockpos1 = p_229285_.chunkPos().getWorldPosition();
            BoundingBox boundingbox = structuretemplate.getBoundingBox(blockpos1, rotation, blockpos, mirror);
            BlockPos blockpos2 = boundingbox.getCenter();
            int i = chunkgenerator.getBaseHeight(
                    blockpos2.getX(),
                    blockpos2.getZ(),
                    RuinedPortalPiece.getHeightMapType(ruinedportalstructure$setup3.placement()),
                    levelheightaccessor,
                    randomstate
                )
                - 1;
            int j = findSuitableY(
                worldgenrandom,
                chunkgenerator,
                ruinedportalstructure$setup3.placement(),
                ruinedportalpiece$properties.airPocket,
                i,
                boundingbox.getYSpan(),
                boundingbox,
                levelheightaccessor,
                randomstate
            );
            BlockPos blockpos3 = new BlockPos(blockpos1.getX(), j, blockpos1.getZ());
            return Optional.of(
                new Structure.GenerationStub(
                    blockpos3,
                    p_229297_ -> {
                        if (ruinedportalstructure$setup3.canBeCold()) {
                            ruinedportalpiece$properties.cold = isCold(
                                blockpos3,
                                p_229285_.chunkGenerator()
                                    .getBiomeSource()
                                    .getNoiseBiome(
                                        QuartPos.fromBlock(blockpos3.getX()),
                                        QuartPos.fromBlock(blockpos3.getY()),
                                        QuartPos.fromBlock(blockpos3.getZ()),
                                        randomstate.sampler()
                                    ),
                                chunkgenerator.getSeaLevel()
                            );
                        }

                        p_229297_.addPiece(
                            new RuinedPortalPiece(
                                p_229285_.structureTemplateManager(),
                                blockpos3,
                                ruinedportalstructure$setup3.placement(),
                                ruinedportalpiece$properties,
                                resourcelocation,
                                structuretemplate,
                                rotation,
                                mirror,
                                blockpos
                            )
                        );
                    }
                )
            );
        }
    }

    private static boolean sample(WorldgenRandom pRandom, float pThreshold) {
        if (pThreshold == 0.0F) {
            return false;
        } else {
            return pThreshold == 1.0F ? true : pRandom.nextFloat() < pThreshold;
        }
    }

    private static boolean isCold(BlockPos pPos, Holder<Biome> pBiome, int pSeaLevel) {
        return pBiome.value().coldEnoughToSnow(pPos, pSeaLevel);
    }

    private static int findSuitableY(
        RandomSource pRandom,
        ChunkGenerator pChunkGenerator,
        RuinedPortalPiece.VerticalPlacement pVerticalPlacement,
        boolean pAirPocket,
        int pHeight,
        int pBlockCountY,
        BoundingBox pBox,
        LevelHeightAccessor pLevel,
        RandomState pRandomState
    ) {
        int j = pLevel.getMinY() + 15;
        int i;
        if (pVerticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_NETHER) {
            if (pAirPocket) {
                i = Mth.randomBetweenInclusive(pRandom, 32, 100);
            } else if (pRandom.nextFloat() < 0.5F) {
                i = Mth.randomBetweenInclusive(pRandom, 27, 29);
            } else {
                i = Mth.randomBetweenInclusive(pRandom, 29, 100);
            }
        } else if (pVerticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_MOUNTAIN) {
            int k = pHeight - pBlockCountY;
            i = getRandomWithinInterval(pRandom, 70, k);
        } else if (pVerticalPlacement == RuinedPortalPiece.VerticalPlacement.UNDERGROUND) {
            int j1 = pHeight - pBlockCountY;
            i = getRandomWithinInterval(pRandom, j, j1);
        } else if (pVerticalPlacement == RuinedPortalPiece.VerticalPlacement.PARTLY_BURIED) {
            i = pHeight - pBlockCountY + Mth.randomBetweenInclusive(pRandom, 2, 8);
        } else {
            i = pHeight;
        }

        List<BlockPos> list1 = ImmutableList.of(
            new BlockPos(pBox.minX(), 0, pBox.minZ()),
            new BlockPos(pBox.maxX(), 0, pBox.minZ()),
            new BlockPos(pBox.minX(), 0, pBox.maxZ()),
            new BlockPos(pBox.maxX(), 0, pBox.maxZ())
        );
        List<NoiseColumn> list = list1.stream()
            .map(p_229280_ -> pChunkGenerator.getBaseColumn(p_229280_.getX(), p_229280_.getZ(), pLevel, pRandomState))
            .collect(Collectors.toList());
        Heightmap.Types heightmap$types = pVerticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR
            ? Heightmap.Types.OCEAN_FLOOR_WG
            : Heightmap.Types.WORLD_SURFACE_WG;

        int l;
        for (l = i; l > j; l--) {
            int i1 = 0;

            for (NoiseColumn noisecolumn : list) {
                BlockState blockstate = noisecolumn.getBlock(l);
                if (heightmap$types.isOpaque().test(blockstate)) {
                    if (++i1 == 3) {
                        return l;
                    }
                }
            }
        }

        return l;
    }

    private static int getRandomWithinInterval(RandomSource pRandom, int pMin, int pMax) {
        return pMin < pMax ? Mth.randomBetweenInclusive(pRandom, pMin, pMax) : pMax;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.RUINED_PORTAL;
    }

    public static record Setup(
        RuinedPortalPiece.VerticalPlacement placement,
        float airPocketProbability,
        float mossiness,
        boolean overgrown,
        boolean vines,
        boolean canBeCold,
        boolean replaceWithBlackstone,
        float weight
    ) {
        public static final Codec<RuinedPortalStructure.Setup> CODEC = RecordCodecBuilder.create(
            p_229327_ -> p_229327_.group(
                        RuinedPortalPiece.VerticalPlacement.CODEC.fieldOf("placement").forGetter(RuinedPortalStructure.Setup::placement),
                        Codec.floatRange(0.0F, 1.0F).fieldOf("air_pocket_probability").forGetter(RuinedPortalStructure.Setup::airPocketProbability),
                        Codec.floatRange(0.0F, 1.0F).fieldOf("mossiness").forGetter(RuinedPortalStructure.Setup::mossiness),
                        Codec.BOOL.fieldOf("overgrown").forGetter(RuinedPortalStructure.Setup::overgrown),
                        Codec.BOOL.fieldOf("vines").forGetter(RuinedPortalStructure.Setup::vines),
                        Codec.BOOL.fieldOf("can_be_cold").forGetter(RuinedPortalStructure.Setup::canBeCold),
                        Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(RuinedPortalStructure.Setup::replaceWithBlackstone),
                        ExtraCodecs.POSITIVE_FLOAT.fieldOf("weight").forGetter(RuinedPortalStructure.Setup::weight)
                    )
                    .apply(p_229327_, RuinedPortalStructure.Setup::new)
        );
    }
}