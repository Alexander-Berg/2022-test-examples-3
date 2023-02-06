echo "===================="
echo "Lazy merge two tables"
echo "====================\n"

python3 ./processing/lazy_merge.py \
    --input1 __tests__/lazy_merge/input/source.json \
    --input2 __tests__/lazy_merge/input/diff.json \
    --param1 input,model_id \
    --output1 __tests__/lazy_merge/output/result.json
