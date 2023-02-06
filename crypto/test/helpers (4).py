import os

import yatest.common
import yt.wrapper as yt

from crypta.dmp.common.data.python import (
    bindings,
    meta
)
from crypta.dmp.yandex.bin.common.python import (
    config_fields,
    errors_schema,
    statistics_schema
)
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests
)

import data


DATA_PATH = yatest.common.test_source_path("data")

INPUT_DIR = "//dmp/input"
META_STATE = "//dmp/meta"
BINDINGS_STATE = "//dmp/bindings"
STATISTICS_OUTPUT_DIR = "//dmp/statistics"
QUARANTINE_DIR = "//dmp/quarantine"


def execute_binary(yt_stuff, test_name):
    diff = tests.Diff()

    config = get_config(yt_stuff, test_name)
    output_files = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/update_segments/bin/crypta-dmp-yandex-update-segments"),
        args=["--config", yaml_config.dump(config)],
        data_path=DATA_PATH,
        input_tables=list(create_input(yt_stuff, test_name)),
        output_tables=[
            (tables.YsonTable("meta", META_STATE, yson_format="pretty"), tests.IfExists([tests.SchemaEquals(meta.get_schema()), diff])),
            (tables.YsonTable("bindings", BINDINGS_STATE, yson_format="pretty"), tests.IfExists([tests.SchemaEquals(bindings.get_ext_id_schema()), diff])),
            (cypress.CypressNode(QUARANTINE_DIR), tests.TestNodesInMapNodeChildren([tests.SchemaEquals(errors_schema.get()), diff], tag="quarantine")),
            (cypress.CypressNode(STATISTICS_OUTPUT_DIR), tests.TestNodesInMapNode([tests.SchemaEquals(statistics_schema.get()), diff], tag="statistics"))
        ],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: str(data.DATA[test_name][time_utils.CRYPTA_FROZEN_TIME_ENV])
        },
    )
    return {os.path.basename(output_file["file"]["uri"]): output_file for output_file in output_files}


def get_config(yt_stuff, test_name):
    inactive_segments_ttl = data.DATA[test_name]["inactive-segments-ttl"]
    bindings_ttl = data.DATA[test_name]["bindings-ttl"]
    should_drop_input = data.DATA[test_name]["should-drop-input"]
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.PARSED_SEGMENTS_DIR: INPUT_DIR,
        config_fields.META_TABLE: META_STATE,
        config_fields.EXT_ID_BINDINGS_TABLE: BINDINGS_STATE,
        config_fields.STATISTICS_DIR: STATISTICS_OUTPUT_DIR,
        config_fields.QUARANTINE_DIR: QUARANTINE_DIR,
        config_fields.SHOULD_DROP_INPUT: should_drop_input,
        config_fields.INACTIVE_SEGMENTS_TTL: inactive_segments_ttl,
        config_fields.BINDINGS_TTL: bindings_ttl
    }


def create_input(yt_stuff, test_name):
    yt_stuff.yt_wrapper.mkdir(INPUT_DIR, recursive=True)
    if "meta" in data.DATA[test_name]:
        yield tables.get_yson_table_with_schema(os.path.join(DATA_PATH, data.DATA[test_name]["meta"]), META_STATE, meta.get_schema()), None
    if "bindings" in data.DATA[test_name]:
        yield tables.get_yson_table_with_schema(os.path.join(DATA_PATH, data.DATA[test_name]["bindings"]), BINDINGS_STATE, bindings.get_ext_id_schema()), None
    if "fresh" in data.DATA[test_name]:
        for dirname, l in data.DATA[test_name]["fresh"].iteritems():
            yt_dirpath = yt.ypath_join(INPUT_DIR, dirname)
            yt_stuff.yt_wrapper.mkdir(yt_dirpath, recursive=True)
            for tablename, filename in l.iteritems():
                local_path = os.path.join(DATA_PATH, filename)
                yt_path = yt.ypath_join(yt_dirpath, tablename)
                on_write = tables.OnWrite()
                if tablename == "meta":
                    on_write = tables.OnWrite(attributes={"schema": meta.get_schema()})
                elif tablename == "bindings":
                    on_write = tables.OnWrite(attributes={"schema": bindings.get_ext_id_schema()})
                yield tables.YsonTable(local_path, yt_path, on_write=on_write), None
