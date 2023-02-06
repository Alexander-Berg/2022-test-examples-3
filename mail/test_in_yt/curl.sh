#!/bin/bash

sudo apt-get update

sudo apt-get install libcurl3 -y

[ -f "/usr/lib/x86_64-linux-gnu/libcurl.so.3" ] && sudo cp /usr/lib/x86_64-linux-gnu/libcurl.so.3 /usr/lib/

sudo apt-get remove libcurl3 -y

sudo apt-get install libcurl4 libcurl4-openssl-dev -y

