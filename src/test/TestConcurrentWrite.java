import shared.FileSystemInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestConcurrentWrite {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            FileSystemInterface fs = (FileSystemInterface) registry.lookup("VirtualFS");

            // Crea e avvia 5 thread concorrenti
            for (int i = 0; i < 1000; i++) {
                int finalI = i; // necessario perché "i" deve essere effettivamente final
                new Thread(() -> {
                    try {
                        String content = "Hello from client " + finalI;
                        System.out.println("✍️  Client " + finalI + " writing...");
                        fs.write("/shared.txt", content.getBytes());
                        System.out.println("✅ Client " + finalI + " wrote.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            // aspetta per permettere ai thread di completare
            Thread.sleep(3000);

            // leggi il contenuto finale
            byte[] data = fs.read("/shared.txt");
            String finalContent = data != null ? new String(data) : "(null)";
            System.out.println("Final content in /shared.txt:");
            System.out.println(finalContent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
