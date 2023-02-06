import os

import pytest
import yatest.common

from crypta.cm.services.common.test_utils import (
    crypta_cm_service,
    mock_quoter_servers,
)
from crypta.cm.services.common.test_utils.clients_config import ClientsConfig
from crypta.cm.services.common.test_utils.mutator import Mutator
from crypta.cm.services.common.test_utils.quoter import Quoter
from crypta.cm.services.common.test_utils.quoter_http_proxy import QuoterHttpProxy
from crypta.cm.services.common.test_utils.rt_duid_message_generator import RtDuidMessageGenerator
from crypta.cm.services.common.test_utils.tvm_ids import TvmIds
from crypta.lib.python import yaml2proto
from crypta.lib.python.logbroker.test_helpers.logbroker_config import LogbrokerConfig
from crypta.lib.python.logbroker.test_helpers.simple_logbroker_client import SimpleLogbrokerClient

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


@pytest.fixture(scope="session")
def evacuate_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "evacuate-log")


@pytest.fixture(scope="session")
def evacuate_log_logbroker_client(evacuate_log_logbroker_config):
    with SimpleLogbrokerClient(evacuate_log_logbroker_config) as client:
        yield client


@pytest.fixture(scope="module")
def mutator(request, yt_kv, change_log_logbroker_config, evacuate_log_logbroker_config):
    app_working_dir = os.path.join(yatest.common.test_output_path(), "mutator")

    template_args_marker = request.node.get_closest_marker("mutator_template_args")
    template_args = template_args_marker.kwargs if template_args_marker else dict()

    with Mutator(working_dir=app_working_dir,
                 master=yt_kv.master,
                 change_log_lb_config=change_log_logbroker_config,
                 evacuate_log_lb_config=evacuate_log_logbroker_config,
                 frozen_time=getattr(request.module, "FROZEN_TIME", None),
                 template_args=template_args) as mutator:
        yield mutator


@pytest.fixture(scope="module")
@crypta_cm_service.create(allowed_internal_types=["yandexuid", "icookie"])
def cm_client():
    return


@pytest.fixture
def rt_duid_msg_generator(producer):
    return RtDuidMessageGenerator(producer)


@pytest.fixture(scope="module")
def mock_quoter_server():
    with mock_quoter_servers.MockQuoterServer(is_full=False) as mock:
        yield mock


@pytest.fixture(scope="module")
def quoter(mock_solomon_server):
    app_working_dir = os.path.join(yatest.common.test_output_path(), "quoter")
    with Quoter(working_dir=app_working_dir, solomon_port=mock_solomon_server.port) as quoter:
        yield quoter


@pytest.fixture(scope="module")
def quoter_http_proxy(quoter):
    app_working_dir = os.path.join(yatest.common.test_output_path(), "quoter_http_proxy")
    with QuoterHttpProxy(working_dir=app_working_dir, quoter_port=quoter.port) as quoter_http_proxy:
        yield quoter_http_proxy
