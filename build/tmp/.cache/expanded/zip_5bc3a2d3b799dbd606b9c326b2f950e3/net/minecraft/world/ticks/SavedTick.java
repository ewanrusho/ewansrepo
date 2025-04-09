package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;

public record SavedTick<T>(T type, BlockPos pos, int delay, TickPriority priority) {
    private static final String TAG_ID = "i";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";
    private static final String TAG_DELAY = "t";
    private static final String TAG_PRIORITY = "p";
    public static final Strategy<SavedTick<?>> UNIQUE_TICK_HASH = new Strategy<SavedTick<?>>() {
        public int hashCode(SavedTick<?> p_193364_) {
            return 31 * p_193364_.pos().hashCode() + p_193364_.type().hashCode();
        }

        public boolean equals(@Nullable SavedTick<?> p_193366_, @Nullable SavedTick<?> p_193367_) {
            if (p_193366_ == p_193367_) {
                return true;
            } else {
                return p_193366_ != null && p_193367_ != null
                    ? p_193366_.type() == p_193367_.type() && p_193366_.pos().equals(p_193367_.pos())
                    : false;
            }
        }
    };

    public static <T> List<SavedTick<T>> loadTickList(ListTag pTickList, Function<String, Optional<T>> pIdParser, ChunkPos pChunkPos) {
        List<SavedTick<T>> list = new ArrayList<>(pTickList.size());
        long i = pChunkPos.toLong();

        for (int j = 0; j < pTickList.size(); j++) {
            CompoundTag compoundtag = pTickList.getCompound(j);
            loadTick(compoundtag, pIdParser).ifPresent(p_360704_ -> {
                if (ChunkPos.asLong(p_360704_.pos()) == i) {
                    list.add((SavedTick<T>)p_360704_);
                }
            });
        }

        return list;
    }

    public static <T> Optional<SavedTick<T>> loadTick(CompoundTag pTag, Function<String, Optional<T>> pIdParser) {
        return pIdParser.apply(pTag.getString("i")).map(p_210668_ -> {
            BlockPos blockpos = new BlockPos(pTag.getInt("x"), pTag.getInt("y"), pTag.getInt("z"));
            return new SavedTick<>((T)p_210668_, blockpos, pTag.getInt("t"), TickPriority.byValue(pTag.getInt("p")));
        });
    }

    private static CompoundTag saveTick(String pId, BlockPos pPos, int pDelay, TickPriority pPriority) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("i", pId);
        compoundtag.putInt("x", pPos.getX());
        compoundtag.putInt("y", pPos.getY());
        compoundtag.putInt("z", pPos.getZ());
        compoundtag.putInt("t", pDelay);
        compoundtag.putInt("p", pPriority.getValue());
        return compoundtag;
    }

    public CompoundTag save(Function<T, String> pIdGetter) {
        return saveTick(pIdGetter.apply(this.type), this.pos, this.delay, this.priority);
    }

    public ScheduledTick<T> unpack(long pGameTime, long pSubTickOrder) {
        return new ScheduledTick<>(this.type, this.pos, pGameTime + (long)this.delay, this.priority, pSubTickOrder);
    }

    public static <T> SavedTick<T> probe(T pType, BlockPos pPos) {
        return new SavedTick<>(pType, pPos, 0, TickPriority.NORMAL);
    }
}