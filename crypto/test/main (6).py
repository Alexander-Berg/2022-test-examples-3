import os

import pytest
import yatest.common
import yt.wrapper as yt

from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests
)
from crypta.cm.offline.bin.common.python import (
    config_fields,
    match
)


INPUT_DIR = "//input"
OUTPUT_DIR = "//output"
TAGS = [
    {config_fields.TAG: i, config_fields.TAG_FRESH_DIR: yt.ypath_join(OUTPUT_DIR, i)}
    for i in ["xxx", "yyy", "www", "zzz"]
]
TAGS_WITH_DUPLICATE_DIRS = [
    {config_fields.TAG: "xxx", config_fields.TAG_FRESH_DIR: "//output/xxx"},
    {config_fields.TAG: "zzz", config_fields.TAG_FRESH_DIR: "//output/xxx"}
]
TAGS_WITH_DUPLICATE_TAGS = [
    {config_fields.TAG: "xxx", config_fields.TAG_FRESH_DIR: "//output/xxx"},
    {config_fields.TAG: "xxx", config_fields.TAG_FRESH_DIR: "//output/zzz"}
]


def get_config(yt_stuff, drop_input, tags):
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.FRESH_CM_DIR: INPUT_DIR,
        config_fields.TABLE_PACK_SIZE: 4,
        config_fields.SHOULD_DROP_INPUT: drop_input,
        config_fields.TAGS: tags
    }


@pytest.mark.parametrize("tags,drop_input,must_be_execution_error", [
    pytest.param(TAGS, True, False, id="valid config"),
    pytest.param(TAGS, False, False, id="don't drop input"),
    pytest.param(TAGS_WITH_DUPLICATE_DIRS, True, True, id="config with duplicate dirs"),
    pytest.param(TAGS_WITH_DUPLICATE_TAGS, True, True, id="config with duplicate tags")
])
def test_split_by_tag(yt_stuff, tags, drop_input, must_be_execution_error):
    data_path = yatest.common.test_source_path("data")

    schema_test = tests.SchemaEquals(match.get_unsorted_schema())
    diff = tests.Diff()

    def input_test():
        return tests.IsAbsent() if (drop_input and not must_be_execution_error) else tests.TableIsNotChanged()

    output_files = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/offline/bin/split_by_tag/bin/crypta-offline-cm-split-by-tag"),
        args=[
            "--config", yaml_config.dump(get_config(yt_stuff, drop_input, tags))
        ],
        data_path=data_path,
        input_tables=[
            (tables.get_yson_table_with_schema(filename, yt.ypath_join(INPUT_DIR, tablename), match.get_unsorted_schema()), input_test())
            for filename, tablename in [("1400000000.yson", "1400000000"), ("1500000000.yson", "1500000000")]
        ],
        output_tables=[
            (cypress.CypressNode(OUTPUT_DIR), tests.TestNodesInMapNodeChildren([schema_test, diff], tag="output"))
        ],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: "1600000000"
        },
        must_be_execution_error=must_be_execution_error
    )

    result = {os.path.basename(output_file["file"]["uri"]): output_file for output_file in output_files}

    if yt_stuff.yt_wrapper.exists(OUTPUT_DIR):
        result["output"] = yt_stuff.yt_wrapper.list(OUTPUT_DIR, absolute=False, sort=True)

    result["input"] = yt_stuff.yt_wrapper.list(INPUT_DIR, absolute=False, sort=True)

    return result
