package dev.smto.moremechanics.util.shape.generator;

import dev.smto.moremechanics.util.shape.ShapeUtils;

public class ShapeAxesGenerator extends ShapeGenerator {
    @Override
    public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, ShapeUtils.Shapeable shapeable) {
        for (int x = minX; x <= maxX; x++)
            shapeable.setBlock(x, 0, 0);

        for (int y = minY; y <= maxY; y++)
            shapeable.setBlock(0, y, 0);

        for (int z = minZ; z <= maxZ; z++)
            shapeable.setBlock(0, 0, z);
    }
}
