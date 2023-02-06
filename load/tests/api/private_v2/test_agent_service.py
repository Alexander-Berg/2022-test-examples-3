from collections import namedtuple
from google.protobuf.pyext._message import RepeatedCompositeContainer
from unittest import mock
from datetime import datetime

import grpc
from google.protobuf.any_pb2 import Any
from google.rpc.status_pb2 import Status
from pytest import mark, raises

from load.projects.cloud.loadtesting.config import EnvType
from load.projects.cloud.loadtesting.db.tables import TankTable, AgentVersionTable, STATUS_COMMENTS, OperationTable
from load.projects.cloud.loadtesting.server.api.private_v1.tank import METADATA_AGENT_VERSION_ATTR
from ..private_v1.test_tank_service import raise_
from yandex.cloud.priv.compute.v1 import instance_pb2, instance_service_pb2
from yandex.cloud.priv.loadtesting.v2 import agent_instance_pb2, agent_instance_service_pb2 as agent_service
from yandex.cloud.priv.operation import operation_pb2

Metadata = namedtuple('Metadata', ['instance_id'])

UnsetVersion = agent_instance_pb2.AgentVersion(
    status=agent_instance_pb2.AgentVersion.VersionStatus.UNSET,
    status_comment=STATUS_COMMENTS['ru']['UNSET']
)


