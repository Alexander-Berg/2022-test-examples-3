#!/bin/bash

set -e
apt-get update

# INSTALL UTILS
DEBIAN_FRONTEND=noninteractive
dpkg --add-architecture i386
apt-get install -y curl software-properties-common unzip wget
apt-get install -y libpulse0 libglu1
apt-get -y install unzip ca-certificates libgconf-2-4 libx11-xcb1 nodejs
apt-get install -yq software-properties-common libstdc++6 zlib1g libncurses5 \
                        locales ca-certificates apt-transport-https curl unzip redir iproute2 \
                        xvfb x11vnc fluxbox nano libpulse0 telnet expect \
                        --no-install-recommends
#apt-instance libqt5gui5

# CONFIGURE kvm
# apt-instance install -y cpu-checker qemu-kvm
# modprobe kvm_intel
# kvm-ok

# CREATE AGENT ACCOUNT
# useradd agent

# INSTALL NODE
curl -sL https://deb.nodesource.com/setup_12.x | bash -
apt-get -y update
apt-get -y install nodejs

# CONFIGURE NPM
npm i fs-extra node-gyp @types/node mocha @types/mocha tslint typescript typescript-formatter sync-request ts-node mocha-silent-reporter -g

# INSTALL JAVA
apt-get install -y yandex-jdk8/stable

# INSTALL ANDROID SDK
ANDROID_HOME="/opt/android-sdk"
ANDROID_SDK_ROOT="/opt/android-sdk"
mkdir -p ${ANDROID_HOME}
wget "https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip" --quiet -O /tmp/android-sdk.zip
unzip -qq /tmp/android-sdk.zip -d ${ANDROID_HOME}
ln -s ${ANDROID_HOME}/tools/bin/avdmanager /usr/bin/avdmanager
ln -s ${ANDROID_HOME}/tools/bin/sdkmanager /usr/bin/sdkmanager
sdkmanager --update
yes | sdkmanager --licenses

# CREATE AVD
# export ANDROID_AVD_HOME="/usr/.android/avd"
#ANDROID_SDK_HOME="/usr/.android/"
#ANDROID_EMULATOR_HOME="/usr/.android/"
mkdir /usr/.android/
sdkmanager --sdk_root=${ANDROID_SDK_ROOT} "emulator" "platform-tools" "platforms;android-23" "system-images;android-23;google_apis;x86" >> install_log
ln -s ${ANDROID_HOME}/platform-tools/adb /usr/bin/adb
ln -s ${ANDROID_HOME}/emulator/emulator /usr/bin/emulator
ln -s ${ANDROID_HOME}/emulator/emulator-headless /usr/bin/emulator-headless
#echo "no" | avdmanager -v create avd --force --name "pixel_6.0" --device "pixel" --package "system-images;android-23;google_apis;x86" --tag "google_apis" --abi "x86" -p ${ANDROID_AVD_HOME}/pixel_6.0.avd
echo "no" | avdmanager -v create avd --force --name "pixel_6.0" --device "pixel" --package "system-images;android-23;google_apis;x86" --tag "google_apis" --abi "x86"
#mv /root/.android/avd/pixel_6.0.ini ${ANDROID_AVD_HOME}/pixel_6.0.ini
# print logs
avdmanager list avds
emulator -list-avds
# chown -R agent /usr/.android
# chmod -R 777 /usr/.android/

emulator-headless -avd pixel_6.0 -accel off -verbose -gpu swiftshader_indirect -memory 4096 -no-boot-anim -wipe-data >/dev/null 2>&1 &
sleep 1000

# CONFIGURE RIGHTS
for i in $(seq 400 500); do echo "yt_slot_${i} ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers; done
cat /etc/sudoers
cat /etc/passwd

# APPIUM
npm install -g appium
