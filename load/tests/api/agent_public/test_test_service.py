import grpc
import pytest
from yandex.cloud.priv.loadtesting.agent.v1 import test_service_pb2
from load.projects.cloud.loadtesting.db.tables import JobTable, TankTable, OperationTable
from load.projects.cloud.loadtesting.server.api.private_v1.job_config import JobConfig


@pytest.mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_job_add', 'patch_db_operation_add', 'patch_db_job_get',
    'patch_db_operation_update'
)
def test_create_test(agent_test_service_stub, patch_db_tank_get, patch_job_create_config, patch_job_create_job, patch_db_operation_get):
    patch_db_tank_get.return_value = TankTable(
        folder_id='folder',
        id='sdc',
    )
    patch_job_create_config.return_value = JobConfig()
    patch_job_create_job.return_value = JobTable()
    patch_db_operation_get.return_value = OperationTable(
        target_resource_id='test_id'
    )
    operation = agent_test_service_stub.Create(
        test_service_pb2.CreateTestRequest(
            target_address='host',
            agent_instance_id='sdc',
            load_schedule={'load_type': 'RPS', 'load_profile': ['const(30, 30)'], 'load_schedule': [{'type': 'CONST', 'duration': '10s'}]}
        ),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert operation.description == 'Create Job'
    metadata = test_service_pb2.CreateTestMetadata()
    operation.metadata.Unpack(metadata)
    assert metadata.test_id.startswith('abc')


@pytest.mark.parametrize(('input_attr', 'input_value'), [
    ('name', 'new_name'),
    ('description', 'new_description'),
    ('favorite', False),
    ('target_version', 'new_version'),
    ('imbalance_ts', 1646907260),
    ('imbalance_point', 12),
]
)
@pytest.mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_job_add', 'patch_db_operation_add', 'patch_db_job_get',
    'patch_db_operation_update'
)
def test_update_test(agent_test_service_stub, patch_db_operation_get, patch_db_job_get, patch_db_job_add, input_attr, input_value):
    job = JobTable(
        id='job_id',
        name="old_name",
        description="old_description",
        favorite=True,
        version='old_version',
        imbalance_ts=0,
        imbalance_point=0
    )
    patch_db_job_get.return_value = job
    request = test_service_pb2.UpdateTestRequest(test_id=job.id)
    # request.__setattr__(input_attr, input_value)
    operation = agent_test_service_stub.Update(
        request,
        metadata=(('authorization', 'Bearer bebearer'),)
    )
    assert operation.description == 'Update Job'


@pytest.mark.usefixtures('patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token', 'patch_db_job_get')
def test_update_non_existent_job(agent_test_service_stub, patch_db_job_get):
    with pytest.raises(grpc.RpcError):
        job = JobTable(id='job_id')
        patch_db_job_get.return_value = job
        agent_test_service_stub.Update(
            test_service_pb2.UpdateTestRequest(test_id='srfg', name='vs'),
            metadata=(('authorization', 'Bearer bebearer'),)
        )
        raise AssertionError('We should never be here, exception was expected')
