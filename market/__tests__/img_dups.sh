echo "===================="
echo "Find duplicates of images"
echo "====================\n"

python3 ./processing/image_duplicates.py \
    --input1 __tests__/img_dups/input/input.json \
    --param1 10 \
    --param3 2 \
    --output1 __tests__/img_dups/output/result.csv


echo "\n===================="
echo "Convert to json"
echo "====================\n"
python3 ./processing/csv_to_json.py \
    --input1 __tests__/img_dups/output/result.csv \
    --param1 '{"d_url":"duplicate_url", "d_page":"duplicate_page"}' \
    --param2 ";" \
    --param3 "|" \
    --param4 "QUOTE_MINIMAL" \
    --output1 __tests__/img_dups/output/output.json
