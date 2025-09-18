#!/bin/bash
echo "Compilazione in corso..."

mkdir -p build

# Trova tutti i file .java e compila dentro la cartella build/
find shared/ peer/ . -name "*.java" > sources.txt
javac -d build @sources.txt

if [ $? -eq 0 ]; then
  echo "Compilazione completata"
else
  echo "Errore durante la compilazione"
  exit 1
fi

