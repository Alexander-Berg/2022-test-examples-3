#!/bin/sh
TANK="tank01ht.market.yandex.net"
TANK_CONFIG=$1
if [ -z "${TANK_CONFIG}"];
then
    TANK_CONFIG="tank.yaml"
fi

TANK_CMD_DIR="../../../../load/projects/tankapi_cmd/bin/client"
TANK_CMD_EXEC="tankapi-cmd"
TANK_CMD="$TANK_CMD_DIR"/"$TANK_CMD_EXEC"
JOBNO="./jobno.txt"

set -x
${TANK_CMD} -t "$TANK" -c ${TANK_CONFIG} -j "${JOBNO}"
set +x
