import os

from yatest import common


binary_file = common.binary_path("extsearch/images/kernel/new_runtime/doc_factors/tests/fill_factors_test/fill_factors_test")
input_data_file = os.path.join(common.runtime.work_path("input_data"), "metadoc100.txt")


def test_fill_features():
    return common.canonical_execute(binary_file,
                                    ["TestFillFeatures", "--input-file", input_data_file],
                                    check_exit_code=True)
