package outpost.group3_3;

import java.util.ArrayList;

import outpost.group3_3.Board;
import outpost.group3_3.Loc;

public abstract class Strategy {
    public Strategy() {}

    /* Return a list of the target destinations of the outposts */
    public abstract void run(Board board, ArrayList<Outpost> outposts);
}
