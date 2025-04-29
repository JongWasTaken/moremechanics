package dev.smto.moremechanics.util.shape.generator;

import com.google.common.collect.Lists;
import dev.smto.moremechanics.util.shape.ShapeUtils;

import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Function;

public class ShapeEquilateral2dGenerator extends ShapeGenerator {

    private static class Trig {
        final double sin;
        final double cos;

        public Trig(double sin, double cos) {
            this.sin = sin;
            this.cos = cos;
        }
    }

    private static class Point {
        final int x;
        final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "[" + this.x + "," + this.y + "]";
        }

    }

    private enum Symmetry {
        TwoFold(Math.PI) {
            @Override
            public ShapeUtils.Shapeable createMirroredShapeable(ShapeUtils.Shapeable shapeable) {
                return (x, y, z) -> {
                    if (z >= 0) {
                        shapeable.setBlock(x, y, +z);
                        shapeable.setBlock(x, y, -z);
                    }
                };
            }

            @Override
            public Point mirrorLastPoint(Point point) {
                return new Point(point.x, -point.y);
            }
        },
        FourFold(Math.PI / 2) {
            @Override
            public ShapeUtils.Shapeable createMirroredShapeable(ShapeUtils.Shapeable shapeable) {
                return (x, y, z) -> {
                    if (x >= 0 && z >= 0) {
                        shapeable.setBlock(+x, y, -z);
                        shapeable.setBlock(-x, y, -z);
                        shapeable.setBlock(-x, y, +z);
                        shapeable.setBlock(+x, y, +z);
                    }
                };
            }

            @Override
            public Point mirrorLastPoint(Point point) {
                return new Point(-point.x, point.y);
            }
        },
        EightFold(Math.PI / 4) {
            @Override
            public ShapeUtils.Shapeable createMirroredShapeable(ShapeUtils.Shapeable shapeable) {
                return (x, y, z) -> {
                    if (x >= z) {
                        shapeable.setBlock(+x, y, -z);
                        shapeable.setBlock(-x, y, -z);
                        shapeable.setBlock(-x, y, +z);
                        shapeable.setBlock(+x, y, +z);

                        shapeable.setBlock(+z, y, -x);
                        shapeable.setBlock(-z, y, -x);
                        shapeable.setBlock(-z, y, +x);
                        shapeable.setBlock(+z, y, +x);
                    }
                };
            }

            @Override
            public Point mirrorLastPoint(Point point) {
                return new Point(point.y, point.x);
            }
        };

        public abstract ShapeUtils.Shapeable createMirroredShapeable(ShapeUtils.Shapeable shapeable);

        public abstract Point mirrorLastPoint(Point point);

        public final double angleLimit;

        Symmetry(double angleLimit) {
            this.angleLimit = angleLimit;
        }
    }

    private static Symmetry findSymmetry(int sides) {
        if (sides % 4 == 0) return Symmetry.EightFold;
        if (sides % 2 == 0) return Symmetry.FourFold;
        return Symmetry.TwoFold;
    }

    private final Symmetry symmetry;

    private final Trig[] angles;

    public ShapeEquilateral2dGenerator(int sides) {
        this.symmetry = ShapeEquilateral2dGenerator.findSymmetry(sides);

        List<Trig> angles = Lists.newArrayList();
        for (int i = 0; i < sides; i++) {
            double d = 2 * Math.PI * i / sides;
            if (d > this.symmetry.angleLimit) break;
            angles.add(new Trig(Math.sin(d), Math.cos(d)));
        }

        this.angles = angles.toArray(new Trig[angles.size()]);

    }

    @Override
    public void generateShape(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, ShapeUtils.Shapeable shapeable) {
        ShapeUtils.Shapeable columnShapeable = (x, ingored, z) -> {
            for (int y = minY; y <= maxY; y++)
                shapeable.setBlock(x, y, z);
        };

        double middleX = (maxX + minX) / 2.0;
        double radiusX = (maxX - minX) / 2.0;

        double middleZ = (maxZ + minZ) / 2.0;
        double radiusZ = (maxZ - minZ) / 2.0;

        Point[] points = ShapeEquilateral2dGenerator.transform(Point.class, this.angles, input -> {
            int x = (int)Math.round(middleX + radiusX * input.cos);
            int z = (int)Math.round(middleZ + radiusZ * input.sin);
            return new Point(x, z);
        });

        ShapeUtils.Shapeable mirroredShapeable = this.symmetry.createMirroredShapeable(columnShapeable);

        Point prevPoint = points[0];

        for (int i = 1; i < points.length; i++) {
            Point point = points[i];
            ShapeUtils.line2D(0, prevPoint.x, prevPoint.y, point.x, point.y, mirroredShapeable);
            prevPoint = point;
        }

        Point lastPoint = this.symmetry.mirrorLastPoint(prevPoint);
        ShapeUtils.line2D(0, prevPoint.x, prevPoint.y, lastPoint.x, lastPoint.y, mirroredShapeable);
    }


    @SuppressWarnings("unchecked")
    public static <A, B> B[] transform(Class<? extends B> cls, A[] input, Function<A, B> transformer) {
        Object result = Array.newInstance(cls, input.length);
        ShapeEquilateral2dGenerator.transform(input, transformer, result);
        return (B[])result;
    }

    private static <B, A> void transform(A[] input, Function<A, B> transformer, Object result) {
        for (int i = 0; i < input.length; i++) {
            B o = transformer.apply(input[i]);
            Array.set(result, i, o);
        }
    }
}
