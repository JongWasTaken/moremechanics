package dev.smto.moremechanics.util.shape;

import dev.smto.moremechanics.util.GuiUtils;
import net.minecraft.util.Identifier;
import dev.smto.moremechanics.util.shape.generator.*;

public enum ShapeType {
    SPHERE(new ShapeSphereGenerator(ShapeUtils.Octant.ALL), "sphere", GuiUtils.Models.Shapes.SPHERE),
    CYLINDER(new ShapeCylinderGenerator(), "cylinder", GuiUtils.Models.Shapes.CYLINDER),
    CUBOID(new ShapeCuboidGenerator(ShapeCuboidGenerator.Elements.EDGES), "cuboid", GuiUtils.Models.Shapes.CUBOID),
    FULL_CUBOID(new ShapeCuboidGenerator(ShapeCuboidGenerator.Elements.WALLS), "full_cuboid", GuiUtils.Models.Shapes.FULL_CUBOID),
    DOME(new ShapeSphereGenerator(ShapeUtils.Octant.TOP), "dome", GuiUtils.Models.Shapes.DOME),
    TRIANGLE(new ShapeEquilateral2dGenerator(3), "triangle", GuiUtils.Models.Shapes.TRIANGLE),
    PENTAGON(new ShapeEquilateral2dGenerator(5), "pentagon", GuiUtils.Models.Shapes.PENTAGON),
    HEXAGON(new ShapeEquilateral2dGenerator(6), "hexagon", GuiUtils.Models.Shapes.HEXAGON),
    OCTAGON(new ShapeEquilateral2dGenerator(8), "octagon", GuiUtils.Models.Shapes.OCTAGON),
    AXES(new ShapeAxesGenerator(), "axes", GuiUtils.Models.Shapes.AXES),
    PLANES(new ShapePlanesGenerator(), "planes", GuiUtils.Models.Shapes.PLANES);

    public final String name;
    public final String translationKey;
    public final Identifier model;
    public final ShapeGenerator generator;

    ShapeType(ShapeGenerator generator, String name, Identifier model) {
        this.translationKey = "misc.moremechanics.shape." + name;
        this.name = name;
        this.model = model;
        this.generator = generator;
    }

    public Identifier getModel() {
        return this.model;
    }
}
