package test;

import shared.FileSystemInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

/**
 * Simula la rimozione casuale di un link in un grafo completo.
 * Sceglie un peer a caso e gli fa rimuovere uno dei suoi vicini.
 */
public class RandomLinkFailure {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: RandomLinkFailure <peerName> <host> <port>");
            System.exit(1);
        }

        String peerName = args[0];   // es: A
        String host = args[1];       // es: localhost
        int port = Integer.parseInt(args[2]); // es: 1099

        try {
            Registry reg = LocateRegistry.getRegistry(host, port);
            FileSystemInterface stub = (FileSystemInterface) reg.lookup(peerName);

            // prendo la lista dei vicini
            List<String> neighbors = stub.getNeighbors();
            if (neighbors.isEmpty()) {
                System.out.println("Nessun vicino da rimuovere per " + peerName);
                return;
            }

            // scelgo un vicino random
            Random rand = new Random();
            String toRemove = neighbors.get(rand.nextInt(neighbors.size()));

            String neighName = toRemove.split(":")[0];
            String neighHost = toRemove.split(":")[1];
            int neighPort = Integer.parseInt(toRemove.split(":")[2]);

            // rimuovo il vicino dal peer scelto
            stub.removeNeighbor(neighName);

            System.out.println("Link rimosso: " + peerName + " non vede più " +
                               neighName + "@" + neighHost + ":" + neighPort);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
