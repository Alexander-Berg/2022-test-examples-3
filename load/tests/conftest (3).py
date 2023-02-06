from unittest.mock import patch, PropertyMock

import pytest
from yandextank.core.tankcore import JobsStorage

from load.projects.cloud.tank_client.client import LoadTestingGRPCClient, TankApiClient
from load.projects.cloud.tank_client.service import TankClientService, Job


@pytest.fixture()
def patch_write_config_file():
    with patch('load.projects.cloud.tank_client.utils.write_config_file') as p:
        p.return_value = True
        yield p


@pytest.fixture()
def patch_agent_id():
    with patch('load.projects.cloud.tank_client.client.LoadTestingGRPCClient.agent_id', new_callable=PropertyMock) as p:
        p.return_value = 'agent_id'


@pytest.fixture()
def patch_claim_tank_status():
    with patch.object(LoadTestingGRPCClient, 'claim_tank_status') as p:
        yield p


@pytest.fixture()
def patch_claim_job_status():
    with patch.object(LoadTestingGRPCClient, 'claim_job_status') as p:
        p.return_value = 0
        yield p


@pytest.fixture()
def patch_get_job():
    with patch.object(LoadTestingGRPCClient, 'get_job') as p:
        yield p


@pytest.fixture()
def patch_get_job_signal():
    with patch.object(LoadTestingGRPCClient, 'get_job_signal') as p:
        yield p


@pytest.fixture(autouse=True)
def patch_agent_registaration():
    with patch.object(LoadTestingGRPCClient, '_identify_agent_id') as p:
        yield p


@pytest.fixture()
def patch_download_ammo():
    with patch.object(LoadTestingGRPCClient, 'download_ammo') as p:
        yield p


@pytest.fixture()
def patch_run_job():
    with patch.object(TankApiClient, 'run_job') as p:
        yield p


@pytest.fixture()
def patch_prepare_job():
    with patch.object(TankApiClient, 'prepare_job') as p:
        p.return_value = {'success': True,
                          'id': 'MockedTankId'}
        yield p


@pytest.fixture()
def patch_get_tank_status():
    with patch.object(TankApiClient, 'get_tank_status') as p:
        yield p


@pytest.fixture()
def patch_get_job_status():
    with patch.object(TankApiClient, 'get_job_status') as p:
        yield p


@pytest.fixture()
def patch_stop_job():
    with patch.object(TankApiClient, 'stop_job') as p:
        yield p


@pytest.fixture()
def patch_get_current_job():
    with patch.object(TankClientService, 'get_current_job') as p:
        yield p


@pytest.fixture()
def patch_get_config():
    with patch.object(Job, '_get_config') as p:
        yield p


@pytest.fixture(autouse=True)
def patch_push_job():
    with patch.object(JobsStorage, 'push_job') as p:
        yield p


@pytest.fixture(autouse=True)
def patch_create_storage_file():
    with patch.object(JobsStorage, '_create_storage_file') as p:
        yield p


@pytest.fixture(autouse=True)
def patch_get_cloud_job_id():
    with patch.object(JobsStorage, 'get_cloud_job_id') as p:
        p.return_value = 'cloud_job_id'
        yield p


@pytest.fixture(autouse=True)
def patch_compute_get_instance_metadata():
    with patch('load.projects.cloud.cloud_helper.metadata_compute.get_instance_metadata') as p:
        p.return_value = {}
        yield p
