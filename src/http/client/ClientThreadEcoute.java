package http.client;

import java.io.BufferedReader;

public class ClientThreadEcoute extends Thread {

    private final BufferedReader socIn;

    public ClientThreadEcoute(BufferedReader scI) {
        this.socIn = scI;
    }

    public void run() {
        try {

            while (true) {
                String line = this.socIn.readLine();
                System.out.println(line);
            }
        } catch (Exception e) {
            System.err.println("Error in EchoServer:" + e);
        }

    }


}
