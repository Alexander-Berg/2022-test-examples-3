import pytest
import yatest.common
import yt.wrapper as yt

from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)
from crypta.cm.offline.bin.common.python import (
    config_fields,
    match
)


FRESH_DIR = "//input"
STATE_TABLE = "//state"


def get_config(yt_stuff, drop_input):
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.TABLE_PACK_SIZE: 4,
        config_fields.SHOULD_DROP_INPUT: drop_input,
        config_fields.TTL: 120000000,
        config_fields.TAG_FRESH_DIR: FRESH_DIR,
        config_fields.TAG_STATE_TABLE: STATE_TABLE
    }


def get_input_tables(state_filename, drop_input):
    def input_test():
        return tests.IsAbsent() if drop_input else tests.TableIsNotChanged()

    input_tables = [
        (tables.get_yson_table_with_schema("{}.yson".format(ts), yt.ypath_join(FRESH_DIR, str(ts)), match.get_unsorted_schema()), input_test())
        for ts in [1300000000, 1400000000, 1500000000]
    ]

    if state_filename is not None:
        input_tables.append((tables.get_yson_table_with_schema(state_filename, STATE_TABLE, match.get_schema_with_unique_keys()), None))

    return input_tables


@pytest.mark.parametrize("state_filename,drop_input", [
    pytest.param(None, True, id="without state"),
    pytest.param("state.yson", True, id="with state"),
    pytest.param(None, False, id="don't drop input")
])
def test_update_state(yt_stuff, state_filename, drop_input):
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/offline/bin/update_state/bin/crypta-offline-cm-update-state"),
        args=[
            "--config", yaml_config.dump(get_config(yt_stuff, drop_input))
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=get_input_tables(state_filename, drop_input),
        output_tables=[
            (tables.YsonTable("output.yson", STATE_TABLE, yson_format="pretty"), [tests.Diff(), tests.SchemaEquals(match.get_schema_with_unique_keys())])
        ],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: "1510000000"
        }
    )
