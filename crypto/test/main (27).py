import pytest
import yatest.common

from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


INDEX_TABLE = "//dmp/index"


@pytest.fixture
def config(yt_stuff):
    return {
        "yt-proxy": yt_stuff.get_server(),
        "index-table": INDEX_TABLE,
    }


@pytest.mark.parametrize("env", ["testing", "stable"])
def test_publish_index(yt_stuff, config, env):
    config["environment"] = env
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/publish_index/bin/crypta-dmp-yandex-publish-index"),
        args=[
            "--config", yaml_config.dump(config),
            "--index", yatest.common.test_source_path("data/index.yaml"),
        ],
        output_tables=[
            (tables.YsonTable("output.yson", INDEX_TABLE, yson_format="pretty"), tests.Diff())
        ],
        env={
            "YT_TOKEN": "yttoken"
        }
    )
