#!/bin/bash

set -o noglob
echo -n "" > urls.txt
for i in $(seq 1 10000); do
	# big, too diverse
	r=${RANDOM}${RANDOM}${RANDOM}
	s50=$(shuf -i 1-50 -n 1)
	s3=$(shuf -i 1-3 -n 1)
	q[0]='/* 0_comment_'${r}' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '$(shuf -i 1000-6000 -n 1)';'
	# big, cacheable
	q[1]='/* 1_comment_'$(shuf -i 1-2 -n 1)' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '$(shuf -i 2330-2350 -n 1)';'
	# small, too diverse
	q[2]='/* 2_comment_'${r}' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '$(shuf -i 1-50 -n 1)';'
	# small, cacheable, most of requests should be like these
	q[3]='/* 3_comment_'${s3}' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '${s50}';'
	q[4]='/* 3_comment_'${s3}' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '${s50}';'
	q[5]='/* 3_comment_'${s3}' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '${s50}';'
	q[6]='/* 3_comment_'${s3}' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '${s50}';'
	q[7]='/* 3_comment_'${s3}' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '${s50}';'
	q[8]='/* 3_comment_'${s3}' */ select * from cubes_clickhouse__cube_cpc_clicks_b2b_analyst limit '${s50}';'
	rand=$[ $RANDOM % 9 ]
	echo ${q[$rand]} >> urls.txt
done
