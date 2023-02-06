#!/bin/sh
#
#

me=${0##*/}	# strip path
me=${me%.*}	# strip extension

WAR=14 # Days before sertificate expires
CRIT=7
KEY=/opt/drweb/drweb32.key

S_WARN=`expr "$WAR" '*' 3600 '*' 24`
S_CRIT=`expr "$CRIT" '*' 3600 '*' 24`

die() {
	echo "$1;$2"
	exit 0
}

EXPIRES=`cat $KEY | grep '^Expires=' 2>/dev/null | awk '{ print $1 }'`

	set -- `echo $EXPIRES | awk -F ":" '{

		date=substr($1,9,10);
		split(date,v,"-");
		print v[1], v[2], v[3];
	}'`
	EXPIRES=${EXPIRES%:*}

	SEC_DATE=`date -d "${1}${2}${3}" +%s`
	CUR_DATE=`date +%s`

	DELTA=`expr $SEC_DATE - $CUR_DATE`
	
	[ "$DELTA" -le 0 ] && die 2 "Certificate $KEY expired"
	
	INT_DELTA=`expr "$DELTA" \/ '(' 3600 '*' 24 ')'`

	[ "$DELTA" -le "$S_CRIT" ] && die 2 "Certificate $KEY expires in $INT_DELTA days"
	[ "$DELTA" -le "$S_WARN" ] && die 1 "Certificate $KEY expires in $INT_DELTA days"
die 0 "Ok" 
