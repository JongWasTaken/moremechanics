package dev.smto.moremechanics.util;

import net.minecraft.util.math.*;

import java.util.HashMap;

public class DisplayTransformations {
    private static final HashMap<Direction, Transformation> BLOCK_TRANSFORMATIONS = new HashMap<>() {{
        this.put(Direction.SOUTH, new Transformation());
        this.put(Direction.WEST, new Transformation().setTranslation(1.0f, 0.0f, 0.0f).rotateY(Degrees.D90));
        this.put(Direction.NORTH, new Transformation().setTranslation(1.0f, 0.0f, 1.0f).rotateY(Degrees.D180));
        this.put(Direction.EAST, new Transformation().setTranslation(0.0f, 0.0f, 1.0f).rotateY(Degrees.D270));
        this.put(Direction.DOWN, new Transformation().setTranslation(0.0f, 1.0f, 0.0f).rotateX(Degrees.D270));
        this.put(Direction.UP, new Transformation().setTranslation(0.0f, 0.0f, 1.0f).rotateX(Degrees.D90));
    }};

    private static final HashMap<Direction, Transformation> ITEM_TRANSFORMATIONS = new HashMap<>() {{
        this.put(Direction.SOUTH, new Transformation().setTranslation(0.5f, 0.5f, 0.5f));
        this.put(Direction.WEST, new Transformation().setTranslation(0.5f, 0.5f, 0.5f).rotateY(Degrees.D90));
        this.put(Direction.NORTH, new Transformation().setTranslation(0.5f, 0.5f, 0.5f).rotateY(Degrees.D180));
        this.put(Direction.EAST, new Transformation().setTranslation(0.5f, 0.5f, 0.5f).rotateY(Degrees.D270));
        this.put(Direction.DOWN, new Transformation().setTranslation(0.0f, 0.5f, 0.0f).rotateX(Degrees.D270));
        this.put(Direction.UP, new Transformation().setTranslation(0.0f, 0.0f, 0.5f).rotateX(Degrees.D90));
    }};

    public static Transformation getForBlock(Direction direction) {
        if (direction == null) direction = Direction.NORTH;
        return DisplayTransformations.BLOCK_TRANSFORMATIONS.get(direction).copy();
    }

    public static Transformation getForItem(Direction direction) {
        if (direction == null) direction = Direction.NORTH;
        return DisplayTransformations.ITEM_TRANSFORMATIONS.get(direction).copy();
    }

}
