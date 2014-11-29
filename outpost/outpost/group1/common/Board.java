package outpost.group1.common;

import java.util.*;

public class Board {
    public static enum TileType {
        Land,
        Water
    };

    public static final int BOARD_SIZE = 100;
    TileType[][] tiles = new TileType[BOARD_SIZE][BOARD_SIZE];

    public Board(outpost.sim.Point[] points) {
        for (outpost.sim.Point p : points) {
            if (p.water) {
                tiles[p.y][p.x] = TileType.Water;
                water.add(new Point(p));

            } else {
                tiles[p.y][p.x] = TileType.Land;
                land.add(new Point(p));
            }
        }
        this.lakes = identifyLakes();
    }

    List<Point> water = new ArrayList<Point>();
    List<Point> land  = new ArrayList<Point>();

    List<Rectangle> lakes;

    public List<Rectangle> getLakes() { return lakes; };

    List<Rectangle> identifyLakes() {
        // run disjoint sets on water
        // call lakeFromPointSet on each disjoint item
        Set<Point> used = new HashSet<Point>();
        List<Rectangle> lakes = new ArrayList<Rectangle>();
        for (Point p : water) {
            if (used.contains(p)) continue;

            Set<Point> lake = getLakePointsAdjacentTo(p);
            used.addAll(lake);

            lakes.add(lakeFromPointSet(lake));
        }

        return lakes;
    }

    Set<Point> getLakePointsAdjacentTo(Point p) {
        Set<Point> lake = new HashSet<Point>(p.neighbors());
        Set<Point> last_added = new HashSet<Point>(p.neighbors());

        boolean added;

        do {
            added = false;
            Set<Point> recent = new HashSet<Point>(last_added);
            last_added.clear();

            for (Point l : recent) {
                List<Point> neighbors = l.neighbors();

                for (Point neighbor : neighbors) {
                    if (!lake.contains(neighbor) && isWater(neighbor)) {
                        added = true;

                        lake.add(neighbor);
                        last_added.add(neighbor);
                    }
                }
            }
        } while (added);

        return lake;
    }

    Rectangle lakeFromPointSet(Set<Point> points) {
        int top = 10000;
        int bottom = 0;
        int left = 10000;
        int right = 0;

        for (Point p : points) {
            if (p.getX() < left) {
                left = p.getX();
            }
            if (p.getX() > right) {
                right = p.getX();
            }

            if (p.getY() < top) {
                top = p.getY();
            }
            if (p.getY() > bottom) {
                bottom = p.getY();
            }
        }
        return new Rectangle(top, bottom, left, right);
    }

    public TileType get(int row, int col) {
        return tiles[row][col];
    }
    public TileType get(Point p) {
        return tiles[p.getY()][p.getX()];
    }

    public boolean isLand(int row, int col) {
        return tiles[row][col] == TileType.Land;
    }
    public boolean isLand(Point p) {
        if (!Rectangle.BOARD_RECTANGLE.contains(p)) {
            return false;
        }
        return tiles[p.getY()][p.getX()] == TileType.Land;
    }

    public boolean isWater(int row, int col) {
        return tiles[row][col] == TileType.Water;
    }
    public boolean isWater(Point p) {
        if (!Rectangle.BOARD_RECTANGLE.contains(p)) {
            return false;
        }
        return tiles[p.getY()][p.getX()] == TileType.Water;
    }

    public List<Point> getLandNeighbors(Point p) {
        List<Point> neighbors = p.neighbors();
        List<Point> land = new ArrayList<Point>();
        for (Point n : neighbors) {
            if (isLand(n)) {
                land.add(n);
            }
        }
        return land;
    }

    int unit_land_cost;
    int unit_water_cost;
};
