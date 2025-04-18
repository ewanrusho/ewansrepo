package net.minecraft.util;

public class SmoothDouble {
    private double targetValue;
    private double remainingValue;
    private double lastAmount;

    public double getNewDeltaValue(double pInput, double pMultiplier) {
        this.targetValue += pInput;
        double d0 = this.targetValue - this.remainingValue;
        double d1 = Mth.lerp(0.5, this.lastAmount, d0);
        double d2 = Math.signum(d0);
        if (d2 * d0 > d2 * this.lastAmount) {
            d0 = d1;
        }

        this.lastAmount = d1;
        this.remainingValue += d0 * pMultiplier;
        return d0 * pMultiplier;
    }

    public void reset() {
        this.targetValue = 0.0;
        this.remainingValue = 0.0;
        this.lastAmount = 0.0;
    }
}