@mark.parametrize(('compute_instance', 'tank_table', 'exp_result'), [
    (instance_pb2.Instance(folder_id='folder'),
     TankTable(id='agent_id', compute_instance_id='compute_id', folder_id='folder', labels='{"l": "1"}'),
     agent_instance_pb2.AgentInstance(id='agent_id', folder_id='folder', labels={"l": "1"}, agent_version=UnsetVersion)),
    (instance_pb2.Instance(folder_id='folder', name='compute', service_account_id='compute_sa', labels={"l": "12"}),
     TankTable(id='agent_id', compute_instance_id='compute_id', folder_id='folder', labels='{"l": "1"}', name='load', service_account_id='load_sa'),
     agent_instance_pb2.AgentInstance(id='agent_id', folder_id='folder', labels={"l": "1"}, name='compute',
                                      service_account_id='compute_sa', agent_version=UnsetVersion))
])
@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_user_iam_token',
    'patch_db_agent_version_get',
)
def test_get_agent(agent_service_stub, patch_iam_authorize, patch_db_tank_get, patch_compute_get_instance,
                   compute_instance, tank_table, exp_result):
    patch_db_tank_get.return_value = tank_table
    patch_compute_get_instance.return_value = compute_instance
    agent = agent_service_stub.Get(
        agent_service.GetAgentInstanceRequest(agent_instance_id='agent_id'),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert agent.agent_instance.id == exp_result.id
    patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_user_iam_token',
    'patch_compute_get_instance',
    'patch_db_agent_version_get',
)
@mark.parametrize(('compute_instances', 'tank_table', 'exp_result', 'call_delete'), [
    ([instance_pb2.Instance(folder_id='folder', id='1')],
     [TankTable(id='1', folder_id='folder', compute_instance_id='1')],
     [agent_instance_pb2.AgentInstance(id='1', folder_id='folder', compute_instance_id='1', agent_version=UnsetVersion)],
     False),
    ([instance_pb2.Instance(folder_id='folder', id='1'), instance_pb2.Instance(folder_id='folder', id='2')],
     [TankTable(id='1', folder_id='folder', compute_instance_id='1'),
      TankTable(id='2', folder_id='folder', compute_instance_id='2')],
     [agent_instance_pb2.AgentInstance(id='1', folder_id='folder', compute_instance_id='1', agent_version=UnsetVersion),
      agent_instance_pb2.AgentInstance(id='2', folder_id='folder', compute_instance_id='2', agent_version=UnsetVersion)],
     False),
    ([None],
     [TankTable(id='1', folder_id='folder', compute_instance_id='1')],
     [],
     True),
])
def test_list_agent(agent_service_stub, patch_iam_authorize, patch_db_tanks_with_version_by_folder,
                    patch_list_get_compute_instances,
                    patch_db_tank_delete, compute_instances, tank_table, call_delete, exp_result):
    patch_db_tanks_with_version_by_folder.return_value = list(zip(tank_table, [None] * len(tank_table)))
    patch_list_get_compute_instances.return_value = compute_instances
    tanks = agent_service_stub.List(
        agent_service.ListAgentInstancesRequest(folder_id='folder'),
        metadata=(('authorization', 'Bearer bebearer'),))
    for agent_instance, exp in zip(tanks.agent_instances, exp_result):
        assert agent_instance.id == exp.id
        assert agent_instance.folder_id == exp.folder_id
        assert agent_instance.compute_instance_id == exp.compute_instance_id
    patch_iam_authorize.assert_called()
    if call_delete:
        patch_db_tank_delete.assert_called()
    else:
        patch_db_tank_delete.assert_not_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_tank_delete',
    'patch_user_iam_token',
)
@mark.parametrize('error_code', [
    grpc.StatusCode.NOT_FOUND,
    grpc.StatusCode.UNIMPLEMENTED
])
def test_get_agent_method_failed(agent_service_stub, patch_db_tank_get, error_code):
    with mock.patch('load.projects.cloud.cloud_helper.compute.get_instance', lambda *x: raise_(error_code)):
        patch_db_tank_get.return_value = TankTable(id='agent_id', compute_instance_id='compute_id')
        try:
            _ = agent_service_stub.Get(
                agent_service.GetAgentInstanceRequest(agent_instance_id='agent_id'),
                metadata=(('authorization', 'Bearer bebearer'),))
            assert False, 'We should never be here'
        except grpc.RpcError as error:
            assert error.code() == error_code


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_db_tank_add',
    'patch_db_tank_set_update_time',
    'patch_db_operation_add',
    'patch_db_agent_version_get_target',
    'patch_waiting_for_operation',
    'patch_user_iam_token',
    'patch_get_preset_resources',
)
def test_create_agent_common(agent_service_stub, patch_iam_authorize, patch_get_preset_resources, patch_db_tank_get, patch_db_operation_get,
                             patch_compute_create_instance):
    with mock.patch("load.projects.cloud.loadtesting.config.ENV_CONFIG.SERVER_URL", 'loadtesting:443'):
        patch_compute_create_instance.return_value = (operation_pb2.Operation(id='op_id'), Metadata('id'))
        patch_db_operation_get.return_value = OperationTable(id='op_id', done_resource_snapshot='{}')
        patch_db_tank_get.return_value = TankTable(
            id='agent_id', compute_instance_id='compute_id', folder_id='folder', labels='{"l": "1"}', name='load', service_account_id='load_sa', created_at=datetime(2022, 5, 27, 10, 54, 11, 749576))
        operation = agent_service_stub.Create(
            agent_service.CreateAgentInstanceRequest(
                folder_id='folder_id',
                labels={'l': '1'},
                preset_id='1',
                service_account_id='service_account_id',
                zone_id='zone_id',
                description='description'),
            metadata=(('authorization', 'Bearer bebearer'),))
        assert operation.description == 'Create agent'
        # check metadata
        metadata = agent_service.CreateAgentInstanceMetadata()
        operation.metadata.Unpack(metadata)
        assert metadata.agent_instance_id.startswith('abc')  # created id
        # check response
        response = agent_instance_pb2.AgentInstance()
        operation.response.Unpack(response)
        assert response.id == 'agent_id'  # id from TankTable
        patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_db_tank_get',
    'patch_db_tank_add',
    'patch_db_tank_set_update_time',
    'patch_db_operation_add',
    'patch_db_agent_version_get_target',
    'patch_waiting_for_operation',
    'patch_user_iam_token',
    'patch_get_preset_resources',
)
@mark.parametrize('ssk_key', ['', 'user:somesshkey'])
def test_create_agent_ssh_key(agent_service_stub, patch_iam_authorize, patch_compute_create_instance, patch_db_operation_get, ssk_key):
    with mock.patch("load.projects.cloud.loadtesting.config.ENV_CONFIG.SERVER_URL", 'loadtesting:443'):
        patch_compute_create_instance.return_value = (operation_pb2.Operation(id='op_id'), Metadata('id'))
        patch_db_operation_get.return_value = OperationTable(id='op_id')
        _ = agent_service_stub.Create(
            agent_service.CreateAgentInstanceRequest(
                folder_id='folder_id',
                preset_id='1',
                service_account_id='service_account_id',
                zone_id='zone_id',
                description='description',
                metadata={'ssh-keys': ssk_key}),
            metadata=(('authorization', 'Bearer bebearer'),))
        is_there_dict_arg = False
        call_args = patch_compute_create_instance.call_args
        for arg in call_args.args:
            if isinstance(arg, dict):
                is_there_dict_arg = True
                if ssk_key:
                    assert 'user-data' in arg
                else:
                    assert 'user-data' not in arg
        assert is_there_dict_arg
        patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_db_tank_set_update_time',
    'patch_db_operation_add',
    'patch_waiting_for_operation',
    'patch_user_iam_token',
    'patch_get_preset_resources',
)
@mark.parametrize('image', ['image_id', None])
def test_create_agent_image(agent_service_stub, patch_iam_authorize, patch_compute_create_instance, patch_db_operation_get,
                            patch_db_agent_version_get_target, image, patch_db_tank_add):
    with mock.patch("load.projects.cloud.loadtesting.config.ENV_CONFIG.SERVER_URL", 'loadtesting:443'):
        patch_db_agent_version_get_target.return_value = AgentVersionTable(image_id=image) if image else None
        patch_compute_create_instance.return_value = (operation_pb2.Operation(id='op_id'), Metadata('id'))
        patch_db_operation_get.return_value = OperationTable(id='op_id')
        try:
            operation = agent_service_stub.Create(
                agent_service.CreateAgentInstanceRequest(
                    folder_id='folder_id',
                    preset_id='1',
                    service_account_id='service_account_id',
                    zone_id='zone_id'),
                metadata=(('authorization', 'Bearer bebearer'),))
            if image is None:
                raise AssertionError('We should never be here, exception was expected')
            else:
                assert operation.description == 'Create agent'
                patch_iam_authorize.assert_called_once()

                patch_compute_create_instance.assert_called_once()
                args, _ = patch_compute_create_instance.call_args
                metadata_arg_index = 8
                assert (METADATA_AGENT_VERSION_ATTR, image) in args[metadata_arg_index].items()

                patch_db_tank_add.assert_called_once()
                args, _ = patch_db_tank_add.call_args
                tank_added_to_db = args[0]
                assert image == tank_added_to_db.agent_version

        except grpc.RpcError as error:
            if error.code() == grpc.StatusCode.NOT_FOUND and image is None:  # TODO: INTERNAL_ERROR ?
                assert error.details() == "Failed to define target agent version."
            else:
                raise AssertionError('We should never be here')


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_db_tank_get',
    'patch_db_tank_add',
    'patch_db_tank_set_update_time',
    'patch_db_operation_add',
    'patch_waiting_for_operation',
    'patch_user_iam_token',
    'patch_get_preset_resources',
)
@mark.parametrize('env_type', [EnvType.PREPROD, EnvType.PROD, EnvType.LOCAL])
def test_create_agent_env_type(agent_service_stub, patch_iam_authorize, patch_compute_create_instance, patch_db_operation_get,
                               patch_db_agent_version_get_target, env_type):
    with mock.patch("load.projects.cloud.loadtesting.config.ENV_CONFIG.SERVER_URL", 'loadtesting:443'):
        with mock.patch("load.projects.cloud.loadtesting.config.ENV_CONFIG.ENV_TYPE", env_type.value):
            patch_compute_create_instance.return_value = (operation_pb2.Operation(id='op_id'), Metadata('id'))
            patch_db_operation_get.return_value = OperationTable(id='op_id')
            _ = agent_service_stub.Create(
                agent_service.CreateAgentInstanceRequest(
                    folder_id='folder_id',
                    preset_id='1',
                    service_account_id='service_account_id',
                    zone_id='zone_id',
                    network_interface_specs=[instance_service_pb2.NetworkInterfaceSpec(subnet_id='subnet_id')]),
                metadata=(('authorization', 'Bearer bebearer'),))
            is_there_list_arg = False
            call_args = patch_compute_create_instance.call_args
            for arg in call_args.args:
                if isinstance(arg, (RepeatedCompositeContainer, list)):
                    is_there_list_arg = True
                    assert len(arg) > 0
                    for subnet in arg:
                        if env_type == EnvType.PREPROD:
                            assert subnet.HasField('primary_v6_address_spec')
                        else:
                            assert not subnet.HasField('primary_v6_address_spec')
            assert is_there_list_arg
            patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_waiting_for_operation',
    'patch_user_iam_token',
)
@mock.patch('load.projects.cloud.cloud_helper.compute.restart_instance',
            lambda *x: raise_(grpc.StatusCode.FAILED_PRECONDITION))
