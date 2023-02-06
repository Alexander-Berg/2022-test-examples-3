#!/bin/bash

DIR=$1
[[ $DIR ]] || DIR="."
mkdir -p "$DIR"

tvmknife unittest public_keys > "$DIR/keys"

tvmknife unittest service -s 1000502 -d 1000501 > "$DIR/publisher-tst"
tvmknife unittest service -s 1000503 -d 1000501 > "$DIR/publisher-suspended"
tvmknife unittest service -s 1000504 -d 1000501 > "$DIR/publisher-production"
tvmknife unittest service -s 1000505 -d 1000501 > "$DIR/subscriber-tst"
