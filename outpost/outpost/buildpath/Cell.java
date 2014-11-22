package outpost.buildpath;

import java.util.ArrayList;

import outpost.sim.Point;

public class Cell {
    public int x;
    public int y;

    public Cell() { x = 0; y = 0;}

    public Cell(int xx, int yy) {
        x = xx;
        y = yy;
       
    }

    public Cell(Cell o) {
        this.x = o.x;
        this.y = o.y;
    }

    public boolean equals(Cell o) {
        return o.x == x && o.y == y ;
    }
 
    public int hashCode() { return 0; }
}