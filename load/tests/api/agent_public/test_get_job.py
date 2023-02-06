import json

import grpc
import pytest
import yandex.cloud.priv.loadtesting.agent.v1.job_service_pb2 as messages
from yandex.cloud.priv.loadtesting.agent.v1.job_service_pb2_grpc import JobServiceStub

from load.projects.cloud.loadtesting.db.tables import AgentVersionTable, AgentVersionStatus
from load.projects.cloud.loadtesting.db.tables.ammo import AmmoTable
from load.projects.cloud.loadtesting.db.tables.job import JobTable, Status
from load.projects.cloud.loadtesting.db.tables.job_config import JobConfigTable
from load.projects.cloud.loadtesting.db.tables.tank import TankTable


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_job_add',
    'patch_db_job_append_error',
    'patch_db_job_update_status',
    'patch_db_agent_version_get',
)
@pytest.mark.parametrize('is_job_stored', [True, False])
def test_get_job(job_stub: JobServiceStub, patch_db_get_tank_by_compute_instance_id,
                 patch_db_job_get_waiting_for_tank,
                 patch_db_get_config,
                 patch_db_job_get,
                 is_job_stored,
                 ):
    tank = TankTable(folder_id='some_folder_id',
                     id='some tank id', current_job='some_job')
    current_job = JobTable(
        id='job_id')
    patch_db_job_get.return_value = current_job
    patch_db_get_tank_by_compute_instance_id.return_value = tank
    config = JobConfigTable(
        id='config id',
        content='kon\' fig',
    )
    patch_db_get_config.return_value = config
    job_stored = JobTable(
        id='some job id',
        tank_id=tank.id,
        config=config,
        logging_log_group_id='log_group_id'
    )
    patch_db_job_get_waiting_for_tank.return_value = job_stored if is_job_stored else None

    job_got: messages.Job = job_stub.Get(
        messages.GetJobRequest(
            compute_instance_id=tank.id
        ),
        metadata=(('authorization', 'Bearer bebearer'),)
    )
    assert tank.current_job is None
    if not is_job_stored:
        assert job_got == messages.Job()
    else:
        assert job_got.id == job_stored.id
        assert job_got.config == json.dumps(config.content)
        assert job_got.logging_log_group_id == 'log_group_id'


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_job_append_error',
    'patch_db_agent_version_get',
)
def test_get_job_with_failed_download(job_stub: JobServiceStub,
                                      patch_db_get_tank_by_compute_instance_id,
                                      patch_db_job_get_waiting_for_tank,
                                      patch_db_get_ammo,
                                      patch_db_get_config,
                                      patch_aws_download_file_to_buffer,
                                      patch_db_job_update_status,
                                      ):
    tank = TankTable(folder_id='some_folder_id', id='some tank id')
    patch_db_get_tank_by_compute_instance_id.return_value = tank
    config = JobConfigTable(id='config id', content='kon\' fig')
    patch_db_get_config.return_value = config
    job_stored = JobTable(id='some job id', tank_id=tank.id, config=config)
    ammo = AmmoTable(id='ammo id', s3_name='ammo_name')
    job_stored.ammos.append(ammo)
    patch_db_job_get_waiting_for_tank.return_value = job_stored
    patch_db_get_ammo.return_value = ammo

    patch_aws_download_file_to_buffer.return_value = None

    with pytest.raises(grpc.RpcError, match='INTERNAL'):
        job_stub.Get(messages.GetJobRequest(compute_instance_id=tank.id),
                     metadata=(('authorization', 'Bearer bebearer'),)
                     )
    patch_db_job_update_status.assert_called_once_with(job_stored, Status.FAILED)


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token'
)
def test_get_job_no_auth(job_stub: JobServiceStub):
    try:
        job_stub.Get(
            messages.GetJobRequest(
                compute_instance_id='lalala'
            ),
            metadata=(('authorization', 'bebearer'),)
        )
        raise AssertionError('RpcError expected, but it had not been raised.')
    except grpc.RpcError as e:
        assert 'Authentication' in e.details()


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_job_get_waiting_for_tank',
    'patch_db_job_append_error',
)
def test_get_job_for_outdated_agent(job_stub: JobServiceStub,
                                    patch_db_agent_version_get,
                                    patch_db_get_tank_by_compute_instance_id,
                                    patch_db_job_update_status,
                                    patch_db_job_get_waiting_for_tank,
                                    ):
    patch_db_job_get_waiting_for_tank.return_value = JobTable()
    tank = TankTable()
    patch_db_get_tank_by_compute_instance_id.return_value = tank
    patch_db_agent_version_get.return_value = AgentVersionTable(status=AgentVersionStatus.OUTDATED.value)
    with pytest.raises(grpc.RpcError) as err_info:
        job_stub.Get(
            messages.GetJobRequest(
                compute_instance_id='lalala'
            ),
            metadata=(('authorization', 'Bearer bebearer'),)
        )
    assert 'OUTDATED' in err_info.value.details()
    patch_db_job_update_status.assert_called_once()
    args, _ = patch_db_job_update_status.call_args
    assert Status.FAILED == args[1]
