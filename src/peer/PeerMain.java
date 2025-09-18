package peer;

/**
 * Avvio peer federato con RMI e grafo completo.
 *
 * Uso:
 *   java peer.PeerMain <name> <host> <port> <dataDir> [bootstrapName:bootstrapHost:bootstrapPort]
 *
 * Se non specifichi il bootstrap → il nodo parte da solo.
 * Se specifichi bootstrap → entra in rete con joinNetwork.
 */
public class PeerMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: PeerMain <name> <host> <port> <dataDir> [bootstrapName:bootstrapHost:bootstrapPort]");
            System.exit(1);
        }

        String name = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        String dataDir = args[3];

        DistributedNode node = new DistributedNode(name, host, port, dataDir);
        node.start();

        // === Join se bootstrap presente ===
        if (args.length == 5) {
            String[] parts = args[4].split(":");
            String bootstrapName = parts[0];
            String bootstrapHost = parts[1];
            int bootstrapPort = Integer.parseInt(parts[2]);

            node.joinNetwork(bootstrapName, bootstrapHost, bootstrapPort);
        }

        // === Shutdown hook per leave automatico ===
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                node.leaveNetwork();
            } catch (Exception e) {
                System.err.println("Errore nello shutdown: " + e.getMessage());
            }
        }));
    }
}
