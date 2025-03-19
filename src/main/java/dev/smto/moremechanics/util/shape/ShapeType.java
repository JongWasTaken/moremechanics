package dev.smto.moremechanics.util.shape;

import net.minecraft.util.Identifier;
import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.util.shape.generator.*;

public enum ShapeType {
    SPHERE(new ShapeSphereGenerator(ShapeUtils.Octant.ALL), "sphere"),
    CYLINDER(new ShapeCylinderGenerator(), "cylinder"),
    CUBOID(new ShapeCuboidGenerator(ShapeCuboidGenerator.Elements.EDGES), "cuboid"),
    FULL_CUBOID(new ShapeCuboidGenerator(ShapeCuboidGenerator.Elements.WALLS), "full_cuboid"),
    DOME(new ShapeSphereGenerator(ShapeUtils.Octant.TOP), "dome"),
    TRIANGLE(new ShapeEquilateral2dGenerator(3), "triangle"),
    PENTAGON(new ShapeEquilateral2dGenerator(5), "pentagon"),
    HEXAGON(new ShapeEquilateral2dGenerator(6), "hexagon"),
    OCTAGON(new ShapeEquilateral2dGenerator(8), "octagon"),
    AXES(new ShapeAxesGenerator(), "axes"),
    PLANES(new ShapePlanesGenerator(), "planes");

    public final String name;
    public final String translationKey;
    public final ShapeGenerator generator;

    ShapeType(ShapeGenerator generator, String name) {
        this.translationKey = "misc.moremechanics.shape." + name;
        this.name = name;
        this.generator = generator;
    }

    public Identifier getModel() {
        return MoreMechanics.id("gui/shapes/" + this.name);
    }

    public ShapeType decrement() {
        return ShapeType.values()[(this.ordinal() + ShapeType.values().length - 1) % ShapeType.values().length];
    }

    public ShapeType increment() {
        return ShapeType.values()[(this.ordinal() + 1) % ShapeType.values().length];
    }
}
