package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyValueCondition implements Condition {
    private static final Splitter PIPE_SPLITTER = Splitter.on('|').omitEmptyStrings();
    private final String key;
    private final String value;

    public KeyValueCondition(String pKey, String pValue) {
        this.key = pKey;
        this.value = pValue;
    }

    @Override
    public Predicate<BlockState> getPredicate(StateDefinition<Block, BlockState> p_111960_) {
        Property<?> property = p_111960_.getProperty(this.key);
        if (property == null) {
            throw new RuntimeException(String.format(Locale.ROOT, "Unknown property '%s' on '%s'", this.key, p_111960_.getOwner()));
        } else {
            String s = this.value;
            boolean flag = !s.isEmpty() && s.charAt(0) == '!';
            if (flag) {
                s = s.substring(1);
            }

            List<String> list = PIPE_SPLITTER.splitToList(s);
            if (list.isEmpty()) {
                throw new RuntimeException(
                    String.format(Locale.ROOT, "Empty value '%s' for property '%s' on '%s'", this.value, this.key, p_111960_.getOwner())
                );
            } else {
                Predicate<BlockState> predicate;
                if (list.size() == 1) {
                    predicate = this.getBlockStatePredicate(p_111960_, property, s);
                } else {
                    predicate = Util.anyOf(list.stream().map(p_111958_ -> this.getBlockStatePredicate(p_111960_, property, p_111958_)).toList());
                }

                return flag ? predicate.negate() : predicate;
            }
        }
    }

    private Predicate<BlockState> getBlockStatePredicate(StateDefinition<Block, BlockState> pDefinition, Property<?> pProperty, String pValue) {
        Optional<?> optional = pProperty.getValue(pValue);
        if (optional.isEmpty()) {
            throw new RuntimeException(
                String.format(
                    Locale.ROOT, "Unknown value '%s' for property '%s' on '%s' in '%s'", pValue, this.key, pDefinition.getOwner(), this.value
                )
            );
        } else {
            return p_325519_ -> p_325519_.getValue(pProperty).equals(optional.get());
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("key", this.key).add("value", this.value).toString();
    }
}