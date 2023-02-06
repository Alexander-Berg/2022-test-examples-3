echo "===================="
echo "Vectorize train set"
echo "====================\n"

python tfid_vectorizer.py \
    --input1 __tests__/tv_test/input/train.json \
    --param2 7 \
    --param3 title \
    --param4 model_id \
    --output1 __tests__/tv_test/output/vectorizer \
    --output2 __tests__/tv_test/output/y-column-train.json \
    --output3 __tests__/tv_test/output/vector-train.json


echo "\n===================="
echo "Run cosine similarity"
echo "====================\n"

python ./cosine_similarity/smaller.py \
    --input1 __tests__/tv_test/output/vector-train.json \
    --input3 __tests__/tv_test/output/vectorizer \
    --input4 __tests__/tv_test/input/test_flatten.json \
    --param1 1 \
    --param2 0.1 \
    --param3 2 \
    --param4 model_id \
    --output1 __tests__/tv_test/output/small_cosine_similarity.csv


echo "\n===================="
echo "Unpack using Y"
echo "====================\n"

python ./processing/populate_y.py \
    --input1 __tests__/tv_test/output/small_cosine_similarity.csv \
    --input2 __tests__/tv_test/output/y-column-train.json \
    --param2 model_id \
    --param3 y_id \
    --output1 __tests__/tv_test/output/final_similarities.json

