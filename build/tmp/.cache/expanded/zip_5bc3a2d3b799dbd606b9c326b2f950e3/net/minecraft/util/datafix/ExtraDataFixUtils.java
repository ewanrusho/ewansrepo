package net.minecraft.util.datafix;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.View;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public class ExtraDataFixUtils {
    public static Dynamic<?> fixBlockPos(Dynamic<?> pData) {
        Optional<Number> optional = pData.get("X").asNumber().result();
        Optional<Number> optional1 = pData.get("Y").asNumber().result();
        Optional<Number> optional2 = pData.get("Z").asNumber().result();
        return !optional.isEmpty() && !optional1.isEmpty() && !optional2.isEmpty()
            ? pData.createIntList(IntStream.of(optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue()))
            : pData;
    }

    public static <T, R> Typed<R> cast(Type<R> pType, Typed<T> pData) {
        return new Typed<>(pType, pData.getOps(), (R)pData.getValue());
    }

    public static Type<?> patchSubType(Type<?> pType, Type<?> pOldSubType, Type<?> pNewSubType) {
        return pType.all(typePatcher(pOldSubType, pNewSubType), true, false).view().newType();
    }

    private static <A, B> TypeRewriteRule typePatcher(Type<A> pOldType, Type<B> pNewType) {
        RewriteResult<A, B> rewriteresult = RewriteResult.create(View.create("Patcher", pOldType, pNewType, p_358817_ -> p_358825_ -> {
                throw new UnsupportedOperationException();
            }), new BitSet());
        return TypeRewriteRule.everywhere(TypeRewriteRule.ifSame(pOldType, rewriteresult), PointFreeRule.nop(), true, true);
    }

    @SafeVarargs
    public static <T> Function<Typed<?>, Typed<?>> chainAllFilters(Function<Typed<?>, Typed<?>>... pFilters) {
        return p_344666_ -> {
            for (Function<Typed<?>, Typed<?>> function : pFilters) {
                p_344666_ = function.apply(p_344666_);
            }

            return p_344666_;
        };
    }

    public static Dynamic<?> blockState(String pBlockId, Map<String, String> pProperties) {
        Dynamic<Tag> dynamic = new Dynamic<>(NbtOps.INSTANCE, new CompoundTag());
        Dynamic<Tag> dynamic1 = dynamic.set("Name", dynamic.createString(pBlockId));
        if (!pProperties.isEmpty()) {
            dynamic1 = dynamic1.set(
                "Properties",
                dynamic.createMap(
                    pProperties.entrySet()
                        .stream()
                        .collect(
                            Collectors.toMap(p_358821_ -> dynamic.createString(p_358821_.getKey()), p_358819_ -> dynamic.createString(p_358819_.getValue()))
                        )
                )
            );
        }

        return dynamic1;
    }

    public static Dynamic<?> blockState(String pBlockId) {
        return blockState(pBlockId, Map.of());
    }

    public static Dynamic<?> fixStringField(Dynamic<?> pData, String pFieldName, UnaryOperator<String> pFixer) {
        return pData.update(
            pFieldName, p_358824_ -> DataFixUtils.orElse(p_358824_.asString().map(pFixer).map(pData::createString).result(), p_358824_)
        );
    }
}