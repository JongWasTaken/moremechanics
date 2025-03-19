package dev.smto.moremechanics.util.shape.generator;

import dev.smto.moremechanics.util.shape.ShapeUtils;
import dev.smto.moremechanics.util.shape.Shapeable;

import java.util.Set;

public class ShapeCylinderGenerator extends DefaultShapeGenerator {
    private final Set<ShapeUtils.Quadrant> quadrants;

    public ShapeCylinderGenerator() {
        this(ShapeUtils.Quadrant.ALL);
    }

    public ShapeCylinderGenerator(Set<ShapeUtils.Quadrant> quadrants) {
        this.quadrants = quadrants;
    }

    @Override
    public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Shapeable shapeable) {
        ShapeUtils.makeEllipse(minX, minZ, maxX, maxZ, 0, (x, ignore, z) -> {
            for (int y = minY; y <= maxY; y++)
                shapeable.setBlock(x, y, z);
        }, this.quadrants);
    }
}
