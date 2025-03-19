package dev.smto.moremechanics.util.shape.generator;

import dev.smto.moremechanics.util.shape.ShapeGenerator;
import dev.smto.moremechanics.util.shape.Shapeable;

public class DefaultShapeGenerator implements ShapeGenerator {
    @Override
    public void generateShape(int xSize, int ySize, int zSize, Shapeable shapeable) {
        this.generateShape(-xSize, -ySize, -zSize, +xSize, +ySize, +zSize, shapeable);

    }

    @Override
    public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Shapeable shapeable) {}
}
