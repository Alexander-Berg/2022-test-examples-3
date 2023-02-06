import uuid
from unittest.mock import patch

import grpc
import pytest

from load.projects.cloud.cloud_helper.metadata_compute import SaToken
from load.projects.cloud.loadtesting.server.api.private_v1.tank import METADATA_AGENT_VERSION_ATTR as server_meta_version_attr
from load.projects.cloud.tank_client.client import LoadTestingGRPCClient
from load.projects.cloud.tank_client.client import METADATA_AGENT_VERSION_ATTR
from load.projects.cloud.tank_client.client import agent_registration_service_pb2, job_service_pb2


@pytest.fixture()
def patch_agent_registaration():
    """
    В этих тестах нам нужно заходить в функцию _agent_registaration,
    так что глобальную фикстуру patch_agent_registaration надо выключить.
    """
    pass


@pytest.fixture(autouse=True)
def path_sa_token():
    with patch.object(SaToken, 'get') as p:
        p.return_value = None
        yield


@pytest.fixture()
def patch_agent_registration_stub():
    class Stb:
        Register = None

    with patch.object(Stb, 'Register') as sh:
        sh.return_value = agent_registration_service_pb2.RegisterResponse(
            agent_instance_id='abc'
        )
        with patch('load.projects.cloud.tank_client.client.agent_registration_service_pb2_grpc.AgentRegistrationServiceStub') as stb:
            stb.return_value = Stb
            yield Stb


@pytest.fixture()
def patch_job_stub():
    class Stb:
        Get = None

    with patch.object(Stb, 'Get') as sh:
        sh.return_value = job_service_pb2.Job(
            id='fake_id'
        )
        with patch('load.projects.cloud.tank_client.client.job_service_pb2_grpc.JobServiceStub') as stb:
            stb.return_value = Stb
            yield Stb


def creds_mock(*args, **kwargs):
    return grpc.local_channel_credentials()


@pytest.mark.usefixtures('patch_agent_registration_stub')
def test_agent_send_version_on_greet(patch_agent_registration_stub):
    version = str(uuid.uuid4())

    with patch('load.projects.cloud.tank_client.client.get_creds',
               new=creds_mock):
        LoadTestingGRPCClient('0.0.0.0', 0, version, 'agent_id_file', 'storage_url', 'iam_token_service_url')

    patch_agent_registration_stub.Register.assert_called_once()
    _, kwargs = patch_agent_registration_stub.Register.call_args
    assert 'metadata' in kwargs
    assert (METADATA_AGENT_VERSION_ATTR, version) in kwargs['metadata']


@pytest.mark.usefixtures('patch_agent_registration_stub')
def test_agent_send_version_on_get_job(patch_job_stub):
    version = str(uuid.uuid4())

    with patch('load.projects.cloud.tank_client.client.get_creds',
               new=creds_mock):
        client = LoadTestingGRPCClient('0.0.0.0', 0, version, 'agent_id_file', 'storage_url', 'iam_token_service_url')
        client.get_job()

    patch_job_stub.Get.assert_called_once()
    _, kwargs = patch_job_stub.Get.call_args
    assert 'metadata' in kwargs
    assert (METADATA_AGENT_VERSION_ATTR, version) in kwargs['metadata']


def test_meta_version_args_are_in_sync():
    assert server_meta_version_attr == METADATA_AGENT_VERSION_ATTR
