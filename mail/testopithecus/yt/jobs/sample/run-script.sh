#!/bin/bash

export HOME=$(pwd)
export ANDROID_AVD_HOME="${HOME}/.android/avd"
export ANDROID_SDK_HOME="${HOME}/.android/"
echo ${HOME} ${ANDROID_AVD_HOME} ${ANDROID_SDK_HOME} >&2
mkdir -p ${ANDROID_AVD_HOME}

export SCREEN_DIRECTORY=${HOME}/screen
mkdir -p ${SCREEN_DIRECTORY}

function takeScreenShot() {
    sudo adb exec-out screencap -p > screen.png
    base64=$(cat screen.png | base64 -w 0)
    echo "{ \"date\" : \"$(date)\", \"screen\" : \"${base64}\" }"
}

echo "no" | sudo -E avdmanager -v create avd --force --name "pixel" --device "pixel" --package "system-images;android-23;google_apis;x86" --tag "google_apis" --abi "x86" >> log
mksdcard -l my_card 25GB my_card.img >&2
sudo -E emulator-headless -avd pixel -accel off -no-audio -verbose -gpu swiftshader_indirect -memory 4096 -no-boot-anim -no-cache -wipe-data -no-snapstorage -skin 480x800 -sdcard my_card.img >/dev/null 2>&1 &

sleep 60

for (( i=1; i <= 15; i++ ))
do
    sleep 60
    takeScreenShot
done

date >&2
takeScreenShot >&2
adb shell input tap 261 575 >&2
# adb shell settings get global development_settings_enabled
sleep 20
takeScreenShot >&2
adb install -st mail.apk >&2
# adb install -t mail.apk >/dev/null 2>&1 &
# date >&2

# sleep 600
# adb install -t mail.apk >&2
# takeScreenShot
# takeScreenShot >&2

