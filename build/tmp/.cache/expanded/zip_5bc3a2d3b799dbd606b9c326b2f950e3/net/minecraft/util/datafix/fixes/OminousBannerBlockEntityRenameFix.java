package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class OminousBannerBlockEntityRenameFix extends NamedEntityFix {
    public OminousBannerBlockEntityRenameFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "OminousBannerBlockEntityRenameFix", References.BLOCK_ENTITY, "minecraft:banner");
    }

    @Override
    protected Typed<?> fix(Typed<?> p_16551_) {
        return p_16551_.update(DSL.remainderFinder(), this::fixTag);
    }

    private Dynamic<?> fixTag(Dynamic<?> pTag) {
        Optional<String> optional = pTag.get("CustomName").asString().result();
        if (optional.isPresent()) {
            String s = optional.get();
            s = s.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"");
            return pTag.set("CustomName", pTag.createString(s));
        } else {
            return pTag;
        }
    }
}