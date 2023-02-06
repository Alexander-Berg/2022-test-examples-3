import json
import logging
from datetime import timedelta, datetime

import grpc
from google.protobuf.any_pb2 import Any
from google.protobuf.empty_pb2 import Empty
from google.protobuf.json_format import MessageToJson
from google.protobuf.json_format import Parse
from google.protobuf.timestamp_pb2 import Timestamp
from google.rpc.code_pb2 import Code as rpc_code
from google.rpc.status_pb2 import Status as rpc_status
from pytest import mark

from load.projects.cloud.loadtesting.db.tables import ResourceType, OperationTable
from load.projects.cloud.loadtesting.server.api.private_v1.operation import create_operation_message, create_nested_message
from yandex.cloud.priv.loadtesting.v1 import tank_instance_pb2, operation_service_pb2, tank_job_service_pb2


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_user_iam_token',
)
def test_operation_get_snapshot(operation_stub, patch_db_operation_get):
    time = datetime.utcnow()
    tank_instance = tank_instance_pb2.TankInstance(
        id='tank_id',
        folder_id='folder_id',
        created_at=Timestamp().FromDatetime(time),
        compute_instance_id='compute_instance_id',
        compute_instance_updated_at=Timestamp().FromDatetime(time),
        description='description',
        labels={"h": "2"},
        service_account_id='service_account_id',
        preset_id='preset_id',
        status=tank_instance_pb2.TankInstance.Status.READY_FOR_TEST)
    patch_db_operation_get.return_value = OperationTable(
        id='operation_id',
        target_resource_type=ResourceType.TANK.value,
        created_at=datetime.utcnow(),
        done=True,
        done_resource_snapshot=MessageToJson(tank_instance)
    )
    operation = operation_stub.Get(
        operation_service_pb2.GetOperationRequest(operation_id='operation_id'),
        metadata=(('authorization', 'Bearer bebearer'),))
    response = tank_instance_pb2.TankInstance()
    operation.response.Unpack(response)
    assert response == tank_instance
    assert operation.id == 'operation_id'


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
@mark.parametrize(('error', 'done_resource_snapshot'), [
    ('some error', "{\n  \"id\": \"id\",\n  \"folderId\": \"folderId\"}"),
    ('some error', None),
    (None, "{\n  \"id\": \"id\",\n  \"folderId\": \"folderId\"}"),
    (None, None),
])
def test_create_operation_message(operation_stub, error, done_resource_snapshot):
    db_operation = OperationTable(
        id='operation_id',
        error=error,
        target_resource_type=ResourceType.TANK.value,
        done_resource_snapshot=done_resource_snapshot,
        done=True)

    metadata = tank_job_service_pb2.CreateTankJobMetadata(id='job_id')
    message = create_nested_message(metadata)
    db_operation.resource_metadata = message

    operation = create_operation_message(db_operation, logging.getLogger())
    assert operation.done is True
    assert operation.metadata == Parse(message, Any())
    if error:
        exp_error = rpc_status(message=error, code=rpc_code.INVALID_ARGUMENT)
        assert operation.error == exp_error
    else:
        assert not operation.HasField("error")
    response = Any()
    if error is None:
        snapshot_instance = Parse(done_resource_snapshot, tank_instance_pb2.TankInstance()) \
            if done_resource_snapshot is not None else Empty()
        response.Pack(snapshot_instance)
        assert operation.response == response
    else:
        assert not operation.HasField("response")


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
)
def test_get_operation(operation_stub, patch_db_operation_get, patch_iam_authorize):
    patch_db_operation_get.return_value = \
        OperationTable(id='operation_id', target_resource_type=ResourceType.TANK.value, done=True)
    operation = operation_stub.Get(
        operation_service_pb2.GetOperationRequest(operation_id='operation_id'),
        metadata=(('authorization', 'Bearer bebearer'),))
    patch_iam_authorize.assert_called()
    assert operation.id == 'operation_id'


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
)
def test_get_operation_failed(operation_stub, patch_db_operation_get, patch_iam_authorize):
    patch_db_operation_get.return_value = None
    try:
        _ = operation_stub.Get(
            operation_service_pb2.GetOperationRequest(operation_id='operation_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
        raise AssertionError('We should never be here, exception was expected')
    except grpc.RpcError as error:
        assert error.code() == grpc.StatusCode.NOT_FOUND


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
)
@mark.parametrize('db_operations', [
    [],
    [OperationTable(id='operation_id', done=True)],
    [OperationTable(id='operation_id', done=True), OperationTable(id='operation_id_2', done=True)]
])
def test_list_operation(operation_stub, patch_db_operation_get_by_folder, patch_iam_authorize, db_operations):
    patch_db_operation_get_by_folder.return_value = db_operations
    operations = operation_stub.List(
        operation_service_pb2.ListOperationsRequest(folder_id='folder_id'),
        metadata=(('authorization', 'Bearer bebearer'),))
    if not db_operations:
        assert list(operations.operations) == db_operations
    else:
        for op, db_op in zip(operations.operations, db_operations):
            assert op.id == db_op.id
    patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_compute_get_operation',
)
@mark.parametrize(('resource_type', 'is_done', 'foreign_operation_id', 'test_timedelta'), [
    (ResourceType.TANK.value, False, 'some_id', timedelta(minutes=1)),
    (ResourceType.JOB.value, False, 'some_id', timedelta(minutes=1)),
    (ResourceType.TANK.value, True, 'some_id', timedelta(minutes=1)),
    (ResourceType.TANK.value, False, None, timedelta(minutes=1)),
    (ResourceType.TANK.value, True, 'some_id', timedelta(minutes=11)),
    (ResourceType.JOB.value, False, 'some_id', timedelta(minutes=11)),
])
def test_update_operation_method(operation_stub, patch_db_operation_update, patch_db_operation_get, resource_type, is_done, foreign_operation_id, test_timedelta):
    stable_time = datetime(2021, 10, 23, 12, 51, 46, 252455)
    created_time = datetime.utcnow() - test_timedelta
    db_operation = OperationTable(id='op_id', error='error', created_at=created_time, modified_at=stable_time,
                                  target_resource_type=resource_type, foreign_operation_id=foreign_operation_id, done=is_done)
    patch_db_operation_get.return_value = db_operation
    _ = operation_stub.Get(
        operation_service_pb2.GetOperationRequest(operation_id='operation_id'),
        metadata=(('authorization', 'Bearer bebearer'),))
    if resource_type == ResourceType.TANK.value and not is_done and foreign_operation_id:
        patch_db_operation_update.assert_called_once()
    elif not is_done and test_timedelta >= timedelta(minutes=10):
        patch_db_operation_update.call_count == 2
    else:
        patch_db_operation_update.assert_not_called()


def test_create_nested_message():
    message = create_nested_message(tank_job_service_pb2.CreateTankJobMetadata(id='job_id'))
    assert json.loads(message)['id'] == 'job_id'
