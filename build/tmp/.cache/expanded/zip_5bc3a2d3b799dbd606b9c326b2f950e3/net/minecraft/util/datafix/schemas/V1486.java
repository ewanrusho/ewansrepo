package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1486 extends NamespacedSchema {
    public V1486(int p_17722_, Schema p_17723_) {
        super(p_17722_, p_17723_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        map.put("minecraft:cod", map.remove("minecraft:cod_mob"));
        map.put("minecraft:salmon", map.remove("minecraft:salmon_mob"));
        return map;
    }
}