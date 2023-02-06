echo "===================="
echo "Extract field from objects"
echo "====================\n"

python3 ./processing/extract_field.py \
    --input1 __tests__/processing/input-semitable.json \
    --param1 input \
    --output1 __tests__/processing/output-semitable.json

echo "\n===================="
echo "Extract field from objects"
echo "====================\n"


python3 ./processing/text_cleanup.py \
    --input1 __tests__/processing/output-semitable.json \
    --param1 lower_case,strip,remove_punctuation,remove_emoji,remove_emoticons,remove_multi_spaces,remove_urls,remove_nones,remove_numeric \
    --param2 4 \
    --param3 5 \
    --param4 50 \
    --output1 __tests__/processing/output-semitable-cleared.json
