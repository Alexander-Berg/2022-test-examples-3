from unittest.mock import patch

import grpc
import pytest
import time
from pytest import mark

from load.projects.cloud.loadtesting.db import PresetQueries
from load.projects.cloud.loadtesting.db.tables.preset import PresetTable
from yandex.cloud.priv.loadtesting.v1 import resource_preset_service_pb2 as preset_service


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_preset_get_too_long(preset_stub):
    def tormoz(*args):
        time.sleep(25)
        return PresetTable(id='preset_id', name='small', cores=2, memory=1234, disk_size=123456678876)

    with patch.object(PresetQueries, 'get', tormoz):
        with pytest.raises(grpc.RpcError) as err_info:
            preset_stub.Get(
                preset_service.GetResourcePresetRequest(preset_id='preset_id'),
                metadata=(('authorization', 'Bearer bebearer'),))
        assert 'Timeout' in err_info.value.details()
