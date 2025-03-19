package dev.smto.moremechanics.util.shape;

public interface ShapeGenerator {
    void generateShape(int xSize, int ySize, int zSize, Shapeable shapeable);

    void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Shapeable shapeable);
}
