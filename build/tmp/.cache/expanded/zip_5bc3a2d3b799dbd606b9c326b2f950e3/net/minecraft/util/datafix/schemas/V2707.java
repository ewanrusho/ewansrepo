package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2707 extends NamespacedSchema {
    public V2707(int p_145894_, Schema p_145895_) {
        super(p_145894_, p_145895_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        this.registerSimple(map, "minecraft:marker");
        return map;
    }
}