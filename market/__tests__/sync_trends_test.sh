echo "===================="
echo "Sync trends to MongoDB"
echo "====================\n"

python3 ./mongo-db/sync_trends.py \
    --input1 __tests__/sync_trends/input.json \
    --param1 svt_user \
    --param2 5 \
    --param3 testing \
    --sec1 *******
