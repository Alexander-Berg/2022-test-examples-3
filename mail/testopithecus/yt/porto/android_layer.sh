#!/bin/bash -e

###############################################################################
# Nvidia 396.44-0 + CUDA 9.0.176-1
###############################################################################

export DEBIAN_FRONTEND=noninteractive

OS=ubuntu1604

nvidia_version=396.44-0
cuda_version=9.0.176-1

cuda_repo="http://developer.download.nvidia.com/compute/cuda/repos/${OS}/x86_64"
ml_repo="http://developer.download.nvidia.com/compute/machine-learning/repos//${OS}/x86_64"

download_package() {
    local repo=${1}
    local file=${2}

    echo "Upload ${file}..."
    wget --quiet ${repo}/${file}
}

###############################################################################
# Install CUDA
###############################################################################

nvidia_version_major=${nvidia_version%.*} # a.b -> a

cuda_version_major_minor=${cuda_version%.*} #a.b.c -> a.b
cuda_version_code=${cuda_version_major_minor//./-} #a.b -> a-b

cuda_packages=(
    "nvidia-${nvidia_version_major}_${nvidia_version}ubuntu1_amd64.deb"
    "cuda-license-${cuda_version_code}_${cuda_version}_amd64.deb"
    "libcuda1-${nvidia_version_major}_${nvidia_version}ubuntu1_amd64.deb"
    "cuda-cufft-${cuda_version_code}_${cuda_version}_amd64.deb"
    "cuda-cublas-${cuda_version_code}_${cuda_version}_amd64.deb"
    "cuda-cusolver-${cuda_version_code}_${cuda_version}_amd64.deb"
    "cuda-curand-${cuda_version_code}_${cuda_version}_amd64.deb"
    "cuda-cudart-${cuda_version_code}_${cuda_version}_amd64.deb"
    "cuda-cusparse-${cuda_version_code}_${cuda_version}_amd64.deb"
)

ml_packages=(
    "libcudnn7_7.2.1.38-1+cuda${cuda_version_major_minor}_amd64.deb"
    "libnccl2_2.2.13-1+cuda${cuda_version_major_minor}_amd64.deb"
)

echo "deb http://dist.yandex.ru/common unstable/all/" >> /etc/apt/sources.list
echo "deb http://dist.yandex.ru/common stable/all/" >> /etc/apt/sources.list

apt update
mkdir /usr/lib/nvidia

install_package() {
    local repo=${1}
    local package=${2}

    download_package $repo $package
    apt install --yes --fix-broken --no-install-recommends ./$package
    rm -f $package
}

for package in ${cuda_packages[*]}
do
    install_package $cuda_repo $package
done;

for package in ${ml_packages[*]}
do
    install_package $ml_repo $package
done;


###############################################################################
# Configure dynamic linker
###############################################################################

cuda_conf=/etc/ld.so.conf.d/cuda-${cuda_version_major_minor}.conf
sudo echo /usr/local/cuda-${cuda_version_major_minor}/lib64 > "${cuda_conf}"
echo /usr/lib/x86_64-linux-gnu >> "${cuda_conf}"

nvidia_conf=/etc/ld.so.conf.d/nvidia-${nvidia_version_major}.conf
echo /usr/lib/nvidia-${nvidia_version_major} > "${nvidia_conf}"

ldconfig

apt-get update

# INSTALL UTILS
dpkg --add-architecture i386
apt-get update
apt-get install -y curl software-properties-common unzip wget
apt-get install -y libpulse0 libglu1

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
# ANDROID_SDK_HOME="/usr/.android/"
#ANDROID_EMULATOR_HOME="/usr/.android/"
# mkdir /usr/.android/
sdkmanager --sdk_root=${ANDROID_SDK_ROOT} "emulator" "platform-tools" "platforms;android-23" "system-images;android-23;google_apis;x86" >> install_log
ln -s ${ANDROID_HOME}/platform-tools/adb /usr/bin/adb
ln -s ${ANDROID_HOME}/emulator/emulator /usr/bin/emulator
ln -s ${ANDROID_HOME}/emulator/emulator/mksdcard /usr/bin/mksdcard
ln -s ${ANDROID_HOME}/emulator/emulator-headless /usr/bin/emulator-headless
# echo "no" | avdmanager -v create avd --force --name "pixel_6.0" --device "pixel" --package "system-images;android-23;google_apis;x86" --tag "google_apis" --abi "x86" -p ${ANDROID_AVD_HOME}/pixel_6.0.avd
# mv /root/.android/avd/pixel_6.0.ini ${ANDROID_AVD_HOME}/pixel_6.0.ini
# print logs
# avdmanager list avds
# emulator -list-avds
# chown -R agent /usr/.android
# chmod -R 777 /usr/.android/

# CONFIGURE RIGHTS
for i in $(seq 1 500); do echo "yt_slot_${i} ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers; done
cat /etc/sudoers

# APPIUM
npm install -g appium
