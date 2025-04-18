package net.minecraft.client;

import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.OptionEnum;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum GraphicsStatus implements OptionEnum {
    FAST(0, "options.graphics.fast"),
    FANCY(1, "options.graphics.fancy"),
    FABULOUS(2, "options.graphics.fabulous");

    private static final IntFunction<GraphicsStatus> BY_ID = ByIdMap.continuous(GraphicsStatus::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    private final int id;
    private final String key;

    private GraphicsStatus(final int pId, final String pKey) {
        this.id = pId;
        this.key = pKey;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return switch (this) {
            case FAST -> "fast";
            case FANCY -> "fancy";
            case FABULOUS -> "fabulous";
        };
    }

    public static GraphicsStatus byId(int pId) {
        return BY_ID.apply(pId);
    }
}