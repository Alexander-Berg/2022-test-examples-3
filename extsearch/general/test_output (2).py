import pytest
import yatest.common

_TOP_DIR = ("extsearch", "images", "tools", "url2fastban")
_TOOL_PATH = _TOP_DIR + ("url2fastban",)
_TOOL_MODES = ("thumb-lines", "index-lines", "hashes")


def _data_path(file_name):
    return _TOP_DIR + ("ut", "data", file_name)


@pytest.mark.parametrize("mode", _TOOL_MODES)
def test_mode(mode):
    hasher = yatest.common.binary_path("/".join(_TOOL_PATH))
    input_file_path = yatest.common.source_path("/".join(_data_path("{}.txt".format(mode))))
    res = yatest.common.execute([hasher, "--no-timestamp", "--mode", mode, "--input", input_file_path])
    assert res.exit_code == 0
    output_file_path = "{}.out".format(mode)
    with open(output_file_path, "w") as out_file:
        out_file.write(res.std_out)
    return yatest.common.canonical_file(output_file_path)
