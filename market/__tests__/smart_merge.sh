echo "===================="
echo "Smart merge two tables"
echo "====================\n"

python3 ./processing/smart_merge.py \
    --input1 __tests__/processing/left.json \
    --input2 __tests__/processing/right.json \
    --param1 model_id \
    --param2 pros,cons \
    --param3 name,picture \
    --output1 __tests__/processing/smart_merge-output.json
