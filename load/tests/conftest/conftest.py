import logging
import socket

import grpc
import pytest
import sys
import time
import traceback
import yandex.cloud.priv.loadtesting.agent.v1.agent_registration_service_pb2_grpc as agent_registration_grpc
import yandex.cloud.priv.loadtesting.agent.v1.greeter_service_pb2_grpc as greeter_grpc
import yandex.cloud.priv.loadtesting.agent.v1.job_service_pb2_grpc as job_grpc
import yandex.cloud.priv.loadtesting.agent.v1.tank_service_pb2_grpc as tank_grpc
from yandex.cloud.priv.loadtesting.agent.v1 import test_service_pb2_grpc as agent_test_service_pb2_grpc
from yandex.cloud.priv.loadtesting.v1 import operation_service_pb2_grpc as operation_grpc
from yandex.cloud.priv.loadtesting.v1 import resource_preset_service_pb2_grpc as preset_grpc
from yandex.cloud.priv.loadtesting.v1 import tank_instance_service_pb2_grpc
from yandex.cloud.priv.loadtesting.v1 import tank_job_service_pb2_grpc
from yandex.cloud.priv.loadtesting.v1 import storage_service_pb2_grpc as storage_grpc
from yandex.cloud.priv.loadtesting.v2 import agent_instance_service_pb2_grpc
from yandex.cloud.priv.loadtesting.v2 import test_service_pb2_grpc as test_private_grpc
from yandex.cloud.priv.loadtesting.v2 import stats_service_pb2_grpc as stats_grpc

import load.projects.cloud.loadtesting.server.api.server as server
from load.projects.cloud.cloud_helper.grpc_options import COMMON_CHANNEL_OPTIONS
from load.projects.cloud.loadtesting.server.admin import adminized
from load.projects.cloud.loadtesting.server.admin.healthcheck import healthcheck

pytest_plugins = ['load.projects.cloud.loadtesting.server.tests.conftest.conftest_patches']


@pytest.fixture()
def port_for_server():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        s.bind(("", 0))
        # TODO: разобраться почему с yield не работает.
        return s.getsockname()[1]
    finally:
        s.close()


@pytest.fixture()
def port_for_healthcheck(port_for_server):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        s.bind(("", 0))
        port = s.getsockname()[1]
        assert port != port_for_server
        return port
    finally:
        s.close()


@pytest.fixture(autouse=True)
def check_threads_leak(do_not_send_own_metrics_to_pushgateway):
    """
    Если надо выполнить до определённой фикстуры, нужно явно передать эту фикстуру в аргументах,
    а не через pytest.mark.usefixtures. pytest.mark.usefixtures гарантирует только вызов до тестовой функции,
    но не до какой-либо фикстуры.
    """
    default_ya_test_limit = 60  # ya make default time limit

    start = time.time()
    threads_before_test = set()
    for thread_ident, frame in sys._current_frames().items():
        threads_before_test.add(thread_ident)

    yield

    while True:
        extra_frames = []
        for thread_ident, frame in sys._current_frames().items():
            if thread_ident not in threads_before_test:
                extra_frames.append(frame)
        if not extra_frames:
            return
        # logging just for case test ends by ya make timeout
        logging.warn(f'Test generate {len(extra_frames)} extra threads. Waiting...')
        logging.info('Extra thread trace: ')
        logging.info("".join(traceback.format_stack(extra_frames[0], limit=50)))
        if time.time() - start < default_ya_test_limit:
            time.sleep(1)
            continue
        raise AssertionError(f'Test generate extra {len(extra_frames)} threads.')


@pytest.fixture()
def public_api(port_for_server, prepare_logging_settings, set_log_level_to_debug, check_threads_leak,
               port_for_healthcheck):
    with server.API(insecure_port=port_for_server) as api:
        with adminized(server_to_follow=api.server, logging_controller=api.logging_controller):
            with healthcheck(api.server, port_for_healthcheck):
                yield


@pytest.fixture()
def channel(public_api, port_for_server):
    return grpc.insecure_channel(f'0.0.0.0:{port_for_server}',
                                 options=COMMON_CHANNEL_OPTIONS)


@pytest.fixture()
def tank_stub(channel):
    return tank_grpc.TankServiceStub(channel)


@pytest.fixture()
def agent_service_stub(channel):
    return agent_instance_service_pb2_grpc.AgentInstanceServiceStub(channel)


@pytest.fixture()
def tank_service_stub(channel):
    return tank_instance_service_pb2_grpc.TankInstanceServiceStub(channel)


@pytest.fixture()
def agent_test_service_stub(channel):
    return agent_test_service_pb2_grpc.TestServiceStub(channel)


@pytest.fixture()
def job_service_stub(channel):
    return tank_job_service_pb2_grpc.TankJobServiceStub(channel)


@pytest.fixture()
def job_stub(channel):
    return job_grpc.JobServiceStub(channel)


@pytest.fixture()
def tank_job_service_stub(channel):
    return tank_job_service_pb2_grpc.TankJobServiceStub(channel)


@pytest.fixture()
def preset_stub(channel):
    return preset_grpc.ResourcePresetServiceStub(channel)


@pytest.fixture()
def operation_stub(channel):
    return operation_grpc.OperationServiceStub(channel)


@pytest.fixture()
def greeter_stub(channel):
    return greeter_grpc.GreeterServiceStub(channel)


@pytest.fixture()
def agent_registration_stub(channel):
    return agent_registration_grpc.AgentRegistrationServiceStub(channel)


@pytest.fixture()
def storage_stub(channel):
    return storage_grpc.StorageServiceStub(channel)


@pytest.fixture()
def test_private_stub(channel):
    return test_private_grpc.TestServiceStub(channel)


@pytest.fixture()
def stats_stub(channel):
    return stats_grpc.StatsServiceStub(channel)


@pytest.fixture(autouse=True)
def set_log_level_to_debug():
    logging.root.setLevel(logging.DEBUG)
