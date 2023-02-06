#!/bin/bash

set -u

##
# 1. check files availability on remote host
# 2. scp required files into tmp dir
# 3. cp files through sudo from tmp dir into target dir
#
# * ssh-forwarding should be enabled on user-site
# * runs under current user (cauth login must be used)
#   or SUDO_USER if defined (SSH_AUTH_SOCK should be keeped in env sudo configuration)
##


trap "on_exit" EXIT SIGINT SIGTERM

function on_exit() {
    if [ ${OPT_KEEP_TMP_FILES} -eq 0 ]; then
        log_msg "Cleaning temporary dir (${TMP_DIR})" 7
        rm -rf ${TMP_DIR}
    else
        log_msg "Temporary files are keeped in ${TMP_DIR}" 7
    fi
    log_msg "Exit now" 7
}

function log_msg() {
    MSG=$1
    LEVEL=$2
    if [ ${OPT_VERBOSE_LEVEL} -ge ${LEVEL} ]; then
        printf "%s\n" "${MSG}"
    fi  
}


function log_err() {
    MSG=$1
    printf "%s\n" "${MSG}" 2>&1
}


### TODO: move options into config file

OPT_VERBOSE_LEVEL=3
OPT_KEEP_TMP_FILES=0

SERVICE=disk_uploader

SECDIST_LOGIN=${LC_USER:-${USER}}
SECDIST_HOST=secdist.yandex.net
SECDIST_PATH=/repo/projects/ugc/media/certs

TMP_DIR=${HOME}/${SERVICE}_ssl
TMP_UMASK=077
TARGET_UMASK=377

#SOURCE=("${SECDIST_PATH}/${SERVICE}.*.jks")
SOURCE=("/repo/projects/ugc/media/certs/disk_uploader.*.jks")
TARGET=("/etc/yandex/disk/uploader/keys/kladun-keystore.jks")

USAGE="$0 [GO]"

if [ $# -le 0 ]; then
    log_msg "${USAGE}" 0
    exit 1
fi

if [ "$1" != "GO" ]; then
    log_msg "${USAGE}" 0
    exit 1
fi


umask ${TMP_UMASK} #TODO: replace by separate mode values from config

if [ ${#SOURCE[@]} -eq ${#TARGET[@]} ]; then
    FILES_NUM=${#SOURCE[@]}
else
    log_err "ERROR: count of varios settings must be equal"
fi

for (( i=0; i< ${FILES_NUM}; i++)); do
    TMP[$i]="${TMP_DIR}/`basename ${TARGET[$i]}`"
done


if [ -d ${TMP_DIR} ]; then
    OPT_KEEP_TMP_FILES=1
    log_err "ERROR: temporary dir (${TMP_DIR}) already exists"
    exit 1
fi

mkdir -p ${TMP_DIR}
if [ $? -ne 0 ]; then
    OPT_KEEP_TMP_FILES=1
    log_err "ERROR: can not create temporary directory"
    exit 1
fi

if [ -e /etc/yandex/environment.type ]
then
        yandex_environment=`cat /etc/yandex/environment.type`
else
        yandex_environment="development"
fi

development="development testing prestable production"
testing="testing prestable production"
prestable="prestable production"
production="production"

log_msg "Secdist repo: ${SECDIST_LOGIN}@${SECDIST_HOST}" 5
log_msg "Host environment: ${yandex_environment}" 5

for env in $(eval echo $`echo $yandex_environment`); do

    log_msg "Trying '${env}' environment" 5

    for (( i=0; i< ${FILES_NUM}; i++)); do
        STATUS=1
        SRC=`echo ${SOURCE[$i]} | sed "s/*/${env}/"`
        
        ssh ${SECDIST_LOGIN}@${SECDIST_HOST} "test -r ${SRC}"
        RVAL=$?
        if [ ${RVAL} -eq 1 ]; then  
           log_msg "File ${SRC} does not exist on remote host or not readable. Will try next environment" 10
           break
        elif [ ${RVAL} -ne 0 ]; then
           log_err "ERROR: SSH returns ${RVAL}"
           exit 1
        fi
            
        scp -q ${SECDIST_LOGIN}@${SECDIST_HOST}:${SRC} ${TMP[$i]}
        RVAL=$?
        if [ ${RVAL} -eq 0 ] ; then
            log_msg "File ${SRC} was successfully copied from remote host to ${TMP[$i]}" 10
            STATUS=0
        else
            log_err "ERROR: scp can not copy file ${SECDIST_LOGIN}@${SECDIST_HOST}:${SRC} to ${TMP[$i]}, return code ${RVAL}"
            exit 2
        fi

    done

    if [ ${STATUS} -eq 0 ]; then
        umask ${TARGET_UMASK}
        for (( i=0; i< ${FILES_NUM}; i++)); do
            sudo cp --remove-destination ${TMP[$i]} ${TARGET[$i]}
            RVAL=$?
            if [ ${RVAL} -eq 0 ] ; then
                log_msg "File ${TMP[$i]} was successfully copied to ${TARGET[$i]}" 10
            else
                log_err "ERROR: cp can not copy file ${TMP[$i]} to ${TARGET[$i]}, return code ${RVAL}"
                exit 2
            fi  
            sudo chown disk:root ${TARGET[$i]}

        done
        
        log_msg "* Successfully copied files from environment '${env}' (current host environment '${yandex_environment}')" 3
        exit 0
    fi

done

log_err "ERROR: required files can not be found in any environment"

exit 0

