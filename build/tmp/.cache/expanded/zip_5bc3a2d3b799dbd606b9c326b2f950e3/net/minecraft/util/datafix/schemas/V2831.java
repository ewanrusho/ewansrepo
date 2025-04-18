package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2831 extends NamespacedSchema {
    public V2831(int p_185208_, Schema p_185209_) {
        super(p_185208_, p_185209_);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            true,
            References.UNTAGGED_SPAWNER,
            () -> DSL.optionalFields(
                    "SpawnPotentials",
                    DSL.list(DSL.fields("data", DSL.fields("entity", References.ENTITY_TREE.in(pSchema)))),
                    "SpawnData",
                    DSL.fields("entity", References.ENTITY_TREE.in(pSchema))
                )
        );
    }
}