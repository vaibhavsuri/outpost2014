package outpost.group1.common;

public class Outpost {
    Point position;
    public Point getPosition() { return position; }

    Commander parent; 
    public Commander getParent() { return parent; }

    public void setParent(Commander parent) { this.parent = parent; };

    int id;
    public int getId() { return id; };
    public Outpost(int id, Point position) {
        this.id = id;
        this.position = position;
    }

    public Outpost(int id, outpost.sim.Pair position) {
        this(id, new Point(position.x, position.y));
    }

    ScoreMover mover = new TileScoreMover();

    public int distanceTo(Point p) {
        return p.distanceTo(this.position);
    }

    public int distanceTo(Outpost o) {
        return this.position.distanceTo(o.position);
    }

    public Move getMove(Game game) {
        return mover.getMove(game, this);
    }

    @Override
    public String toString() {
        return String.format("<Outpost %d belonging to %s at %s>", id, parent.isMe() ? "me" : "opponent", position);
    }

    public Move getMoveTo(Point destination) {
        if (destination == null) {
            System.out.format("Selected null destination for %s. Doing a no-move.\n", this);
            return new Move(position);
        }
        return new Move(position, destination);
    }
}
