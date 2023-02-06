import pytest
import grpc
from yandex.cloud.priv.loadtesting.v1 import tank_job_pb2 as job_messages
from yandex.cloud.priv.loadtesting.v1 import tank_job_service_pb2 as messages
from load.projects.cloud.loadtesting.db import DB
from load.projects.cloud.loadtesting.db.tables import JobTable, AmmoTable, TankTable, AgentVersionTable, \
    AgentVersionStatus
from load.projects.cloud.loadtesting.server.api.private_v1.job import extract_job_attrs, InvalidAmmo
from load.projects.cloud.loadtesting.server.api.private_v1.job_config import JobConfig


@pytest.mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_job_add', 'patch_db_operation_add',
    'patch_db_operation_update'
)
def test_create_job_operation(job_service_stub, patch_db_tank_get, patch_job_create_config, patch_job_create_job):
    patch_db_tank_get.return_value = TankTable(
        folder_id='folder',
        id='sdc'
    )
    patch_job_create_config.return_value = JobConfig()
    patch_job_create_job.return_value = JobTable()
    operation = job_service_stub.Create(
        messages.CreateTankJobRequest(
            folder_id='folder',
            generator='PHANTOM',
            tank_instance_id='sdc',
            load_schedule={'load_type': 'RPS', 'load_profile': ['const(30, 30)']}
        ),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert operation.description == 'Create Job'


@pytest.mark.usefixtures('patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token')
def test_extract_job_atrrs_from_request(patch_db_get_ammo):
    ammo = AmmoTable(id='1111', folder_id='f1111', s3_name='aaaaaa')
    patch_db_get_ammo.return_value = ammo
    config = JobConfig()
    config.content = {'cloudloader': {}, 'phantom': {'ammofile': '3333', 'enabled': True}}
    job = JobTable()
    job.config = config
    job.folder_id = 'f1111'

    req = messages.CreateTankJobRequest(
        folder_id='f1111',
        name='req name',
        description='req dsc',
        generator=job_messages.TankJob.Generator.PHANTOM,
        ammo_id='1111',
        target_address='req_target',
        target_port=11,
        target_version='req version'
    )

    result_job = extract_job_attrs(req, DB(), job, 'cloudloader')

    assert result_job.name == 'req name'
    assert result_job.description == 'req dsc'
    assert result_job.generator == 'PHANTOM'
    assert ammo in result_job.ammos
    assert result_job.target_address == req.target_address
    assert result_job.target_port == req.target_port
    assert result_job.version == req.target_version


@pytest.mark.usefixtures('patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token')
def test_extract_job_atrrs_from_config(patch_db_get_ammo):
    ammo = AmmoTable(id='1111', folder_id='f1111', s3_name='sdfr')
    patch_db_get_ammo.return_value = ammo
    config = JobConfig()
    config.content = {
        'cloudloader': {
            'job_name': 'config name',
            'job_dsc': 'config dsc',
            'ver': 'new version'
        },
        'pandora': {
            'enabled': True,
            'config_content': {
                'pools': [{
                    'gun': {'target': '192.168.1.1:20'},
                    'ammo': {'file': '2222'}
                }]
            }
        }
    }
    job = JobTable()
    job.config = config
    job.folder_id = 'f1111'

    request = messages.CreateTankJobRequest(folder_id='f1111', ammo_id='1111')
    new_job = extract_job_attrs(request, DB(), job, 'cloudloader')

    assert new_job.name == 'config name'
    assert new_job.description == 'config dsc'
    assert new_job.generator == 'PANDORA'
    assert ammo in new_job.ammos
    assert new_job.target_address == '192.168.1.1'
    assert new_job.target_port == 20
    assert new_job.version == 'new version'


def test_wrong_ammo(patch_db_get_ammo):
    ammo = AmmoTable(id='1111', folder_id='f2222')
    patch_db_get_ammo.return_value = ammo
    config = JobConfig()
    config.content = {'cloudloader': {}, 'phantom': {'ammofile': '3333'}}
    job = JobTable()
    job.config = config

    req = messages.CreateTankJobRequest(
        folder_id='f1111',
        name='req name',
        description='req dsc',
        generator=job_messages.TankJob.Generator.PHANTOM,
        ammo_id='1111',
        target_address='req_target',
        target_port=11,
        target_version='req version'
    )

    with pytest.raises(InvalidAmmo):
        extract_job_attrs(req, DB(), job, 'cloudloader')


@pytest.mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_job_add', 'patch_db_operation_add',
    'patch_db_operation_update'
)
def test_create_job_for_outdated_agent(job_service_stub, patch_db_tank_get, patch_job_create_config,
                                       patch_job_create_job, patch_db_agent_version_get):
    patch_db_tank_get.return_value = TankTable(
        folder_id='folder',
        id='sdc',
        agent_version='asfa'
    )
    patch_db_agent_version_get.return_value = AgentVersionTable(status=AgentVersionStatus.OUTDATED.value)
    patch_job_create_config.return_value = JobConfig()
    patch_job_create_job.return_value = JobTable()
    with pytest.raises(grpc.RpcError) as err_info:
        _ = job_service_stub.Create(
            messages.CreateTankJobRequest(
                folder_id='folder',
                generator='PHANTOM',
                tank_instance_id='sdc',
                load_schedule={'load_type': 'RPS', 'load_profile': ['const(30, 30)']}
            ),
            metadata=(('authorization', 'Bearer bebearer'),))
    assert 'OUTDATED' in err_info.value.details()
