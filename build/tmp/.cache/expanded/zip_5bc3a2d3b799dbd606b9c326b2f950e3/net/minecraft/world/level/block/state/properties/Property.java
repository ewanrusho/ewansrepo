package net.minecraft.world.level.block.state.properties;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.StateHolder;

public abstract class Property<T extends Comparable<T>> {
    private final Class<T> clazz;
    private final String name;
    @Nullable
    private Integer hashCode;
    private final Codec<T> codec = Codec.STRING
        .comapFlatMap(
            p_61698_ -> this.getValue(p_61698_)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unable to read property: " + this + " with value: " + p_61698_)),
            this::getName
        );
    private final Codec<Property.Value<T>> valueCodec = this.codec.xmap(this::value, Property.Value::value);

    protected Property(String pName, Class<T> pClazz) {
        this.clazz = pClazz;
        this.name = pName;
    }

    public Property.Value<T> value(T pValue) {
        return new Property.Value<>(this, pValue);
    }

    public Property.Value<T> value(StateHolder<?, ?> pHolder) {
        return new Property.Value<>(this, pHolder.getValue(this));
    }

    public Stream<Property.Value<T>> getAllValues() {
        return this.getPossibleValues().stream().map(this::value);
    }

    public Codec<T> codec() {
        return this.codec;
    }

    public Codec<Property.Value<T>> valueCodec() {
        return this.valueCodec;
    }

    public String getName() {
        return this.name;
    }

    public Class<T> getValueClass() {
        return this.clazz;
    }

    public abstract List<T> getPossibleValues();

    public abstract String getName(T pValue);

    public abstract Optional<T> getValue(String pValue);

    public abstract int getInternalIndex(T pValue);

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", this.name).add("clazz", this.clazz).add("values", this.getPossibleValues()).toString();
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof Property<?> property) ? false : this.clazz.equals(property.clazz) && this.name.equals(property.name);
        }
    }

    @Override
    public final int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = this.generateHashCode();
        }

        return this.hashCode;
    }

    public int generateHashCode() {
        return 31 * this.clazz.hashCode() + this.name.hashCode();
    }

    public <U, S extends StateHolder<?, S>> DataResult<S> parseValue(DynamicOps<U> pOps, S pStateHolder, U pUnparsedValue) {
        DataResult<T> dataresult = this.codec.parse(pOps, pUnparsedValue);
        return dataresult.<S>map(p_156030_ -> pStateHolder.setValue(this, p_156030_)).setPartial(pStateHolder);
    }

    public static record Value<T extends Comparable<T>>(Property<T> property, T value) {
        public Value(Property<T> property, T value) {
            if (!property.getPossibleValues().contains(value)) {
                throw new IllegalArgumentException("Value " + value + " does not belong to property " + property);
            } else {
                this.property = property;
                this.value = value;
            }
        }

        @Override
        public String toString() {
            return this.property.getName() + "=" + this.property.getName(this.value);
        }
    }
}