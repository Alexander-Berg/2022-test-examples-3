#!/bin/bash

if echo mntr | nc localhost 2181 2>/dev/null | grep -q zk_version; then
	echo "0;Ok"
else
	echo "2;closed"
fi
