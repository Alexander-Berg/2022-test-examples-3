#!/bin/bash

#sudo find /u0/nginx/cache -mindepth 1 -maxdepth 1 -exec rm -rf {} \;


#TIMESTAMP=`date "+%s" | xargs printf "%x"`
#TIMESTAMP=ff
TIMESTAMP=mpfs

MPFS_HASH=""
#MPFS_HASH="mpfs_hash"
#MPFS_HASH="GpcUq1Qaeanz1AqYgfEEw3AQ0yvyA9pY9t7785z95j4="

# NOTE: /zip handle requires base64 encoding for its TARGET_REF
# and content-type parameter absence (in token should be 'application/zip')
# Also, use path with uid > 0 and hash for uid == 0.
#TARGET_REF="/disk/Test/Img/Several"
#TARGET_REF="$MPFS_HASH"
#CONTENT_TYPE_TOKEN="application/zip"

TARGET_REF="1000005.1.42433480221430165204869206570" # dev, small text
#TARGET_REF="1000005.yadisk:173337688.3983296384203508725332872709265" # dev, preview image (png)
#TARGET_REF="1000008.yadisk:5181427.3983296384118990298559902240724" # dev, preview image 2 (jpeg)
#TARGET_REF="1000004.16011578.27925765274971127813449852652" # dev, 36m file
#TARGET_REF="2877.0.326341060310225634732078346411" # production


#FOR_UID=5181427
#FOR_UID=128280859
FOR_UID=0
SESSION_ID=""

FILENAME="privet.txt"
#FILENAME="0" # CHEMODAN-4301

CONTENT_TYPE="text/plain"
#CONTENT_TYPE="image/png"
CONTENT_TYPE_TOKEN="${CONTENT_TYPE}"

DISPOSITION="inline"
#DISPOSITION="attachment"


SIZE="XS"
CROP=0
LOGO=1

LIMIT=0
FSIZE=10

DIGEST="sha1sum"
TOKEN=`echo -n "7NFFDn7869fdSNFSdf76sn-${TIMESTAMP}-${TARGET_REF}-${FOR_UID}-${FILENAME}-${CONTENT_TYPE_TOKEN}-${DISPOSITION}-${MPFS_HASH}-${LIMIT}" | $DIGEST | cut -d ' ' -f 1`
ENC_TARGET_REF=`echo -n $TARGET_REF | base64 -`
#ENC_TARGET_REF=`./encrypt_stid.pl "$TARGET_REF"`

LOGIN="mpfs-test"
PASSWORD="efdf2a79dd23f1f3"
#BASIC_AUTH=`echo -n "$LOGIN:$PASSWORD" | base64 -`
#echo "$BASIC_AUTH"

#HOSTNAME='downloader-local-check-auth.disk.dev.yandex.ru'
HOSTNAME='akirakozov2.dev.yandex.net'
MY_IP=`ifconfig | grep -oP "(?<=inet addr:)[\d+\.]+" | grep -v "127.0.0.1" | head -n 1`

URL="localhost/disk/${TOKEN}/${TIMESTAMP}/${ENC_TARGET_REF}?uid=${FOR_UID}&filename=${FILENAME}&content_type=${CONTENT_TYPE}&disposition=${DISPOSITION}&hash=${MPFS_HASH}&limit=${LIMIT}&fsize=${FSIZE}&tknv=v2"
#URL="$HOSTNAME/zip/${TOKEN}/${TIMESTAMP}/${ENC_TARGET_REF}?uid=${FOR_UID}&filename=${FILENAME}&disposition=${DISPOSITION}&hash=${MPFS_HASH}&limit=${LIMIT}"
#URL="localhost/preview/${TOKEN}/${TIMESTAMP}/${ENC_TARGET_REF}?uid=${FOR_UID}&filename=${FILENAME}&content_type=${CONTENT_TYPE}&disposition=${DISPOSITION}&hash=${MPFS_HASH}&limit=${LIMIT}&size=${SIZE}&crop=${CROP}&logo=${LOGO}"
#URL="$HOSTNAME/sample"

OPTS="-L -k"
#COMMAND="curl $OPTS -v -H 'X-Real-IP: ${MY_IP}' -H 'Cookie: Session_id=${SESSION_ID}' -H 'Host: $HOSTNAME' -H 'Range: bytes=0-4' -H 'Accept-Encoding: gzip, deflate' '$URL'"
COMMAND="curl $OPTS -v -H 'X-Real-IP: ${MY_IP}' -H 'Cookie: Session_id=${SESSION_ID}' -H 'Host: $HOSTNAME' '$URL'"
#COMMAND="curl $OPTS -v -H 'X-Real-IP: ${MY_IP}' -H 'Authorization: Basic $BASIC_AUTH' -H 'Host: $HOSTNAME' '$URL'"

echo "$COMMAND"
eval "$COMMAND"
#eval "$COMMAND >out"
