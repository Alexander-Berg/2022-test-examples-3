#!/bin/sh

[[ -f /tmp/traf ]] && mv -f /tmp/traf /tmp/traf.old
fgrep eth /proc/net/dev | cut -d: -f2 | awk '{rx += $1 ; tx += $9} END {print rx,tx}' > /tmp/traf
cat /tmp/traf /tmp/traf.old | xargs | awk '{print $1 - $3, $2 - $4}'

