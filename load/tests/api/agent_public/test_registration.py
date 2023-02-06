import grpc
import pytest
from pytest import mark
from yandex.cloud.priv.loadtesting.agent.v1 import agent_registration_service_pb2

from load.projects.cloud.loadtesting.db.tables.tank import TankTable
from load.projects.cloud.loadtesting.server.api.private_v1.tank import METADATA_AGENT_VERSION_ATTR


@mark.parametrize('is_there_tank', [True, False])
@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_agent_registration(agent_registration_stub, patch_db_get_tank_by_compute_instance_id, is_there_tank):
    patch_db_get_tank_by_compute_instance_id.return_value = TankTable(id='tank_id', compute_instance_id='compute_id') if is_there_tank else None
    try:
        response = agent_registration_stub.Register(
            agent_registration_service_pb2.RegisterRequest(compute_instance_id='tank_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
        if is_there_tank:
            assert response.agent_instance_id == 'tank_id'
        else:
            assert False, 'We should never be here'
    except grpc.RpcError as error:
        assert error.code() == grpc.StatusCode.NOT_FOUND


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_set_agent_version_for_unknown_agent(agent_registration_stub, patch_db_get_tank_by_compute_instance_id,
                                             patch_db_tank_update_agent_version):
    patch_db_get_tank_by_compute_instance_id.return_value = TankTable(id='tank_id', compute_instance_id='compute_id')
    version = "test_version"
    agent_registration_stub.Register(
        agent_registration_service_pb2.RegisterRequest(compute_instance_id='tank_id'),
        metadata=(
            ('authorization', 'Bearer bebearer'),
            (METADATA_AGENT_VERSION_ATTR, version),
        )
    )
    patch_db_tank_update_agent_version.assert_called_once()
    args, _ = patch_db_tank_update_agent_version.call_args
    assert args[1] == version


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_validate_agent_version_for_known_agent(agent_registration_stub, patch_db_get_tank_by_compute_instance_id,
                                                patch_db_tank_update_agent_version):
    patch_db_get_tank_by_compute_instance_id.return_value = TankTable(id='tank_id', compute_instance_id='compute_id',
                                                                      agent_version='version by create instance')
    version = 'version by create instance'
    agent_registration_stub.Register(
        agent_registration_service_pb2.RegisterRequest(compute_instance_id='tank_id'),
        metadata=(
            ('authorization', 'Bearer bebearer'),
            (METADATA_AGENT_VERSION_ATTR, version),
        )
    )
    patch_db_tank_update_agent_version.assert_called_once()
    args, _ = patch_db_tank_update_agent_version.call_args
    assert args[1] == version


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_validate_agent_version_for_known_agent_wrong_version(agent_registration_stub, patch_db_get_tank_by_compute_instance_id,
                                                              patch_db_tank_update_agent_version):
    patch_db_get_tank_by_compute_instance_id.return_value = TankTable(id='tank_id', compute_instance_id='compute_id',
                                                                      agent_version='version by create instance')
    version = 'wrong version'
    with pytest.raises(grpc.RpcError) as err:
        agent_registration_stub.Register(
            agent_registration_service_pb2.RegisterRequest(compute_instance_id='tank_id'),
            metadata=(
                ('authorization', 'Bearer bebearer'),
                (METADATA_AGENT_VERSION_ATTR, version),
            )
        )
    assert 'inconsistency' in err.value.details()
