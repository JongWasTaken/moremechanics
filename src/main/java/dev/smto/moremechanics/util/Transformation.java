package dev.smto.moremechanics.util;

import net.minecraft.util.math.AffineTransformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@SuppressWarnings("unused")
public class Transformation {
    public static Transformation DEFAULT = new Transformation();

    private Vector3f translation;
    private Quaternionf leftRotation;
    private Vector3f scale;
    private final Quaternionf rightRotation;

    public Transformation() {
        this.translation = new Vector3f();
        this.leftRotation = new Quaternionf();
        this.scale = new Vector3f(1.0F, 1.0F, 1.0F);
        this.rightRotation = new Quaternionf();
    };

    public Transformation(Transformation copy) throws CloneNotSupportedException {
        this.translation = (Vector3f) copy.translation.clone();
        this.leftRotation = (Quaternionf) copy.leftRotation.clone();
        this.scale = (Vector3f) copy.scale.clone();
        this.rightRotation = (Quaternionf) copy.rightRotation.clone();
    };

    public Transformation(Quaternionf rightRotation) {
        this.translation = new Vector3f();
        this.leftRotation = new Quaternionf();
        this.scale = new Vector3f(1.0F, 1.0F, 1.0F);
        this.rightRotation = rightRotation;
    };

    public static Transformation of(AffineTransformation transformation) {
        return new Transformation(transformation.getRightRotation())
                .setTranslation(transformation.getTranslation())
                .setRotation(transformation.getLeftRotation())
                .setScale(transformation.getScale());
    }

    public Transformation setTranslation(Vector3f translation) {
        this.translation = translation;
        return this;
    }

    public Transformation setTranslation(float x, float y, float z) {
        this.translation = new Vector3f(x, y, z);
        return this;
    }

    public Transformation setTranslation(float all) {
        this.translation = new Vector3f(all, all, all);
        return this;
    }

    public Transformation addTranslation(Vector3f translation) {
        this.translation.add(translation);
        return this;
    }

    public Transformation addTranslation(float x, float y, float z) {
        this.translation.add(x, y, z);
        return this;
    }

    public Transformation addTranslation(float all) {
        this.translation.add(all, all, all);
        return this;
    }

    /**
     * Z is 2d rotation. X is rotation up and down. Y is depth rotation.<br>
     * I hope that makes sense...
     */
    public Transformation setRotation(Quaternionf leftRotation) {
        this.leftRotation = leftRotation;
        return this;
    }

    public Transformation rotateX(Degrees degrees) {
        this.leftRotation.rotateX(degrees.getRadians());
        return this;
    }

    public Transformation rotateY(Degrees degrees) {
        this.leftRotation.rotateY(degrees.getRadians());
        return this;
    }

    public Transformation rotateZ(Degrees degrees) {
        this.leftRotation.rotateZ(degrees.getRadians());
        return this;
    }

    public Transformation rotateX(float radians) {
        this.leftRotation.rotateX(radians);
        return this;
    }

    public Transformation rotateY(float radians) {
        this.leftRotation.rotateY(radians);
        return this;
    }

    public Transformation rotateZ(float radians) {
        this.leftRotation.rotateZ(radians);
        return this;
    }

    public Transformation setScale(Vector3f scale) {
        this.scale = scale;
        return this;
    }

    public Transformation setScale(float x, float y, float z) {
        this.scale = new Vector3f(x, y, z);
        return this;
    }

    public Transformation setScale(float all) {
        this.scale = new Vector3f(all, all, all);
        return this;
    }

    public Transformation copy() {
        try {
            return new Transformation(this);
        } catch (Throwable ignored) {}
        return new Transformation();
    }

    public AffineTransformation toVanilla() {
        return new AffineTransformation(
                this.translation,
                this.leftRotation,
                this.scale,
                this.rightRotation
        );
    }
}
