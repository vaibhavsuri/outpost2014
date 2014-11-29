package outpost.group1.common;

import java.util.*;

public class TileScoreMover extends ScoreMover {
    public static Random rand = new Random();

    public double scoreTile(Point p, Game g, Outpost o) {
        return 30.0  * distanceScore(p, g, o) 
             + 0.0  * avoidEdgesScore(p, g) 
             + 1.0  * avoidNeighborsScore(p, g, o)
             + 2.0  * waterScore(p, g, o)
             + 0.1  * landScore(p, g, o)
             + 1.0  * safetyScore(p, g);
    }

    public static final int TARGET_DISTANCE = Board.BOARD_SIZE / 2;

    public static final int GUARDIAN_DISTANCE = 20;
    public static final int GUARDIAN_COUNT = 5;
    public double distanceScore(Point p, Game g, Outpost o) {
        Commander me = g.getMe();
        double dist = p.distanceTo(me.getCorner());
        double result = dist;

        int outposts_count = me.getOutposts().size();
        if (outposts_count > 1.5 * GUARDIAN_COUNT && 
            o.getId() >= outposts_count - GUARDIAN_COUNT && 
            dist > GUARDIAN_DISTANCE) {

            return GUARDIAN_DISTANCE-dist;
        }
        return result;
    }

    double SAFETY_RADIUS = 30;
    public double safetyScore(Point p, Game g) {
        List<Outpost> opposing_outposts = g.getOpposingOutposts();
        double sum = 0;
        for (Outpost o : opposing_outposts) {
            double dist = p.distanceTo(o.getPosition());
            if (dist < SAFETY_RADIUS) {
                sum -= dist;
            }
        } 
        return sum;
    }

    /*
    public double falloff(double x, double y_intercept, double x_intercept) {
        double b =  - y_intercept / x_intercept;

        return (x - x_intercept) * (
    }
    */

    public double avoidEdgesScore(Point p, Game g) {
        Point closestEdge = p.closestTo(Rectangle.BOARD_RECTANGLE.getCornersList());
        Point offset = p.sub(closestEdge);

        double min_distance = Math.min(Math.abs(offset.getX()), Math.abs(offset.getY()));

        // cap the score at radius
        return Math.min(g.radius, min_distance);
    }

    public double targetDistanceScore(Point current, Point other, double target) {
        return targetDistanceScore(current, other, target, Double.POSITIVE_INFINITY);
    }

    public double targetDistanceScore(Point current, Point other, double target, double cap) {
        double dist = current.distanceTo(other);
        double score = target - Math.abs(target - dist);
        return Math.min(cap, score);
    }

    public double avoidNeighborsScore(Point p, Game g, Outpost o) {
        List<Point> neighbor_positions = new ArrayList<Point>();

        double sum = 0;
        for (Outpost other : g.getMe().getOutposts()) {
            if (other == o) continue;
            int dist = p.distanceTo(other.getPosition());

            if (dist < g.radius / 2.0) {
                sum -= 1000.0;
                if (dist == 0) {
                    sum = Double.NEGATIVE_INFINITY;
                    return sum;
                }
            } else if (dist > g.radius && dist < 1.5 * g.radius) {
                sum += 0.0;
            }
        }

        return sum;
    }

    public double waterScore(Point p, Game g, Outpost o) {
        Map<Point, Set<Outpost>> owners_by_point = g.getOwnersByPoint();

        Point my_corner = g.getMe().getCorner();

        int count = 0;
        for (int i = p.getX() - g.radius; i <= p.getX() + g.radius; i++) {
            for (int j = p.getY() - g.radius; j <= p.getY() + g.radius; j++) {
                Point pt = new Point(i,j);
                if (g.getBoard().isWater(pt)) {
                    if (!owners_by_point.containsKey(pt)) {
                        count += 1;//pt.distanceTo(my_corner);
                    } else if (owners_by_point.get(pt).size() == 1 && 
                               owners_by_point.get(pt).iterator().next().equals(o)) {
                        count += 1;//pt.distanceTo(my_corner);
                    }
                }
            }
        }

        return count;
    }

    public double landScore(Point p, Game g, Outpost o) {
        Map<Point, Set<Outpost>> owners_by_point = g.getOwnersByPoint();

        Point my_corner = g.getMe().getCorner();

        int count = 0;
        for (int i = p.getX() - g.radius; i <= p.getX() + g.radius; i++) {
            for (int j = p.getY() - g.radius; j <= p.getY() + g.radius; j++) {
                Point pt = new Point(i,j);
                if (g.getBoard().isLand(pt)) {
                    if (!owners_by_point.containsKey(pt)) {
                        count += 1;//pt.distanceTo(my_corner);
                    } else if (owners_by_point.get(pt).size() == 1 && 
                               owners_by_point.get(pt).iterator().next().equals(o)) {
                        count += 1;//pt.distanceTo(my_corner);
                    }
                }
            }
        }

        return count;
    }
}
