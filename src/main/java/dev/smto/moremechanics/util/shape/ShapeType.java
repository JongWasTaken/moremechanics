package dev.smto.moremechanics.util.shape;

import dev.smto.moremechanics.util.GuiUtils;
import net.minecraft.util.Identifier;
import dev.smto.moremechanics.util.shape.generator.*;

public enum ShapeType {
    SPHERE(new ShapeSphereGenerator(ShapeUtils.Octant.ALL), "sphere", GuiUtils.Models.SPHERE),
    CYLINDER(new ShapeCylinderGenerator(), "cylinder", GuiUtils.Models.CYLINDER),
    CUBOID(new ShapeCuboidGenerator(ShapeCuboidGenerator.Elements.EDGES), "cuboid", GuiUtils.Models.CUBOID),
    FULL_CUBOID(new ShapeCuboidGenerator(ShapeCuboidGenerator.Elements.WALLS), "full_cuboid", GuiUtils.Models.FULL_CUBOID),
    DOME(new ShapeSphereGenerator(ShapeUtils.Octant.TOP), "dome", GuiUtils.Models.DOME),
    TRIANGLE(new ShapeEquilateral2dGenerator(3), "triangle", GuiUtils.Models.TRIANGLE),
    PENTAGON(new ShapeEquilateral2dGenerator(5), "pentagon", GuiUtils.Models.PENTAGON),
    HEXAGON(new ShapeEquilateral2dGenerator(6), "hexagon", GuiUtils.Models.HEXAGON),
    OCTAGON(new ShapeEquilateral2dGenerator(8), "octagon", GuiUtils.Models.OCTAGON),
    AXES(new ShapeAxesGenerator(), "axes", GuiUtils.Models.AXES),
    PLANES(new ShapePlanesGenerator(), "planes", GuiUtils.Models.PLANES);

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
