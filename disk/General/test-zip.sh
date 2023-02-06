#!/bin/bash

CUR_TIME=`date "+%s"`
TIME=`expr $CUR_TIME + 100`
TIMESTAMP=`echo $TIME | xargs printf "%x"`

MPFS_HASH="GpcUq1Qaeanz1AqYgfEEw3AQ0yvyA9pY9t7785z95j4="

# NOTE: /zip handle requires base64 encoding for its TARGET_REF
# and content-type parameter absence (in token should be 'application/zip')
# Also, use path with uid > 0 and hash for uid == 0.
#TARGET_REF="/disk/Test/Img/Several"
TARGET_REF="$MPFS_HASH"
CONTENT_TYPE_TOKEN="application/zip"

FOR_UID=0
FILENAME="result.zip"
DISPOSITION="attachment"

DIGEST="md5sum"
TOKEN=`echo -n "7NFFDn7869fdSNFSdf76sn-${TIMESTAMP}-${TARGET_REF}-${FOR_UID}-${FILENAME}-${CONTENT_TYPE_TOKEN}-${DISPOSITION}-${MPFS_HASH}-${LIMIT}" | $DIGEST | cut -d ' ' -f 1`
ENC_TARGET_REF=`echo -n $TARGET_REF | base64 -`

HOSTNAME='downloader-local-check-auth.disk.dev.yandex.ru'

URL="localhost/zip/${TOKEN}/${TIMESTAMP}/${ENC_TARGET_REF}?uid=${FOR_UID}&filename=${FILENAME}&disposition=${DISPOSITION}&hash=${MPFS_HASH}&limit=${LIMIT}"

OPTS="-L -k"
COMMAND="curl $OPTS -v -H 'Host: $HOSTNAME' '$URL'"

echo "$COMMAND"
eval "$COMMAND"
#eval "$COMMAND >out"
