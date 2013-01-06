package minesweeper.server;

/*
 * This server is thread-safe because each time a new client is connected, a new thread is created and ran, 
 * independent from the server itself. The server blocks until a thread is created, so we are confident 
 * that there are no race conditions during creation. Any mutations to any of the server's objects are either synchronized 
 * (through the use of the Object lock), or are accessed by the thread itself. For example, each thread can 
 * access the board simultaneously because the board is declared thread-safe, and the thread can only 
 * access the synchronized public methods of the board. With that being said, this implementation 
 * of the server is thread-safe for the objectives that we want to accomplish (multiple clients running 
 * on different threads). 
 */
import java.net.*;
import java.io.*;

public class MinesweeperServer {
    private final ServerSocket serverSocket;
    /** True if the server should _not_ disconnect a client after a BOOM message. */
    private final boolean debug;
    Object lock = new Object();
    int players = 0;
    private static Board board;

    /**
     * Make a MinesweeperServer that listens for connections on port.
     * @param port port number, requires 0 <= port <= 65535.
     */
    public MinesweeperServer(int port, boolean debug) throws IOException {
        serverSocket = new ServerSocket(port);
        this.debug = debug;
    }

    /**
     * Run the server, listening for client connections and handling them.  
     * Never returns unless an exception is thrown.
     * @throws IOException if the main server socket is broken
     * (IOExceptions from individual clients do *not* terminate serve()).
     */
    public void serve() throws IOException {
        while (true) {
            // block until a client connects
            Socket socket = serverSocket.accept();
            // Creates new thread for each connection
            Thread t = new Thread(new newRunnable(socket));
            
            // Make sure these two commands occur are synchronized
            synchronized(lock) {
                t.start();
                players++;
            }
            
        }
    }
    
