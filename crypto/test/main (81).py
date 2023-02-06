import logging

import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

logger = logging.getLogger(__name__)


def get_input_schema():
    return schema_utils.yt_schema_from_dict({
        'crypta_id_count': 'uint64',
        'host_id': 'uint64',
        'host_name': 'string',
        'cluster_id': 'uint64',
    })


def test_basic(local_yt, config, config_file, local_yt_and_yql_env):
    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path(
            "crypta/siberia/bin/custom_audience/hosts_filter/bin/crypta-siberia-custom-audience-hosts-filter"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema(
                'site_clusters.yson',
                config.SiteClusters,
                schema=get_input_schema()
            ), (tests.TableIsNotChanged())),
        ],
        output_tables=[
            (tables.YsonTable("hosts.yson", config.FilteredHostsTable, yson_format="pretty"), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
