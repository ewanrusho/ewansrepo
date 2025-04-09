package net.minecraft.world.level;

public class FoliageColor {
    public static final int FOLIAGE_EVERGREEN = -10380959;
    public static final int FOLIAGE_BIRCH = -8345771;
    public static final int FOLIAGE_DEFAULT = -12012264;
    public static final int FOLIAGE_MANGROVE = -7158200;
    private static int[] pixels = new int[65536];

    public static void init(int[] pFoliageBuffer) {
        pixels = pFoliageBuffer;
    }

    public static int get(double pTemperature, double pHumidity) {
        pHumidity *= pTemperature;
        int i = (int)((1.0 - pTemperature) * 255.0);
        int j = (int)((1.0 - pHumidity) * 255.0);
        int k = j << 8 | i;
        return k >= pixels.length ? -12012264 : pixels[k];
    }
}