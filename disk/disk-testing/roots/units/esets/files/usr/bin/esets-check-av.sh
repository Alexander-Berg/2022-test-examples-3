#!/bin/bash

rout=`/opt/eset/esets/bin/esets_cli /u0/av_tmp/test-av 2>&1 | tr '\n' '; '`

if [ $? -eq 0 ]
then
    echo "0; ${rout}"
else
    echo "2; ${rout}"
fi

exit 0
