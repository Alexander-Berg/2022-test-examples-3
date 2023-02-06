import pytest

from market.front.tools.service_updater.helpers import \
    find_sandbox_resource, get_resource_by_filename, \
    update_or_create_file_resource, remove_file_resource, \
    update_or_create_sandbox_resource, remove_sandbox_resource, \
    ResourceMismatch

from .assert_idempotent import assert_idempotent
from .mocks import create_sandbox_resource, create_sandbox_file, create_static_file, create_service


def test_find_sandbox_resource():
    service = create_service(resources={'sandbox_files': [
        create_sandbox_file('first.txt', 'resType', 'resId', 'taskId', 'taskType')
    ], 'static_files': []})

    res = create_sandbox_resource('resType', 'resId', 'taskId', 'taskType', 'url')
    res2 = create_sandbox_resource('resType', 'resId2', 'taskId2', 'taskType2', 'url2')
    res3 = create_sandbox_resource('resType2', 'resId2', 'taskId2', 'taskType2', 'url2')

    assert find_sandbox_resource(service, res) == {
        'local_path': 'first.txt',
        'resource_type': 'resType',
        'resource_id': 'resId',
        'task_type': 'taskType',
        'task_id': 'taskId',
        'is_dynamic': False
    }

    assert find_sandbox_resource(service, res2) == {
        'local_path': 'first.txt',
        'resource_type': 'resType',
        'resource_id': 'resId',
        'task_type': 'taskType',
        'task_id': 'taskId',
        'is_dynamic': False
    }

    assert find_sandbox_resource(service, res3) is None


def test_get_resource_by_filename():
    service = create_service(resources={'sandbox_files': [
        create_sandbox_file('sandbox.txt', 'resType', 'resId', 'taskId', 'taskType')
    ], 'static_files': [
        create_static_file('static.txt', 'first content'),
    ]})

    assert get_resource_by_filename(service, 'sandbox.txt') == {
        'local_path': 'sandbox.txt',
        'resource_type': 'resType',
        'resource_id': 'resId',
        'task_type': 'taskType',
        'task_id': 'taskId',
        'is_dynamic': False
    }

    assert get_resource_by_filename(service, 'static.txt') == {
        'local_path': 'static.txt',
        'content': 'first content',
        'is_dynamic': False
    }

    assert get_resource_by_filename(service, '') is None


def test_update_or_create_file_resource():
    service = create_service(resources={'static_files': [
        create_static_file('first.txt', 'first content')
    ]})

    assert_idempotent(service, lambda: update_or_create_file_resource(
        service, 'first.txt', 'updated content'
    ))

    assert_idempotent(service, lambda: update_or_create_file_resource(
        service, 'second.txt', 'new content'
    ))

    assert '{}'.format(service.runtime_attrs.diff) == \
           "{update: {'resources': {update: {'static_files': {0: " \
           "{update: {'content': 'updated content'}}, " \
           "insert: [(1, {'is_dynamic': False, 'content': 'new content', 'local_path': 'second.txt'})]}}}}}"


def test_remove_file_resource():
    service = create_service(resources={'sandbox_files': [], 'static_files': [
        create_static_file('first.txt', 'first content'),
        create_static_file('second.txt', 'second content'),
    ]})

    assert_idempotent(service, lambda: remove_file_resource(service, 'first.txt'))
    assert_idempotent(service, lambda: remove_file_resource(service, 'unknown.txt'))

    assert '{}'.format(service.runtime_attrs.diff) == \
           "{update: {'resources': {update: {'static_files': {delete: [0]}}}}}"


def test_remove_file_resource_bad_resource():
    service = create_service(resources={'sandbox_files': [
        create_sandbox_file('first.txt', 'resType', 'resId', 'taskId', 'taskType')
    ], 'static_files': []})

    with pytest.raises(ResourceMismatch):
        remove_file_resource(service, 'first.txt')


def test_update_or_create_sandbox_resource():
    service = create_service(resources={'sandbox_files': [
        create_sandbox_file('first.txt', 'resType', 'resId', 'taskId', 'taskType')
    ], 'static_files': []})

    assert_idempotent(service, lambda: update_or_create_sandbox_resource(
        service, 'first.txt', create_sandbox_resource('resTypeUpd', 'resIdUpd', 'taskIdUpd', 'taskTypeUpd', 'urlUpd')
    ))

    assert_idempotent(service, lambda: update_or_create_sandbox_resource(
        service, 'second.txt', create_sandbox_resource('resType2', 'resId2', 'taskId2', 'taskType2', 'url2')
    ))

    assert '{}'.format(service.runtime_attrs.diff) == \
           "{update: {'resources': {update: {'sandbox_files': {0: " \
           "{update: {'task_type': 'taskTypeUpd', 'resource_type': 'resTypeUpd', 'task_id': 'taskIdUpd', 'resource_id': 'resIdUpd'}}, " \
           "insert: [(1, {'task_type': 'taskType2', 'task_id': 'taskId2', 'resource_id': 'resId2', 'is_dynamic': False, 'local_path': 'second.txt', 'resource_type': 'resType2'})]}}}}}"


def test_update_or_create_sandbox_resource_bad_resource():
    service = create_service(resources={'sandbox_files': [], 'static_files': [
        create_static_file('first.txt', 'first content')
    ]})

    with pytest.raises(ResourceMismatch):
        update_or_create_sandbox_resource(service, 'first.txt', create_sandbox_resource('resTypeUpd', 'resIdUpd', 'taskIdUpd', 'taskTypeUpd', 'urlUpd'))


def test_remove_sandbox_resource():
    service = create_service(resources={'sandbox_files': [
        create_sandbox_file('first.txt', 'resType', 'resId', 'taskId', 'taskType'),
        create_sandbox_file('second.txt', 'resType2', 'resId2', 'taskId2', 'taskType2'),
    ], 'static_files': []})

    assert_idempotent(service, lambda: remove_sandbox_resource(service, 'first.txt'))

    assert '{}'.format(service.runtime_attrs.diff) == \
           "{update: {'resources': {update: {'sandbox_files': {delete: [0]}}}}}"


def test_remove_sandbox_resource_bad_resource():
    service = create_service(resources={'sandbox_files': [], 'static_files': [
        create_static_file('first.txt', 'first content')
    ]})

    with pytest.raises(ResourceMismatch):
        remove_sandbox_resource(service, 'first.txt')
