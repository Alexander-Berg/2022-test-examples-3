import json
import os

import yatest.common

REARRS_UPPER_PREFIX = os.path.join("search", "web", "rearrs_upper")
RD_PREFIX = os.path.join(REARRS_UPPER_PREFIX, 'rearrange.dynamic')

BINARY_PATH = yatest.common.binary_path(os.path.join(REARRS_UPPER_PREFIX, "tests", "test_app", "test_app"))
TEST_DATA_PREFIX = yatest.common.build_path(os.path.join(REARRS_UPPER_PREFIX, "tests", "data"))

REARRS_UPPER_BUILD_PREFIX = os.path.join(TEST_DATA_PREFIX, REARRS_UPPER_PREFIX)
REARRANGE_BUILD_PREFIX = os.path.join(REARRS_UPPER_BUILD_PREFIX, "rearrange")
RD_BUILD_PREFIX = os.path.join(REARRS_UPPER_BUILD_PREFIX, "rearrange.dynamic")
REARRANGE_FAST_BUILD_PREFIX = os.path.join(REARRS_UPPER_BUILD_PREFIX, "rearrange.fast")

DATA_PATH = os.path.join(REARRS_UPPER_BUILD_PREFIX, "tests", "data")
FMLS_GRAPHS_PATH = os.path.join(DATA_PATH, "fmls_graphs")


def file_nonempty(path):
    file_path = yatest.common.build_path(os.path.join(TEST_DATA_PREFIX, *path))
    assert os.path.getsize(file_path) > 0


def json_data_valid(data):
    try:
        json.loads(data, encoding="utf-8")
    except:
        assert False


def json_valid(path):
    file_path = yatest.common.build_path(os.path.join(TEST_DATA_PREFIX, *path))
    with open(file_path, "r") as f:
        json_data_valid(f.read())


def scheme_valid(path):
    file_path = yatest.common.build_path(os.path.join(TEST_DATA_PREFIX, *path))
    result = yatest.common.execute(command=[BINARY_PATH, "json", "--path", file_path])
    assert result.exit_code == 0


def scheme_data_valid(data):
    result = yatest.common.execute(command=[BINARY_PATH, "json_from_string", "--data", data])
    assert result.exit_code == 0


def scheme_path_valid(path):
    result = yatest.common.execute(command=[BINARY_PATH, "scheme_path", "--path", path])
    assert result.exit_code == 0


def proto_text_format_valid(path, message_type):
    file_path = yatest.common.build_path(os.path.join(TEST_DATA_PREFIX, *path))
    result = yatest.common.execute(command=[BINARY_PATH, "proto_text_format", "--path", file_path, "--message-type", message_type])
    assert result.exit_code == 0


def check_some_type_items(items, selected_type):
    for item in items:
        try:
            data = selected_type(item)
        except:
            assert False


def get_all_files_recursive(path, depth=5, add_dirs=False):
    res = []
    for item in os.listdir(path):
        full_path = os.path.join(path, item)
        if os.path.isdir(full_path):
            assert depth > 0
            if add_dirs:
                res += [full_path]
            res += get_all_files_recursive(full_path, depth - 1)
        else:
            res += [full_path]
    return res


def get_all_directories_with_formulas(path):
    file_path = yatest.common.build_path(os.path.join(path, "formulas"))
    result = []
    with open(file_path, "r") as f:
        for line in f.readlines():
            line = line.strip()
            if line.startswith("#"):
                continue
            result.append(yatest.common.build_path(os.path.join(RD_BUILD_PREFIX, line)))
    assert len(result) > 0
    return result
