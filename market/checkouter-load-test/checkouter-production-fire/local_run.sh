#!/bin/sh
rm ./app/app
rm run.log
ya make --yt-store
TVM_SECRET=$(ya vault get version 'ver-01f7x59v7ksw4t974cp3w4n0bf' -o 'client_secret')
TVM_SECRET=${TVM_SECRET} ./app/app local.yaml | tee run.log
