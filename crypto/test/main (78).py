import yatest.common
import logging

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.siberia.bin.custom_audience.bs_host_cluster_mapping import lib


logger = logging.getLogger(__name__)


def get_hosts_schema():
    return schema_utils.yt_schema_from_dict({
        "host_id": "uint64",
        "cluster_id": "uint64",
    })


def run_test(clean_local_yt, config, config_file, local_yt_and_yql_env, additional_input=None):
    additional_input = additional_input or []
    return tests.yt_test(
        yt_client=clean_local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/custom_audience/bs_host_cluster_mapping/bin/crypta-siberia-custom-audience-bs-host-cluster-mapping"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema(
                'hosts.yson',
                config.Hosts,
                schema=get_hosts_schema(),
            ), (tests.TableIsNotChanged())),

        ] + additional_input,
        output_tables=[
            (tables.YsonTable('state.yson', config.State, yson_format="pretty"), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )


def test_without_state(clean_local_yt, config, config_file, local_yt_and_yql_env, frozen_time):
    return run_test(clean_local_yt, config, config_file, local_yt_and_yql_env)


def test_with_state(clean_local_yt, config, config_file, local_yt_and_yql_env, frozen_time):
    return run_test(clean_local_yt, config, config_file, local_yt_and_yql_env, additional_input=[
        (tables.get_yson_table_with_schema(
            'state.yson',
            config.State,
            schema=lib.get_state_schema(),
        ), None),
    ])
