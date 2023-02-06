#!/bin/bash
set -e

TUNNELER_HOST="ekb-tunneler.ldev.yandex-team.ru"
LOCAL_PORT="$1"
PROJECT_ID="$2"

if [ -z $PROJECT_ID ]; then
    echo "Usage: ./client.sh <local_port> <project_id>"
    exit -1
fi

echo "Asking $TUNNELER_HOST for instance list"

IFS=',' INST=($(curl -s "https://$TUNNELER_HOST/api/v1/instances?format=ipv6" | tr -d [] | awk '{FS=","; print $0 }' | tr -d '"'))
HOST=${INST[0]}
PORT=${INST[1]}

echo "Connecting to ${HOST}:${PORT}"

echo "Tunnel available at https://`whoami`-$PROJECT_ID-ekb.ldev.yandex.ru"

ssh -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o StrictHostKeyChecking=no -N -R "$PROJECT_ID:localhost:$LOCAL_PORT" -p $PORT $HOST
