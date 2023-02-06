#!/bin/bash

metric=$1
counter=$(echo mntr | nc localhost 2181 | grep ^$metric | awk '{print $NF}'| cut -d'.' -f1)
comparator=${2:-le}

if [ -z "$counter" ]; then
	echo "2;no data"
	exit 0
fi

if [ -z "$3" ]; then
	echo "0;$counter"
	exit 0
fi


if [ $counter -$comparator $3 ]; then
	echo "0;OK"
else
	echo "2;$counter"
fi
