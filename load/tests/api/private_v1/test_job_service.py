import datetime
import pytz

import grpc

from pytest import mark
from google.protobuf.timestamp_pb2 import Timestamp
from yandex.cloud.priv.loadtesting.v1 import tank_job_pb2 as job_message
from yandex.cloud.priv.loadtesting.v1 import tank_job_service_pb2 as job_service
from load.projects.cloud.loadtesting.db.tables import JobTable, JobStatus, AmmoTable, TankTable
import yandex.cloud.priv.loadtesting.v1.storage_pb2 as storage_message


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_operation_add',
    'patch_db_job_delete',
)
@mark.parametrize(('status', 'exp_operation'), [
    (JobStatus.RUNNING.value, grpc.StatusCode.FAILED_PRECONDITION),
    (JobStatus.PREPARING.value, grpc.StatusCode.FAILED_PRECONDITION),
    (JobStatus.AUTOSTOPPED.value, True),
    (JobStatus.FAILED.value, True),
    (JobStatus.CREATED.value, True),
    (JobStatus.STOPPED.value, True)]
)
def test_delete_job(job_service_stub, patch_db_job_get, status, exp_operation):
    patch_db_job_get.return_value = JobTable(id='job_id', folder_id='folder_id', status=status)
    try:
        operation = job_service_stub.Delete(
            job_service.DeleteTankJobRequest(
                id='job_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
        assert operation.description == 'Delete Job'
        if exp_operation is not True:
            assert False, 'We should never be here'
    except grpc.RpcError as error:
        assert error.code() == exp_operation


@mark.usefixtures('patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token')
def test_get_report(job_service_stub, patch_db_job_get):
    imbalance_time = datetime.datetime.utcfromtimestamp(1643657426)
    patch_db_job_get.return_value = JobTable(
        id='job_id', folder_id='folder_id',
        imbalance_point=12, imbalance_ts=imbalance_time)
    result = job_service_stub.GetReport(
        job_service.GetReportRequest(job_id='job_id'),
        metadata=(('authorization', 'Bearer bebearer'),)
    )
    assert result == job_message.TankReport(
        job_id='job_id',
        imbalance_point=12,
        imbalance_ts=int(imbalance_time.astimezone(pytz.utc).timestamp()),
        imbalance_at=Timestamp(seconds=1643657426)
    )


@mark.parametrize(
    ('field_name', 'field_value'),
    [
        ('name', 'New name'),
        ('description', 'New description'),
        ('favorite', True),
        ('favorite', False),
        ('target_version', '1.2'),
        ('imbalance_ts', 1650311116),
        ('imbalance_point', 12),
        ('imbalance_point', 0)
    ]
)
@mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_operation_add',
)
def test_update_job(job_service_stub, patch_db_job_get, patch_db_job_add, field_name, field_value):
    job = JobTable(id='job_id', folder_id='folder_id')
    patch_db_job_get.return_value = job
    patch_db_job_add.return_value = job
    update_request = job_service.UpdateTankJobRequest(id='job_id')
    update_request.__setattr__(field_name, field_value)

    result = job_service_stub.Update(update_request, metadata=(('authorization', 'Bearer bebearer'),))
    assert result.done is True


@mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_operation_add',
)
def test_update_job_imbalance_at(job_service_stub, patch_db_job_get, patch_db_job_add):
    job = JobTable(id='job_id', folder_id='folder_id')
    patch_db_job_get.return_value = job
    patch_db_job_add.return_value = job
    update_request = job_service.UpdateTankJobRequest(
        id='job_id',
        imbalance_at=Timestamp(seconds=1652885870, nanos=255387067),
    )
    result = job_service_stub.Update(update_request, metadata=(('authorization', 'Bearer bebearer'),))
    assert result.done is True


@mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_get_token',
    'patch_db_job_add', 'patch_db_operation_add',
    'patch_db_operation_update',
)
def test_create_with_test_data(job_service_stub, patch_db_tank_get, patch_db_job_get, patch_iam_authorize,
                               patch_db_ammo_get_by_name, patch_aws_check_access_to_file):
    patch_db_ammo_get_by_name.return_value = AmmoTable(s3_name='my_ammo', folder_id='folder')
    patch_db_job_get.return_value = JobTable(id='test_id')
    patch_db_tank_get.return_value = TankTable(
        folder_id='folder',
        id='agent_id'
    )
    operation = job_service_stub.Create(
        job_service.CreateTankJobRequest(
            folder_id='folder',
            name='req name',
            description='req dsc',
            generator=job_message.TankJob.Generator.PHANTOM,
            target_address='req_target',
            target_port=11,
            target_version='req version',
            tank_instance_id='agent_id',
            load_schedule={'load_type': 'RPS', 'load_profile': ['const(30, 30)']},
            test_data=storage_message.StorageObject(object_storage_filename='ammo', object_storage_bucket='bucket')
        ),
        metadata=(('authorization', 'Bearer bebearer'),))
    response = job_message.TankJob()
    operation.response.Unpack(response)
    assert 'my_ammo' in response.config
    patch_aws_check_access_to_file.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_get_token',
    'patch_db_job_add', 'patch_db_operation_add',
    'patch_db_operation_update',
)
def test_create_with_test_data_config(job_service_stub, patch_db_tank_get, patch_db_job_get, patch_iam_authorize,
                                      patch_db_ammo_get_by_name, patch_aws_check_access_to_file):
    patch_db_ammo_get_by_name.return_value = AmmoTable(s3_name='my_ammo', folder_id='folder')
    patch_db_job_get.return_value = JobTable(id='test_id')
    patch_db_tank_get.return_value = TankTable(
        folder_id='folder',
        id='agent_id'
    )
    operation = job_service_stub.Create(
        job_service.CreateTankJobRequest(
            folder_id='folder',
            config="{\"phantom\": {\"enabled\": true, \"package\": \"yandextank.plugins.Phantom\", \"address\": \"req_target:11\", \
                \"ammo_type\": \"phantom\", \"load_profile\": {\"load_type\": \"rps\", \"schedule\": \"const(30, 30)\"}, \"ssl\": false, \"uris\": []}, \
                \"core\": {}, \"cloudloader\": {\"enabled\": true, \"package\": \"yandextank.plugins.CloudUploader\", \"job_name\": \"req name\", \
                \"job_dsc\": \"req dsc\", \"ver\": \"req version\", \"api_address\": null}}",
            tank_instance_id='agent_id',
            test_data=storage_message.StorageObject(object_storage_filename='ammo', object_storage_bucket='bucket')
        ),
        metadata=(('authorization', 'Bearer bebearer'),))
    response = job_message.TankJob()
    operation.response.Unpack(response)
    assert 'my_ammo' in response.config
    patch_aws_check_access_to_file.assert_called()
