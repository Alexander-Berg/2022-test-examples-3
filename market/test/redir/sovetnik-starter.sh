#!/usr/bin/env bash

# usage: ./sovetnik-starter --docroot sovetnik-redir --port 23672 --logs-dir /var/logs/yandex/desktop --environ-type production
set -e

docroot=
port=
logs_dir=
environ_type=
ext_env=
node_workers=

msg()   { echo -e "$@"; }
err()   { msg "$@" 1>&2; }
fatal() { err "$@"; exit 1; }

fatal_parameter() {
    fatal "parameter \"$1\" is required"
}

check() {
    if [[ -z "$docroot" ]]; then
        fatal_parameter "--docroot"
    elif [[ -z "$port" ]]; then
        fatal_parameter "--port"
    elif [[ -z "$logs_dir" ]]; then
        fatal_parameter "--logs-dir"
    elif [[ -z "$environ_type" ]]; then
        fatal_parameter "--environ-type"
    elif ! [[ -d "$docroot/configs/$environ_type" ]]; then
        fatal "$docroot/configs/$environ_type: not exist / not a directory. Check --environ-type option"
    fi
}

run() {
    # logging date to stderr, right to -x output
    err "$(date)"

    set -x

    # getting absolute paths
    docroot="$(readlink -f "$docroot")"
    logs_dir="$(readlink -f "$logs_dir")"

    cd "$docroot"

    export NODE_PORT="$port"
    export LOGS_DIR="$logs_dir"
    export YENV="$environ_type"
    export PATH="/opt/nodejs/8/bin/:$PATH"
    export NODE_PATH=/opt/nodejs/8/lib/node_modules/

    export EXT_ENV="$ext_env"
    export NODE_WORKERS="$node_workers"

    echo $$ > master.pid

    exec node "node_modules/luster/bin/luster.js" "configs/current/luster.conf.js"
}

while [ $# -gt 0 ]; do
    case "$1" in
        (--docroot) docroot="$2"; shift; shift; ;;
        (--port) port="$2"; shift; shift; ;;
        (--logs-dir) logs_dir="$2"; shift; shift; ;;
        (--environ-type) environ_type="$2"; shift; shift; ;;
        (--ext-env) ext_env="$2"; shift; shift; ;;
        (--workers) node_workers="$2"; shift; shift; ;;
        (-*) err "$0: error - unrecognized option $1"; exit 1;;
        (*) fatal "Unreachable"; ;;
    esac
done

check

run
