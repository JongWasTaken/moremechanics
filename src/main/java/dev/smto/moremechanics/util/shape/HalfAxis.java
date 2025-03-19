package dev.smto.moremechanics.util.shape;

import net.minecraft.util.math.Direction;

public enum HalfAxis {
    NEG_X("XN", Direction.WEST),
    NEG_Y("YN", Direction.DOWN),
    NEG_Z("ZN", Direction.NORTH),
    POS_X("XP", Direction.EAST),
    POS_Y("YP", Direction.UP),
    POS_Z("ZP", Direction.SOUTH);

    public final int x;
    public final int y;
    public final int z;

    public final Direction dir;

    public final String shortName;

    HalfAxis(String shortName, Direction dir) {
        this.x = dir.getOffsetX();
        this.y = dir.getOffsetY();
        this.z = dir.getOffsetZ();

        this.dir = dir;
        this.shortName = shortName;
    }

    public static final HalfAxis[] VALUES = HalfAxis.values();

    private static final HalfAxis _ZERO = null;

    private static final HalfAxis[][] CROSS_PRODUCTS = {
            {HalfAxis._ZERO, HalfAxis.POS_Z, HalfAxis.NEG_Y, HalfAxis._ZERO, HalfAxis.NEG_Z, HalfAxis.POS_Y}, // NEG_X
            {HalfAxis.NEG_Z, HalfAxis._ZERO, HalfAxis.POS_X, HalfAxis.POS_Z, HalfAxis._ZERO, HalfAxis.NEG_X}, // NEG_Y
            {HalfAxis.POS_Y, HalfAxis.NEG_X, HalfAxis._ZERO, HalfAxis.NEG_Y, HalfAxis.POS_X, HalfAxis._ZERO}, // NEG_Z
            {HalfAxis._ZERO, HalfAxis.NEG_Z, HalfAxis.POS_Y, HalfAxis._ZERO, HalfAxis.POS_Z, HalfAxis.NEG_Y}, // POS_X
            {HalfAxis.POS_Z, HalfAxis._ZERO, HalfAxis.NEG_X, HalfAxis.NEG_Z, HalfAxis._ZERO, HalfAxis.POS_X}, // POS_Y
            {HalfAxis.NEG_Y, HalfAxis.POS_X, HalfAxis._ZERO, HalfAxis.POS_Y, HalfAxis.NEG_X, HalfAxis._ZERO}, // POS_Z
    };

    public static HalfAxis cross(HalfAxis a, HalfAxis b) {
        return HalfAxis.CROSS_PRODUCTS[a.ordinal()][b.ordinal()];
    }

    public HalfAxis cross(HalfAxis other) {
        return HalfAxis.cross(this, other);
    }

    private static final HalfAxis[] NEGATIONS = {
            /* NEG_X = */HalfAxis.POS_X,
            /* NEG_Y = */HalfAxis.POS_Y,
            /* NEG_Z = */HalfAxis.POS_Z,
            /* POS_X = */HalfAxis.NEG_X,
            /* POS_Y = */HalfAxis.NEG_Y,
            /* POS_Z = */HalfAxis.NEG_Z,
    };

    public static HalfAxis negate(HalfAxis axis) {
        return HalfAxis.NEGATIONS[axis.ordinal()];
    }

    public HalfAxis negate() {
        return HalfAxis.negate(this);
    }

    public static HalfAxis fromDirection(Direction dir) {
        switch (dir) {
            case EAST:
                return HalfAxis.POS_X;
            case WEST:
                return HalfAxis.NEG_X;
            case NORTH:
                return HalfAxis.NEG_Z;
            case SOUTH:
                return HalfAxis.POS_Z;
            case DOWN:
                return HalfAxis.NEG_Y;
            case UP:
                return HalfAxis.POS_Y;
            default:
                throw new IllegalArgumentException(dir.toString());
        }
    }
}