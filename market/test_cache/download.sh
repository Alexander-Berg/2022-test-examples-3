#!/bin/sh
CUR_DIRNAME=$(cd $(dirname $0); pwd)
YAMAKE_DIRNAME=$(cd $CUR_DIRNAME/../../content-api/make/download-test-cache; pwd)

YAMAKE=ya.make
ARCHIVE=cache.tar.gz

RESOURCE=$(grep -A 1 'FROM_SANDBOX' "$YAMAKE_DIRNAME/$YAMAKE" | tail -n 1 | egrep -o '[[:digit:]]+')
echo $RESOURCE

TMP=$(mktemp -d)
echo "$TMP"
cd $TMP

wget -O $ARCHIVE "https://proxy.sandbox.yandex-team.ru/$RESOURCE" >/dev/null 2>/dev/null
tar -xzf $ARCHIVE
rm $ARCHIVE
cd $CUR_DIRNAME
