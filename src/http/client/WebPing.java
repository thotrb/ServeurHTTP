package http.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Classe permettant de simuler le comportement d'un client web
 */
public class WebPing {
    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage java WebPing <server host name> <server port number>");
            return;
        }

        String httpServerHost = args[0];
        int httpServerPort = Integer.parseInt(args[1]);

        try {
            InetAddress addr;
            Socket sock = new Socket(httpServerHost, httpServerPort);
            addr = sock.getInetAddress();
            System.out.println("Connected to " + addr);


            BufferedReader socIn = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));

            PrintStream socOut = new PrintStream(sock.getOutputStream());

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            ClientThreadEcoute ctEcoute = new ClientThreadEcoute(socIn);
            ctEcoute.start();

            String line;
            while (true) {
                line = stdIn.readLine();
                if (line.equals(".")) break;
                //System.err.println(line);
                socOut.println(line);
            }

            sock.close();
            socIn.close();
            socOut.close();
        } catch (java.io.IOException e) {
            System.out.println("Can't connect to " + httpServerHost + ":" + httpServerPort);
            System.out.println(e);
        }
    }
}