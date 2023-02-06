#!/usr/bin/env bash

# Скрипт, запускающий сборки всех test-app приложений.

set -ex

export VERSION=999.0.0
export SIGN=true

build() {
    echo "Start build $1."

    npm run clean
    echo "- build folder was cleaned"

    npm run webpack $1
    echo "- js files was built"

    npm run builder $1 -- --mac
    echo "- [Mac OS] dist files was built"

    BUILD_IN_DOCKER=true npm run builder $1 -- --win
    echo "- [Windows] dist files was built"

    npm run clean:trash
    echo "- build trash was cleaned"

    npm run upload app/999.0.0
    echo "- dist files was uploaded"
}

BUCKET=chat-app-test build test_chats
BUCKET=chat-app-test build test_team_chats
