#!/bin/bash

cp /etc/yarl/yarl.yaml /tmp/yarl.prev
/usr/local/bin/yarl-conf-from-sd.sh {{ salt['pillar.get']('yarl:stage') }} {{ salt['pillar.get']('yarl:du') }} > /etc/yarl/yarl.yaml

diff -q /tmp/yarl.prev /etc/yarl/yarl.yaml || ( ubic restart yarl; logger -p daemon.err "YARL restarted to sync root instances")
