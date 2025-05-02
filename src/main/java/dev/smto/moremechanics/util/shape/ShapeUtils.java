package dev.smto.moremechanics.util.shape;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ShapeUtils {

    public enum Octant {
        TopSouthWest("Top South West", Direction.WEST, Direction.UP, Direction.SOUTH),
        TopNorthEast("Top North East", Direction.EAST, Direction.UP, Direction.NORTH),
        TopNorthWest("Top North West", Direction.WEST, Direction.UP, Direction.NORTH),
        TopSouthEast("Top South East", Direction.EAST, Direction.UP, Direction.SOUTH),
        BottomSouthWest("Bottom South West", Direction.WEST, Direction.DOWN, Direction.SOUTH),
        BottomNorthEast("Bottom North East", Direction.EAST, Direction.DOWN, Direction.NORTH),
        BottomNorthWest("Bottom North West", Direction.WEST, Direction.DOWN, Direction.NORTH),
        BottomSouthEast("Bottom South East", Direction.EAST, Direction.DOWN, Direction.SOUTH);

        public static final EnumSet<Octant> ALL = EnumSet.allOf(Octant.class);
        public static final EnumSet<Octant> TOP = Octant.select(Direction.UP);
        public static final EnumSet<Octant> BOTTOM = Octant.select(Direction.DOWN);
        public static final EnumSet<Octant> NORTH = Octant.select(Direction.NORTH);
        public static final EnumSet<Octant> SOUTH = Octant.select(Direction.SOUTH);
        public static final EnumSet<Octant> EAST = Octant.select(Direction.EAST);
        public static final EnumSet<Octant> WEST = Octant.select(Direction.WEST);

        public final EnumSet<Direction> dirs;
        public final int x, y, z;
        public final String name;

        public int getXOffset() {
            return this.x;
        }

        public int getYOffset() {
            return this.y;
        }

        public int getZOffset() {
            return this.z;
        }

        public String getFriendlyName() {
            return this.name;
        }

        Octant(String friendlyName, Direction dirX, Direction dirY, Direction dirZ) {
            this.x = dirX.getOffsetX() + dirY.getOffsetX() + dirZ.getOffsetX();
            this.y = dirX.getOffsetY() + dirY.getOffsetY() + dirZ.getOffsetY();
            this.z = dirX.getOffsetZ() + dirY.getOffsetZ() + dirZ.getOffsetZ();
            this.dirs = EnumSet.of(dirX, dirY, dirZ);
            this.name = friendlyName;
        }

        private static EnumSet<Octant> select(Direction dir) {
            Set<Octant> result = Sets.newIdentityHashSet();
            for (Octant o : Octant.values())
                if (o.dirs.contains(dir))
                    result.add(o);

            return EnumSet.copyOf(result);
        }
    }

    public enum Quadrant {
        TopSouthWest(-1, 1),
        TopNorthEast(1, -1),
        TopNorthWest(-1, -1),
        TopSouthEast(1, 1);

        public static final EnumSet<Quadrant> ALL = EnumSet.allOf(Quadrant.class);

        public final int x;
        public final int z;

        Quadrant(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    public static void makeLine(int startX, int startY, int startZ, Direction.Axis axis, int length, Shapeable shapeable) {
        ShapeUtils.makeLine(startX, startY, startZ, axis.getPositiveDirection(), length, shapeable);
    }

    /**
     * Makes a link of blocks in a shape
     */
    public static void makeLine(int startX, int startY, int startZ, Direction direction, int length, Shapeable shapeable) {
        if (length < 0) return;
        Vec3i v = direction.getVector();
        for (int offset = 0; offset <= length; offset++)
            // Create a line in the direction of direction, length in size
            shapeable.setBlock(
                    startX + (offset * v.getX()),
                    startY + (offset * v.getY()),
                    startZ + (offset * v.getZ()));

    }

    public static void makePlane(int startX, int startY, int startZ, int width, int height, Direction.Axis right, Direction.Axis up, Shapeable shapeable) {
        ShapeUtils.makePlane(startX, startY, startZ, width, height, right.getPositiveDirection(), up.getPositiveDirection(), shapeable);
    }

    /**
     * Makes a flat plane along two directions
     */
    public static void makePlane(int startX, int startY, int startZ, int width, int height, Direction right, Direction up, Shapeable shapeable) {
        if (width < 0 || height < 0) return;
        int lineOffsetX, lineOffsetY, lineOffsetZ;
        // We offset each line by up, and then apply it right

        Vec3i v = up.getVector();
        for (int h = 0; h <= height; h++) {
            lineOffsetX = startX + (h * v.getX());
            lineOffsetY = startY + (h * v.getY());
            lineOffsetZ = startZ + (h * v.getZ());
            ShapeUtils.makeLine(lineOffsetX, lineOffsetY, lineOffsetZ, right, width, shapeable);
        }
    }

    @Deprecated
    public static void makeSphere(int radiusX, int radiusY, int radiusZ, Shapeable shapeable, EnumSet<Octant> octants) {
        ShapeUtils.makeEllipsoid(radiusX, radiusY, radiusZ, shapeable, octants);
    }

    public static void makeEllipsoid(int radiusX, int radiusY, int radiusZ, Shapeable shapeable, Set<Octant> octants) {
        List<Octant> octantsList = ImmutableList.copyOf(octants);

        double invRadiusX = 1.0 / (radiusX + 0.5);
        double invRadiusY = 1.0 / (radiusY + 0.5);
        double invRadiusZ = 1.0 / (radiusZ + 0.5);

        double nextXn = 0;
        forX: for (int x = 0; x <= radiusX; ++x) {
            double xn = nextXn;
            nextXn += invRadiusX;
            double nextYn = 0;
            forY: for (int y = 0; y <= radiusY; ++y) {
                double yn = nextYn;
                nextYn += invRadiusY;
                double nextZn = 0;
                forZ: for (int z = 0; z <= radiusZ; ++z) {
                    double zn = nextZn;
                    nextZn += invRadiusZ;

                    double distanceSq = ShapeUtils.lengthSq(xn, yn, zn);
                    if (distanceSq > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break;
                    }

                    if (ShapeUtils.lengthSq(nextXn, yn, zn) <= 1
                            && ShapeUtils.lengthSq(xn, nextYn, zn) <= 1
                            && ShapeUtils.lengthSq(xn, yn, nextZn) <= 1) {
                        continue;
                    }

                    for (Octant octant : octantsList)
                        shapeable.setBlock(x * octant.x, y * octant.y, z * octant.z);
                }
            }
        }
    }

    public static final double lengthSq(double x, double y, double z) {
        return x * x + y * y + z * z;
    }

    public static final double lengthSq(double x, double z) {
        return x * x + z * z;
    }

    public static void makeEllipsoid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Shapeable shapeable, Set<Octant> octants) {
        {
            int centerX = (minX + maxX) / 2;
            int centerY = (minY + maxY) / 2;
            int centerZ = (minZ + maxZ) / 2;

            Shapeable prevShapeable = shapeable;
            shapeable = (x, y, z) -> prevShapeable.setBlock(centerX + x, centerY + y, centerZ + z);
        }

        int radiusX;
        int radiusY;
        int radiusZ;

        // Cutting middle of shape == terrible hack. No idea if it works in any case
        // Anyone now better algorithm for ellipsoids with non-integer axis?
        {
            int diffX = maxX - minX;
            if ((diffX & 1) == 0) {
                radiusX = diffX / 2;
            } else {
                radiusX = diffX / 2 + 1;
                shapeable = ShapeUtils.skipMiddleX(shapeable);
            }
        }

        {
            int diffY = maxY - minY;
            if ((diffY & 1) == 0) {
                radiusY = diffY / 2;
            } else {
                radiusY = diffY / 2 + 1;
                shapeable = ShapeUtils.skipMiddleY(shapeable);
            }
        }

        {
            int diffZ = maxZ - minZ;
            if ((diffZ & 1) == 0) {
                radiusZ = diffZ / 2;
            } else {
                radiusZ = diffZ / 2 + 1;
                shapeable = ShapeUtils.skipMiddleZ(shapeable);
            }
        }

        ShapeUtils.makeEllipsoid(radiusX, radiusY, radiusZ, shapeable, octants);
    }

    public static void makeEllipse(int radiusX, int radiusZ, int y, Shapeable shapeable, Set<Quadrant> quadrants) {
        double invRadiusX = 1.0 / (radiusX + 0.5);
        double invRadiusZ = 1.0 / (radiusZ + 0.5);

        List<Quadrant> quadrantsList = ImmutableList.copyOf(quadrants);

        double nextXn = 0;
        forX: for (int x = 0; x <= radiusX; ++x) {
            double xn = nextXn;
            nextXn += invRadiusX;
            double nextZn = 0;
            forZ: for (int z = 0; z <= radiusZ; ++z) {
                double zn = nextZn;
                nextZn += invRadiusZ;

                double distanceSq = ShapeUtils.lengthSq(xn, zn);
                if (distanceSq > 1) {
                    if (z == 0) {
                        break forX;
                    }
                    break;
                }

                if (ShapeUtils.lengthSq(nextXn, zn) <= 1 && ShapeUtils.lengthSq(xn, nextZn) <= 1) {
                    continue;
                }

                for (Quadrant quadrant : quadrantsList)
                    shapeable.setBlock(x * quadrant.x, y, z * quadrant.z);
            }
        }
    }

    public static void makeEllipse(int minX, int minZ, int maxX, int maxZ, int y, Shapeable shapeable, Set<Quadrant> quadrants) {
        {
            int centerX = (minX + maxX) / 2;
            int centerZ = (minZ + maxZ) / 2;

            Shapeable prevShapeable = shapeable;
            shapeable = (x, y1, z) -> prevShapeable.setBlock(centerX + x, y1, centerZ + z);
        }

        int radiusX;
        int radiusZ;

        {
            int diffX = maxX - minX;
            if ((diffX & 1) == 0) {
                radiusX = diffX / 2;
            } else {
                radiusX = diffX / 2 + 1;
                shapeable = ShapeUtils.skipMiddleX(shapeable);
            }
        }

        {
            int diffY = maxZ - minZ;
            if ((diffY & 1) == 0) {
                radiusZ = diffY / 2;
            } else {
                radiusZ = diffY / 2 + 1;
                shapeable = ShapeUtils.skipMiddleZ(shapeable);
            }
        }

        ShapeUtils.makeEllipse(radiusX, radiusZ, y, shapeable, quadrants);
    }

    private static Shapeable skipMiddleX(Shapeable shapeable) {
        return (x, y, z) -> {
            if (x != 0) {
                if (x < 0) shapeable.setBlock(x + 1, y, z);
                else shapeable.setBlock(x, y, z);
            }
        };
    }

    private static Shapeable skipMiddleY(Shapeable shapeable) {
        return (x, y, z) -> {
            if (y != 0) {
                if (y < 0) shapeable.setBlock(x, y + 1, z);
                else shapeable.setBlock(x, y, z);
            }
        };
    }

    private static Shapeable skipMiddleZ(Shapeable shapeable) {
        return (x, y, z) -> {
            if (z != 0) {
                if (z < 0) shapeable.setBlock(x, y, z + 1);
                else shapeable.setBlock(x, y, z);
            }
        };
    }

    public static void line2D(int y, int x0, int z0, int x1, int z1, Shapeable shapeable) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1? 1 : -1;
        int dy = -Math.abs(z1 - z0);
        int sy = z0 < z1? 1 : -1;
        int err = dx + dy;

        while (true) {
            shapeable.setBlock(x0, y, z0);
            if (x0 == x1 && z0 == z1) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            } /* e_xy+e_x > 0 */
            if (e2 <= dx) {
                err += dx;
                z0 += sy;
            } /* e_xy+e_y < 0 */
        }
    }

    public static void line3D(Vec3d start, Vec3d end, Shapeable shapeable) {
        ShapeUtils.line3D((int)start.x, (int)start.y, (int)start.z, (int)end.x, (int)end.y, (int)end.z, shapeable);
    }

    public static void line3D(BlockPos start, BlockPos end, Shapeable shapeable) {
        ShapeUtils.line3D(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), shapeable);
    }

    public static void line3D(Vec3i start, Vec3i end, Shapeable shapeable) {
        ShapeUtils.line3D(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), shapeable);
    }

    public static void line3D(int startX, int startY, int startZ, int endX, int endY, int endZ, Shapeable shapeable) {
        int dx = endX - startX;
        int dy = endY - startY;
        int dz = endZ - startZ;

        int ax = Math.abs(dx) << 1;
        int ay = Math.abs(dy) << 1;
        int az = Math.abs(dz) << 1;

        int signx = Integer.signum(dx);
        int signy = Integer.signum(dy);
        int signz = Integer.signum(dz);

        int x = startX;
        int y = startY;
        int z = startZ;

        int deltax, deltay, deltaz;
        if (ax >= Math.max(ay, az)) {
            deltay = ay - (ax >> 1);
            deltaz = az - (ax >> 1);
            while (true) {
                shapeable.setBlock(x, y, z);
                if (x == endX) return;

                if (deltay >= 0) {
                    y += signy;
                    deltay -= ax;
                }

                if (deltaz >= 0) {
                    z += signz;
                    deltaz -= ax;
                }

                x += signx;
                deltay += ay;
                deltaz += az;
            }
        } else if (ay >= Math.max(ax, az)) {
            deltax = ax - (ay >> 1);
            deltaz = az - (ay >> 1);
            while (true) {
                shapeable.setBlock(x, y, z);
                if (y == endY) return;

                if (deltax >= 0) {
                    x += signx;
                    deltax -= ay;
                }

                if (deltaz >= 0) {
                    z += signz;
                    deltaz -= ay;
                }

                y += signy;
                deltax += ax;
                deltaz += az;
            }
        } else if (az >= Math.max(ax, ay)) {
            deltax = ax - (az >> 1);
            deltay = ay - (az >> 1);
            while (true) {
                shapeable.setBlock(x, y, z);
                if (z == endZ) return;

                if (deltax >= 0) {
                    x += signx;
                    deltax -= az;
                }

                if (deltay >= 0) {
                    y += signy;
                    deltay -= az;
                }

                z += signz;
                deltax += ax;
                deltay += ay;
            }
        }
    }

    public static double normalizeAngle(double angle) {
        while (angle > 180.0)
            angle -= 360.0;
        while (angle < -180.0)
            angle += 360.0;
        return angle;
    }

    public static double compareAngles(double current, double target) {
        current = ShapeUtils.normalizeAngle(current);
        target = ShapeUtils.normalizeAngle(target);
        return Math.signum(target - current);
    }

    public static double getAngleDistance(double current, double target) {
        double result = target - current;
        return Math.abs(result) > 180? 180 - result : result;
    }

    @FunctionalInterface
    public static interface Shapeable {
        void setBlock(int x, int y, int z);
    }
}
