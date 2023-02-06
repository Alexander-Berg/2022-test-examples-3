import grpc
import pytest
from yandex.cloud.priv.loadtesting.v1 import tank_job_service_pb2 as job_service
from load.projects.cloud.loadtesting.db.tables import AmmoTable


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_upload_ammo_without_file(job_service_stub):
    try:
        result = job_service_stub.UploadAmmo(
            job_service.UploadAmmoRequest(folder_id='1111'),
            metadata=(('authorization', 'Bearer bebearer'),)
        )
        if result.id:
            assert False, 'We should never be here'
    except grpc.RpcError as error:
        assert error.code() == grpc.StatusCode.INVALID_ARGUMENT


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_upload_ammo_fail_on_aws(job_service_stub, patch_aws_upload_fileobj):
    patch_aws_upload_fileobj.return_value = False
    try:
        result = job_service_stub.UploadAmmo(
            job_service.UploadAmmoRequest(folder_id='1111', ammo=b'df'),
            metadata=(('authorization', 'Bearer bebearer'),)
        )
        if result.id:
            assert False, 'We should never be here'
    except grpc.RpcError as error:
        assert error.code() == grpc.StatusCode.INTERNAL


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_add_ammo'
)
def test_upload_ammo_success(job_service_stub, patch_ammo_create, patch_aws_upload_fileobj):
    ammo = AmmoTable()
    patch_aws_upload_fileobj.return_value = True
    patch_ammo_create.return_value = ammo
    result = job_service_stub.UploadAmmo(
        job_service.UploadAmmoRequest(folder_id='1111', filename='ammo.file', ammo=b'df'),
        metadata=(('authorization', 'Bearer bebearer'),)
    )
    assert result.filename.startswith('ammo_')
    assert result.status == job_service.UploadFileResponse.OK
