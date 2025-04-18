package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class ChunkTracker extends DynamicGraphMinFixedPoint {
    protected ChunkTracker(int p_140701_, int p_140702_, int p_140703_) {
        super(p_140701_, p_140702_, p_140703_);
    }

    @Override
    protected boolean isSource(long pPos) {
        return pPos == ChunkPos.INVALID_CHUNK_POS;
    }

    @Override
    protected void checkNeighborsAfterUpdate(long pPos, int pLevel, boolean pIsDecreasing) {
        if (!pIsDecreasing || pLevel < this.levelCount - 2) {
            ChunkPos chunkpos = new ChunkPos(pPos);
            int i = chunkpos.x;
            int j = chunkpos.z;

            for (int k = -1; k <= 1; k++) {
                for (int l = -1; l <= 1; l++) {
                    long i1 = ChunkPos.asLong(i + k, j + l);
                    if (i1 != pPos) {
                        this.checkNeighbor(pPos, i1, pLevel, pIsDecreasing);
                    }
                }
            }
        }
    }

    @Override
    protected int getComputedLevel(long pPos, long pExcludedSourcePos, int pLevel) {
        int i = pLevel;
        ChunkPos chunkpos = new ChunkPos(pPos);
        int j = chunkpos.x;
        int k = chunkpos.z;

        for (int l = -1; l <= 1; l++) {
            for (int i1 = -1; i1 <= 1; i1++) {
                long j1 = ChunkPos.asLong(j + l, k + i1);
                if (j1 == pPos) {
                    j1 = ChunkPos.INVALID_CHUNK_POS;
                }

                if (j1 != pExcludedSourcePos) {
                    int k1 = this.computeLevelFromNeighbor(j1, pPos, this.getLevel(j1));
                    if (i > k1) {
                        i = k1;
                    }

                    if (i == 0) {
                        return i;
                    }
                }
            }
        }

        return i;
    }

    @Override
    protected int computeLevelFromNeighbor(long pStartPos, long pEndPos, int pStartLevel) {
        return pStartPos == ChunkPos.INVALID_CHUNK_POS ? this.getLevelFromSource(pEndPos) : pStartLevel + 1;
    }

    protected abstract int getLevelFromSource(long pPos);

    public void update(long pPos, int pLevel, boolean pIsDecreasing) {
        this.checkEdge(ChunkPos.INVALID_CHUNK_POS, pPos, pLevel, pIsDecreasing);
    }
}