package outpost.group3sub3;

import java.util.ArrayList;

import outpost.group3sub3.Board;
import outpost.group3sub3.Loc;

public abstract class Strategy {
    public Strategy() {}

    /* Return a list of the target destinations of the outposts */
    public abstract void run(Board board, ArrayList<Outpost> outposts);
}
