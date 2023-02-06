#!/usr/local/bin/bash

POOL_CONVERTER=../pool_converter
GEN=./gen.py

function check_converter {
	POOL_FROM=$1
	FORMAT_FROM=$2
	POOL_TO=$3
	FORMAT_TO=$4
	OPTS=$5

        unset ERROR ;

	## echo "-I- Check ${POOL_FROM} -> ${POOL_TO}"

	MD5_OLD=$(cat ${POOL_TO} | md5sum -b | sed -e 's/ .*//g') ;
	MD5_NEW=$(${POOL_CONVERTER} -i ${POOL_FROM} ${OPTS} ${FORMAT_FROM} ${FORMAT_TO} | md5sum -b | sed -e 's/ .*//g') ;

	if [[ ! ${MD5_OLD} == ${MD5_NEW} ]] ; then
		echo "-E- Diff when converting \"${FORMAT_FROM}\" -> \"${FORMAT_TO}\"" ;
                ERROR=1 ;
		return ;
	fi ;

	MD5_NEW=$(cat ${POOL_FROM} | gzip | ${POOL_CONVERTER} --compressed-input --compressed-output ${OPTS} ${FORMAT_FROM} ${FORMAT_TO} | zcat | md5sum -b | sed -e 's/ .*//g') ;

	if [[ ! ${MD5_OLD} == ${MD5_NEW} ]] ; then
		echo "-E- Diff when converting \"${FORMAT_FROM}\" -> \"${FORMAT_TO}\" (with i/o compression enabled)" ;
                ERROR=2 ;
		return ;
	fi ;

        ERROR=0 ;
}

function check_error {
	if [[ ! -v ERROR ]] ; then
		echo "-E- Error code is not set. Abort." ;
		exit 1 ;
	fi ;

	case ${ERROR} in
	0) let NUM_SUCC=(${NUM_SUCC}+1) ;;
	[1-2]) let NUM_FAIL=(${NUM_FAIL}+1) ;;
	*) echo "-E- Unknown error code: ${ERROR}. Abort." ;
	exit 1 ;;
	esac ;
}

if [[ ! -e ${POOL_CONVERTER} ]] ; then
	echo "-E- Binary (pool_converter) not found. Abort." ;
	exit 1 ;
fi

if [[ ! -e ${GEN} ]] ; then
	echo "-E- Random pool generator (gen.py) not found. Abort." ;
	exit 1 ;
fi

NUM_FAIL=0
NUM_SUCC=0

FORMATS_FROM="mr-proto final sortable tsv mr-tsv"
FORMATS_TO="mr-proto final sortable tsv mr-tsv mr-zeus" ## should be superset of FORMATS_FROM

FILE_TSV=$(mktemp --suffix ".tsv")
${GEN} > ${FILE_TSV}
declare -A FILES
FILES["tsv"]=${FILE_TSV}

for FORMAT in ${FORMATS_TO} ; do
	if [[ ${FORMAT} == "tsv" ]] ; then
		continue ;
	fi ;

	FILES[${FORMAT}]=$(mktemp --suffix ".${FORMAT}") ;

	if [[ ${FORMAT} == "mr-zeus" ]] ; then
		${POOL_CONVERTER} -i ${FILE_TSV} -o ${FILES[${FORMAT}]}  -l 10 tsv ${FORMAT} ;
	else
		${POOL_CONVERTER} -i ${FILE_TSV} -o ${FILES[${FORMAT}]} tsv ${FORMAT} ;
	fi ;

done

for FORMAT_FROM in ${FORMATS_FROM} ; do
	POOL_FROM=${FILES[${FORMAT_FROM}]} ;
	if [[ ! -e ${POOL_FROM} ]] ; then
		echo "-E- File not found: ${POOL_FROM}. Abort." ;
                exit 1 ;
	fi ;

	for FORMAT_TO in ${FORMATS_TO} ; do
		POOL_TO=${FILES[${FORMAT_TO}]} ;
		if [[ ! -e ${POOL_TO} ]] ; then
			echo "-E- File not found: ${POOL_TO}. Abort." ;
			exit 1 ;
		fi ;

		if [[ ${FORMAT_TO} == "mr-zeus" ]] ; then
			check_converter ${POOL_FROM} ${FORMAT_FROM} ${POOL_TO} ${FORMAT_TO}  "-l 10" ;
		else
			check_converter ${POOL_FROM} ${FORMAT_FROM} ${POOL_TO} ${FORMAT_TO} ;
		fi ;
                check_error ;
	done ;
done

echo "-I- Tests failed: ${NUM_FAIL}"
echo "-I- Tests passed: ${NUM_SUCC}"
