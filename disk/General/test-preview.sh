#!/bin/bash

#TIMESTAMP=`date "+%s" | xargs printf "%x"`
#TIMESTAMP=ff
TIMESTAMP=mpfs

MPFS_HASH=""

TARGET_REF="1000005.yadisk:173337688.3983296384203508725332872709265" # dev, preview image (png)
#TARGET_REF="1000008.yadisk:5181427.3983296384118990298559902240724" # dev, preview image 2 (jpeg)


FOR_UID=0
SESSION_ID=""

FILENAME="result.png"

CONTENT_TYPE="image/png"
CONTENT_TYPE_TOKEN="${CONTENT_TYPE}"

DISPOSITION="inline"

SIZE="XS"
CROP=0
LOGO=1

USER_NAME="Testuser"
ALBUM_NAME="MegaPhotoAlbum"

DIGEST="md5sum"
TOKEN=`echo -n "7NFFDn7869fdSNFSdf76sn-${TIMESTAMP}-${TARGET_REF}-${FOR_UID}-${FILENAME}-${CONTENT_TYPE_TOKEN}-${DISPOSITION}-${MPFS_HASH}-${LIMIT}" | $DIGEST | cut -d ' ' -f 1`
ENC_TARGET_REF=`./encrypt_stid.pl "$TARGET_REF"`

# ordinary dynamic preview
URL="localhost/preview/${TOKEN}/${TIMESTAMP}/${ENC_TARGET_REF}?uid=${FOR_UID}&filename=${FILENAME}&content_type=${CONTENT_TYPE}&disposition=${DISPOSITION}&size=${SIZE}&crop=${CROP}&logo=${LOGO}"

# album preview
#URL="localhost/preview/${TOKEN}/${TIMESTAMP}/${ENC_TARGET_REF}?uid=${FOR_UID}&filename=${FILENAME}&content_type=${CONTENT_TYPE}&disposition=${DISPOSITION}&user_name=${USER_NAME}&album_name=${ALBUM_NAME}"

OPTS="-L -k"
COMMAND="curl $OPTS -v '$URL'"

echo "$COMMAND"
eval "$COMMAND"
#eval "$COMMAND >out"
