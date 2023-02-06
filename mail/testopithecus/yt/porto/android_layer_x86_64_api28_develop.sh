#!/bin/bash -e

###############################################################################
# Nvidia 396.44-0 + CUDA 9.0.176-1
###############################################################################

export DEBIAN_FRONTEND=noninteractive

OS=ubuntu1604
export DEBIAN_FRONTEND=noninteractive

echo "deb http://dist.yandex.ru/common unstable/all/" >> /etc/apt/sources.list
echo "deb http://dist.yandex.ru/common stable/all/" >> /etc/apt/sources.list

apt update

ldconfig

apt-get update

# INSTALL UTILS
dpkg --add-architecture i386
apt-get update
apt-get install -y curl software-properties-common unzip wget
openssl version -a
echo "Installing new version of openssl"
apt-get install -y libpulse0 libglu1 openssl
echo "Check version of openssl"
openssl version -a

# INSTALL IMAGEMAGIC
sudo apt-get install -y imagemagick

# INSTALL JQ for parsing json from command line
sudo apt-get install -y jq

# INSTALL JAVA
apt-get install -y yandex-jdk8/stable

# INSTALL KVM FOR LINUX
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y qemu-kvm libvirt-bin ubuntu-vm-builder bridge-utils cpu-checker
sudo adduser `id -un` libvirtd
sudo adduser `id -un` kvm
#sudo modprobe -a kvm

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
export REPO_OS_OVERRIDE="linux"
mkdir /usr/.android/
sdkmanager --sdk_root=${ANDROID_SDK_ROOT} "emulator" "platform-tools" "platforms;android-28" "system-images;android-28;google_apis;x86_64" >> install_log
ln -s ${ANDROID_HOME}/platform-tools/adb /usr/bin/adb
ln -s ${ANDROID_HOME}/emulator/emulator /usr/bin/emulator
ln -s ${ANDROID_HOME}/emulator/emulator-headless /usr/bin/emulator-headless
echo "no" | avdmanager -v create avd --force --name "android_28" --device "pixel" --package "system-images;android-28;google_apis;x86_64" --tag "google_apis" --abi "x86_64"

chmod -R 777 /usr/.android/
sudo chown -R root /usr/.android
echo "AVD image created.."

sudo apt install -y python-pip
sudo pip install beautifulsoup4

# CONFIGURE RIGHTS
for i in $(seq 0 500); do echo "yt_slot_${i} ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers; done
cat /etc/sudoers
