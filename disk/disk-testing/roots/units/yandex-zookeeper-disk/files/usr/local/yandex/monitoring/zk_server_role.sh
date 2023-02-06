#!/bin/sh

me=${0##*/}     # strip path
me=${me%.*}     # strip extension

die () {
    echo "$1;$2"
    exit 0
}

if [ -f /etc/init.d/yandex-zookeeper-disk ]
then
    current_role=`echo mntr | nc localhost 2181 | grep zk_server_state | cut -f2`

    if [ -f /tmp/$me.prev ]
    then
        prev_role=`cat /tmp/$me.prev`
    else
        prev_role="follower"
        echo $prev_role >/tmp/$me.prev
    fi

    if [ "x$current_role" != "x$prev_role" ]
    then
        echo $current_role >/tmp/$me.prev
        if [ "x$current_role" == "xfollower" -o "x$current_role" == "xleader" ]
        then
            die 1 "$prev_role->$current_role"
        else
            die 2 "$prev_role->$current_role"
        fi
    else
        die 0 "$current_role"
    fi
else
    die 0 "No ZK on this node"
fi
