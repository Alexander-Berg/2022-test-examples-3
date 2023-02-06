echo "===================="
echo "Picking fields from objects"
echo "====================\n"

python3 ./processing/pick_fields.py \
    --input1 __tests__/processing/input-bigtable.json \
    --param1 "model_id, text_reviews" \
    --output1 __tests__/processing/output-bigtable.json

