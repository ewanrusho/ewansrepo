package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiVariantGenerator implements BlockStateGenerator {
    private final Block block;
    private final List<Variant> baseVariants;
    private final Set<Property<?>> seenProperties = Sets.newHashSet();
    private final List<PropertyDispatch> declaredPropertySets = Lists.newArrayList();

    private MultiVariantGenerator(Block pBlock, List<Variant> pBaseVariants) {
        this.block = pBlock;
        this.baseVariants = pBaseVariants;
    }

    public MultiVariantGenerator with(PropertyDispatch pPropertyDispatch) {
        pPropertyDispatch.getDefinedProperties().forEach(p_377751_ -> {
            if (this.block.getStateDefinition().getProperty(p_377751_.getName()) != p_377751_) {
                throw new IllegalStateException("Property " + p_377751_ + " is not defined for block " + this.block);
            } else if (!this.seenProperties.add((Property<?>)p_377751_)) {
                throw new IllegalStateException("Values of property " + p_377751_ + " already defined for block " + this.block);
            }
        });
        this.declaredPropertySets.add(pPropertyDispatch);
        return this;
    }

    public JsonElement get() {
        Stream<Pair<Selector, List<Variant>>> stream = Stream.of(Pair.of(Selector.empty(), this.baseVariants));

        for (PropertyDispatch propertydispatch : this.declaredPropertySets) {
            Map<Selector, List<Variant>> map = propertydispatch.getEntries();
            stream = stream.flatMap(p_376345_ -> map.entrySet().stream().map(p_378185_ -> {
                    Selector selector = ((Selector)p_376345_.getFirst()).extend(p_378185_.getKey());
                    List<Variant> list = mergeVariants((List<Variant>)p_376345_.getSecond(), p_378185_.getValue());
                    return Pair.of(selector, list);
                }));
        }

        Map<String, JsonElement> map1 = new TreeMap<>();
        stream.forEach(p_376533_ -> map1.put(p_376533_.getFirst().getKey(), Variant.convertList(p_376533_.getSecond())));
        JsonObject jsonobject = new JsonObject();
        jsonobject.add("variants", Util.make(new JsonObject(), p_376444_ -> map1.forEach(p_376444_::add)));
        return jsonobject;
    }

    private static List<Variant> mergeVariants(List<Variant> pFirst, List<Variant> pSecond) {
        Builder<Variant> builder = ImmutableList.builder();
        pFirst.forEach(p_376449_ -> pSecond.forEach(p_378326_ -> builder.add(Variant.merge(p_376449_, p_378326_))));
        return builder.build();
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    public static MultiVariantGenerator multiVariant(Block pBlock) {
        return new MultiVariantGenerator(pBlock, ImmutableList.of(Variant.variant()));
    }

    public static MultiVariantGenerator multiVariant(Block pBlock, Variant pVariant) {
        return new MultiVariantGenerator(pBlock, ImmutableList.of(pVariant));
    }

    public static MultiVariantGenerator multiVariant(Block pBlock, Variant... pVariants) {
        return new MultiVariantGenerator(pBlock, ImmutableList.copyOf(pVariants));
    }
}