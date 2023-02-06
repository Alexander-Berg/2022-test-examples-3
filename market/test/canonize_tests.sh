#!/bin/bash
set -e -o pipefail

echo "See https://wiki.yandex-team.ru/yatool/test/#kanonizacija for details!"
echo "Canonizing! ..."

../../../../ya make -r -tA --canonize-tests

echo "All DONE!"
