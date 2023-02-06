import grpc
from pytest import mark
from unittest import mock
from datetime import datetime
from google.protobuf.timestamp_pb2 import Timestamp
from load.projects.cloud.loadtesting.config import EnvType
from load.projects.cloud.loadtesting.server.api.common.utils import ts_from_dt, parse_target
from yandex.cloud.priv.loadtesting.v1 import tank_instance_service_pb2 as tank_service


def test_ts_not_empty():
    now = datetime.utcnow()
    result = ts_from_dt(now)
    assert isinstance(result, Timestamp)
    assert result.ToDatetime() == now


def test_ts_empty():
    result = ts_from_dt(None)
    assert result is None


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_waiting_for_operation',
)
@mark.parametrize('env_type', [EnvType.PREPROD, EnvType.PROD, EnvType.LOCAL])
def test_log_both_ends(tank_service_stub, patch_db_tank_get, env_type):
    with mock.patch("load.projects.cloud.loadtesting.config.ENV_CONFIG.ENV_TYPE", env_type.value):
        patch_db_tank_get.return_value = "TankTable"
        details = "Server internal error" if env_type.value == EnvType.PROD.value else \
                  "'str' object has no attribute"
        try:
            _ = tank_service_stub.Restart(
                tank_service.RestartTankInstanceRequest(id='tank_id'),
                metadata=(('authorization', 'Bearer bebearer'),))
            assert False, 'We should never be here'
        except grpc.RpcError as error:
            assert error.code() == grpc.StatusCode.INTERNAL
            assert details in error.details()


@mark.parametrize('raw_target, expected_target, expected_port',
                  [
                      ('', None, None),
                      ('192.168.1.1:80', '192.168.1.1', 80),
                      ('[2a02:6b8:c02:901:0:fc5f:9a6c:2a2]:80', '2a02:6b8:c02:901:0:fc5f:9a6c:2a2', 80),
                      ('ya.ru:443', 'ya.ru', 443),
                      ('ya.ru', 'ya.ru', None),
                      ('192.168.1.1', '192.168.1.1', None)
                  ])
def test_parse_target(raw_target, expected_target, expected_port):
    assert parse_target(raw_target) == (expected_target, expected_port)
