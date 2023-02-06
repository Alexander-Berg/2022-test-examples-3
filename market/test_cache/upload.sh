#!/bin/sh
DIRNAME=$1

CUR_DIRNAME=$(cd $(dirname $0); pwd)

if [ ! -d "$DIRNAME" ]
then
    echo "Directory '$DIRNAME' is not exists. You must specify directory in cmdline argument"
    exit 1 
fi

cd $DIRNAME
FILENAME=cache.tar.gz

if [ -f "$FILENAME" ]
then
    echo "Result filename already exists remove '$FILENAME' or upload it manually"
    exit 2
fi

tar -czvf $FILENAME .
ya upload --owner MARKET-CONTENT-API --ttl inf -d 'Content api test file' "$FILENAME"
rm $FILENAME
cd $CUR_DIRNAME
