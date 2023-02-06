source config.sh

ASSESSORS="$WORKDIR/assessors.txt"

TEST_Q="$WORKDIR/TestQueries"

cp marked_requests "${TEST_Q}_tmp"

./filter_pool.py "${TEST_Q}_tmp" $ASSESSORS > $TEST_Q

rm "${TEST_Q}_tmp"
