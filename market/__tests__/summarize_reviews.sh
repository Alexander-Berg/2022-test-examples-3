echo "===================="
echo "Summarize reviews"
echo "====================\n"

python3 ./projects/summarize_reviews.py \
    --input1 __tests__/nlp/input-nlp.json \
    --input2 __tests__/nlp/input-nlp-model.bin \
    --param1 5 \
    --param2 0.2 \
    --output1 __tests__/nlp/output-nlp.json

