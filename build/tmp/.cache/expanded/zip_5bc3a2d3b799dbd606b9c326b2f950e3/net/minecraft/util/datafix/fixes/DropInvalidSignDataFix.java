package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class DropInvalidSignDataFix extends NamedEntityFix {
    private static final String[] FIELDS_TO_DROP = new String[]{
        "Text1", "Text2", "Text3", "Text4", "FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4", "Color", "GlowingText"
    };

    public DropInvalidSignDataFix(Schema pOutputSchema, String pName, String pEntityName) {
        super(pOutputSchema, false, pName, References.BLOCK_ENTITY, pEntityName);
    }

    private static <T> Dynamic<T> fix(Dynamic<T> pDynamic) {
        pDynamic = pDynamic.update("front_text", DropInvalidSignDataFix::fixText);
        pDynamic = pDynamic.update("back_text", DropInvalidSignDataFix::fixText);

        for (String s : FIELDS_TO_DROP) {
            pDynamic = pDynamic.remove(s);
        }

        return pDynamic;
    }

    private static <T> Dynamic<T> fixText(Dynamic<T> pTextDynamic) {
        boolean flag = pTextDynamic.get("_filtered_correct").asBoolean(false);
        if (flag) {
            return pTextDynamic.remove("_filtered_correct");
        } else {
            Optional<Stream<Dynamic<T>>> optional = pTextDynamic.get("filtered_messages").asStreamOpt().result();
            if (optional.isEmpty()) {
                return pTextDynamic;
            } else {
                Dynamic<T> dynamic = ComponentDataFixUtils.createEmptyComponent(pTextDynamic.getOps());
                List<Dynamic<T>> list = pTextDynamic.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
                List<Dynamic<T>> list1 = Streams.mapWithIndex(optional.get(), (p_298117_, p_298041_) -> {
                    Dynamic<T> dynamic1 = p_298041_ < (long)list.size() ? list.get((int)p_298041_) : dynamic;
                    return p_298117_.equals(dynamic) ? dynamic1 : p_298117_;
                }).toList();
                return list1.stream().allMatch(p_300495_ -> p_300495_.equals(dynamic))
                    ? pTextDynamic.remove("filtered_messages")
                    : pTextDynamic.set("filtered_messages", pTextDynamic.createList(list1.stream()));
            }
        }
    }

    @Override
    protected Typed<?> fix(Typed<?> p_297432_) {
        return p_297432_.update(DSL.remainderFinder(), DropInvalidSignDataFix::fix);
    }
}