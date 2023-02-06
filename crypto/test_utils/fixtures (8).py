import os

import pytest
import yatest.common

from crypta.styx.services.common.test_utils.clients_config import ClientsConfig
from crypta.styx.services.common.test_utils.crypta_styx_api import CryptaStyxApi
from crypta.styx.services.common.test_utils.crypta_styx_api_client import CryptaStyxApiClient
from crypta.styx.services.common.test_utils.crypta_styx_mutator import CryptaStyxMutator
from crypta.styx.services.common.test_utils.tvm_ids import TvmIds
from crypta.lib.python import yaml2proto
from crypta.lib.python.logbroker.test_helpers.logbroker_config import LogbrokerConfig

pytest_plugins = [
    "crypta.lib.python.juggler.test_utils.fixtures",
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.yt.local_tests.fixtures",
]


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


@pytest.fixture(scope="session")
def access_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "access-log")


@pytest.fixture(scope="session")
def change_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "change-log")


@pytest.fixture(scope="module")
def mutator(request, yt_kv, change_log_logbroker_config, min_delete_interval_sec):
    app_working_dir = os.path.join(yatest.common.test_output_path(), "mutator")

    mutator = CryptaStyxMutator(
        working_dir=app_working_dir,
        master=yt_kv.master,
        change_log_lb_config=change_log_logbroker_config,
        min_delete_interval_sec=min_delete_interval_sec,
        frozen_time=getattr(request.module, "FROZEN_TIME", None),
        template_args={},
    )

    yield mutator


@pytest.fixture(scope="module")
def styx_client(yt_kv, min_delete_interval_sec, tvm_ids, access_log_logbroker_config, mutator, clients_config_path, session_mock_juggler_server):
    with yatest.common.network.PortManager() as port_manager:
        working_dir = yatest.common.test_output_path()

        if not os.path.isdir(working_dir):
            os.mkdir(working_dir)

        template_args = {}
        http_port = port_manager.get_port()

        with CryptaStyxApi(
                yt_kv=yt_kv,
                working_dir=working_dir,
                port=http_port,
                access_log_lb_config=access_log_logbroker_config,
                change_log_lb_config=mutator.change_log_lb_config,
                self_tvm_id=tvm_ids.api,
                min_delete_interval_sec=min_delete_interval_sec,
                clients_config_path=clients_config_path,
                juggler_url_prefix=session_mock_juggler_server.url_prefix,
                **template_args
        ):
            yield CryptaStyxApiClient("localhost", http_port)