    private class newRunnable implements Runnable {
        Socket socket;
        public newRunnable(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                handleConnection(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle a single client connection.  Returns when client disconnects.
     * @param socket socket where the client is connected
     * @throws IOException if connection has an error or terminates unexpectedly
     */
    private void handleConnection(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.println("Welcome to Minesweeper. " + players + " people are playing including you. Type 'help' for help.");
        try {
            for (String line =in.readLine(); line!=null; line=in.readLine()) {
                String output = handleRequest(line);
                if(output == null)
                    continue;
                // Terminates connection for these two cases
                if (output.equals("BOOM!\n") && debug==false) {
                    out.print(output);
                    return;
                }
                if (output.equals("bye")) {
                    return;
                }
                // Print the output message and then flushes the buffer
                if(output != null) {
                    out.print(output);
                    out.flush();
                }
            }
        } finally {
            // Makes sure these three occur together
            synchronized(lock) {
                players--;
                out.close();
                in.close();
            }
        }
    }

    /**
     * handler for client input
     * 
     * make requested mutations on game state if applicable, then return 
     * appropriate message to the user.
     * 
     * @param input
     * @return string that the board returns
     */
    private static String handleRequest(String input) {
        String regex = "(look)|(dig \\d+ \\d+)|(flag \\d+ \\d+)|" +
                "(deflag \\d+ \\d+)|(help)|(bye)";
        if(!input.matches(regex)) {
            //invalid input
            return null;
        }
        String[] tokens = input.split(" ");
        if (tokens[0].equals("look")) {
            // 'look' request
            return board.look();
        } else if (tokens[0].equals("help")) {
            // 'help' request
            return "Please go to office hours for help\n";
        } else if (tokens[0].equals("bye")) {
            // 'bye' request
            return "bye";
        } else {
            int x = Integer.parseInt(tokens[1]);
            int y = Integer.parseInt(tokens[2]);
            if (tokens[0].equals("dig")) {
                // 'dig x y' request
                return board.dig(x, y);
            } else if (tokens[0].equals("flag")) {
                // 'flag x y' request
                return board.flag(x,y);
            } else if (tokens[0].equals("deflag")) {
                // 'deflag x y' request
                return board.deflag(x,y);
            }
        }
        // Should never get here--make sure to return in each of the valid cases above.
        throw new UnsupportedOperationException();
    }

    /**
     * Start a MinesweeperServer running on the default port (4444).
     * 
     * Usage: MinesweeperServer [DEBUG [(-s SIZE | -f FILE)]]
     * 
     * The DEBUG argument should be either 'true' or 'false'. The server should disconnect a client
     * after a BOOM message if and only if the DEBUG flag is set to 'false'.
     * 
     * SIZE is an optional integer argument specifying that a random board of size SIZE*SIZE should
     * be generated. E.g. "MinesweeperServer false -s 15" starts the server initialized with a
     * random board of size 15*15.
     * 
     * FILE is an optional argument specifying a file pathname where a board has been stored. If
     * this argument is given, the stored board should be loaded as the starting board. E.g.
     * "MinesweeperServer false -f boardfile.txt" starts the server initialized with the board
     * stored in boardfile.txt, however large it happens to be (but the board may be assumed to be
     * square).
     * 
     * The board file format, for use by the "-f" option, is specified by the following grammar:
     * 
     * FILE :== LINE+
     * LINE :== (VAL SPACE)* VAL NEWLINE
     * VAL :== 0 | 1
     * SPACE :== " "
     * NEWLINE :== "\n" 
     * 
     * If neither FILE nor SIZE is given, generate a random board of size 10x10. If no arguments are
     * specified, do the same and additionally assume DEBUG is 'false'. FILE and SIZE may not be
     * specified simultaneously, and if one is specified, DEBUG must also be specified.
     * 
     * The system property minesweeper.customport may be used to specify a listening port other than
     * the default (used by the autograder only).
     */
    public static void main(String[] args) {
        // We parse the command-line arguments for you. Do not change this method.
        boolean debug = false;
        File file = null;
        Integer size = 10; // Default size.
        try {
            if (args.length != 0 && args.length != 1 && args.length != 3)
              throw new IllegalArgumentException();
            if (args.length >= 1) {
                if (args[0].equals("true")) {
                    debug = true;
                } else if (args[0].equals("false")) {
                    debug = false;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if (args.length == 3) {
                if (args[1].equals("-s")) {
                    try {
                        size = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException();
                    }
                    if (size < 0)
                        throw new IllegalArgumentException();
                } else if (args[1].equals("-f")) {
                    file = new File(args[2]);
                    if (!file.isFile()) {
                        System.err.println("file not found: \"" + file + "\"");
                        return;
                    }
                    size = null;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("usage: MinesweeperServer DEBUG [(-s SIZE | -f FILE)]");
            return;
        }
        // Allow the autograder to change the port number programmatically.
        final int port;
        String portProp = System.getProperty("minesweeper.customport");
        if (portProp == null) {
            port = 4444; // Default port; do not change.
        } else {
            port = Integer.parseInt(portProp);
        }
        try {
            runMinesweeperServer(debug, file, size, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start a MinesweeperServer running on the specified port, with either a random new board or a
     * board loaded from a file. Either the file or the size argument must be null, but not both.
     * 
     * @param debug The server should disconnect a client after a BOOM message if and only if this
     *        argument is false.
     * @param size If this argument is not null, start with a random board of size size * size.
     * @param file If this argument is not null, start with a board loaded from the specified file,
     *        according to the input file format defined in the JavaDoc for main().
     * @param port The network port on which the server should listen.
     */
    public static void runMinesweeperServer(boolean debug, File file, Integer size, int port)
            throws IOException
    {
        if (size!=null) {
            board = new Board(size);
        }
        if (file!=null) {
            board = new Board(readContent(file));
        }
        MinesweeperServer server = new MinesweeperServer(port, debug);
        server.serve();

    }
    
    /**
     * Read the contents of a file and outputs it as a string
     * @param file name
     * @return String containing the contents of the file
     */
    public static String readContent(File filename) throws IOException {
        StringBuilder result = new StringBuilder();
        FileReader fileReader;
        
        try {
            fileReader = new FileReader(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("File not found");
        }
        
        BufferedReader reader = new BufferedReader(fileReader);
        String line = "";
        
        while ((line = reader.readLine()) != null) {
            result.append(line+"\n");
        }
        
        fileReader.close();
        reader.close();
        
        return result.toString();
    }
}
