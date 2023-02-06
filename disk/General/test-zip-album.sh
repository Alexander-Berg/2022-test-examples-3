#!/bin/bash

CUR_TIME=`date "+%s"`
TIME=`expr $CUR_TIME + 1000`
TIMESTAMP=`echo $TIME | xargs printf "%x"`

PUBLIC_KEY="hrqswxCKCpMch9dN0L2lwjo/Ed1+yxRKWUhOsihA5uNgvahc2S7Hz8Eugb6VQ3rwq/J6bpmRyOJonT3VoXnDag=="

# NOTE: /zip handle requires base64 encoding for its TARGET_REF
# and content-type parameter absence (in token should be 'application/zip')
# Also, use path with uid > 0 and hash for uid == 0.
TARGET_REF="$PUBLIC_KEY"
CONTENT_TYPE_TOKEN="application/zip"

FOR_UID=0
FILENAME="result.zip"
DISPOSITION="attachment"

DIGEST="md5sum"
TOKEN=`echo -n "7NFFDn7869fdSNFSdf76sn-${TIMESTAMP}-${TARGET_REF}-${FOR_UID}-${FILENAME}-${CONTENT_TYPE_TOKEN}-${DISPOSITION}-${PUBLIC_KEY}-${LIMIT}" | $DIGEST | cut -d ' ' -f 1`
ENC_TARGET_REF=`echo -n $TARGET_REF | base64 -w 0 -`

HOSTNAME='downloader-local-check-auth.disk.dev.yandex.ru'

# Escape '+' symbol
HASH=`echo $PUBLIC_KEY | sed 's/+/%2B/g'`
echo $HASH;

URL="localhost/zip-album/${TOKEN}/${TIMESTAMP}/${ENC_TARGET_REF}?uid=${FOR_UID}&filename=${FILENAME}&disposition=${DISPOSITION}&hash=${HASH}&limit=${LIMIT}"

OPTS="-L -k"
COMMAND="curl $OPTS -v -H 'Host: $HOSTNAME' '$URL'"

echo "$COMMAND"
eval "$COMMAND"
#eval "$COMMAND >out"
