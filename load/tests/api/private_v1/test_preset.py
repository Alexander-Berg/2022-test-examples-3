import grpc
from pytest import mark

from yandex.cloud.priv.loadtesting.v1 import resource_preset_pb2, resource_preset_service_pb2 as preset_service
from load.projects.cloud.loadtesting.db.tables.preset import PresetTable


@mark.parametrize('is_there_preset', [True, False])
@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_preset_get(preset_stub, patch_db_preset_get, is_there_preset):
    patch_db_preset_get.return_value = PresetTable(id='preset_id', name='small', cores=2, memory=1234, disk_size=123456678876) if is_there_preset else None
    exp_result = resource_preset_pb2.ResourcePreset(
        preset_id='preset_id',
        name='small',
        cores=2,
        memory=1234,
        disk_size=123456678876) if is_there_preset else grpc.StatusCode.NOT_FOUND
    try:
        preset = preset_stub.Get(
            preset_service.GetResourcePresetRequest(preset_id='preset_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
        if is_there_preset:
            assert preset == exp_result
        else:
            assert False, 'We should never be here'
    except grpc.RpcError as error:
        assert error.code() == exp_result


@mark.parametrize('preset_count', [0, 1, 2])
@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_preset_list(preset_stub, patch_db_preset_list, preset_count):
    preset_list = []
    exp_result_list = []
    for i in range(preset_count):
        preset_list.append(PresetTable(id=str(i), name='small', cores=2, memory=1234, disk_size=123456678876))
        exp_result_list.append(resource_preset_pb2.ResourcePreset(
            preset_id=str(i),
            name='small',
            cores=2,
            memory=1234,
            disk_size=123456678876))
    patch_db_preset_list.return_value = preset_list
    exp_result = preset_service.ListResourcePresetsResponse(resource_presets=exp_result_list)

    result = preset_stub.List(
        preset_service.ListResourcePresetsRequest(),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert result == exp_result
