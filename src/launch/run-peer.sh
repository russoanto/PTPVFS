#!/bin/sh

dir="data-$1"
cd ../build
java peer.PeerMain $1 $2 $3 ../root_file_system/$dir $4:$2:$5
