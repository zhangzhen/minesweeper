package minesweeper.server;

/*
 * This board is threadsafe because all methods that mutate the board are either private methods or synchronized. Per 
 * the lecture notes, if all public mutator methods are synchronized, then we are thread-safe because mutliple calls 
 * cannot be mutating the board at the same time. So we are guranteed that at any one time, only one thread is mutating
 * the board, and the thread will not release the lock until it returns from the public method that was originally called.
 * We do not need to synchronize the private methdods because they will only 
 * be called by the public methods. And since those public methods are synchronized, the private methods will only be 
 * called by one thread at a time. In addition, the board object is private, meaning that it cannot be accessed by any 
 * outside classes or methods, guranteeing that any mutations will only occur from our synchroniezd methods. 
 * Of course, this is slightly inefficient because this implementation basically allows
 * only one thread to access the entire board at a time.
 * 
 *  Also, constructors do not need to be synchronized because we are not leaking any references, as explained during lecture.
 */

public class Board {
    
    private Cell[][] board;
    private int length;
    
    /**
     * Constructor for random board
     * @param length of each side of board
     * @return none
     */
    public Board(int len) {
        this.length = len;
        this.board = new Cell[len][len];
        for (int i=0; i<len; i++) {
            for (int j=0; j<len; j++) {
                board[i][j] = new Cell();
            }
        }
    }
    
    /**
     * Constructor for preset board
     * @param string representation of the input file
     * @return none
     */
    public Board(String str) {
        this.length = -1;
        String lines[] = str.split("\\n");
        for (int i = 0; i<lines.length; i++) {
            String values[] = lines[i].split(" ");
            if (this.length == -1) {
                this.length = values.length;
                if (lines.length != this.length)
                    throw new RuntimeException("Invalid input file: # of values != # of lines");
                this.board = new Cell[this.length][this.length];         
            }
            else {
                if (values.length != this.length)
                    throw new RuntimeException("Invalid input file: inconsistent # of values per line");
            }
            for (int j = 0; j<values.length; j++) {
                if (!values[j].equals("0") && !values[j].equals("1"))
                    throw new RuntimeException("Invalid input file: values must be 0 or 1");
                board[i][j] = new Cell(Integer.parseInt(values[j]));
            }
        }
    }
    
    /**
     * Simply returns the board message
     * @param none
     * @return Board message
     */
    public synchronized String look() {
        return toString();
    }
    
    /**
     * If x and y are invalid are if the cell (location (x,y) on board) is not untouched, return board message.
     * Oherwise, change cell state to dug. If it contains bomb, send "Boom!" message
     * "If the square x,y has no neighbor squares with bombs, then for each of x,y's 'untouched' neighbor squares, change said 
     * square to 'dug' and repeat this step (not the entire DIG procedure) recursively for said neighbor square unless said 
     * neighbor square was already dug before said change."
     * @modifies the bomb count of all neighbors of the cell by subtracting one
     * @param location of cell in x & y coordinates
     * @return "Boom!" if bomb is found, board message otherwise
     */
    public synchronized String dig(int x, int y) {

        boolean bomb = false;
        if (x<0 || y<0 || x>=this.length || y>= this.length)
            return toString();
        Cell currentCell = board[x][y];
        if (currentCell.getStatus() == Cell.Type.UNTOUCHED) {
            currentCell.setStatus(Cell.Type.DUG);
            if (currentCell.hasBomb()) {
                bomb = true;
                currentCell.removeBomb();
                updateCount(x,y);                     
            }
            int count = countNeighbors(x,y);  
            if (count==0) {
                recurseNeighbors(x,y);
            }
        }
        if (bomb)
            return "BOOM!\n";
        return toString();
    }
    
    /**
     * Flags the cell if cell is untouched and valid
     * @param location of cell in x & y coordinates
     * @return Board message
     */
    public synchronized String flag(int x, int y) {
        if (x<0 || y<0 || x>=this.length || y>= this.length)
            return toString();
        Cell currentCell = board[x][y];
        if (currentCell.getStatus() == Cell.Type.UNTOUCHED){
            currentCell.setStatus(Cell.Type.FLAG);
        }
        return toString();
    }
    
    /**
     * Deflags the cell if cell is flagged and valid
     * @param location of cell in x & y coordinates
     * @return Board message
     */
    public synchronized String deflag(int x, int y) {
        if (x<0 || y<0 || x>=this.length || y>= this.length)
            return toString();
        Cell currentCell = board[x][y];
        if (currentCell.getStatus() == Cell.Type.FLAG){
            currentCell.setStatus(Cell.Type.UNTOUCHED);
        }
        return toString();
    }
    
    /**
     * Counts number of bombs in the neighbors of the cell
     * @param location of cell in x & y coordinates
     * @return number of neighbor bombs
     */
    private int countNeighbors(int x, int y) {
        int count = 0;
        int x1 = Math.max(x-1, 0);
        int x2 = Math.min(x+1, this.length-1);
        int y1 = Math.max(y-1, 0);
        int y2 = Math.min(y+1, this.length-1);
        for (int i=x1; i<=x2; i++) {
            for (int j=y1; j<=y2; j++) {
                if (board[i][j].hasBomb())
                    count++;
            }             
        }
        if (board[x][y].hasBomb())
            count--;
        board[x][y].setCount(count);
        return count;      
    }
    
    /**
     * Change all neighbors to dug, then recurse on any cells that have no neighbors with bombs
     * @param location of cell in x & y coordinates
     * @return None
     */
    private void recurseNeighbors(int x, int y) {
        int x1 = Math.max(x-1, 0);
        int x2 = Math.min(x+1, this.length-1);
        int y1 = Math.max(y-1, 0);
        int y2 = Math.min(y+1, this.length-1);
        for (int i=x1; i<=x2; i++) {
            for (int j=y1; j<=y2; j++) {
                if ((i!=x || j!=y) && board[i][j].getStatus()==Cell.Type.UNTOUCHED) {
                    board[i][j].setStatus(Cell.Type.DUG);
                    if (countNeighbors(i,j) == 0)
                        recurseNeighbors(i, j);
                }
            }             
        }
    }
    
    /**
     * Update the bomb count of the cell and all its neighbors
     * @param location of cell in x & y coordinates
     * @return None
     */
    private void updateCount(int x, int y) {
        int x1 = Math.max(x-1, 0);
        int x2 = Math.min(x+1, this.length-1);
        int y1 = Math.max(y-1, 0);
        int y2 = Math.min(y+1, this.length-1);
        int count;
        for (int i=x1; i<=x2; i++) {
            for (int j=y1; j<=y2; j++) {
                count = countNeighbors(i,j);
                board[i][j].setCount(count);
                
            }             
        }
    }
    
    /**
     * Converts the board to a board message
     * @param None
     * @return String representation of board message
     */
    @Override
    public synchronized String toString() {
        String result = "";
        for (int i=0; i<this.length; i++) {
            for (int j=0; j<this.length; j++) {
                result += board[i][j].toString() + " ";
            }
            // Results extra space at the end of the line
            result = result.substring(0, result.length()-1);
            // Appends new line
            result += "\n";
        }
        return result;
    }
}
