#!/bin/sh
# Script for dev test runs

if [ "$#" -lt 0 ]; then
	echo "" && echo "Usage: $0 [suit_name]" && echo ""
	exit 1
fi

suit=$1

curdir=`pwd`
libdir=$curdir/lib/
confdir=~/mpfsconf/
cmd="sudo -u nginx /usr/bin/env PYTHONPATH=$libdir MPFS_CONFIG_PATH=$confdir python "
echo $cmd
cd test

if [ ! -d $confdir ]; then
	mkdir $confdir
fi

function put_configs {
    package=$1

	for configfile in $curdir/$package/conf/*.development
	do
		filename=$(basename $configfile)
		link=$confdir$filename
		if [ -L $link ]; then
			rm $link
		fi
		ln -s $configfile $link
	done
}

put_configs "common"

if [[ $suit == "" ]]; then
    for file in *.py
    do
		if [[ $file == browser_*.py ]]; then
    		conf="browser"
		else
			conf="disk"
		fi
		put_configs $conf
		echo $file
		$cmd $file
    done
else
    fullsuit=$suit\_suit.py
	if [[ $fullsuit == browser_*.py ]]; then
		conf="browser"
	else
		conf="disk"
	fi
	put_configs $conf
    $cmd $fullsuit
fi

rm -R $confdir/*
