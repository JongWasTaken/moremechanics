package dev.smto.moremechanics.util;

public enum Degrees {
    ZERO(0.0f),
    FORTY_FIVE((float)(-Math.PI * 0.25)),
    NINETY((float)(-Math.PI * 0.5)),
    ONE_TWENTY((float)(-Math.PI * 0.75)),
    ONE_EIGHTY((float) Math.PI),
    TWO_TWENTY_FIVE((float)(Math.PI * 0.25)),
    TWO_SEVENTY((float)(Math.PI * 0.5)),
    THREE_FIFTEEN((float)(Math.PI * 0.75));

    private final float radians;
    Degrees(float radians) {
        this.radians = radians;
    }

    public float getRadians() {
        return this.radians;
    }
}