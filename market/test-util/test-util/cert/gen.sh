#!/bin/bash

openssl genrsa -out server.key 1024

openssl req -new -key server.key -out server.csr
CODE=$?
if [[ ${CODE} != 0 ]]; then
    echo "CODE=${CODE}"
    echo "can't continue"
    exit 1
fi

openssl x509 -req -days 366 -in server.csr -signkey server.key -out server.crt
CODE=$?
if [[ ${CODE} != 0 ]]; then
    echo "CODE=${CODE}"
    echo "can't continue"
    exit
fi

