#!/bin/bash

set -euxo pipefail

shard_dir=$1
yt_cluster=$2
fingerprints_table=$3
fingerprint_bytes=$4

# find an unused port
while :
do
        port="`shuf -i 14000-15000 -n 1`"
        ss -lpn | grep -q ":$port " || break
done
echo $port

corruption_factor=0.1
top_size=5
logs_dir=/Berkanavt/yamrec/index/logs
search_dir=/Berkanavt/yamrec/index/search
scripts_dir=/Berkanavt/yamrec/index/scripts
shard_name=$(basename ${shard_dir})
answers_file=${logs_dir}/answers-${shard_name}.log
bad_answers_file=${logs_dir}/bad_answers-${shard_name}.log

function finish {
    # stop search on script termination for any reason
    SEARCH_HOME=$search_dir CONFIG_DIR=$search_dir PORT=$port HTTPSEARCH=yamrecsearch SHARD_DIR=$shard_dir ${scripts_dir}/reindex/search.sh stop
}
trap finish EXIT

cd $shard_dir
SEARCH_HOME=$search_dir CONFIG_DIR=$search_dir PORT=$port HTTPSEARCH=yamrecsearch SHARD_DIR=$shard_dir ${scripts_dir}/reindex/search.sh start
${scripts_dir}/reindex/make-query.py $yt_cluster $fingerprints_table $corruption_factor $fingerprint_bytes | ${scripts_dir}/query_basesearch.py --port $port --limit $top_size > ${answers_file}
cat ${answers_file} | ${scripts_dir}/reindex/parse_answers.py ${bad_answers_file}
nqueries=$(wc -l ${answers_file} | cut -d ' ' -f 1)
unmatched=$(wc -l ${bad_answers_file} | cut -d ' ' -f 1)
matches=$(expr ${nqueries} - ${unmatched})
required_matches=$(echo "${nqueries} * 9 / 10" | bc)
if [ $matches -gt $required_matches ]; then
    exit 0
else
    exit 1
fi
