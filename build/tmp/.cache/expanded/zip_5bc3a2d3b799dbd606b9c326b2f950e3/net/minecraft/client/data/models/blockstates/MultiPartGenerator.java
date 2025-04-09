package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiPartGenerator implements BlockStateGenerator {
    private final Block block;
    private final List<MultiPartGenerator.Entry> parts = Lists.newArrayList();

    private MultiPartGenerator(Block pBlock) {
        this.block = pBlock;
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    public static MultiPartGenerator multiPart(Block pBlock) {
        return new MultiPartGenerator(pBlock);
    }

    public MultiPartGenerator with(List<Variant> pVariants) {
        this.parts.add(new MultiPartGenerator.Entry(pVariants));
        return this;
    }

    public MultiPartGenerator with(Variant pVariant) {
        return this.with(ImmutableList.of(pVariant));
    }

    public MultiPartGenerator with(Condition pCondition, List<Variant> pVariants) {
        this.parts.add(new MultiPartGenerator.ConditionalEntry(pCondition, pVariants));
        return this;
    }

    public MultiPartGenerator with(Condition pCondition, Variant... pVariants) {
        return this.with(pCondition, ImmutableList.copyOf(pVariants));
    }

    public MultiPartGenerator with(Condition pCondition, Variant pVariant) {
        return this.with(pCondition, ImmutableList.of(pVariant));
    }

    public JsonElement get() {
        StateDefinition<Block, BlockState> statedefinition = this.block.getStateDefinition();
        this.parts.forEach(p_376013_ -> p_376013_.validate(statedefinition));
        JsonArray jsonarray = new JsonArray();
        this.parts.stream().map(MultiPartGenerator.Entry::get).forEach(jsonarray::add);
        JsonObject jsonobject = new JsonObject();
        jsonobject.add("multipart", jsonarray);
        return jsonobject;
    }

    @OnlyIn(Dist.CLIENT)
    static class ConditionalEntry extends MultiPartGenerator.Entry {
        private final Condition condition;

        ConditionalEntry(Condition pCondition, List<Variant> pVariants) {
            super(pVariants);
            this.condition = pCondition;
        }

        @Override
        public void validate(StateDefinition<?, ?> p_376140_) {
            this.condition.validate(p_376140_);
        }

        @Override
        public void decorate(JsonObject p_376267_) {
            p_376267_.add("when", this.condition.get());
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Entry implements Supplier<JsonElement> {
        private final List<Variant> variants;

        Entry(List<Variant> pVariants) {
            this.variants = pVariants;
        }

        public void validate(StateDefinition<?, ?> pStateDefinition) {
        }

        public void decorate(JsonObject pJson) {
        }

        public JsonElement get() {
            JsonObject jsonobject = new JsonObject();
            this.decorate(jsonobject);
            jsonobject.add("apply", Variant.convertList(this.variants));
            return jsonobject;
        }
    }
}