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
echo "Vectorize test set"
echo "====================\n"

python tfid_vectorizer.py \
    --input1 __tests__/tv_test/input/test.json \
    --input2 __tests__/tv_test/output/vectorizer \
    --param2 7 \
    --param3 title \
    --param4 title \
    --output2 __tests__/tv_test/output/y-column-test.json \
    --output3 __tests__/tv_test/output/vector-test.json

echo "\n===================="
echo "Run cosine similarity"
echo "====================\n"

python ./cosine_similarity/main.py \
    --input1 __tests__/tv_test/output/vector-train.json \
    --input2 __tests__/tv_test/output/vector-test.json \
    --param2 0 \
    --param3 1 \
    --param4 False \
    --output1 __tests__/tv_test/output/cosine_similarity.json

echo "\n===================="
echo "Run cosine unpack"
echo "====================\n"

python ./cosine_similarity/unpack.py \
    --input1 __tests__/tv_test/output/cosine_similarity.json \
    --input2 __tests__/tv_test/output/y-column-train.json \
    --input3 __tests__/tv_test/output/y-column-test.json \
    --param2 model_id \
    --param3 title \
    --output1 __tests__/tv_test/cosine_preduct.json


