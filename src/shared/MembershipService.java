package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Servizio RMI per gestire la membership dei peer in un DFS distribuito.
 */
public interface MembershipService extends Remote {
    /**
     * Un nuovo peer si unisce al cluster.
     * @param peerId identificativo del peer
     * @param address indirizzo host:port del peer
     * @return lista aggiornata di tutti i peer attivi
     */
    List<String> join(String peerId, String address) throws RemoteException;

    /**
     * Annuncio: notifica a un peer che un nuovo nodo è entrato.
     * @param peerId identificativo del nuovo peer
     * @param address indirizzo host:port del nuovo peer
     */
    void announce(String peerId, String address) throws RemoteException;
}
