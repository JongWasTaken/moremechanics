package dev.smto.moremechanics.util.shape.generator;

import dev.smto.moremechanics.util.shape.ShapeUtils;

import java.util.Set;

public class ShapeCylinderGenerator extends ShapeGenerator {
    private final Set<ShapeUtils.Quadrant> quadrants;

    public ShapeCylinderGenerator() {
        this(ShapeUtils.Quadrant.ALL);
    }

    public ShapeCylinderGenerator(Set<ShapeUtils.Quadrant> quadrants) {
        this.quadrants = quadrants;
    }

    @Override
    public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, ShapeUtils.Shapeable shapeable) {
        ShapeUtils.makeEllipse(minX, minZ, maxX, maxZ, 0, (x, ignore, z) -> {
            for (int y = minY; y <= maxY; y++)
                shapeable.setBlock(x, y, z);
        }, this.quadrants);
    }
}
