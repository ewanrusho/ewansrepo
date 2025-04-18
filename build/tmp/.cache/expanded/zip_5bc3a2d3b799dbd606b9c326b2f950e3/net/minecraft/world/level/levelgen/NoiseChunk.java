package net.minecraft.world.level.levelgen;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;

public class NoiseChunk implements DensityFunction.ContextProvider, DensityFunction.FunctionContext {
    private final NoiseSettings noiseSettings;
    final int cellCountXZ;
    final int cellCountY;
    final int cellNoiseMinY;
    private final int firstCellX;
    private final int firstCellZ;
    final int firstNoiseX;
    final int firstNoiseZ;
    final List<NoiseChunk.NoiseInterpolator> interpolators;
    final List<NoiseChunk.CacheAllInCell> cellCaches;
    private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();
    private final Long2IntMap preliminarySurfaceLevel = new Long2IntOpenHashMap();
    private final Aquifer aquifer;
    private final DensityFunction initialDensityNoJaggedness;
    private final NoiseChunk.BlockStateFiller blockStateRule;
    private final Blender blender;
    private final NoiseChunk.FlatCache blendAlpha;
    private final NoiseChunk.FlatCache blendOffset;
    private final DensityFunctions.BeardifierOrMarker beardifier;
    private long lastBlendingDataPos = ChunkPos.INVALID_CHUNK_POS;
    private Blender.BlendingOutput lastBlendingOutput = new Blender.BlendingOutput(1.0, 0.0);
    final int noiseSizeXZ;
    final int cellWidth;
    final int cellHeight;
    boolean interpolating;
    boolean fillingCell;
    private int cellStartBlockX;
    int cellStartBlockY;
    private int cellStartBlockZ;
    int inCellX;
    int inCellY;
    int inCellZ;
    long interpolationCounter;
    long arrayInterpolationCounter;
    int arrayIndex;
    private final DensityFunction.ContextProvider sliceFillingContextProvider = new DensityFunction.ContextProvider() {
        @Override
        public DensityFunction.FunctionContext forIndex(int p_209253_) {
            NoiseChunk.this.cellStartBlockY = (p_209253_ + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
            NoiseChunk.this.interpolationCounter++;
            NoiseChunk.this.inCellY = 0;
            NoiseChunk.this.arrayIndex = p_209253_;
            return NoiseChunk.this;
        }

        @Override
        public void fillAllDirectly(double[] p_209255_, DensityFunction p_209256_) {
            for (int i2 = 0; i2 < NoiseChunk.this.cellCountY + 1; i2++) {
                NoiseChunk.this.cellStartBlockY = (i2 + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
                NoiseChunk.this.interpolationCounter++;
                NoiseChunk.this.inCellY = 0;
                NoiseChunk.this.arrayIndex = i2;
                p_209255_[i2] = p_209256_.compute(NoiseChunk.this);
            }
        }
    };

    public static NoiseChunk forChunk(
        ChunkAccess pChunk,
        RandomState pState,
        DensityFunctions.BeardifierOrMarker pBeardifierOrMarker,
        NoiseGeneratorSettings pNoiseGeneratorSettings,
        Aquifer.FluidPicker pFluidPicke,
        Blender pBlender
    ) {
        NoiseSettings noisesettings = pNoiseGeneratorSettings.noiseSettings().clampToHeightAccessor(pChunk);
        ChunkPos chunkpos = pChunk.getPos();
        int i = 16 / noisesettings.getCellWidth();
        return new NoiseChunk(i, pState, chunkpos.getMinBlockX(), chunkpos.getMinBlockZ(), noisesettings, pBeardifierOrMarker, pNoiseGeneratorSettings, pFluidPicke, pBlender);
    }

    public NoiseChunk(
        int pCellCountXZ,
        RandomState pRandom,
        int pFirstNoiseX,
        int pFirstNoiseZ,
        NoiseSettings pNoiseSettings,
        DensityFunctions.BeardifierOrMarker pBeardifier,
        NoiseGeneratorSettings pNoiseGeneratorSettings,
        Aquifer.FluidPicker pFluidPicker,
        Blender pBlendifier
    ) {
        this.noiseSettings = pNoiseSettings;
        this.cellWidth = pNoiseSettings.getCellWidth();
        this.cellHeight = pNoiseSettings.getCellHeight();
        this.cellCountXZ = pCellCountXZ;
        this.cellCountY = Mth.floorDiv(pNoiseSettings.height(), this.cellHeight);
        this.cellNoiseMinY = Mth.floorDiv(pNoiseSettings.minY(), this.cellHeight);
        this.firstCellX = Math.floorDiv(pFirstNoiseX, this.cellWidth);
        this.firstCellZ = Math.floorDiv(pFirstNoiseZ, this.cellWidth);
        this.interpolators = Lists.newArrayList();
        this.cellCaches = Lists.newArrayList();
        this.firstNoiseX = QuartPos.fromBlock(pFirstNoiseX);
        this.firstNoiseZ = QuartPos.fromBlock(pFirstNoiseZ);
        this.noiseSizeXZ = QuartPos.fromBlock(pCellCountXZ * this.cellWidth);
        this.blender = pBlendifier;
        this.beardifier = pBeardifier;
        this.blendAlpha = new NoiseChunk.FlatCache(new NoiseChunk.BlendAlpha(), false);
        this.blendOffset = new NoiseChunk.FlatCache(new NoiseChunk.BlendOffset(), false);

        for (int i = 0; i <= this.noiseSizeXZ; i++) {
            int j = this.firstNoiseX + i;
            int k = QuartPos.toBlock(j);

            for (int l = 0; l <= this.noiseSizeXZ; l++) {
                int i1 = this.firstNoiseZ + l;
                int j1 = QuartPos.toBlock(i1);
                Blender.BlendingOutput blender$blendingoutput = pBlendifier.blendOffsetAndFactor(k, j1);
                this.blendAlpha.values[i][l] = blender$blendingoutput.alpha();
                this.blendOffset.values[i][l] = blender$blendingoutput.blendingOffset();
            }
        }

        NoiseRouter noiserouter = pRandom.router();
        NoiseRouter noiserouter1 = noiserouter.mapAll(this::wrap);
        if (!pNoiseGeneratorSettings.isAquifersEnabled()) {
            this.aquifer = Aquifer.createDisabled(pFluidPicker);
        } else {
            int k1 = SectionPos.blockToSectionCoord(pFirstNoiseX);
            int l1 = SectionPos.blockToSectionCoord(pFirstNoiseZ);
            this.aquifer = Aquifer.create(
                this, new ChunkPos(k1, l1), noiserouter1, pRandom.aquiferRandom(), pNoiseSettings.minY(), pNoiseSettings.height(), pFluidPicker
            );
        }

        List<NoiseChunk.BlockStateFiller> list = new ArrayList<>();
        DensityFunction densityfunction = DensityFunctions.cacheAllInCell(
                DensityFunctions.add(noiserouter1.finalDensity(), DensityFunctions.BeardifierMarker.INSTANCE)
            )
            .mapAll(this::wrap);
        list.add(p_209217_ -> this.aquifer.computeSubstance(p_209217_, densityfunction.compute(p_209217_)));
        if (pNoiseGeneratorSettings.oreVeinsEnabled()) {
            list.add(OreVeinifier.create(noiserouter1.veinToggle(), noiserouter1.veinRidged(), noiserouter1.veinGap(), pRandom.oreRandom()));
        }

        this.blockStateRule = new MaterialRuleList(list.toArray(new NoiseChunk.BlockStateFiller[0]));
        this.initialDensityNoJaggedness = noiserouter1.initialDensityWithoutJaggedness();
    }

    protected Climate.Sampler cachedClimateSampler(NoiseRouter pNoiseRouter, List<Climate.ParameterPoint> pPoints) {
        return new Climate.Sampler(
            pNoiseRouter.temperature().mapAll(this::wrap),
            pNoiseRouter.vegetation().mapAll(this::wrap),
            pNoiseRouter.continents().mapAll(this::wrap),
            pNoiseRouter.erosion().mapAll(this::wrap),
            pNoiseRouter.depth().mapAll(this::wrap),
            pNoiseRouter.ridges().mapAll(this::wrap),
            pPoints
        );
    }

    @Nullable
    protected BlockState getInterpolatedState() {
        return this.blockStateRule.calculate(this);
    }

    @Override
    public int blockX() {
        return this.cellStartBlockX + this.inCellX;
    }

    @Override
    public int blockY() {
        return this.cellStartBlockY + this.inCellY;
    }

    @Override
    public int blockZ() {
        return this.cellStartBlockZ + this.inCellZ;
    }

    public int preliminarySurfaceLevel(int pX, int pZ) {
        int i = QuartPos.toBlock(QuartPos.fromBlock(pX));
        int j = QuartPos.toBlock(QuartPos.fromBlock(pZ));
        return this.preliminarySurfaceLevel.computeIfAbsent(ColumnPos.asLong(i, j), this::computePreliminarySurfaceLevel);
    }

    private int computePreliminarySurfaceLevel(long pPackedChunkPos) {
        int i = ColumnPos.getX(pPackedChunkPos);
        int j = ColumnPos.getZ(pPackedChunkPos);
        int k = this.noiseSettings.minY();

        for (int l = k + this.noiseSettings.height(); l >= k; l -= this.cellHeight) {
            if (this.initialDensityNoJaggedness.compute(new DensityFunction.SinglePointContext(i, l, j)) > 0.390625) {
                return l;
            }
        }

        return Integer.MAX_VALUE;
    }

    @Override
    public Blender getBlender() {
        return this.blender;
    }

    private void fillSlice(boolean pIsSlice0, int pStart) {
        this.cellStartBlockX = pStart * this.cellWidth;
        this.inCellX = 0;

        for (int i = 0; i < this.cellCountXZ + 1; i++) {
            int j = this.firstCellZ + i;
            this.cellStartBlockZ = j * this.cellWidth;
            this.inCellZ = 0;
            this.arrayInterpolationCounter++;

            for (NoiseChunk.NoiseInterpolator noisechunk$noiseinterpolator : this.interpolators) {
                double[] adouble = (pIsSlice0 ? noisechunk$noiseinterpolator.slice0 : noisechunk$noiseinterpolator.slice1)[i];
                noisechunk$noiseinterpolator.fillArray(adouble, this.sliceFillingContextProvider);
            }
        }

        this.arrayInterpolationCounter++;
    }

    public void initializeForFirstCellX() {
        if (this.interpolating) {
            throw new IllegalStateException("Staring interpolation twice");
        } else {
            this.interpolating = true;
            this.interpolationCounter = 0L;
            this.fillSlice(true, this.firstCellX);
        }
    }

    public void advanceCellX(int pIncrement) {
        this.fillSlice(false, this.firstCellX + pIncrement + 1);
        this.cellStartBlockX = (this.firstCellX + pIncrement) * this.cellWidth;
    }

    public NoiseChunk forIndex(int p_209240_) {
        int i = Math.floorMod(p_209240_, this.cellWidth);
        int j = Math.floorDiv(p_209240_, this.cellWidth);
        int k = Math.floorMod(j, this.cellWidth);
        int l = this.cellHeight - 1 - Math.floorDiv(j, this.cellWidth);
        this.inCellX = k;
        this.inCellY = l;
        this.inCellZ = i;
        this.arrayIndex = p_209240_;
        return this;
    }

    @Override
    public void fillAllDirectly(double[] p_209224_, DensityFunction p_209225_) {
        this.arrayIndex = 0;

        for (int i = this.cellHeight - 1; i >= 0; i--) {
            this.inCellY = i;

            for (int j = 0; j < this.cellWidth; j++) {
                this.inCellX = j;

                for (int k = 0; k < this.cellWidth; k++) {
                    this.inCellZ = k;
                    p_209224_[this.arrayIndex++] = p_209225_.compute(this);
                }
            }
        }
    }

    public void selectCellYZ(int pY, int pZ) {
        for (NoiseChunk.NoiseInterpolator noisechunk$noiseinterpolator : this.interpolators) {
            noisechunk$noiseinterpolator.selectCellYZ(pY, pZ);
        }

        this.fillingCell = true;
        this.cellStartBlockY = (pY + this.cellNoiseMinY) * this.cellHeight;
        this.cellStartBlockZ = (this.firstCellZ + pZ) * this.cellWidth;
        this.arrayInterpolationCounter++;

        for (NoiseChunk.CacheAllInCell noisechunk$cacheallincell : this.cellCaches) {
            noisechunk$cacheallincell.noiseFiller.fillArray(noisechunk$cacheallincell.values, this);
        }

        this.arrayInterpolationCounter++;
        this.fillingCell = false;
    }

    public void updateForY(int pCellEndBlockY, double pY) {
        this.inCellY = pCellEndBlockY - this.cellStartBlockY;

        for (NoiseChunk.NoiseInterpolator noisechunk$noiseinterpolator : this.interpolators) {
            noisechunk$noiseinterpolator.updateForY(pY);
        }
    }

    public void updateForX(int pCellEndBlockX, double pX) {
        this.inCellX = pCellEndBlockX - this.cellStartBlockX;

        for (NoiseChunk.NoiseInterpolator noisechunk$noiseinterpolator : this.interpolators) {
            noisechunk$noiseinterpolator.updateForX(pX);
        }
    }

    public void updateForZ(int pCellEndBlockZ, double pZ) {
        this.inCellZ = pCellEndBlockZ - this.cellStartBlockZ;
        this.interpolationCounter++;

        for (NoiseChunk.NoiseInterpolator noisechunk$noiseinterpolator : this.interpolators) {
            noisechunk$noiseinterpolator.updateForZ(pZ);
        }
    }

    public void stopInterpolation() {
        if (!this.interpolating) {
            throw new IllegalStateException("Staring interpolation twice");
        } else {
            this.interpolating = false;
        }
    }

    public void swapSlices() {
        this.interpolators.forEach(NoiseChunk.NoiseInterpolator::swapSlices);
    }

    public Aquifer aquifer() {
        return this.aquifer;
    }

    protected int cellWidth() {
        return this.cellWidth;
    }

    protected int cellHeight() {
        return this.cellHeight;
    }

    Blender.BlendingOutput getOrComputeBlendingOutput(int pChunkX, int pChunkZ) {
        long i = ChunkPos.asLong(pChunkX, pChunkZ);
        if (this.lastBlendingDataPos == i) {
            return this.lastBlendingOutput;
        } else {
            this.lastBlendingDataPos = i;
            Blender.BlendingOutput blender$blendingoutput = this.blender.blendOffsetAndFactor(pChunkX, pChunkZ);
            this.lastBlendingOutput = blender$blendingoutput;
            return blender$blendingoutput;
        }
    }

    protected DensityFunction wrap(DensityFunction pDensityFunction) {
        return this.wrapped.computeIfAbsent(pDensityFunction, this::wrapNew);
    }

    private DensityFunction wrapNew(DensityFunction pDensityFunction) {
        if (pDensityFunction instanceof DensityFunctions.Marker densityfunctions$marker) {
            return (DensityFunction)(switch (densityfunctions$marker.type()) {
                case Interpolated -> new NoiseChunk.NoiseInterpolator(densityfunctions$marker.wrapped());
                case FlatCache -> new NoiseChunk.FlatCache(densityfunctions$marker.wrapped(), true);
                case Cache2D -> new NoiseChunk.Cache2D(densityfunctions$marker.wrapped());
                case CacheOnce -> new NoiseChunk.CacheOnce(densityfunctions$marker.wrapped());
                case CacheAllInCell -> new NoiseChunk.CacheAllInCell(densityfunctions$marker.wrapped());
            });
        } else {
            if (this.blender != Blender.empty()) {
                if (pDensityFunction == DensityFunctions.BlendAlpha.INSTANCE) {
                    return this.blendAlpha;
                }

                if (pDensityFunction == DensityFunctions.BlendOffset.INSTANCE) {
                    return this.blendOffset;
                }
            }

            if (pDensityFunction == DensityFunctions.BeardifierMarker.INSTANCE) {
                return this.beardifier;
            } else {
                return pDensityFunction instanceof DensityFunctions.HolderHolder densityfunctions$holderholder
                    ? densityfunctions$holderholder.function().value()
                    : pDensityFunction;
            }
        }
    }

    class BlendAlpha implements NoiseChunk.NoiseChunkDensityFunction {
        @Override
        public DensityFunction wrapped() {
            return DensityFunctions.BlendAlpha.INSTANCE;
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_224365_) {
            return this.wrapped().mapAll(p_224365_);
        }

        @Override
        public double compute(DensityFunction.FunctionContext p_209264_) {
            return NoiseChunk.this.getOrComputeBlendingOutput(p_209264_.blockX(), p_209264_.blockZ()).alpha();
        }

        @Override
        public void fillArray(double[] p_209266_, DensityFunction.ContextProvider p_209267_) {
            p_209267_.fillAllDirectly(p_209266_, this);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return DensityFunctions.BlendAlpha.CODEC;
        }
    }

    class BlendOffset implements NoiseChunk.NoiseChunkDensityFunction {
        @Override
        public DensityFunction wrapped() {
            return DensityFunctions.BlendOffset.INSTANCE;
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_224368_) {
            return this.wrapped().mapAll(p_224368_);
        }

        @Override
        public double compute(DensityFunction.FunctionContext p_209276_) {
            return NoiseChunk.this.getOrComputeBlendingOutput(p_209276_.blockX(), p_209276_.blockZ()).blendingOffset();
        }

        @Override
        public void fillArray(double[] p_209278_, DensityFunction.ContextProvider p_209279_) {
            p_209279_.fillAllDirectly(p_209278_, this);
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return DensityFunctions.BlendOffset.CODEC;
        }
    }

    @FunctionalInterface
    public interface BlockStateFiller {
        @Nullable
        BlockState calculate(DensityFunction.FunctionContext pContext);
    }

    static class Cache2D implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        private final DensityFunction function;
        private long lastPos2D = ChunkPos.INVALID_CHUNK_POS;
        private double lastValue;

        Cache2D(DensityFunction pFunction) {
            this.function = pFunction;
        }

        @Override
        public double compute(DensityFunction.FunctionContext p_209290_) {
            int i = p_209290_.blockX();
            int j = p_209290_.blockZ();
            long k = ChunkPos.asLong(i, j);
            if (this.lastPos2D == k) {
                return this.lastValue;
            } else {
                this.lastPos2D = k;
                double d0 = this.function.compute(p_209290_);
                this.lastValue = d0;
                return d0;
            }
        }

        @Override
        public void fillArray(double[] p_209292_, DensityFunction.ContextProvider p_209293_) {
            this.function.fillArray(p_209292_, p_209293_);
        }

        @Override
        public DensityFunction wrapped() {
            return this.function;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.Cache2D;
        }
    }

    class CacheAllInCell implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        final DensityFunction noiseFiller;
        final double[] values;

        CacheAllInCell(final DensityFunction pNoiseFilter) {
            this.noiseFiller = pNoiseFilter;
            this.values = new double[NoiseChunk.this.cellWidth * NoiseChunk.this.cellWidth * NoiseChunk.this.cellHeight];
            NoiseChunk.this.cellCaches.add(this);
        }

        @Override
        public double compute(DensityFunction.FunctionContext p_209303_) {
            if (p_209303_ != NoiseChunk.this) {
                return this.noiseFiller.compute(p_209303_);
            } else if (!NoiseChunk.this.interpolating) {
                throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
            } else {
                int i = NoiseChunk.this.inCellX;
                int j = NoiseChunk.this.inCellY;
                int k = NoiseChunk.this.inCellZ;
                return i >= 0 && j >= 0 && k >= 0 && i < NoiseChunk.this.cellWidth && j < NoiseChunk.this.cellHeight && k < NoiseChunk.this.cellWidth
                    ? this.values[((NoiseChunk.this.cellHeight - 1 - j) * NoiseChunk.this.cellWidth + i) * NoiseChunk.this.cellWidth + k]
                    : this.noiseFiller.compute(p_209303_);
            }
        }

        @Override
        public void fillArray(double[] p_209305_, DensityFunction.ContextProvider p_209306_) {
            p_209306_.fillAllDirectly(p_209305_, this);
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.CacheAllInCell;
        }
    }

