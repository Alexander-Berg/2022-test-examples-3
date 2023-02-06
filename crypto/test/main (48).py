import pytest
import yatest.common
import yt.wrapper as yt

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.lib.python.utils import yt_schemas
from crypta.lookalike.services.calc_metrika_counter_audiences.lib.proto.calc_metrika_counter_audiences_job_config_pb2 import TCalcMetrikaCounterAudiencesJobConfig


@pytest.fixture(scope="function")
def config_file(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")
    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/calc_metrika_counter_audiences/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "max_audience_size": 2,
        },
    )
    return config_file_path


def test_basic(yt_stuff, config_file):
    config = yaml_config.parse_config(TCalcMetrikaCounterAudiencesJobConfig, config_file)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/calc_metrika_counter_audiences/bin/crypta-lookalike-calc-metrika-counter-audiences"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.YsonTable(
                    "counter_visits.yson",
                    yt.ypath_join(config.SrcDir, "table_1"),
                    on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_metrika_counter_audiences_schema()})
                ), tests.IsAbsent()
            ),
            (tables.YsonTable("metrika_counter_audiences.yson", config.DstTable, on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_metrika_counter_audiences_schema()})), None),
        ],
        output_tables=[
            (tables.YsonTable("metrika_counter_audiences.yson", config.DstTable, yson_format="pretty"),
                              [tests.Diff(), tests.SchemaEquals(yt_schemas.get_metrika_counter_audiences_schema())]),
        ],
        env={"LOCAL_YT_SERVER": yt_stuff.get_server()},
    )
