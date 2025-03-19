package dev.smto.moremechanics.util.shape.generator;

import dev.smto.moremechanics.util.shape.ShapeUtils;
import dev.smto.moremechanics.util.shape.Shapeable;

import java.util.Set;

public class ShapeSphereGenerator extends DefaultShapeGenerator {
    private final Set<ShapeUtils.Octant> octants;

    public ShapeSphereGenerator(Set<ShapeUtils.Octant> octants) {
        this.octants = octants;
    }

    @Override
    public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Shapeable shapeable) {
        ShapeUtils.makeEllipsoid(minX, minY, minZ, maxX, maxY, maxZ, shapeable, this.octants);
    }
}
