import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)
from crypta.tx.proto.ltp_state_pb2 import TLtpState
from crypta.tx.proto.ltp_tx_pb2 import TLtpTx
from crypta.tx.services.common import (
    schema,
)


def get_state_table(config):
    return tables.get_yson_table_with_schema("state.yson", config.StateTable, schema_utils.get_schema_from_proto(TLtpState))


def test_create_state(clean_local_yt, local_yt_and_yql_env, config_file, config, mock_reactor_api, frozen_time):
    return run_test(clean_local_yt, local_yt_and_yql_env, config_file, config, mock_reactor_api)


def test_update_state(clean_local_yt, local_yt_and_yql_env, config_file, config, mock_reactor_api, frozen_time):
    return run_test(clean_local_yt, local_yt_and_yql_env, config_file, config, mock_reactor_api, [(get_state_table(config), None)])


def run_test(clean_local_yt, local_yt_and_yql_env, config_file, config, mock_reactor_api, additional_input=None):
    additional_input = additional_input or []
    schema_test = tests.SchemaEquals(schema_utils.get_schema_from_proto(TLtpTx, ["id", "id_type"]))
    results = tests.yt_test(
        yt_client=clean_local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/tx/services/ltp_logos_export/bin/crypta-tx-ltp-logos-export"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    "{}.yson".format(table),
                    yt.ypath_join(config.DataDir, table),
                    schema.get_transaction_schema(),
                ),
                [tests.TableIsNotChanged()]
            )
            for table in ["ecom", "eda", "edadeal", "market", "taxi"]
        ] + additional_input,
        output_tables=[
            (cypress.CypressNode(config.OutputDir), tests.TestNodesInMapNode([tests.Diff(), schema_test], tag="ltp_daily")),
            (get_state_table(config), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
    return {
        "yt": results,
        "reactor": mock_reactor_api.dump_requests(),
    }
