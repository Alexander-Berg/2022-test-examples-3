#!/bin/sh

SCRIPT=$(readlink -f $0)
SCRIPTPATH=`dirname ${SCRIPT}`
TESTSDIR=$(readlink -f "$SCRIPTPATH/../../")
WORKDIR=$(readlink -f "$TESTSDIR/../")
TMP_DIR=$(readlink -f "$WORKDIR/tmp")

export PYTHONPATH=/skynet
export PATH=$PATH:${WORKDIR}/test_venv/bin


/skynet/python/bin/virtualenv --system-site-packages ${WORKDIR}/test_venv

. ${WORKDIR}/test_venv/bin/activate

pip install -r ${WORKDIR}/requirements.txt -i https://pypi.yandex-team.ru/simple/

pushd .
rm -rf ${TMP_DIR}
mkdir -p ${TMP_DIR}
cd ${WORKDIR}

echo "PARSER TEST:"
for i in `ls ${TESTSDIR}/prototypes/*_rkub.txt` ; do
    test_name=`echo ${i} | sed -e 's/_rkub.txt//g; s/.*prototypes\///g' | xargs echo`
    echo -n ${test_name}:
    parser_name=`${WORKDIR}/rtcc.py factory --type=${test_name} --show=parser`
    echo -n ${parser_name}:
    cat ${TESTSDIR}/prototypes/${test_name}_rkub.txt | ${WORKDIR}/rtcc.py parse ${parser_name} --input-format=OLDSTYLE --output-format=LINE | ${WORKDIR}/rtcc.py parse ${parser_name} --input-format=LINE --output-format=PRETTY | ${WORKDIR}/rtcc.py parse ${parser_name} --input-format=PRETTY --output=OLDSTYLE > ${TMP_DIR}/${test_name}_rkub.txt
    test_result=`diff -wbu ${TESTSDIR}/prototypes/${test_name}_rkub.txt ${TMP_DIR}/${test_name}_rkub.txt && echo OK`
    echo ${test_result}
done
echo

echo "CTL TEST:"
for i in `ls ${TESTSDIR}/prototypes/*_rkub.txt` ; do
    test_name=`echo ${i} | sed -e 's/_rkub.txt//g; s/.*prototypes\///g' | xargs echo`
    echo -n ${test_name}:
    parser_name=`${WORKDIR}/rtcc.py factory --type=${test_name} --show=parser`
    echo -n ${parser_name}:
    ctl_type=$(`echo ${WORKDIR}/rtcc.py factory --type=${test_name} --show=ctl`)
    echo -n ${ctl_type}:
    if [ "$ctl_type" = 'elem' ] ; then
        elem=`cat ${WORKDIR}/data/search/element/${test_name}/allknown.dat | head -n 1`
        echo -n ${elem}:
        ${WORKDIR}/rtcc.py control config --input-file=${TESTSDIR}/prototypes/empty.txt --command=ADD  | ${WORKDIR}/rtcc.py control element --l=MSK --command=ADD --eltype=${test_name} --el=${elem} --data="$elem" | ${WORKDIR}/rtcc.py control config --l=MSK --p=WEB --c=RKUB --s=PRODUCTION | perl -lnae "print 'OK' if \$F[1] =~ /^$elem/"
    elif [ "$ctl_type" = 'data' ] ; then
        if [ "$parser_name" = "sectionblockdata" ] ; then
            data="$test_name(blah blah)"
        elif [ "$parser_name" = "namedblockdata" ] ; then
            data="$test_name(blah blah)"
        else
            data="$test_name blah"
        fi
        echo -n ${data}:
        ${WORKDIR}/rtcc.py control config --input-file=${TESTSDIR}/prototypes/empty.txt --command=ADD  | ${WORKDIR}/rtcc.py control data --l=MSK --command=ADD --eltype=${test_name} --data="$data" | ${WORKDIR}/rtcc.py control config --l=MSK --p=WEB --c=RKUB --s=PRODUCTION | perl -lnae "print 'OK' if \$_ =~ /$test_name/"
    fi
done

deactivate
rm -rf ${WORKDIR}/test_venv
rm -rf ${TMP_DIR}
popd
