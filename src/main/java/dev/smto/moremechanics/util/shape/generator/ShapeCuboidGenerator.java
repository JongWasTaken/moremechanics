package dev.smto.moremechanics.util.shape.generator;

import dev.smto.moremechanics.util.shape.ShapeUtils;
import net.minecraft.util.math.Direction;

public class ShapeCuboidGenerator extends ShapeGenerator {
    public enum Elements {
        CORNERS(true, false, false),
        EDGES(true, true, false),
        WALLS(true, true, true);

        private final boolean corners;

        private final boolean edges;

        private final boolean walls;

        Elements(boolean corners, boolean edges, boolean walls) {
            this.corners = corners;
            this.edges = edges;
            this.walls = walls;
        }
    }

    private final boolean corners;

    private final boolean edges;

    private final boolean walls;

    public ShapeCuboidGenerator(boolean corners, boolean edges, boolean walls) {
        this.corners = corners;
        this.edges = edges;
        this.walls = walls;
    }

    public ShapeCuboidGenerator(Elements elements) {
        this(elements.corners, elements.edges, elements.walls);
    }

    public ShapeCuboidGenerator() {
        this(Elements.WALLS);
    }

    @Override
    public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, ShapeUtils.Shapeable shapeable) {
        int dx = maxX - minX - 2;
        int dy = maxY - minY - 2;
        int dz = maxZ - minZ - 2;

        if (this.corners) {
            shapeable.setBlock(maxX, maxY, maxZ);
            shapeable.setBlock(maxX, maxY, minZ);
            shapeable.setBlock(maxX, minY, maxZ);
            shapeable.setBlock(maxX, minY, minZ);
            shapeable.setBlock(minX, maxY, maxZ);
            shapeable.setBlock(minX, maxY, minZ);
            shapeable.setBlock(minX, minY, maxZ);
            shapeable.setBlock(minX, minY, minZ);
        }

        if (this.edges) {
            ShapeUtils.makeLine(minX, minY + 1, minZ, Direction.Axis.Y, dy, shapeable);
            ShapeUtils.makeLine(minX, minY + 1, maxZ, Direction.Axis.Y, dy, shapeable);
            ShapeUtils.makeLine(maxX, minY + 1, maxZ, Direction.Axis.Y, dy, shapeable);
            ShapeUtils.makeLine(maxX, minY + 1, minZ, Direction.Axis.Y, dy, shapeable);

            ShapeUtils.makeLine(minX + 1, minY, minZ, Direction.Axis.X, dx, shapeable);
            ShapeUtils.makeLine(minX + 1, minY, maxZ, Direction.Axis.X, dx, shapeable);
            ShapeUtils.makeLine(minX + 1, maxY, maxZ, Direction.Axis.X, dx, shapeable);
            ShapeUtils.makeLine(minX + 1, maxY, minZ, Direction.Axis.X, dx, shapeable);

            ShapeUtils.makeLine(minX, minY, minZ + 1, Direction.Axis.Z, dz, shapeable);
            ShapeUtils.makeLine(minX, maxY, minZ + 1, Direction.Axis.Z, dz, shapeable);
            ShapeUtils.makeLine(maxX, maxY, minZ + 1, Direction.Axis.Z, dz, shapeable);
            ShapeUtils.makeLine(maxX, minY, minZ + 1, Direction.Axis.Z, dz, shapeable);
        }

        if (this.walls) {
            ShapeUtils.makePlane(minX + 1, minY + 1, minZ, dx, dy, Direction.Axis.X, Direction.Axis.Y, shapeable);
            ShapeUtils.makePlane(minX + 1, minY + 1, maxZ, dx, dy, Direction.Axis.X, Direction.Axis.Y, shapeable);

            ShapeUtils.makePlane(minX + 1, minY, minZ + 1, dx, dz, Direction.Axis.X, Direction.Axis.Z, shapeable);
            ShapeUtils.makePlane(minX + 1, maxY, minZ + 1, dx, dz, Direction.Axis.X, Direction.Axis.Z, shapeable);

            ShapeUtils.makePlane(minX, minY + 1, minZ + 1, dy, dz, Direction.Axis.Y, Direction.Axis.Z, shapeable);
            ShapeUtils.makePlane(maxX, minY + 1, minZ + 1, dy, dz, Direction.Axis.Y, Direction.Axis.Z, shapeable);
        }
    }
}
