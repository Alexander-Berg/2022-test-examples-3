import grpc
import pytest

import yandex.cloud.priv.loadtesting.agent.v1.tank_service_pb2 as tank_messages
import yandex.cloud.priv.loadtesting.agent.v1.tank_service_pb2_grpc as tank_grpc
from load.projects.cloud.loadtesting.db.tables.tank import TankTable, ClientStatus


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token'
)
def test_tank_claim_status(tank_stub: tank_grpc.TankServiceStub,
                           patch_db_get_tank_by_compute_instance_id, patch_db_update_tank_status):
    tank = TankTable(folder_id='some_folder_id')
    patch_db_get_tank_by_compute_instance_id.return_value = tank
    status = ClientStatus.READY_FOR_TEST
    r: tank_messages.ClaimTankStatusResponse = tank_stub.ClaimStatus(
        tank_messages.ClaimTankStatusRequest(
            compute_instance_id='lll',
            status=status.value,
        ), metadata=(('authorization', 'Bearer kokoko'),))
    patch_db_update_tank_status.assert_called_once_with(tank, status)
    assert 0 == r.code


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token'
)
def test_tank_claim_status_no_auth(tank_stub: tank_grpc.TankServiceStub,
                                   patch_db_get_tank_by_compute_instance_id, patch_db_update_tank_status):
    tank = TankTable(folder_id='some_folder_id')
    patch_db_get_tank_by_compute_instance_id.return_value = tank
    try:
        tank_messages.ClaimTankStatusResponse = tank_stub.ClaimStatus(
            tank_messages.ClaimTankStatusRequest(
                compute_instance_id='lll',
                status=ClientStatus.TESTING.value,
            ), metadata=(('authorization', 'Bear kokoko'),))
        raise AssertionError('RpcError expected, but it had not been raised.')
    except grpc.RpcError as e:
        assert 'Authentication' in e.details()

    patch_db_update_tank_status.assert_not_called()


def test_statuses_are_in_sync():
    db_values = sorted([e.value for e in ClientStatus])
    proto_values = sorted(tank_messages.ClaimTankStatusRequest.Status.keys())
    assert db_values == proto_values
