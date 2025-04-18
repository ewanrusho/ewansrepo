package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;
import java.util.function.IntFunction;

public class EntityVariantFix extends NamedEntityFix {
    private final String fieldName;
    private final IntFunction<String> idConversions;

    public EntityVariantFix(Schema pOutputSchema, String pName, TypeReference pType, String pEntityName, String pFieldName, IntFunction<String> pIdConversions) {
        super(pOutputSchema, false, pName, pType, pEntityName);
        this.fieldName = pFieldName;
        this.idConversions = pIdConversions;
    }

    private static <T> Dynamic<T> updateAndRename(Dynamic<T> pDynamic, String pFieldName, String pNewFieldName, Function<Dynamic<T>, Dynamic<T>> pFixer) {
        return pDynamic.map(
            p_326583_ -> {
                DynamicOps<T> dynamicops = pDynamic.getOps();
                Function<T, T> function = p_216656_ -> pFixer.apply(new Dynamic<>(dynamicops, p_216656_)).getValue();
                return dynamicops.get((T)p_326583_, pFieldName)
                    .map(p_216652_ -> dynamicops.set((T)p_326583_, pNewFieldName, function.apply((T)p_216652_)))
                    .result()
                    .orElse((T)p_326583_);
            }
        );
    }

    @Override
    protected Typed<?> fix(Typed<?> p_216630_) {
        return p_216630_.update(
            DSL.remainderFinder(),
            p_216632_ -> updateAndRename(
                    p_216632_,
                    this.fieldName,
                    "variant",
                    p_326578_ -> DataFixUtils.orElse(
                            p_326578_.asNumber().map(p_216635_ -> p_326578_.createString(this.idConversions.apply(p_216635_.intValue()))).result(), p_326578_
                        )
                )
        );
    }
}