echo "===================="
echo "Vectorize train set"
echo "====================\n"

python tfid_vectorizer.py \
    --input1 __tests__/tv_test/input/train.json \
    --param2 900 \
    --param3 title \
    --param4 model_id \
    --output1 __tests__/tv_test/output/vectorizer \
    --output2 __tests__/tv_test/output/y-column-train.json \
    --output3 __tests__/tv_test/output/vector-train.json

echo "\n===================="
echo "Vectorize test set"
echo "====================\n"

python tfid_vectorizer.py \
    --input1 __tests__/tv_test/input/test.json \
    --input2 __tests__/tv_test/output/vectorizer \
    --param2 900 \
    --param3 title \
    --param4 title \
    --output2 __tests__/tv_test/output/y-column-test.json \
    --output3 __tests__/tv_test/output/vector-test.json

echo "\n===================="
echo "Run logic regression"
echo "====================\n"

python logic_regression.py \
    --input1 __tests__/tv_test/output/vector-train.json \
    --input2 __tests__/tv_test/output/y-column-train.json \
    --param1 42 \
    --param2 saga \
    --param3 model_id \
    --param4 -1 \
    --output1 __tests__/tv_test/output/model.pkl

echo "\n===================="
echo "Try to predict"
echo "====================\n"

python logic_regression_predict.py \
    --input1  __tests__/tv_test/output/vector-test.json \
    --input2 __tests__/tv_test/input/test.json \
    --input3 __tests__/tv_test/output/model.pkl \
    --output1 __tests__/tv_test/prediction.json
