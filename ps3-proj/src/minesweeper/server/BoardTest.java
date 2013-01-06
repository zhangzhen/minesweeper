package minesweeper.server;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.junit.Test;

public class BoardTest {

    /*
     * Testing strategy. This testing suite is broken down into three sections:
     * 1) Tests board implementation, making sure that the specs are followed.
     * 2) Tests for thread-safety. Multiple threads are created and run many times 
     * to make sure the board is thread-safe.
     * 3) Exception throwing: The board must throw exceptions when faced with bad input
     */
    
    @Test
    public void BoardRandomTest() {
        Board b = new Board(10);
        b.dig(1,4);
        b.flag(2,2);
        b.look();
    }
    
    @Test
    public void BoardFileTest() {
        Board b = new Board("0 1 0 0\n1 0 0 0\n1 1 1 1\n0 1 0 0\n");
        b.dig(1, 1);
        b.flag(2,2);
        assertEquals(b.look(), "- - - -\n- 5 - -\n- - F -\n- - - -\n");
    }
    
    @Test
    public void BoardFileTest2() {
        Board b = new Board("0 0 0 0\n0 1 0 0\n0 0 0 0\n1 1 1 1\n");
        b.dig(1, 1);
        assertEquals(b.look(), "       \n       \n2 3 3 2\n- - - -\n");
    }
    
    @Test
    public void BoardFileTest3() {
        Board b = new Board("1 1 0 0 0\n1 0 0 0 0\n1 0 1 0 0\n1 1 0 0 1\n1 1 1 1 1\n");
        b.dig(2, 2);
        assertEquals(b.look(), "- - - - -\n- - - - -\n- - 1 - -\n- - - - -\n- - - - -\n");
    }
    
    @Test
    public void BoardFileTest4() {
        Board b = new Board("1 1 0 0 0\n1 0 0 0 0\n1 0 1 0 0\n1 0 0 0 0\n1 1 0 0 0\n");
        b.dig(2, 2);
        assertEquals(b.look(), "- - 1    \n- 4 1    \n- 3      \n- 4 1    \n- - 1    \n");
    }
    
