package minesweeper.server;

public class Cell {
    public static enum Type {
        DUG, FLAG, UNTOUCHED
    }
    private boolean bomb;
    private Type status = Type.UNTOUCHED;
    private int neighborBombs;
    
    public Cell() {   
        if (Math.random()<0.25)
            this.bomb = true;
        else
            this.bomb = false;
    }
    
    public Cell(int bomb) {
        if (bomb==1)
            this.bomb = true;
        else
            this.bomb = false;
    }
    
    public boolean hasBomb() {
        return this.bomb;
    }
    
    public void removeBomb() {
        this.bomb = false;
    }
    
    public void setFlag() {
        this.status = Type.FLAG;
    }
    
    public void reset() {
        this.status = Type.UNTOUCHED;
    }
    
    public void setStatus(Type t) {
        this.status = t;
    }
    
    public Type getStatus() {
        return this.status;
    }
    
    public int getCount() {
        return this.neighborBombs;
    }
    
    public void setCount(int num) {
        this.neighborBombs = num;
    }
    
    public String toString() {
        switch (status) {
        case UNTOUCHED:
            //if (this.bomb)
            //    return "B";
            return "-";
        case FLAG:
            return "F";
        case DUG:
            if (neighborBombs == 0)
                return " ";
            return Integer.toString(neighborBombs);
        default:
            return null; // Should not reach here
        }
    }
    
    
}
