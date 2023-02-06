#/bin/sh
TANK=tank01ht.market.yandex.net
ssh -t "$TANK"  "cd /var/lib/tankapi/tests/$(head -n 1 jobno.txt); bash "
