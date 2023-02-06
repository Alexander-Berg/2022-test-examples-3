import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.lookalike.lib.python import test_utils
from crypta.lookalike.proto.direct_entry_pb2 import TDirectEntry
from crypta.lookalike.proto.input_lal_entry_pb2 import TInputLalEntry
from crypta.lookalike.services.lal_synchronizer.proto.config_pb2 import TConfig


FROZEN_TIME = "1584085850"


def save_config_file(yt_stuff, logbroker_port, scope):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/lal_synchronizer/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "lb_url": "localhost",
            "lb_topic": "default-topic",
            "lb_port": logbroker_port,
            "scope": scope,
            "input_table": "//home/crypta/qa/{}".format(scope)
        },
    )

    return config_file_path


@pytest.mark.parametrize("scope,input_table_filename,input_table_schema,has_errors", [
    pytest.param("direct", "direct.yson", schema_utils.get_schema_from_proto(TDirectEntry), True, id="direct"),
    pytest.param("autobudget", "autobudget.yson", schema_utils.get_schema_from_proto(TInputLalEntry, ["lal_id"]), False, id="autobudget"),
])
def test_basic(yt_stuff, logbroker_port, consumer, scope, input_table_filename, input_table_schema, has_errors):
    config_file = save_config_file(yt_stuff, logbroker_port, scope)
    config = yaml_config.parse_config(TConfig, config_file)
    errors_on_read = tables.OnRead(row_transformer=row_transformers.remove_frame_info(field="error"))

    yt_results = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/lal_synchronizer/bin/crypta-lookalike-lal-synchronizer"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema(input_table_filename, config.InputTable, input_table_schema), tests.TableIsNotChanged()),
            (tables.YsonTableWithExpressions("state.yson", config.StateTable, on_write=test_utils.lals_on_write()), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable("errors.yson", os.path.join(config.ErrorsDir, FROZEN_TIME), yson_format="pretty", on_read=errors_on_read), tests.Diff() if has_errors else tests.IsAbsent()),
        ],
        env={"YT_TOKEN": "FAKE", time_utils.CRYPTA_FROZEN_TIME_ENV: FROZEN_TIME},
    )
    return {
        "yt_results": yt_results,
        "data_written": sorted(consumer_utils.read_all(consumer, timeout=30)),
    }
