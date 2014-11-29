package outpost.group1.common;

import java.util.*;

public class Game {
    Board board;
    public Board getBoard() { return board; }
    Commander[] commanders = null;

    public List<Outpost> getMyOutposts() {
        return commanders[my_id].getOutposts();
    }


    List<Outpost> opponents = null;
    public List<Outpost> getOpposingOutposts() {
        if (opponents == null) {
            opponents = new ArrayList<Outpost>();
            for (int i = 0; i < commanders.length; i++) {
                if (i == my_id) continue;
                opponents.addAll(commanders[i].getOutposts());
            }
        }

        return opponents;
    }

    Map<Point, Set<Outpost>> owners_by_point = null;

    public Map<Point, Set<Outpost>> getOwnersByPoint() {
        if (owners_by_point == null) {
            owners_by_point = new HashMap<Point, Set<Outpost>>();
            List<Outpost> mine = getMyOutposts();
            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {

                    Point p = new Point(i,j);
                    for (Outpost o : mine) {
                        if (o.distanceTo(p) < this.radius) {
                            if (!owners_by_point.containsKey(p)) {
                                owners_by_point.put(p, new HashSet<Outpost>());
                            }
                            owners_by_point.get(p).add(o);
                        }
                    }

                }
            }
        }

        return owners_by_point;
    }

    public Commander getMe() { return commanders[my_id]; };

    int my_id;

    public int radius = 10;
    public Game(int id, outpost.sim.Point[] game_board) {
        my_id = id;
        board = new Board(game_board);
    }

    private List<Outpost> outpostsFromPairs(List<outpost.sim.Pair> pairs) {
        List<Outpost> posts = new ArrayList<Outpost>();
        for (outpost.sim.Pair p : pairs) {
            posts.add(new Outpost(posts.size(), p));
        }
        return posts;
    }

    public void loadOutposts(ArrayList<ArrayList<outpost.sim.Pair>> outposts) {
        opponents = null;
        Point[] corners = Rectangle.BOARD_RECTANGLE.getCorners();
        commanders = new Commander[4];
        for (int i = 0; i < 4; i++) {
            commanders[i] = new Commander(corners[i], 
                                          outpostsFromPairs(outposts.get(i)),
                                          i == my_id);
        }
    }
}
