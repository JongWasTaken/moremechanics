package dev.smto.moremechanics.util;

import net.minecraft.util.math.*;

import java.util.HashMap;

public class DisplayTransformations {
    private static final HashMap<Direction, Transformation> BLOCK_TRANSFORMATIONS = new HashMap<>() {{
        this.put(Direction.SOUTH, new Transformation());
        this.put(Direction.WEST, new Transformation().setTranslation(1.0f, 0.0f, 0.0f).rotateY(Degrees.NINETY));
        this.put(Direction.NORTH, new Transformation().setTranslation(1.0f, 0.0f, 1.0f).rotateY(Degrees.ONE_EIGHTY));
        this.put(Direction.EAST, new Transformation().setTranslation(0.0f, 0.0f, 1.0f).rotateY(Degrees.TWO_SEVENTY));
        this.put(Direction.DOWN, new Transformation().setTranslation(0.0f, 1.0f, 0.0f).rotateX(Degrees.TWO_SEVENTY));
        this.put(Direction.UP, new Transformation().setTranslation(0.0f, 0.0f, 1.0f).rotateX(Degrees.NINETY));
    }};

    private static final HashMap<Direction, Transformation> ITEM_TRANSFORMATIONS = new HashMap<>() {{
        this.put(Direction.SOUTH, new Transformation().setTranslation(0.5f, 0.5f, 0.5f));
        this.put(Direction.WEST, new Transformation().setTranslation(0.5f, 0.5f, 0.5f).rotateY(Degrees.NINETY));
        this.put(Direction.NORTH, new Transformation().setTranslation(0.5f, 0.5f, 0.5f).rotateY(Degrees.ONE_EIGHTY));
        this.put(Direction.EAST, new Transformation().setTranslation(0.5f, 0.5f, 0.5f).rotateY(Degrees.TWO_SEVENTY));
        this.put(Direction.DOWN, new Transformation().setTranslation(0.0f, 0.5f, 0.0f).rotateX(Degrees.TWO_SEVENTY));
        this.put(Direction.UP, new Transformation().setTranslation(0.0f, 0.0f, 0.5f).rotateX(Degrees.NINETY));
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
