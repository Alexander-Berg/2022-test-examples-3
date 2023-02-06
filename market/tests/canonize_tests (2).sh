#!/bin/bash
set -e -o pipefail

echo "See https://wiki.yandex-team.ru/yatool/test/#kanonizacija for details!"
echo "Canonizing! ..."

../../../../../ya make -tA --canonize-tests --keep-temps

echo "All DONE!"
