#!/usr/bin/env bash

set -e

HOSTS=(
    userdb01g.dst.yandex.net
    userdb01e.dst.yandex.net
    userdb01h.dst.yandex.net
    userdb02e.dst.yandex.net
    userdb02g.dst.yandex.net
    userdb02h.dst.yandex.net
    sysdb01e.dst.yandex.net
    sysdb01g.dst.yandex.net
    sysdb01h.dst.yandex.net
)
PORT=27018
MSTATVER=$(mongostat --version | grep -oE '[0-9].[0-9]+.[0-9]+')

echo "Mongostat version is: ${MSTATVER}"


to_ipv6() {
    local host="${1}"
    local raw=$(host ${host})
    echo ${raw##* }
}

function drop_all_test_dbs {
    local host="${1}"
    while read test_dbname; do
        echo "db.dropDatabase()" |  mongo --quiet --ipv6 --host ${host} ${test_dbname} | grep 'ok" : 1' > /dev/null || true
        local result="SUCCESS"
        [[ ${PIPESTATUS[2]} -eq 0 ]] || local result="FAILED"
        echo "${host} ${test_dbname} ${result}"
    done < <(get_test_dbnames ${host})
}

get_test_dbnames() {
    local host="${1}"
    echo "show dbs" | mongo --ipv6 --host $host | grep TestCase | awk {'print $1'}
}

get_mongostat() {
    if [[ "${MSTATVER}" =~ ^3.* ]]; then
        local ipv6_hosts=($(printf "%s\n" ${HOSTS[@]} | while read h; do to_ipv6 $h; done))
        mongostat --rowcount 1 --host $(printf "[%s]:${PORT}," ${ipv6_hosts[@]})
        return
    fi
    mongostat --rowcount 1 --host $(printf "%s:${PORT}," ${HOSTS[@]})
}

get_primaries() {
    head -15 | grep PRI | awk {'print $1'} | sort | uniq
}

drop_all_dbs() {

    while read pr_host; do
        drop_all_test_dbs ${pr_host} &
    done < <(get_mongostat | get_primaries)

    drop_all_test_dbs localhost:27017 &

    gcount=1
    sleep 5

    while [ $gcount -gt 0 ]
    do
        gcount=0
        let gcount=$gcount+$(ps -u `whoami`|grep -c mongo)
        sleep 0.01
        let gcount=$gcount+$(ps -u `whoami`|grep -c mongo)
        sleep 0.01
        let gcount=$gcount+$(ps -u `whoami`|grep -c mongo)
        sleep 0.01
        sleep 1
        echo "${gcount} workers are still running"
    done
}

echo "Now let's drop all testing dbs"
drop_all_dbs
echo "All testing database dropped"
