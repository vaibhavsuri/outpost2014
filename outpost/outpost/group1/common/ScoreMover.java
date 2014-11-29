package outpost.group1.common;

import java.util.*;

public abstract class ScoreMover {
    public Move getMove(Game g, Outpost o) {
        int radius = 5;
        List<Point> neighbors = Arrays.asList(
                o.getPosition().add( radius, 0),
                o.getPosition().add(-radius, 0),
                o.getPosition().add( 0, radius),
                o.getPosition().add( 0,-radius),
                o.getPosition()
                );

        Double best = null;
        Point destination = null;
        for (Point p : neighbors) {
            if (g.getBoard().isLand(p)) {
                double score = scoreTile(p, g, o);
                if (best == null || score > best) {
                    best = score;
                    Point offset = p.sub(o.getPosition());

                    int dx = offset.getX() / radius;
                    int dy = offset.getY() / radius;

                    destination = o.getPosition().add(dx, dy);
                }
            }
        }

        return o.getMoveTo(destination);
    }

    public abstract double scoreTile(Point p, Game g, Outpost o);
}
