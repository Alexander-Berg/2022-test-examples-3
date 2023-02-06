# $1 - percent of pool that goes to test 

source config.sh

ASSESSORS="$WORKDIR/assessors.txt"

TEST_Q="$WORKDIR/TestQueries"

MARKS="marked_requests"

# Convert auto markup to acceptance marks
cat $ASSESSORS  | awk -F'\t' '{tmp="Левая"; if ($4 == "1") { tmp="Правая" } print $1"\t" $2 "\t" $3 "\t" tmp}' > $MARKS

./choose_subset.py $ASSESSORS $1 > $TEST_Q

./filter_pool.py "$ASSESSORS" $TEST_Q --save_string > "${ASSESSORS}_tmp"

mv "${ASSESSORS}_tmp" $ASSESSORS
