package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Variant implements Supplier<JsonElement> {
    private final Map<VariantProperty<?>, VariantProperty<?>.Value> values = Maps.newLinkedHashMap();

    public <T> Variant with(VariantProperty<T> pProperty, T pValue) {
        VariantProperty<?>.Value variantproperty = this.values.put(pProperty, pProperty.withValue(pValue));
        if (variantproperty != null) {
            throw new IllegalStateException("Replacing value of " + variantproperty + " with " + pValue);
        } else {
            return this;
        }
    }

    public static Variant variant() {
        return new Variant();
    }

    public static Variant merge(Variant pFirst, Variant pSecond) {
        Variant variant = new Variant();
        variant.values.putAll(pFirst.values);
        variant.values.putAll(pSecond.values);
        return variant;
    }

    public JsonElement get() {
        JsonObject jsonobject = new JsonObject();
        this.values.values().forEach(p_377837_ -> p_377837_.addToVariant(jsonobject));
        return jsonobject;
    }

    public static JsonElement convertList(List<Variant> pList) {
        if (pList.size() == 1) {
            return pList.get(0).get();
        } else {
            JsonArray jsonarray = new JsonArray();
            pList.forEach(p_375800_ -> jsonarray.add(p_375800_.get()));
            return jsonarray;
        }
    }
}