def test_agent_restart_fail(agent_service_stub, patch_db_tank_get):
    patch_db_tank_get.return_value = TankTable(id='agent_id', compute_instance_id='compute_id')
    try:
        _ = agent_service_stub.Restart(
            agent_service.RestartAgentInstanceRequest(agent_instance_id='agent_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
        assert False, 'We should never be here'
    except grpc.RpcError as error:
        assert error.code() == grpc.StatusCode.FAILED_PRECONDITION


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_waiting_for_operation',
    'patch_user_iam_token',
    'patch_db_tank_set_update_time',
    'patch_db_operation_get',
    'patch_db_operation_add',
)
@mark.parametrize(('service', 'grpc_request', 'metadata_type', 'tank_table'), [
    ('Delete', agent_service.DeleteAgentInstanceRequest, agent_service.DeleteAgentInstanceMetadata, TankTable(id='agent_id', compute_instance_id='compute_id')),
    ('Delete', agent_service.DeleteAgentInstanceRequest, agent_service.DeleteAgentInstanceMetadata, None),
    ('Start', agent_service.StartAgentInstanceRequest, agent_service.StartAgentInstanceMetadata, TankTable(id='agent_id', compute_instance_id='compute_id')),
    ('Start', agent_service.StartAgentInstanceRequest, agent_service.StartAgentInstanceMetadata, None,),
    ('Stop', agent_service.StopAgentInstanceRequest, agent_service.StopAgentInstanceMetadata, TankTable(id='agent_id', compute_instance_id='compute_id')),
    ('Stop', agent_service.StopAgentInstanceRequest, agent_service.StopAgentInstanceMetadata, None),
    ('Restart', agent_service.RestartAgentInstanceRequest, agent_service.RestartAgentInstanceMetadata, TankTable(id='agent_id', compute_instance_id='compute_id')),
    ('Restart', agent_service.RestartAgentInstanceRequest, agent_service.RestartAgentInstanceMetadata, None),
])
def test_requests_agent(agent_service_stub, patch_iam_authorize, patch_db_tank_get, patch_compute_delete_instance,
                        patch_compute_start_instance, patch_compute_stop_instance, patch_compute_restart_instance,
                        service, grpc_request, metadata_type, tank_table):
    patch_db_tank_get.return_value = tank_table
    operation = operation_pb2.Operation(id='op_id')
    patch_compute_delete_instance.return_value = operation
    patch_compute_start_instance.return_value = operation
    patch_compute_stop_instance.return_value = operation
    patch_compute_restart_instance.return_value = operation
    agent_id = 'agent_id'
    agent_service = agent_service_stub.__dict__[service]
    metadata = metadata_type(agent_instance_id=agent_id)
    meta_message = Any()
    meta_message.Pack(metadata)
    try:
        operation = agent_service(
            grpc_request(agent_instance_id=agent_id),
            metadata=(('authorization', 'Bearer bebearer'),))
        assert operation.description == f'{service} agent'
        assert operation.metadata == meta_message
        patch_iam_authorize.assert_called()
        if tank_table is None:
            raise AssertionError('We should never be here, exception was expected')
    except grpc.RpcError as error:
        if tank_table is not None:
            raise AssertionError('We should never be here, exception was not expected')
        assert error.code() == grpc.StatusCode.NOT_FOUND


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_waiting_for_operation',
    'patch_user_iam_token',
    'patch_db_tank_set_update_time',
    'patch_db_operation_add',
    'patch_db_operation_get',
    'patch_db_operation_update',
    'patch_db_preset_get',
    'patch_db_agent_version_get_target',
    'patch_compute_stop_instance',
    'patch_compute_get_instance',
)
def test_update_agent_image(agent_service_stub, patch_iam_authorize, patch_db_tank_get, patch_compute_update_image_id, patch_compute_update_metadata,
                            patch_compute_start_instance, patch_compute_stop_instance):
    agent_id='agent_id'
    patch_db_tank_get.return_value = TankTable(id=agent_id, agent_version='current_image_id', compute_instance_id='compute_instance_id')
    patch_compute_stop_instance.return_value = operation_pb2.Operation(id='op_id', done=True)
    patch_compute_update_image_id.return_value = operation_pb2.Operation(id='op_id', done=True)
    patch_compute_update_metadata.return_value = operation_pb2.Operation(id='op_id', done=True)
    patch_compute_stop_instance.return_value = operation_pb2.Operation(id='op_id', done=True)
    meta_message = Any()
    meta_message.Pack(agent_service.UpgradeImageAgentInstanceMetadata(agent_instance_id=agent_id))
    operation = agent_service_stub.UpgradeImage(
        agent_service.UpgradeImageAgentInstanceRequest(agent_instance_id=agent_id),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert operation.description == 'Upgrade agent'
    assert operation.metadata == meta_message
    patch_iam_authorize.assert_called()
    patch_compute_stop_instance.assert_called()
    patch_compute_update_image_id.assert_called()
    patch_compute_update_metadata.assert_called()
    patch_compute_start_instance.assert_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_waiting_for_operation',
    'patch_user_iam_token',
    'patch_db_tank_set_update_time',
    'patch_db_operation_add',
    'patch_db_operation_get',
    'patch_db_operation_update',
    'patch_db_preset_get',
    'patch_db_agent_version_get_target',
)
def test_update_agent_image_error(agent_service_stub, patch_iam_authorize, patch_db_tank_get, patch_compute_update_image_id, patch_compute_update_metadata, patch_compute_stop_instance):
    agent_id='agent_id'
    patch_compute_stop_instance.return_value = operation_pb2.Operation(id='op_id', error=Status(message='error'), done=True)
    patch_db_tank_get.return_value = TankTable(id=agent_id, agent_version='current_image_id', compute_instance_id='compute_instance_id')
    meta_message = Any()
    meta_message.Pack(agent_service.UpgradeImageAgentInstanceMetadata(agent_instance_id=agent_id))
    operation = agent_service_stub.UpgradeImage(
        agent_service.UpgradeImageAgentInstanceRequest(agent_instance_id=agent_id),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert operation.description == 'Upgrade agent'
    assert operation.metadata == meta_message
    patch_iam_authorize.assert_called()
    patch_compute_update_image_id.assert_not_called()


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_get_token',
    'patch_user_iam_token',
    'patch_db_agent_version_get_target',
)
def test_update_agent_image_abort(agent_service_stub, patch_iam_authorize, patch_db_tank_get):
    with raises(grpc.RpcError) as error:
        patch_db_tank_get.return_value = TankTable(id='agent_id', agent_version='target_image_id', compute_instance_id='compute_instance_id')
        agent_service_stub.UpgradeImage(
            agent_service.UpgradeImageAgentInstanceRequest(agent_instance_id='agent_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
    assert error.value.code() == grpc.StatusCode.FAILED_PRECONDITION
