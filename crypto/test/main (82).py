import logging

import yatest.common
import yt.wrapper as yt

from crypta.lib.python import time_utils
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.siberia.bin.custom_audience.to_bigb_collector import lib
from crypta.siberia.bin.custom_audience.to_bigb_collector.proto import collector_pb2


logger = logging.getLogger(__name__)


def get_input_schema():
    return schema_utils.yt_schema_from_dict({
        'id': 'uint64',
        'cluster_id': 'uint64',
    }, sort_by=["id"])


def test_basic(config, config_file, yt_stuff, date):
    collector_row_transformer = row_transformers.yson_to_proto_dict(collector_pb2.TCollector)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path(
            "crypta/siberia/bin/custom_audience/to_bigb_collector/bin/crypta-siberia-custom-audience-to-bigb-collector"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema(
                'yandexuid_clusters.yson',
                yt.ypath_join(config.SourceDir, date),
                schema=get_input_schema(),
            ), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                "collector.yson",
                yt.ypath_join(config.CollectorDir, f"{time_utils.get_current_moscow_datetime().isoformat()}-{config.TableSuffix}"),
                on_read=tables.OnRead(row_transformer=collector_row_transformer),
                yson_format="pretty"
            ), [tests.Diff(), tests.SchemaEquals(lib.get_collector_schema())]),
        ],
    )
