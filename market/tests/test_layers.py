from market.front.tools.service_updater.helpers import update_layer
from .assert_idempotent import assert_idempotent
from .mocks import create_sandbox_resource, create_layer, create_service


def test_update_layer():
    service = create_service({'layersConfig': {'layer': [
        create_layer('ResType01', 'ResId01', 'TaskType01', 'TaskId01', 'url01', 'id01'),
        create_layer('ResType02', 'ResId02', 'TaskType02', 'TaskId02', 'url02', 'id02'),
    ]}})

    res = create_sandbox_resource('ResType01', 'ResId01u', 'TaskType01u', 'TaskId01u', 'url01u')

    assert_idempotent(service, lambda: update_layer(service, lambda res_type, _: res_type == 'ResType01', res))

    assert '{}'.format(service.runtime_attrs.diff) == \
           "{update: {'instance_spec': {update: {'layersConfig': {update: {'layer': {0: " \
           "{update: {'url': ['url01u'], 'fetchableMeta': " \
           "{update: {'sandboxResource': {update: {'resourceId': 'ResId01u', 'taskId': 'TaskType01u', 'taskType': 'TaskId01u'}}}}}}}}}}}}}"
