package net.minecraft.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public record LockCode(ItemPredicate predicate) {
    public static final LockCode NO_LOCK = new LockCode(ItemPredicate.Builder.item().build());
    public static final Codec<LockCode> CODEC = ItemPredicate.CODEC.xmap(LockCode::new, LockCode::predicate);
    public static final String TAG_LOCK = "lock";

    public boolean unlocksWith(ItemStack pStack) {
        return this.predicate.test(pStack);
    }

    public void addToTag(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        if (this != NO_LOCK) {
            DataResult<Tag> dataresult = CODEC.encode(this, pRegistries.createSerializationContext(NbtOps.INSTANCE), new CompoundTag());
            dataresult.result().ifPresent(p_362847_ -> pTag.put("lock", p_362847_));
        }
    }

    public static LockCode fromTag(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        if (pTag.contains("lock", 10)) {
            DataResult<Pair<LockCode, Tag>> dataresult = CODEC.decode(pRegistries.createSerializationContext(NbtOps.INSTANCE), pTag.get("lock"));
            if (dataresult.isSuccess()) {
                return dataresult.getOrThrow().getFirst();
            }
        }

        return NO_LOCK;
    }
}