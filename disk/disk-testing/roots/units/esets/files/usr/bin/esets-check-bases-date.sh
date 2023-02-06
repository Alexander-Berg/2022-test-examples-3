#!/bin/sh
#
# Provides: check_eset_bases_date


me=${0##*/}     # strip path
me=${me%.*}     # strip extension
BASE=$HOME/agents
INTERVAL=5 # 5 minutes
day=3
fn=/var/opt/eset/esets/lib/data/data.txt

die () {
        echo "PASSIVE-CHECK:$me;$1;$2"
        exit 0
}

check_date() {
    if [ -e $1 ]; then
        ctime=$(date +%s) # current time
        mtime=$(fgrep -m1 LastUpdate $1 | cut -d '=' -f2)
        ltime=$(( $ctime - $mtime - $2 * 86400 )) # limit time
        
        if [ $ltime -le 0 ]; then
            die 0 "OK! Virus base updated less than $2 days ago"
        else
             die 2  "Too old virus base ($2 days)"
        fi
    else
         die 2  "File not exists"
    fi
}


check_date $fn $day
