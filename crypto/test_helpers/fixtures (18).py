import pytest
import yatest

from crypta.lib.python import yaml2proto
from crypta.siberia.bin.core.lib.test_helpers.clients_config import ClientsConfig
from crypta.siberia.bin.core.lib.test_helpers.local_siberia import LocalSiberia
from crypta.siberia.bin.core.lib.test_helpers.tvm_ids import TvmIds


@pytest.fixture(scope="session")
def tvm_ids(tvm_api):
    return TvmIds(tvm_api)


@pytest.fixture(scope="session")
def clients_config(tvm_ids):
    return ClientsConfig(tvm_ids)


@pytest.fixture(scope="session")
def clients_config_path(clients_config):
    path = yatest.common.test_output_path("clients.yaml")

    with open(path, "w") as f:
        f.write(yaml2proto.proto2yaml(clients_config.proto))

    return path


@pytest.fixture(scope="module")
def local_siberia(
        request,
        local_ydb,
        access_log_logbroker_config,
        change_log_logbroker_config,
        describe_log_logbroker_config,
        describe_slow_log_logbroker_config,
        segmentate_log_logbroker_config,
        tvm_ids,
        clients_config_path,
        session_mock_juggler_server,
):
    with yatest.common.network.PortManager() as pm:
        kwargs = dict(
            working_dir=yatest.common.test_output_path("core"),
            port=pm.get_port(),
            ydb_endpoint=local_ydb.endpoint,
            ydb_database=local_ydb.database,
            access_log_logbroker_config=access_log_logbroker_config,
            change_log_logbroker_config=change_log_logbroker_config,
            describe_log_logbroker_config=describe_log_logbroker_config,
            describe_slow_log_logbroker_config=describe_slow_log_logbroker_config,
            segmentate_log_logbroker_config=segmentate_log_logbroker_config,
            frozen_time=getattr(request.module, "FROZEN_TIME", None),
            self_tvm_id=tvm_ids.api,
            clients_config_path=clients_config_path,
            juggler_url_prefix=session_mock_juggler_server.url_prefix,
        )
        with LocalSiberia(**kwargs) as siberia:
            yield siberia


@pytest.fixture(scope="module")
def siberia_client(local_siberia):
    yield local_siberia.get_client()
