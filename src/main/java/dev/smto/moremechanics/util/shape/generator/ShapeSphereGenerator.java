package dev.smto.moremechanics.util.shape.generator;

import dev.smto.moremechanics.util.shape.ShapeUtils;

import java.util.Set;

public class ShapeSphereGenerator extends ShapeGenerator {
    private final Set<ShapeUtils.Octant> octants;

    public ShapeSphereGenerator(Set<ShapeUtils.Octant> octants) {
        this.octants = octants;
    }

    @Override
    public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, ShapeUtils.Shapeable shapeable) {
        ShapeUtils.makeEllipsoid(minX, minY, minZ, maxX, maxY, maxZ, shapeable, this.octants);
    }
}
