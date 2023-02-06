#!/bin/bash
set -e

GENERATION=20160117_2021  # Sunday; interacts with data/schedule.txt
FORMAT=orig

while getopts c:t:s:f:P:L:S:C:d:g:F:p:T: opt; do
    case $opt in
    p)
        YT_PROXY=$OPTARG
        ;;
    T)
        YT_TOKENPATH=$OPTARG
        ;;
    c)
        CONFIG=$OPTARG
        ;;
    t)
        TMP=$OPTARG
        ;;
    s)
        TYPE=$OPTARG
        ;;
    f)
        FORMAT=$OPTARG
        ;;
    F)
        ALLOWED_PARAM_FILTER=$OPTARG
        ;;
    P)
        PBSNCAT=$OPTARG
        ;;
    L)
        SORT_LENVAL=$OPTARG
        ;;
    S)
        STATS_CALC=$OPTARG
        ;;
    C)
        STATS_CONVERT=$OPTARG
        ;;
    d)
        DATADIR=$OPTARG
        ;;
    g)
        GENLOG=$OPTARG
    esac
done

# echo each command before executing
set -v

$PBSNCAT --input-format json --format pbsn --magic MBOC $DATADIR/gl_mbo.json > $TMP/gl_mbo.pbuf.sn

$ALLOWED_PARAM_FILTER $TMP/gl_mbo.pbuf.sn $TMP/allowed_params $TMP/gl_params.gz

#$PBSNCAT $DATADIR/cluster_pictures.pbuf.sn | grep ^cluster_id | awk '{print $2}' > $DATADIR/clusters_with_pics.csv

$STATS_CALC --yt-proxy $YT_PROXY --yt-tokenpath "$YT_TOKENPATH" --yt-home //tmp  --config $CONFIG -l $TMP/log --rates $DATADIR/currency_rates.xml \
    --geoprefix $DATADIR/ --categories $DATADIR/tovar-tree.pb --vendors $DATADIR/global.vendors.xml \
    -s $DATADIR/shops-utf8.dat \
    -e $DATADIR/model_group.csv --unpackedctr $TMP -t ${TYPE} -g $GENERATION -C $DATADIR/cpa-categories.xml \
    -f $TMP/allowed_params -w $DATADIR/clusters_with_pics.csv

$STATS_CONVERT --yt-proxy $YT_PROXY --yt-tokenpath "$YT_TOKENPATH" --yt-home //tmp/out --home=$TMP/ --type=$TYPE --format=$FORMAT --rates $DATADIR/currency_rates.xml --categories $DATADIR/tovar-tree.pb --geoprefix $DATADIR
