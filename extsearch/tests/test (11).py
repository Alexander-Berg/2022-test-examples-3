import os
import pytest
import shutil
import yatest.common

env = os.environ.copy()
SHARDNAME = "shard_prefix-0000-20180603-024010"
REQUIRED_SHARD = "shard_prefix-0000-20180602-024010"
env["BSCONFIG_SHARDNAME"] = SHARDNAME
env["BSCONFIG_REQUIRES_SHARDS"] = REQUIRED_SHARD
env["MR_SERVER"] = "banach.yt.yandex.net"
env["BASE_PREFIX"] = "images"

shard_download_prefix = "extsearch/images/robot/index/index_download"
test_shardwriter = "extsearch/images/robot/index/index_download/ut/mock_shardwriter/mock_shardwriter"

tests = [
    {
        "binary_directory": "main_commercial_data",
        "binary_name": "main_commercial_data_download",
    },
    {
        "binary_directory": "main_imtub",
        "binary_name": "main_imtub_download",
    },
    {
        "binary_directory": "main_index",
        "binary_name": "main_index_download",
    },
    {
        "binary_directory": "main_mmeta",
        "binary_name": "main_mmeta_download",
    },
    {
        "binary_directory": "main_rim",
        "binary_name": "main_rim_download",
    },
]


@pytest.mark.parametrize("args", tests, ids=[test["binary_name"] for test in tests])
def test_shard_download(args):
    binary_directory = args["binary_directory"]
    binary_name = args["binary_name"]
    cwd = os.path.join(yatest.common.work_path(), binary_directory)

    os.makedirs(os.path.join(cwd, SHARDNAME))

    shutil.copy2(
        yatest.common.binary_path(os.path.join(shard_download_prefix,
                                               binary_directory,
                                               binary_name)),
        cwd)

    shutil.copy2(
        yatest.common.binary_path(test_shardwriter),
        os.path.join(cwd, "shardwriter"))

    yatest.common.execute(os.path.join(cwd, binary_name), env=env, cwd=cwd)
