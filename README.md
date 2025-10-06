# Distributed Peer-to-Peer File System (Java RMI)

Questo progetto implementa un **file system distribuito peer-to-peer** con **Java RMI**.  
Ogni nodo mantiene un file system locale montato su disco ed è in grado di collegarsi ad altri peer per eseguire operazioni distribuite su file e directory.  
Un client (`DistributedFSClient`) permette di interagire da terminale con un peer della rete.

---

## Requisiti

- **openjdk** 25.0.1 2025-09-16
- **Bash** (per gli script `compile.bash`, `run-*.sh`)

---

## Compilazione

Per compilare tutti i sorgenti:

```bash
bash compile.bash
```

Per pulire i file compilati:

```bash
bash clean.bash
```

---

## Avvio dei peer

Sono forniti degli script di utilità per avviare i nodi.

### Avvio del primo nodo (leader)

```bash
bash run-leader.sh <name> <host> <port>
```

Esempio:

```bash
bash run-leader.sh A localhost 1099 
```

Questo crea il primo peer della rete (senza bootstrap).

### Avvio di un peer che si unisce a un leader

```bash
bash run-peer.sh <name> <host> <port> <bootstrapName bootstrapPort>
```

Esempio:

```bash
bash run-peer.sh B localhost 1100 ./dataB A:localhost:1099
```

In questo modo `B` entra nella rete del peer `A`.

---

## Avvio del client

Per interagire con un peer in esecuzione:

```bash
bash run-client.sh <peerName> <host> <port>
```

Esempio:

```bash
bash run-client.sh A localhost 1099
```

---

## Comandi del client

All’interno della shell interattiva puoi eseguire:

- mkdir /path → crea directory
- mknod /file → crea file vuoto
- symlink <target> <linkPath> → crea symlink
- write /file contenuto → scrive contenuto nel file
- read /file → legge contenuto
- ls /dir → mostra contenuto di una directory
- locate /file → individua su quale peer si trova un file
- neighbors → mostra vicini noti del peer
- exit → chiude il client

---

## Esempio di utilizzo

1. Avvia un leader:

```bash
bash run-leader.sh A localhost 1099
```

2. Avvia un altro peer con bootstrap:

```bash
bash run-peer.sh B localhost 1100  A 1099
```

3. Connettiti al peer A:

```bash
bash run-client.sh A localhost 1099
```

4. Esegui comandi:

```
>>> mkdir /docs
>>> mknod /docs/file1.txt
>>> write /docs/file1.txt HelloWorld
>>> read /docs/file1.txt
HelloWorld
>>> locate /docs/file1.txt
A
>>> neighbors
 - B@localhost:1100
```
