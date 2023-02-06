#!/usr/bin/env bash

ssh xrater-vm-max.sas.yp-c.yandex.net << EOF
adb exec-out screencap -p > screen.png
EOF
scp xrater-vm-max.sas.yp-c.yandex.net:~/screen.png "screens/$(date +"%Y-%m-%d-%H:%M:%S")_screen.png"
