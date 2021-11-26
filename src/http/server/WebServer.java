///A Simple Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 * <p>
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 *
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {

    protected static final String INDEX = "files/index.html";
    protected static final String FILE_NOT_FOUND = "files/404_Error.html";
    protected static final String FILE_TEST_POST = "files/testPOST.txt";
    protected static final String FILE_TEST_PUT = "files/testPUT.txt";


    /**
     * Méthode de lancement du thread serveur.
     */
    protected void start() {
        ServerSocket s;

        System.out.println("Webserver starting up on port 3000");
        System.out.println("(press ctrl-c to exit)");
        try {
            // create the main server socket
            s = new ServerSocket(3000);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            return;
        }

        System.out.println("Waiting for connection");
        for (; ; ) {
            try {
                // wait for a connection
                Socket remote = s.accept();
                //System.err.println("Connexion from:" + remote.getInetAddress());

                // remote is now the connected socket
                System.out.println("Connection, sending data.");

                BufferedInputStream in = new BufferedInputStream(remote.getInputStream());
                //BufferedReader in = new BufferedReader(new InputStreamReader(remote.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(remote.getOutputStream());

                // read the data sent. We basically ignore it,
                // stop reading once a blank line is hit. This
                // blank line signals the end of the client HTTP
                // headers.

                // Lecture du Header (il se termine par une ligne vide)
                System.out.println("Waiting for data...");
                //StringBuilder header = new StringBuilder();

                // read the data sent. We basically ignore it,
                // stop reading once a blank line is hit. This
                // blank line signals the end of the client HTTP
                // headers.

                StringBuilder header = new StringBuilder();
                int bcur, bprec = '\0';
                boolean newline = false;
                while ((bcur = in.read()) != -1 && !(newline && bprec == '\r' && bcur == '\n')) {
                    if (bprec == '\r' && bcur == '\n') {
                        newline = true;
                    } else if (!(bprec == '\n' && bcur == '\r')) {
                        newline = false;
                    }
                    bprec = bcur;
                    header.append((char) bcur);
                }


                System.out.println("REQUEST :");
                System.out.println(header);


                if (header.length() > 0) {
                    String[] words = header.toString().split(" ");
                    if (words.length > 1) {
                        String requestType = words[0];
                        String resourceName = words[1];

                        switch (requestType) {
                            case "GET":
                                if (resourceName.equals("/") || resourceName.equals("")) {
                                    answerGETRequest(out, INDEX);
                                } else {
                                    resourceName = resourceName.split("/")[1];
                                    answerGETRequest(out, "files/" + resourceName);
                                }
                                break;

                            case "POST":

                                answerPOSTRequest(in, out);
                                break;

                            case "HEAD":
                                if (resourceName.equals("/") || resourceName.equals("")) {
                                    answerHEADRequest(out, INDEX);
                                } else {
                                    resourceName = resourceName.split("/")[1];
                                    answerHEADRequest(out, "files/" + resourceName);
                                }
                                break;

                            case "PUT":
                                answerPUTRequest(in, out);
                                break;

                            case "DELETE":
                                if (resourceName.equals("/") || resourceName.equals("")) {
                                    out.write(generateHeader("404 Not Found", FILE_NOT_FOUND, resourceName.length()).getBytes());
                                } else {
                                    resourceName = resourceName.split("/")[1];
                                    answerDELETERequest(out, "files/" + resourceName);
                                }
                                break;
                        }
                        remote.close();
                    }

                }

            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }

    /**
     * Méthode permettant de répondre à une requête de type HEAD
     * @param out le flux de sortie
     * @param filename le fichier auquel le client souhaite accéder
     */
    protected void answerHEADRequest(BufferedOutputStream out, String filename) {
        try {
            File resource = new File(filename);
            if (resource.exists() && resource.isFile()) {
                out.write(generateHeader("200 OK", filename, resource.length()).getBytes());
            } else {
                resource = new File(FILE_NOT_FOUND);
                // Envoi du Header signalant une erreur
                out.write(generateHeader("404 Not Found", FILE_NOT_FOUND, resource.length()).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(generateHeader("500 Serveur Error").getBytes());
                out.flush();
            } catch (Exception ignored) {
            }
        }

    }

    /**
     * Méthode permettant de répondre à une requête de type GET
     * @param out le flux de sortie
     * @param filename le fichier auquel le client souhaite accéder
     */
    protected void answerGETRequest(BufferedOutputStream out, String filename) {
        System.out.println("GET " + filename);
        try {
            // Vérification de l'existence de la ressource demandée
            File resource = new File(filename);
            if (resource.exists() && resource.isFile()) {
                // Envoi du Header signalant un succés
                out.write(generateHeader("200 OK", filename, resource.length()).getBytes());
            } else {
                // Si la ressource n'existe pas, on va plutôt envoyer une page d'erreur
                resource = new File(FILE_NOT_FOUND);
                // Envoi du Header signalant une erreur
                out.write(generateHeader("404 Not Found", FILE_NOT_FOUND, resource.length()).getBytes());
            }

            // Ouverture d'un flux de lecture binaire sur le fichier demandé
            BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(resource));
            // Envoi du corps : le fichier (page HTML)
            byte[] buffer = new byte[256];
            int nbReadLines;
            while ((nbReadLines = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, nbReadLines);
            }
            // Fermeture du flux de lecture
            fileIn.close();
            //Envoi des données
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            // En cas d'erreur on essaie d'avertir le client
            try {
                out.write(generateHeader("500 Serveur Error").getBytes());
                out.flush();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Méthode permettant de répondre à une requête de type PUT
     * @param in flux d'entrée
     * @param out flux de sortie
     */
    protected void answerPUTRequest(BufferedInputStream in, BufferedOutputStream out) {
        System.out.println("PUT ");
        try {
            File resource = new File(FILE_TEST_PUT);
            boolean existed = resource.exists();

            // Ouverture d'un flux d'écriture binaire vers le fichier, en mode insertion à la fin
            FileWriter fileOut = new FileWriter(resource, false);

            byte[] buffer = new byte[256];
            in.read(buffer);
            StringBuilder content = new StringBuilder();
            char c;
            for (byte b : buffer) {
                // convert byte to character
                c = (char) b;
                if (b != 0) {
                    content.append(c);
                }
            }

            System.out.println(content);
            if(content.toString().contains("&")){
                String[] parameters = content.toString().split("&");
                for (String param : parameters) {
                    String name = param.split("=")[0];
                    String value = param.split("=")[1];
                    fileOut.write(name + " : " + value + " \n");
                    fileOut.write("-------------------------------------------------------- \n");
                }
            }
            else{
                fileOut.write(content + " \n");
                fileOut.write("-------------------------------------------------------- \n");
            }
            fileOut.close();

            // Envoi du Header
            if (existed) {
                // Ressource modifiée avec succès
                out.write(generateHeader("200 OK").getBytes());
            } else {
                // Ressource créée avec succès
                out.write(generateHeader("201 Created").getBytes());
            }
            // Envoi des données
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            // En cas d'erreur on essaie d'avertir le client
            try {
                out.write(generateHeader("500 Internal Server Error").getBytes());
                out.flush();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Méthode permettant de répondre à une requête de type POST
     * @param in flux d'entrée
     * @param out flux de sortie
     */
    protected void answerPOSTRequest(BufferedInputStream in, BufferedOutputStream out) {
        System.out.println("POST ");

        try {
            File resource = new File(FILE_TEST_POST);
            boolean existed = resource.exists();

            // Ouverture d'un flux d'écriture binaire vers le fichier
            FileWriter fileOut = new FileWriter(resource, true);


            byte[] buffer = new byte[256];
            in.read(buffer);
            StringBuilder content = new StringBuilder();
            char c;
            for (byte b : buffer) {
                // convert byte to character
                c = (char) b;
                if (b != 0) {
                    content.append(c);
                }
            }

            System.out.println(content);

            String[] parameters = content.toString().split("&");
            for (String param : parameters) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                fileOut.write(name + " : " + value + " \n");
                fileOut.write("-------------------------------------------------------- \n");
            }
            fileOut.close();


            // Envoi du Header
            if (existed) {
                // Ressource modifiée avec succès
                out.write(generateHeader("200 OK").getBytes());
            } else {
                // Ressource créée avec succès
                out.write(generateHeader("201 Created").getBytes());
            }
            // Envoi des données
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            // En cas d'erreur on essaie d'avertir le client
            try {
                out.write(generateHeader("500 Internal Server Error").getBytes());
                out.flush();
            } catch (Exception ignored) {
            }
        }
    }


    /**
     * Méthode permettant de répondre à une requête de type DELETE
     * @param out flux de sortie
     * @param filename nom du fichier que le client souhaite supprimer
     */
    protected void answerDELETERequest(BufferedOutputStream out, String filename) {
        System.out.println("DELETE " + filename);
        try {
            // Vérification de l'existence de la ressource demandée
            File resource = new File(filename);
            if (resource.exists() && resource.isFile()) {
                if (resource.delete())
                    out.write(generateHeader(resource.getName(), " est supprimé." + "200 OK", +resource.length()).getBytes());
            } else if (!resource.exists()) {
                // Si la ressource n'existe pas, on va plutôt envoyer une page d'erreur
                resource = new File(FILE_NOT_FOUND);
                // Envoi du Header signalant une erreur
                out.write(generateHeader("404 Not Found", FILE_NOT_FOUND, resource.length()).getBytes());
            } else {
                // Si la ressource existe, mais que l'on ne peut pas l'autoriser
                out.write(generateHeader("403 Forbidden").getBytes());
            }
            //Envoi des données
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            // En cas d'erreur on essaie d'avertir le client
            try {
                out.write(generateHeader("500 Serveur Error").getBytes());
                out.flush();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Méthode permettant de générer un header à partir d'un code de retour
     * @param code code de retour
     * @return un header suivant la syntaxe HTTP
     */
    protected String generateHeader(String code) {

        String header;
        header = "HTTP/1.1 " + code + " \n";
        header += "Content-Type: text/html \n";
        header += "Server: Bot \n";
        header += "\n";
        System.out.println("ANSWER HEADER :");
        System.out.println(header);
        return header;
    }

    /**
     * Méthode permettant de générer un header
     * @param status code de retour
     * @param filename nom fichier que l'on souhaite retourner
     * @param length longueur du fichier
     * @return un header suivant la syntaxe HTTP
     */
    protected String generateHeader(String status, String filename, long length) {
        String header = "HTTP/1.1 " + status + "\r\n";
        if (filename.endsWith(".html")) header += "Content-Type: text/html\r\n";
        header += "Content-Length: " + length + "\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        System.out.println("ANSWER HEADER :");
        System.out.println(header);
        return header;
    }


    /**
     * Start the application.
     *
     * @param args Command line parameters are not used.
     */
    public static void main(String[] args) {
        WebServer ws = new WebServer();
        ws.start();
    }
}
