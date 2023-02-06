import pytest

import yatest.common
import yt.wrapper as yt

from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


@pytest.fixture(scope="function")
def config(yt_stuff):
    return {
        "yt-proxy": yt_stuff.get_server(),
        "yt-pool": "crypta_adobe",

        "typed-yandexuid-bindings-dir": "//adobe/typed_yandexuid_bindings",
        "all-types-yandexuid-bindings-table": "//adobe/all_types_yandexuid_bindings"
    }


def get_input_table_test(config, name):
    on_write = tables.OnWrite(sort_by="yandexuid")
    return tables.YsonTable(name, yt.ypath_join(config["typed-yandexuid-bindings-dir"], name), on_write=on_write), tests.Exists()


def test_merge_yandexuid_bindings_multiple_tables(yt_stuff, config):
    config_path = yaml_config.dump(config)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/adobe/bin/merge_yandexuid_bindings/bin/crypta-adobe-merge-yandexuid-bindings"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            get_input_table_test(config, "yandexuid_bindings__type_1"),
            get_input_table_test(config, "yandexuid_bindings__type_2")
        ],
        output_tables=[
            (tables.YsonTable("dst_table", config["all-types-yandexuid-bindings-table"], yson_format="pretty"), tests.Diff())
        ]
    )


def test_merge_yandexuid_bindings_one_table(yt_stuff, config):
    config_path = yaml_config.dump(config)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/adobe/bin/merge_yandexuid_bindings/bin/crypta-adobe-merge-yandexuid-bindings"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            get_input_table_test(config, "yandexuid_bindings__type_1")
        ],
        output_tables=[
            (tables.YsonTable("dst_table", config["all-types-yandexuid-bindings-table"], yson_format="pretty"), tests.Diff())
        ]
    )
