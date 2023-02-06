echo "===================="
echo "Built network-x graph"
echo "====================\n"

python3 ./network-x/build_graph.py \
    --input1 __tests__/graph/input/input.json \
    --param1 True \
    --param2 10 \
    --output1 __tests__/graph/output/result.gpickle

echo "\n===================="
echo "Unwrap network-x graph"
echo "====================\n"

python3 ./network-x/unwrap_graph.py \
    --input1 __tests__/graph/output/result.gpickle \
    --param1 True \
    --param2 10 \
    --param3 30 \
    --param4 True \
    --output1 __tests__/graph/output/unwrapped_result.json
