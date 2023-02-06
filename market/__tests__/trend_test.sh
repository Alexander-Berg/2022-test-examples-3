echo "===================="
echo "Calculate trend"
echo "====================\n"

python3 ./processing/calculate_trend.py \
    --input1 __tests__/trend/input.json \
    --param1 median_prices \
    --output1 __tests__/trend/output.json


echo "\n===================="
echo "[Parallel] Calculate trend"
echo "====================\n"

python3 ./processing/calculate_trend.py \
    --input1 __tests__/trend/input.json \
    --param1 median_prices \
    --param2 10 \
    --output1 __tests__/trend/output_parallel.json
