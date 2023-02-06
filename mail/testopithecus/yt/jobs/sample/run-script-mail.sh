#!/bin/bash

export HOME=$(pwd)
export ANDROID_SDK_ROOT="/opt/android-sdk"
export ANDROID_AVD_HOME="${HOME}/.android/avd"

printenv >>/proc/self/fd/2

sudo -E mkdir "${HOME}/.android" >>/proc/self/fd/2
sudo -E unzip -qq android_28_google_apis_x86_64.zip -d "${HOME}/.android" >>/proc/self/fd/2
sudo -E sed -i 's@/root@'"$HOME"'@' ${HOME}/.android/avd/android_28.ini >>/proc/self/fd/2
#sudo -E sed -i 's@/root@'"$HOME"'@' ${HOME}/.android/avd/android_28.avd/hardware-qemu.ini >>/proc/self/fd/2

function takeScreenShotAndLogs() {
    sudo adb -s emulator-${PORT_NUM} exec-out screencap -p > screen.png
    sudo adb -s emulator-${PORT_NUM} logcat -s *:W -d > log.txt
    base64=$(cat screen.png | base64 -w 0)
    logs=$(cat log.txt | base64 -w 0)
    echo "{ \"date\" : \"$(date)\", \"screen\" : \"${base64}\", \"logs\" : \"${logs}\" }"
}

sudo -E chmod -R 777 ${HOME}/.android >>/proc/self/fd/2
declare me=$(whoami)
sudo -E chown $me -R /dev/kvm >>/proc/self/fd/2

# GENERATING RANDOM PORT FOR EMULATOR
declare -i x
x="$(shuf -i 2778-2792 -n 1)"
PORT_NUM=$((x*2))

echo "Selected port ${PORT_NUM} as console port.." >>/proc/self/fd/2

# START EMULATOR
echo "Starting emulator emulator-${PORT_NUM} .." >>/proc/self/fd/2
emulator -no-window -port ${PORT_NUM} -avd android_28 -delay-adb -camera-back none -camera-front none -accel on -memory 4096 -cores 2 -restart-when-stalled -no-sim -no-passive-gps -skip-adb-auth -no-audio -gpu swiftshader_indirect -no-boot-anim -no-cache -no-snapstorage -no-snapshot-save -no-snapshot-load -skin 270x480 -no-jni -verbose -read-only -qemu -lcd-density 120 1>&2 &
sleep 60 >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done;'
sleep 60 >>/proc/self/fd/2

# TURN SKIA RENDERER ON FOR BETTER PERFORMANCE
adb -s emulator-${PORT_NUM} root shell "su && setprop debug.hwui.renderer skiagl && stop && start" >>/proc/self/fd/2

echo "Following devices available:" >>/proc/self/fd/2
adb devices >>/proc/self/fd/2

# DISABLE ANIMATIONS
adb -s emulator-${PORT_NUM} shell settings put global window_animation_scale 0 >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell settings put global transition_animation_scale 0 >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell settings put global animator_duration_scale 0 >>/proc/self/fd/2

# INSTALLING HOST AND TEST APKs
echo "Installing apk on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} install -t mail2-v2147483647-beta-debug.apk >>/proc/self/fd/2
echo "APK installed on emulator-${PORT_NUM} .." >>/proc/self/fd/2
sleep 1 >>/proc/self/fd/2

echo "Installing test apk on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} install -t mail2-v2147483647-beta-debug-androidTest.apk >>/proc/self/fd/2
echo "Test APK installed on emulator-${PORT_NUM} .." >>/proc/self/fd/2
sleep 1 >>/proc/self/fd/2

#echo "Starting espresso tests 0 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
#adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.yandex.mail.testopithecus.ExampleTest' ru.yandex.mail.beta.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2

# clear the logs
adb logcat -c >>/proc/self/fd/2
echo "Starting mail application on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb shell am start -W -S -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n ru.yandex.mail.beta/ru.yandex.mail.ui.LoginActivity >>/proc/self/fd/2
echo "Started mail application on emulator-${PORT_NUM} .." >>/proc/self/fd/2
sleep 5 >>/proc/self/fd/2
takeScreenShotAndLogs
# clear the logs
adb logcat -c >>/proc/self/fd/2
echo "Choosing yandex mail provider in auth UI on emulator-${PORT_NUM} .." >>/proc/self/fd/2
echo "Authorizing in mail app using yandex account on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell input tap 138 96 >>/proc/self/fd/2
sleep 5 >>/proc/self/fd/2
takeScreenShotAndLogs

# ------------------ NOW IN LOGIN ACTIVITY
echo "Entering username: lamertester on emulator-${PORT_NUM} .." >>/proc/self/fd/2
# focus in login edittext
adb -s emulator-${PORT_NUM} shell input tap 130 231 >>/proc/self/fd/2
sleep 5 >>/proc/self/fd/2
takeScreenShotAndLogs
# enter login
adb -s emulator-${PORT_NUM} shell input text "lamertester" >>/proc/self/fd/2
sleep 5 >>/proc/self/fd/2
takeScreenShotAndLogs
# tap on Next button
adb -s emulator-${PORT_NUM} shell input tap 135 250 >>/proc/self/fd/2
sleep 15 >>/proc/self/fd/2
takeScreenShotAndLogs
# ------------------ NOW IN PASSWORD ACTIVITY
echo "Entering password: 326tester!! on emulator-${PORT_NUM} .." >>/proc/self/fd/2
# focus in password edittext
adb -s emulator-${PORT_NUM} shell input tap 150 135 >>/proc/self/fd/2
takeScreenShotAndLogs
# enter password
adb -s emulator-${PORT_NUM} shell input text "326tester!!" >>/proc/self/fd/2
takeScreenShotAndLogs
echo "Proceeding to mail list on emulator-${PORT_NUM} .." >>/proc/self/fd/2
# tap on Next button
adb -s emulator-${PORT_NUM} shell input tap 150 217 >>/proc/self/fd/2
sleep 15 >>/proc/self/fd/2
takeScreenShotAndLogs
echo "Tapping on Continue to inbox on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell input tap 150 420 >>/proc/self/fd/2
sleep 30 >>/proc/self/fd/2
takeScreenShotAndLogs
echo "Finished.." >>/proc/self/fd/2