    @Test
    public void BoardFileTest5() {
        try {
            Board b = new Board(MinesweeperServer.readContent(new File("inputs/board1")));
            b.flag(0, 1);
            b.dig(2,2);
            assertEquals(b.look(), "- F - - - -\n- - - - - -\n- - 2 - - -\n- - - - - -\n- - - - - -\n- - - - - -\n");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    @Test
    public void flagTest() {
        Board b = new Board("1 1 0 0 0\n1 0 0 0 0\n1 0 1 0 0\n1 0 0 0 0\n1 1 0 0 0\n");
        b.dig(2, 2);
        b.flag(4, 4);
        b.deflag(4,4);
        assertEquals(b.look(), "- - 1    \n- 4 1    \n- 3      \n- 4 1    \n- - 1    \n");
    }
    
    /*
     * Test that runs the server
     */
    @Test
    public void ServerTest() throws InterruptedException, IOException {
        startServer(new String[] {"true" , "-f", "inputs/board1" });
        Thread.sleep(100); // Avoid race condition where we try to connect to server too early
        Socket socket;
        try {
            socket = new Socket("127.0.0.1",4444);
            socket.setSoTimeout(3000);
            BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter outputWriter = new PrintWriter(socket.getOutputStream(),true);
            
            assertEquals(true, nextNonEmptyLine(inputBuffer).startsWith("Welcome"));
            
            outputWriter.println("dig 3 1");
              assertEquals("BOOM!", nextNonEmptyLine(inputBuffer));
            
            outputWriter.println("look"); // Debug true
              assertEquals("- - - - - -", nextNonEmptyLine(inputBuffer));
              assertEquals("- - - - - -", nextNonEmptyLine(inputBuffer));
              assertEquals("- - - - - -", nextNonEmptyLine(inputBuffer));
              assertEquals("- 2 - - - -", nextNonEmptyLine(inputBuffer));
              assertEquals("- - - - - -", nextNonEmptyLine(inputBuffer));
              assertEquals("- - - - - -", nextNonEmptyLine(inputBuffer));
              
            outputWriter.println("bye");
              socket.close();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }
    
    /*
     * Digs the same square multiple times
     */
    @Test
    public void multipleDigsTest() throws InterruptedException, IOException {
        final int REPEATS = 1000;
        final Board board = new Board("1 1 0 0 0\n1 0 0 0 0\n1 0 1 0 0\n1 0 0 0 0\n1 1 0 0 0\n");
        assertEquals("- - - - -\n- - - - -\n- - - - -\n- - - - -\n- - - - -\n", board.look());
        for (int i=0; i<REPEATS; i++) {
            Thread thread1 = new Thread(new Runnable() {
                public void run() {
                    Thread.yield();
                    board.dig(4,1);
                }
            });
            Thread thread2 = new Thread(new Runnable() {
                public void run() {
                    Thread.yield(); 
                    board.dig(4,1);  
                }
            });
            thread1.start();
            thread2.start();
            
            thread1.join();
            thread2.join();
            
            assertEquals("- - - - -\n- - - - -\n- - - - -\n- - - - -\n- 2 - - -\n", board.look());
        }
    }
    
    /* This test breaks if we add a print statement in the dig method and make the Board class not thread-safe (i.e. remove the 
    * "synchronized" keyword. This means that the thread is placing a flag even while the dig method is in the process of running, 
    * meaning that there is a flag when there shouldn't be.
    */
    @Test
    public void multipleDigsTest2() throws InterruptedException, IOException {
        final int REPEATS = 1000;
        final Board board = new Board("0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n" +
        		"0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n" +
        		"0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 1\n");
        for (int i=0; i<REPEATS; i++) {
            Thread thread1 = new Thread(new Runnable() {
                public void run() {
                    board.dig(10,10);
                }
            });
            
            Thread thread2 = new Thread(new Runnable() {
                public void run() {
                    // Ensures thread1 runs first
                    Thread.yield();
                    board.flag(0, 0);  
                }
            });
            thread1.start();            
            thread2.start();
            
            thread1.join();
            thread2.join();
            
            assertEquals("                     \n                     \n                     \n                     \n" +
            		"                     \n                     \n                     \n                     \n" +
            		"                     \n                     \n                     \n", board.look());
        }
    }

    /*
     * Repeated dig and flag the same square. Resulting board must be one of two possibilities
     */
    @Test
    public void sameSquareTest() throws InterruptedException, IOException {
        final int REPEATS = 1000;
        final Board board = new Board("1 0 0 0\n1 0 0 0\n0 0 1 1\n1 0 0 0\n");
        for (int i=0; i<REPEATS; i++) {
            
            Thread thread1 = new Thread(new Runnable() {
                public void run() {
                    Thread.yield();
                    board.dig(2, 2);
                }
            });
            Thread thread2 = new Thread(new Runnable() {
                public void run() {
                    Thread.yield();
                    board.flag(2, 2);
                    
                }
            });
            thread1.start();
            thread2.start();
          
            thread1.join();
            thread2.join();
            
            String output = board.look();
            
            String answer1 = "- - - -\n- - - -\n- - 1 -\n- - - -\n";
            String answer2 = "- - - -\n- - - -\n- - F -\n- - - -\n";

            assertEquals(true, output.equals(answer1) || output.equals(answer2));       
            
        }
    }
    
    /*
     * This test ensures that one is only looking at the board before an operation or after an operation, but never 
     * during. It breaks if the board is not synchronized.
     */
    @Test
    public void multipleLookTest() throws InterruptedException, IOException {
        final int REPEATS = 1000;
        final Board board = new Board("0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n" +
                "0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 0\n" +
                "0 0 0 0 0 0 0 0 0 0 0\n0 0 0 0 0 0 0 0 0 0 1\n");
        for (int i=0; i<REPEATS; i++) {
            
            Thread thread1 = new Thread(new Runnable() {
                public void run() {
                    board.dig(10,10);
                }
            });
            Thread thread2 = new Thread(new Runnable() {
                public void run() {
                    String output = board.look();
                    
                    String answer1 = "                     \n                     \n                     \n                     \n" +
                            "                     \n                     \n                     \n                     \n" +
                            "                     \n                     \n                     \n";
                    String answer2 = "- - - - - - - - - - -\n- - - - - - - - - - -\n- - - - - - - - - - -\n- - - - - - - - - - -\n" +
                            "- - - - - - - - - - -\n- - - - - - - - - - -\n- - - - - - - - - - -\n- - - - - - - - - - -\n" +
                            "- - - - - - - - - - -\n- - - - - - - - - - -\n- - - - - - - - - - -\n";

                    try {
                        assertEquals(true, output.equals(answer1) || output.equals(answer2));
                    } catch (AssertionError e) {
                        fail("Error found!");
                    }
                }
            });
            thread1.start();
            thread2.start();
            
            thread1.join();
            thread2.join();
            
            
        }
    }
    
    // Wrong board input
    @Test(expected=RuntimeException.class)
    public void ExtraColumn() {
        new Board("1 1 0 0 0 0\n1 0 0 0 0\n1 0 1 0 0\n1 0 0 0 0\n1 1 0 0 0\n");
    }
    
    // Wrong board input
    @Test(expected=RuntimeException.class)
    public void ExtraRow() {
        new Board("1 1 0 0 0\n1 0 0 0 0\n1 0 1 0 0\n1 0 0 0 0\n1 1 0 0 0\n0 0 0 0 0");
    }
    
    // Weird input
    @Test(expected=RuntimeException.class)
    public void NotZeroOrOne() {
        new Board("1 1 0 0 0\n1 0 0 0 0\n1 0 1 0 0\n1 0 0 0 0\n1 1 2 0 0");
    }
    
    // extra space
    @Test(expected=RuntimeException.class)
    public void ExtraSpace() {
        new Board("1 1 0 0 0\n1 0 0 0 0\n1 0 1 0 0\n1 0 0 0  0\n1 1 0 0 0");
    }
    
    // Bad input file
    @Test(expected=RuntimeException.class)
    public void badInputFile() {
        try {
            new Board(MinesweeperServer.readContent(new File("inputs/board2")));
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    private String nextNonEmptyLine(BufferedReader in) throws IOException {
        while (true) {
          String ret = in.readLine();
          if (ret == null || !ret.equals(""))
            return ret;
        }
    }
      
    private static void startServer(String args[]) {
        final String myArgs[] = args;
        new Thread(new Runnable() {
            public void run() {
                try {
                    MinesweeperServer.main(myArgs);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    }

}
