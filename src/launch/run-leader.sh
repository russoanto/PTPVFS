#!/bin/bash

echo "Avvio del leader peer "
cd ../build
java peer.PeerMain A localhost 1099 ../root_file_system/data-A
