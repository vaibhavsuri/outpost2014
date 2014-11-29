package outpost.group1.common;

import java.util.*;

public class Commander {
    List<Outpost> outposts;
    Point corner;

    boolean is_me;
    public boolean isMe() { return is_me; }

    public List<Outpost> getOutposts() { return outposts; };
    public Point getCorner() { return corner; };

    public Commander(Point corner, List<Outpost> outposts, boolean is_me) {
        this.outposts = outposts;
        for (Outpost o : outposts) {
            o.setParent(this);
        }
        this.corner = corner;
        this.is_me = is_me;
    }
}