    class CacheOnce implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        private final DensityFunction function;
        private long lastCounter;
        private long lastArrayCounter;
        private double lastValue;
        @Nullable
        private double[] lastArray;

        CacheOnce(final DensityFunction pFunction) {
            this.function = pFunction;
        }

        @Override
        public double compute(DensityFunction.FunctionContext p_209319_) {
            if (p_209319_ != NoiseChunk.this) {
                return this.function.compute(p_209319_);
            } else if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
                return this.lastArray[NoiseChunk.this.arrayIndex];
            } else if (this.lastCounter == NoiseChunk.this.interpolationCounter) {
                return this.lastValue;
            } else {
                this.lastCounter = NoiseChunk.this.interpolationCounter;
                double d0 = this.function.compute(p_209319_);
                this.lastValue = d0;
                return d0;
            }
        }

        @Override
        public void fillArray(double[] p_209321_, DensityFunction.ContextProvider p_209322_) {
            if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
                System.arraycopy(this.lastArray, 0, p_209321_, 0, p_209321_.length);
            } else {
                this.wrapped().fillArray(p_209321_, p_209322_);
                if (this.lastArray != null && this.lastArray.length == p_209321_.length) {
                    System.arraycopy(p_209321_, 0, this.lastArray, 0, p_209321_.length);
                } else {
                    this.lastArray = (double[])p_209321_.clone();
                }

                this.lastArrayCounter = NoiseChunk.this.arrayInterpolationCounter;
            }
        }

        @Override
        public DensityFunction wrapped() {
            return this.function;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.CacheOnce;
        }
    }

    class FlatCache implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        private final DensityFunction noiseFiller;
        final double[][] values;

        FlatCache(final DensityFunction pNoiseFiller, final boolean pComputeValues) {
            this.noiseFiller = pNoiseFiller;
            this.values = new double[NoiseChunk.this.noiseSizeXZ + 1][NoiseChunk.this.noiseSizeXZ + 1];
            if (pComputeValues) {
                for (int i = 0; i <= NoiseChunk.this.noiseSizeXZ; i++) {
                    int j = NoiseChunk.this.firstNoiseX + i;
                    int k = QuartPos.toBlock(j);

                    for (int l = 0; l <= NoiseChunk.this.noiseSizeXZ; l++) {
                        int i1 = NoiseChunk.this.firstNoiseZ + l;
                        int j1 = QuartPos.toBlock(i1);
                        this.values[i][l] = pNoiseFiller.compute(new DensityFunction.SinglePointContext(k, 0, j1));
                    }
                }
            }
        }

        @Override
        public double compute(DensityFunction.FunctionContext p_209333_) {
            int i = QuartPos.fromBlock(p_209333_.blockX());
            int j = QuartPos.fromBlock(p_209333_.blockZ());
            int k = i - NoiseChunk.this.firstNoiseX;
            int l = j - NoiseChunk.this.firstNoiseZ;
            int i1 = this.values.length;
            return k >= 0 && l >= 0 && k < i1 && l < i1 ? this.values[k][l] : this.noiseFiller.compute(p_209333_);
        }

        @Override
        public void fillArray(double[] p_209335_, DensityFunction.ContextProvider p_209336_) {
            p_209336_.fillAllDirectly(p_209335_, this);
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.FlatCache;
        }
    }

    interface NoiseChunkDensityFunction extends DensityFunction {
        DensityFunction wrapped();

        @Override
        default double minValue() {
            return this.wrapped().minValue();
        }

        @Override
        default double maxValue() {
            return this.wrapped().maxValue();
        }
    }

    public class NoiseInterpolator implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        double[][] slice0;
        double[][] slice1;
        private final DensityFunction noiseFiller;
        private double noise000;
        private double noise001;
        private double noise100;
        private double noise101;
        private double noise010;
        private double noise011;
        private double noise110;
        private double noise111;
        private double valueXZ00;
        private double valueXZ10;
        private double valueXZ01;
        private double valueXZ11;
        private double valueZ0;
        private double valueZ1;
        private double value;

        NoiseInterpolator(final DensityFunction pNoiseFilter) {
            this.noiseFiller = pNoiseFilter;
            this.slice0 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
            this.slice1 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
            NoiseChunk.this.interpolators.add(this);
        }

        private double[][] allocateSlice(int pCellCountY, int pCellCountXZ) {
            int i = pCellCountXZ + 1;
            int j = pCellCountY + 1;
            double[][] adouble = new double[i][j];

            for (int k = 0; k < i; k++) {
                adouble[k] = new double[j];
            }

            return adouble;
        }

        void selectCellYZ(int pY, int pZ) {
            this.noise000 = this.slice0[pZ][pY];
            this.noise001 = this.slice0[pZ + 1][pY];
            this.noise100 = this.slice1[pZ][pY];
            this.noise101 = this.slice1[pZ + 1][pY];
            this.noise010 = this.slice0[pZ][pY + 1];
            this.noise011 = this.slice0[pZ + 1][pY + 1];
            this.noise110 = this.slice1[pZ][pY + 1];
            this.noise111 = this.slice1[pZ + 1][pY + 1];
        }

        void updateForY(double pY) {
            this.valueXZ00 = Mth.lerp(pY, this.noise000, this.noise010);
            this.valueXZ10 = Mth.lerp(pY, this.noise100, this.noise110);
            this.valueXZ01 = Mth.lerp(pY, this.noise001, this.noise011);
            this.valueXZ11 = Mth.lerp(pY, this.noise101, this.noise111);
        }

        void updateForX(double pX) {
            this.valueZ0 = Mth.lerp(pX, this.valueXZ00, this.valueXZ10);
            this.valueZ1 = Mth.lerp(pX, this.valueXZ01, this.valueXZ11);
        }

        void updateForZ(double pZ) {
            this.value = Mth.lerp(pZ, this.valueZ0, this.valueZ1);
        }

        @Override
        public double compute(DensityFunction.FunctionContext p_209347_) {
            if (p_209347_ != NoiseChunk.this) {
                return this.noiseFiller.compute(p_209347_);
            } else if (!NoiseChunk.this.interpolating) {
                throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
            } else {
                return NoiseChunk.this.fillingCell
                    ? Mth.lerp3(
                        (double)NoiseChunk.this.inCellX / (double)NoiseChunk.this.cellWidth,
                        (double)NoiseChunk.this.inCellY / (double)NoiseChunk.this.cellHeight,
                        (double)NoiseChunk.this.inCellZ / (double)NoiseChunk.this.cellWidth,
                        this.noise000,
                        this.noise100,
                        this.noise010,
                        this.noise110,
                        this.noise001,
                        this.noise101,
                        this.noise011,
                        this.noise111
                    )
                    : this.value;
            }
        }

        @Override
        public void fillArray(double[] p_209349_, DensityFunction.ContextProvider p_209350_) {
            if (NoiseChunk.this.fillingCell) {
                p_209350_.fillAllDirectly(p_209349_, this);
            } else {
                this.wrapped().fillArray(p_209349_, p_209350_);
            }
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        private void swapSlices() {
            double[][] adouble = this.slice0;
            this.slice0 = this.slice1;
            this.slice1 = adouble;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.Interpolated;
        }
    }
}