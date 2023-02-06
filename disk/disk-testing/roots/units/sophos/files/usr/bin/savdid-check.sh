#!/bin/bash

rout=`/usr/bin/sophos-check-fname.py /u0/savdi_tmp/test-av 2>&1`
if [ $? -eq 0 ]
then
    echo "0; ${rout}"
else
    echo "2; ${rout}"
fi

exit 0 

