#!/usr/bin/env bash

export ANDROID_HOME=/Users/$USER/Library/Android/sdk
export PATH=${PATH}:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

adb pull /storage/emulated/0/Android/data/ru.yandex.mail.beta/cache/log.txt