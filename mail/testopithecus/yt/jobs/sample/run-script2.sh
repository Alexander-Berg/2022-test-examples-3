#!/bin/bash

export HOME=$(pwd)
export ANDROID_SDK_ROOT="/opt/android-sdk"
export ANDROID_AVD_HOME="${HOME}/.android/avd"
sudo -E mkdir "${HOME}/.android" >>/proc/self/fd/2
sudo -E unzip -qq android_28_google_apis.zip -d "${HOME}/.android" >>/proc/self/fd/2
sudo -E sed -i 's@/root@'"$HOME"'@' ${HOME}/.android/avd/android_28.ini >>/proc/self/fd/2
sudo -E sed -i 's@/root@'"$HOME"'@' ${HOME}/.android/avd/android_28.avd/hardware-qemu.ini >>/proc/self/fd/2

sudo -E chmod -R 777 ${HOME}/.android >>/proc/self/fd/2
declare me=$(whoami)
sudo -E chown $me -R /dev/kvm >>/proc/self/fd/2

# GENERATING RANDOM PORT FOR EMULATOR
declare -i x
x="$(shuf -i 2778-2792 -n 1)"
PORT_NUM=$((x*2))

echo "Selected port ${PORT_NUM} as console port.." >>/proc/self/fd/2
# READ LINES FROM INPUT
mkdir texts_${PORT_NUM} >>/proc/self/fd/2
while read line; do
  id=$(jq -r '.id' <(echo "$line"))
  text=$(jq -r '.text' <(echo "$line"))
  echo "$text" > texts_${PORT_NUM}/${id}.txt
done < /proc/self/fd/0


# START EMULATOR
echo "Starting emulator emulator-${PORT_NUM} .." >>/proc/self/fd/2
emulator-headless -port ${PORT_NUM} -avd android_28 -delay-adb -camera-back none -camera-front none -accel on -gpu host -memory 4096 -cores 2 -restart-when-stalled -ranchu -no-sim -no-passive-gps -skip-adb-auth -no-audio -gpu swiftshader_indirect -no-boot-anim -no-cache -no-snapstorage -no-snapshot-save -no-snapshot-load -skin 1080x1920 1>&2 &
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

# ADDING RUSSIAN KEYBOARD
echo "Adding russian keyboard for device emulator-${PORT_NUM} .." >>/proc/self/fd/2

adb -s emulator-${PORT_NUM} shell am start -a android.settings.LOCALE_SETTINGS >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell input tap 540 515 >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell input tap 1017 136 >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell input text "russ" >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell input tap 540 305 >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell input tap 540 403 >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell input keyevent 3 >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am force-stop com.android.settings >>/proc/self/fd/2

# INSTALLING HOST AND TEST APKs
echo "Installing apk on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} install -t app-debug.apk >>/proc/self/fd/2
echo "APK installed on emulator-${PORT_NUM} .." >>/proc/self/fd/2
sleep 1 >>/proc/self/fd/2

echo "Installing test apk on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} install -t app-debug-androidTest.apk >>/proc/self/fd/2
echo "Test APK installed on emulator-${PORT_NUM} .." >>/proc/self/fd/2
sleep 1 >>/proc/self/fd/2

adb -s emulator-${PORT_NUM} push texts_${PORT_NUM} /storage/emulated/0/Android/data/com.example.myapplication/files/Download/ >>/proc/self/fd/2

# RUNNING ESPRESSO TEST

echo "Starting espresso tests 0 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity0' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 1 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity1' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 2 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity2' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 3 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity3' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 4 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity4' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 5 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity5' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 6 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity6' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 7 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity7' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 8 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity8' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests 9 on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity9' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests A on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity10' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests B on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity11' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests C on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity12' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests D on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity13' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests E on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity14' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2
echo "Starting espresso tests F on emulator-${PORT_NUM} .." >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} shell am instrument -w -r -e debug false --no-window-animation -e class 'com.example.myapplication.ChangeTextBehaviorTest#changeText_sameActivity15' com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner >>/proc/self/fd/2


mkdir -p hier_dump_${PORT_NUM}
# PULLING DIR WITH HIERARCHY DUMP FROM DEVICE
dir_to_pull=/storage/emulated/0/Android/data/com.example.myapplication/files/Pictures/hier_dump
echo "dir to pull: $dir_to_pull" >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} root >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} pull ${dir_to_pull} hier_dump_${PORT_NUM} >>/proc/self/fd/2

python parse_hierarchy_dump.py ${HOME}/hier_dump_${PORT_NUM}/hier_dump "$(date)"
echo "Finished..killing emulator" >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} emu kill >>/proc/self/fd/2
adb -s emulator-${PORT_NUM} kill-server >>/proc/self/fd/2
echo "Emulator emulator-${PORT_NUM} killed" >>/proc/self/fd/2
rm -rf texts_${PORT_NUM} >>/proc/self/fd/2
rm -rf hier_dump_${PORT_NUM} >>/proc/self/fd/2