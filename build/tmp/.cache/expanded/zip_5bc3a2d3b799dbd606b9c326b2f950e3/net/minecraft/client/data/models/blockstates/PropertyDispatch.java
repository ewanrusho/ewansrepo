package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class PropertyDispatch {
    private final Map<Selector, List<Variant>> values = Maps.newHashMap();

    protected void putValue(Selector pSelector, List<Variant> pVariants) {
        List<Variant> list = this.values.put(pSelector, pVariants);
        if (list != null) {
            throw new IllegalStateException("Value " + pSelector + " is already defined");
        }
    }

    Map<Selector, List<Variant>> getEntries() {
        this.verifyComplete();
        return ImmutableMap.copyOf(this.values);
    }

    private void verifyComplete() {
        List<Property<?>> list = this.getDefinedProperties();
        Stream<Selector> stream = Stream.of(Selector.empty());

        for (Property<?> property : list) {
            stream = stream.flatMap(p_378010_ -> property.getAllValues().map(p_378010_::extend));
        }

        List<Selector> list1 = stream.filter(p_377619_ -> !this.values.containsKey(p_377619_)).collect(Collectors.toList());
        if (!list1.isEmpty()) {
            throw new IllegalStateException("Missing definition for properties: " + list1);
        }
    }

    abstract List<Property<?>> getDefinedProperties();

    public static <T1 extends Comparable<T1>> PropertyDispatch.C1<T1> property(Property<T1> pProperty1) {
        return new PropertyDispatch.C1<>(pProperty1);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<T1, T2> properties(Property<T1> pProperty1, Property<T2> pProperty2) {
        return new PropertyDispatch.C2<>(pProperty1, pProperty2);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<T1, T2, T3> properties(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3
    ) {
        return new PropertyDispatch.C3<>(pProperty1, pProperty2, pProperty3);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<T1, T2, T3, T4> properties(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4
    ) {
        return new PropertyDispatch.C4<>(pProperty1, pProperty2, pProperty3, pProperty4);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<T1, T2, T3, T4, T5> properties(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4, Property<T5> pProperty5
    ) {
        return new PropertyDispatch.C5<>(pProperty1, pProperty2, pProperty3, pProperty4, pProperty5);
    }

    @OnlyIn(Dist.CLIENT)
    public static class C1<T1 extends Comparable<T1>> extends PropertyDispatch {
        private final Property<T1> property1;

        C1(Property<T1> pProperty1) {
            this.property1 = pProperty1;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1);
        }

        public PropertyDispatch.C1<T1> select(T1 pValue1, List<Variant> pVariants) {
            Selector selector = Selector.of(this.property1.value(pValue1));
            this.putValue(selector, pVariants);
            return this;
        }

        public PropertyDispatch.C1<T1> select(T1 pValue1, Variant pVariant) {
            return this.select(pValue1, Collections.singletonList(pVariant));
        }

        public PropertyDispatch generate(Function<T1, Variant> pGenerator) {
            this.property1.getPossibleValues().forEach(p_376551_ -> this.select((T1)p_376551_, pGenerator.apply((T1)p_376551_)));
            return this;
        }

        public PropertyDispatch generateList(Function<T1, List<Variant>> pGenerator) {
            this.property1.getPossibleValues().forEach(p_376612_ -> this.select((T1)p_376612_, pGenerator.apply((T1)p_376612_)));
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C2<T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends PropertyDispatch {
        private final Property<T1> property1;
        private final Property<T2> property2;

        C2(Property<T1> pProperty1, Property<T2> pProperty2) {
            this.property1 = pProperty1;
            this.property2 = pProperty2;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1, this.property2);
        }

        public PropertyDispatch.C2<T1, T2> select(T1 pValue1, T2 pValue2, List<Variant> pVariants) {
            Selector selector = Selector.of(this.property1.value(pValue1), this.property2.value(pValue2));
            this.putValue(selector, pVariants);
            return this;
        }

        public PropertyDispatch.C2<T1, T2> select(T1 pValue1, T2 pValue2, Variant pVariant) {
            return this.select(pValue1, pValue2, Collections.singletonList(pVariant));
        }

        public PropertyDispatch generate(BiFunction<T1, T2, Variant> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_377154_ -> this.property2
                            .getPossibleValues()
                            .forEach(p_376123_ -> this.select((T1)p_377154_, (T2)p_376123_, pGenerator.apply((T1)p_377154_, (T2)p_376123_)))
                );
            return this;
        }

        public PropertyDispatch generateList(BiFunction<T1, T2, List<Variant>> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_375491_ -> this.property2
                            .getPossibleValues()
                            .forEach(p_375416_ -> this.select((T1)p_375491_, (T2)p_375416_, pGenerator.apply((T1)p_375491_, (T2)p_375416_)))
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C3<T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> extends PropertyDispatch {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;

        C3(Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3) {
            this.property1 = pProperty1;
            this.property2 = pProperty2;
            this.property3 = pProperty3;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1, this.property2, this.property3);
        }

        public PropertyDispatch.C3<T1, T2, T3> select(T1 pValue1, T2 pValue2, T3 pValue3, List<Variant> pVariants) {
            Selector selector = Selector.of(this.property1.value(pValue1), this.property2.value(pValue2), this.property3.value(pValue3));
            this.putValue(selector, pVariants);
            return this;
        }

        public PropertyDispatch.C3<T1, T2, T3> select(T1 pValue1, T2 pValue2, T3 pValue3, Variant pVariant) {
            return this.select(pValue1, pValue2, pValue3, Collections.singletonList(pVariant));
        }

        public PropertyDispatch generate(PropertyDispatch.TriFunction<T1, T2, T3, Variant> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_377047_ -> this.property2
                            .getPossibleValues()
                            .forEach(
                                p_377231_ -> this.property3
                                        .getPossibleValues()
                                        .forEach(
                                            p_378204_ -> this.select(
                                                    (T1)p_377047_,
                                                    (T2)p_377231_,
                                                    (T3)p_378204_,
                                                    pGenerator.apply((T1)p_377047_, (T2)p_377231_, (T3)p_378204_)
                                                )
                                        )
                            )
                );
            return this;
        }

        public PropertyDispatch generateList(PropertyDispatch.TriFunction<T1, T2, T3, List<Variant>> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_378613_ -> this.property2
                            .getPossibleValues()
                            .forEach(
                                p_376416_ -> this.property3
                                        .getPossibleValues()
                                        .forEach(
                                            p_377650_ -> this.select(
                                                    (T1)p_378613_,
                                                    (T2)p_376416_,
                                                    (T3)p_377650_,
                                                    pGenerator.apply((T1)p_378613_, (T2)p_376416_, (T3)p_377650_)
                                                )
                                        )
                            )
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C4<T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> extends PropertyDispatch {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;
        private final Property<T4> property4;

        C4(Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4) {
            this.property1 = pProperty1;
            this.property2 = pProperty2;
            this.property3 = pProperty3;
            this.property4 = pProperty4;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1, this.property2, this.property3, this.property4);
        }

        public PropertyDispatch.C4<T1, T2, T3, T4> select(T1 pValue1, T2 pValue2, T3 pValue3, T4 pValue4, List<Variant> pVariants) {
            Selector selector = Selector.of(
                this.property1.value(pValue1), this.property2.value(pValue2), this.property3.value(pValue3), this.property4.value(pValue4)
            );
            this.putValue(selector, pVariants);
            return this;
        }

        public PropertyDispatch.C4<T1, T2, T3, T4> select(T1 pValue1, T2 pValue2, T3 pValue3, T4 pValue4, Variant pVariant) {
            return this.select(pValue1, pValue2, pValue3, pValue4, Collections.singletonList(pVariant));
        }

        public PropertyDispatch generate(PropertyDispatch.QuadFunction<T1, T2, T3, T4, Variant> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_376254_ -> this.property2
                            .getPossibleValues()
                            .forEach(
                                p_375541_ -> this.property3
                                        .getPossibleValues()
                                        .forEach(
                                            p_376281_ -> this.property4
                                                    .getPossibleValues()
                                                    .forEach(
                                                        p_378745_ -> this.select(
                                                                (T1)p_376254_,
                                                                (T2)p_375541_,
                                                                (T3)p_376281_,
                                                                (T4)p_378745_,
                                                                pGenerator.apply((T1)p_376254_, (T2)p_375541_, (T3)p_376281_, (T4)p_378745_)
                                                            )
                                                    )
                                        )
                            )
                );
            return this;
        }

        public PropertyDispatch generateList(PropertyDispatch.QuadFunction<T1, T2, T3, T4, List<Variant>> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_375676_ -> this.property2
                            .getPossibleValues()
                            .forEach(
                                p_377581_ -> this.property3
                                        .getPossibleValues()
                                        .forEach(
                                            p_377467_ -> this.property4
                                                    .getPossibleValues()
                                                    .forEach(
                                                        p_378381_ -> this.select(
                                                                (T1)p_375676_,
                                                                (T2)p_377581_,
                                                                (T3)p_377467_,
                                                                (T4)p_378381_,
                                                                pGenerator.apply((T1)p_375676_, (T2)p_377581_, (T3)p_377467_, (T4)p_378381_)
                                                            )
                                                    )
                                        )
                            )
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C5<T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>>
        extends PropertyDispatch {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;
        private final Property<T4> property4;
        private final Property<T5> property5;

        C5(Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4, Property<T5> pProperty5) {
            this.property1 = pProperty1;
            this.property2 = pProperty2;
            this.property3 = pProperty3;
            this.property4 = pProperty4;
            this.property5 = pProperty5;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1, this.property2, this.property3, this.property4, this.property5);
        }

        public PropertyDispatch.C5<T1, T2, T3, T4, T5> select(T1 pValue1, T2 pValue2, T3 pValue3, T4 pValue4, T5 pValue5, List<Variant> pVariants) {
            Selector selector = Selector.of(
                this.property1.value(pValue1),
                this.property2.value(pValue2),
                this.property3.value(pValue3),
                this.property4.value(pValue4),
                this.property5.value(pValue5)
            );
            this.putValue(selector, pVariants);
            return this;
        }

        public PropertyDispatch.C5<T1, T2, T3, T4, T5> select(T1 pValue1, T2 pValue2, T3 pValue3, T4 pValue4, T5 pValue5, Variant pVariant) {
            return this.select(pValue1, pValue2, pValue3, pValue4, pValue5, Collections.singletonList(pVariant));
        }

        public PropertyDispatch generate(PropertyDispatch.PentaFunction<T1, T2, T3, T4, T5, Variant> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_376257_ -> this.property2
                            .getPossibleValues()
                            .forEach(
                                p_378211_ -> this.property3
                                        .getPossibleValues()
                                        .forEach(
                                            p_376810_ -> this.property4
                                                    .getPossibleValues()
                                                    .forEach(
                                                        p_378107_ -> this.property5
                                                                .getPossibleValues()
                                                                .forEach(
                                                                    p_375506_ -> this.select(
                                                                            (T1)p_376257_,
                                                                            (T2)p_378211_,
                                                                            (T3)p_376810_,
                                                                            (T4)p_378107_,
                                                                            (T5)p_375506_,
                                                                            pGenerator.apply(
                                                                                (T1)p_376257_, (T2)p_378211_, (T3)p_376810_, (T4)p_378107_, (T5)p_375506_
                                                                            )
                                                                        )
                                                                )
                                                    )
                                        )
                            )
                );
            return this;
        }

        public PropertyDispatch generateList(PropertyDispatch.PentaFunction<T1, T2, T3, T4, T5, List<Variant>> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_378354_ -> this.property2
                            .getPossibleValues()
                            .forEach(
                                p_377191_ -> this.property3
                                        .getPossibleValues()
                                        .forEach(
                                            p_376509_ -> this.property4
                                                    .getPossibleValues()
                                                    .forEach(
                                                        p_375940_ -> this.property5
                                                                .getPossibleValues()
                                                                .forEach(
                                                                    p_377883_ -> this.select(
                                                                            (T1)p_378354_,
                                                                            (T2)p_377191_,
                                                                            (T3)p_376509_,
                                                                            (T4)p_375940_,
                                                                            (T5)p_377883_,
                                                                            pGenerator.apply(
                                                                                (T1)p_378354_, (T2)p_377191_, (T3)p_376509_, (T4)p_375940_, (T5)p_377883_
                                                                            )
                                                                        )
                                                                )
                                                    )
                                        )
                            )
                );
            return this;
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface PentaFunction<P1, P2, P3, P4, P5, R> {
        R apply(P1 pP1, P2 pP2, P3 pP3, P4 pP4, P5 pP5);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface QuadFunction<P1, P2, P3, P4, R> {
        R apply(P1 pP1, P2 pP2, P3 pP3, P4 pP4);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface TriFunction<P1, P2, P3, R> {
        R apply(P1 pP1, P2 pP2, P3 pP3);
    }
}