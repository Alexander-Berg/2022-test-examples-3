#!/bin/bash

mntr=$(echo mntr | nc localhost 2181 2>/dev/null)

if ! grep -q 'zk_server_state	leader' <(echo "$mntr"); then
	echo "0;Not leader"
	exit 0
fi

config_servers=$(cat /etc/yandex/zookeeper-disk/zoo.cfg | grep -c '^server\.')
config_followers=$(( config_servers - 1 ))
actual_followers=$(echo "$mntr" | egrep 'zk_followers|zk_learners' | awk '{print $NF}')

if [[ x"$config_followers" == x"$actual_followers" ]]; then
	echo "0;OK"
else
	echo "2;$actual_followers (actual) != $config_followers (config)"
fi
