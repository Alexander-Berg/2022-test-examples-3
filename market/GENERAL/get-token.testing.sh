#!/bin/sh
tvmknife >>/dev/null 2>&1 || {
    echo "command 'tvmknife' not found"
    echo "see https://wiki.yandex-team.ru/passport/tvm2/debug/#tvmknife or just run:"
    echo "$ sudo apt install yandex-passport-tvmknife"
    exit 1
}

tvmknife get_service_ticket sshkey -s 2000611 -d 2000611
