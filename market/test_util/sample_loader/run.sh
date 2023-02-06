cd ../../../../../../../.. || exit 1

./ya make market/replenishment/algorithms/vicugna/test_util/sample_loader || exit 2
market/replenishment/algorithms/vicugna/test_util/sample_loader/sample_loader $@
