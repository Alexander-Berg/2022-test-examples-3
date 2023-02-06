#!/usr/bin/env bash
./fastres2pycl --mongo-uri $FASTRES2_MONGO_TEST --mongo-db fastres2 --mongo-collection wizards $@
