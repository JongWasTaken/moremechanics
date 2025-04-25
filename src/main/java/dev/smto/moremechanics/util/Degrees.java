package dev.smto.moremechanics.util;

@SuppressWarnings("unused")
public enum Degrees {
    D0(0.0f),
    D1_125((float)(-Math.PI * 0.00625)),
    D2_25((float)(-Math.PI * 0.0125)),
    D4_5((float)(-Math.PI * 0.025)),
    D9((float)(-Math.PI * 0.05)),
    D18((float)(-Math.PI * 0.1)),
    D45((float)(-Math.PI * 0.25)),
    D90((float)(-Math.PI * 0.5)),
    D120((float)(-Math.PI * 0.75)),
    D180((float) Math.PI),
    D225((float)(Math.PI * 0.25)),
    D270((float)(Math.PI * 0.5)),
    D315((float)(Math.PI * 0.75));

    private final float radians;
    Degrees(float radians) {
        this.radians = radians;
    }

    public float getRadians() {
        return this.radians;
    }
}