package outpost.group1_3;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.movePair;

import outpost.group1.common.Game;
import outpost.group1.common.Outpost;
import outpost.group1.common.Move;

public class Player extends outpost.sim.Player {
    public Player(int id_in) {
        super(id_in);
    }
    
    public void init() {
        System.out.format("group1 has id %d\n", id);
    }

    public static final boolean PRINT_MOVES = false;

    Game game = null;
    public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> outposts, outpost.sim.Point[] board, int r, int L, int W, int T) {
        if (game == null) {
            loadGame(outposts, board);
        }
        game.loadOutposts(outposts);

        ArrayList<movePair> moves = new ArrayList<movePair>();

        List<Outpost> mine = game.getMyOutposts();
        for (Outpost o : mine) {
            Move m = o.getMove(game);
            moves.add(m.toMovePair(o.getId()));
            if (PRINT_MOVES) {
                System.out.format("%s moves %s\n", o, m);
            }
        }
        return moves;
    }

    private void loadGame(ArrayList<ArrayList<Pair>> outposts, outpost.sim.Point[] board) {
        game = new Game(id, board);
    }

    public int delete(ArrayList<ArrayList<Pair>> outposts, outpost.sim.Point[] board) {
        // delete the oldest outpost
        return 0;
    }
}
