package net.minecraft.world.level.levelgen.structure;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class StructureFeatureIndexSavedData extends SavedData {
    private static final String TAG_REMAINING_INDEXES = "Remaining";
    private static final String TAG_All_INDEXES = "All";
    private final LongSet all;
    private final LongSet remaining;

    public static SavedData.Factory<StructureFeatureIndexSavedData> factory() {
        return new SavedData.Factory<>(
            StructureFeatureIndexSavedData::new, StructureFeatureIndexSavedData::load, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES
        );
    }

    private StructureFeatureIndexSavedData(LongSet pAll, LongSet pRemaining) {
        this.all = pAll;
        this.remaining = pRemaining;
    }

    public StructureFeatureIndexSavedData() {
        this(new LongOpenHashSet(), new LongOpenHashSet());
    }

    public static StructureFeatureIndexSavedData load(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        return new StructureFeatureIndexSavedData(new LongOpenHashSet(pTag.getLongArray("All")), new LongOpenHashSet(pTag.getLongArray("Remaining")));
    }

    @Override
    public CompoundTag save(CompoundTag p_73372_, HolderLookup.Provider p_329815_) {
        p_73372_.putLongArray("All", this.all.toLongArray());
        p_73372_.putLongArray("Remaining", this.remaining.toLongArray());
        return p_73372_;
    }

    public void addIndex(long pIndex) {
        this.all.add(pIndex);
        this.remaining.add(pIndex);
        this.setDirty();
    }

    public boolean hasStartIndex(long pIndex) {
        return this.all.contains(pIndex);
    }

    public boolean hasUnhandledIndex(long pIndex) {
        return this.remaining.contains(pIndex);
    }

    public void removeIndex(long pIndex) {
        if (this.remaining.remove(pIndex)) {
            this.setDirty();
        }
    }

    public LongSet getAll() {
        return this.all;
    }
}