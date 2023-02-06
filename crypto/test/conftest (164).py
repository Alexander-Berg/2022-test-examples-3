import pytest
import yaml
import yatest.common

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.siberia.bin.common import test_helpers
import crypta.siberia.bin.describer.lib.test_helpers as describer_test_helpers


pytest_plugins = [
    "crypta.lib.python.ydb.test_helpers.fixtures",
    "crypta.siberia.bin.describer.lib.test_helpers.fixtures",
]


def convert_to_bytes(data):
    return [dict([k, v.encode("utf-8") if isinstance(v, str) else v] for k, v in item.items()) for item in data]


@pytest.fixture(scope="function", autouse=True)
def setup(request, local_ydb, describe_log_logbroker_client):
    crypta_id_user_data_version_marker = request.node.get_closest_marker("crypta_id_user_data_version")
    crypta_id_user_data_version = crypta_id_user_data_version_marker.args[0] if crypta_id_user_data_version_marker is not None else None

    id_to_crypta_id_table_name = "1500000000"
    crypta_id_user_data_table_name = "1500000000"

    local_ydb.remove_all()

    test_helpers.create_user_set_stats_table(local_ydb)

    test_helpers.create_id_to_crypta_id_table(local_ydb, ".1600000000")
    test_helpers.create_id_to_crypta_id_table(local_ydb, id_to_crypta_id_table_name)

    test_helpers.create_crypta_id_user_data_table(local_ydb, ".1600000000", version=crypta_id_user_data_version)
    test_helpers.create_crypta_id_user_data_table(local_ydb, crypta_id_user_data_table_name, version=crypta_id_user_data_version)

    with open(yatest.common.test_source_path("data/id_2_crypta_id.json")) as f:
        test_helpers.upload_id_to_crypta_id_table(local_ydb, id_to_crypta_id_table_name, convert_to_bytes(yaml.safe_load(f)))

    test_helpers.upload_crypta_id_user_data_table(local_ydb, crypta_id_user_data_table_name, describer_test_helpers.get_crypta_id_user_data("data/user_data.json"), version=crypta_id_user_data_version)

    consumer_utils.read_all(describe_log_logbroker_client.create_consumer())
