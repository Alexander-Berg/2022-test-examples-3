import grpc
import pytest
from pytest import mark
from datetime import datetime

from load.projects.cloud.loadtesting.db.tables import StorageTable, AmmoTable
from yandex.cloud.priv.loadtesting.v1 import storage_pb2, storage_service_pb2 as storage_service
from yandex.cloud.priv.storage.v1 import bucket_pb2


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
)
def test_storage_get(storage_stub, patch_iam_authorize, patch_db_storage_get, patch_aws_get_bucket_stats):
    patch_db_storage_get.return_value = StorageTable(id='storage_id', folder_id='folder_id', bucket='bucket')
    patch_aws_get_bucket_stats.return_value = bucket_pb2.BucketStats(used_size=123)  # , storage_class_used_sizes=[bucket_pb2.SizeByClass(storage_class="1", class_size=10)])
    response = storage_stub.Get(
        storage_service.GetStorageRequest(
            storage_id='storage_id'),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert response == storage_pb2.Storage(
        id='storage_id',
        folder_id='folder_id',
        object_storage_bucket='bucket',
        used_size=123
    )
    patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_aws_get_bucket_stats',
)
def test_storage_get_error(storage_stub, patch_iam_authorize, patch_db_storage_get):
    patch_db_storage_get.return_value = None
    with pytest.raises(grpc.RpcError) as error:
        storage_stub.Get(
            storage_service.GetStorageRequest(
                storage_id='storage_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
    assert error.value.code() == grpc.StatusCode.NOT_FOUND


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
)
def test_storage_list(storage_stub, patch_iam_authorize, patch_db_storage_get, patch_aws_get_bucket_stats, patch_db_storage_get_by_folder):
    patch_db_storage_get_by_folder.return_value = [StorageTable(id='storage_id', folder_id='folder_id', bucket='bucket'), StorageTable(id='storage_id2', folder_id='folder_id', bucket='bucket2')]
    patch_aws_get_bucket_stats.return_value = bucket_pb2.BucketStats(used_size=123)  # , storage_class_used_sizes=[bucket_pb2.SizeByClass(storage_class="1", class_size=10)])
    response = storage_stub.List(
        storage_service.ListStorageRequest(
            folder_id='folder_id'),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert response == storage_service.ListStorageResponse(storages=[
        storage_pb2.Storage(
            id='storage_id',
            folder_id='folder_id',
            object_storage_bucket='bucket',
            used_size=123),
        storage_pb2.Storage(
            id='storage_id2',
            folder_id='folder_id',
            object_storage_bucket='bucket2',
            used_size=123)
        ])
    patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_db_storage_delete',
    'patch_db_operation_add',
    'patch_db_operation_get',
)
def test_storage_delete(storage_stub, patch_db_storage_get, patch_db_operation_add_snapshot, patch_iam_authorize):
    patch_db_storage_get.return_value = StorageTable(id='storage_id', folder_id='folder_id', bucket='bucket',
                                                     created_at=datetime.utcnow())
    operation = storage_stub.Delete(
        storage_service.DeleteStorageRequest(
            storage_id='storage_id'),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert operation.description == 'Delete storage'
    patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_db_operation_add',
    'patch_db_storage_add'
)
@mark.parametrize('name', [None, 'storage_name'])
def test_storage_create(storage_stub, patch_db_storage_get, patch_iam_authorize, name):
    patch_db_storage_get.return_value = StorageTable(id='storage_id', folder_id='folder_id', bucket='bucket_name')
    bucket_name = 'bucket_name'
    operation = storage_stub.Create(
        storage_service.CreateStorageRequest(
            folder_id='folder_id',
            object_storage_bucket_name=bucket_name,
            name=name, ),
        metadata=(('authorization', 'Bearer bebearer'),))

    response = storage_pb2.Storage()
    operation.response.Unpack(response)
    assert operation.description == 'Create storage'
    assert response.name == name or bucket_name
    patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_db_operation_add',
    'patch_db_storage_add'
)
@mark.parametrize(('name', 'description'), [(None, 'desc'), ('storage_name', None), ('storage_name', 'desc')])
def test_storage_update(storage_stub, patch_db_storage_get, patch_iam_authorize, name, description):
    patch_db_storage_get.return_value = StorageTable(id='storage_id', folder_id='folder_id', bucket='bucket_name', name='name', description='description')
    operation = storage_stub.Update(
        storage_service.UpdateStorageRequest(
            storage_id='storage_id',
            description=description,
            name=name, ),
        metadata=(('authorization', 'Bearer bebearer'),))

    response = storage_pb2.Storage()
    operation.response.Unpack(response)
    assert operation.description == 'Update storage'
    assert response.name == name or 'name'
    assert response.description == name or 'description'
    patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_db_operation_add',
    'patch_db_storage_delete'
)
def test_storage_delete_error(storage_stub, patch_db_storage_get, patch_iam_authorize):
    patch_db_storage_get.return_value = None
    with pytest.raises(grpc.RpcError) as error:
        storage_stub.Delete(
            storage_service.DeleteStorageRequest(
                storage_id='storage_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
    assert error.value.code() == grpc.StatusCode.NOT_FOUND


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_upload_object_without_file(storage_stub):
    with pytest.raises(grpc.RpcError) as error:
        _ = storage_stub.UploadObject(
            storage_service.UploadStorageObjectRequest(folder_id='1111'),
            metadata=(('authorization', 'Bearer bebearer'),)
        )
    assert error.value.code() == grpc.StatusCode.INVALID_ARGUMENT


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_storage_upload_object_fail_on_aws(storage_stub, patch_aws_upload_by_presign_url):
    patch_aws_upload_by_presign_url.return_value = False
    with pytest.raises(grpc.RpcError) as error:
        _ = storage_stub.UploadObject(
            storage_service.UploadStorageObjectRequest(folder_id='1111', test_data=b'df'),
            metadata=(('authorization', 'Bearer bebearer'),)
        )
    assert error.value.code() == grpc.StatusCode.INTERNAL


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_add_ammo'
)
def test_upload_object_diff_buckets(storage_stub, patch_ammo_create, patch_db_storage_get):
    with pytest.raises(grpc.RpcError) as error:
        patch_db_storage_get.return_value = StorageTable(id='storage_id', folder_id='folder_id', bucket='bucket1')
        _ = storage_stub.UploadObject(
            storage_service.UploadStorageObjectRequest(folder_id='1111', filename='ammo.file', test_data=b'df', storage_id='storage_id'),
            metadata=(('authorization', 'Bearer bebearer'),)
        )
    assert error.value.code() == grpc.StatusCode.INVALID_ARGUMENT


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_add_ammo'
)
@mark.parametrize('storage_id', [None, 'storage_id'])
def test_upload_object_success(storage_stub, patch_ammo_create, patch_aws_upload_by_presign_url, patch_aws_upload_fileobj, patch_db_storage_get, storage_id):
    ammo = AmmoTable()
    patch_db_storage_get.return_value = StorageTable(id='storage_id', folder_id='folder_id', bucket='bucket')
    patch_aws_upload_by_presign_url.return_value = True
    patch_aws_upload_fileobj.return_value = True
    patch_ammo_create.return_value = ammo
    result = storage_stub.UploadObject(
        storage_service.UploadStorageObjectRequest(storage_id=storage_id, folder_id='folder_id', filename='ammo.file', test_data=b'df'),
        metadata=(('authorization', 'Bearer bebearer'),)
    )
    assert result.object_storage_filename.startswith('test_data_')
    if storage_id:
        assert result.object_storage_bucket == 'bucket'
        patch_aws_upload_by_presign_url.assert_called_once()
    else:
        patch_aws_upload_fileobj.assert_called_once()
