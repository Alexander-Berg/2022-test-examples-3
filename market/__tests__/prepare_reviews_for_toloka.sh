echo "===================="
echo "Prepare reviews for Toloka"
echo "====================\n"

python3 ./projects/prepare_reviews_for_toloka.py \
    --input1 __tests__/input/input-toloka-reviews.json \
    --param1 input \
    --output1 __tests__/output/output-toloka-reviews.json
