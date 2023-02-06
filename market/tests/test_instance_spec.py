from market.front.tools.service_updater.helpers import (
    find_prepare_script,
    update_or_create_prepare_script,
    remove_prepare_script,
    find_container_script,
    update_container_script,
    remove_container_script,
    BadUpdateOrCreateMatcher
)
from .assert_idempotent import assert_idempotent
from .mocks import create_service


def test_find_prepare_script():
    first_section = {"name": "first", "command": ["/bin/bash", "-c", "first_script"]}
    second_section = {"name": "second", "command": ["/bin/bash", "-c", "second_script"]}

    service = create_service({"initContainers": [first_section, second_section]})

    assert find_prepare_script(service, lambda name, _: name == 'first') == first_section
    assert find_prepare_script(service, lambda _, command: command == 'first_script') == first_section

    assert find_prepare_script(service, lambda name, _: name == 'second') == second_section
    assert find_prepare_script(service, lambda _, command: command == 'second_script') == second_section


def test_update_or_create_prepare_script():
    old_prepare = {"name": "old_prepare", "command": ["/bin/bash", "-c", "old_prepare_script"]}

    service = create_service({"initContainers": [old_prepare]})

    assert_idempotent(service, lambda: update_or_create_prepare_script(
        service, lambda name, _: name == 'old_prepare', 'old_prepare', 'updated_prepare_script'
    ))

    assert_idempotent(service, lambda: update_or_create_prepare_script(
        service, lambda name, _: name == 'newborn', 'newborn', 'newborn_prepare_script'
    ))

    diff = '{}'.format(service.runtime_attrs.diff)

    assert diff == \
           "{update: {'instance_spec': {update: {'initContainers': {0: " \
           "{update: {'command': {insert: [(2, 'updated_prepare_script')], delete: [2]}}}, " \
           "insert: [(1, {'command': ['/bin/bash', '-c', 'newborn_prepare_script'], 'name': 'newborn'})]}}}}}" \
           or diff == \
           "{update: {'instance_spec': {update: {'initContainers': {0: " \
           "{update: {'command': {delete: [2], insert: [(2, 'updated_prepare_script')]}}}, " \
           "insert: [(1, {'command': ['/bin/bash', '-c', 'newborn_prepare_script'], 'name': 'newborn'})]}}}}}"


def test_update_or_create_prepare_script_bad_matcher():
    service = create_service({"initContainers": []})

    try:
        update_or_create_prepare_script(service, lambda name, _: name == 'old', 'new', 'command')
        test_result = "Bad name matcher not failed"
    except BadUpdateOrCreateMatcher:
        test_result = True

    assert test_result is True

    try:
        update_or_create_prepare_script(service, lambda _, command: command == 'old', 'new', 'command')
        test_result = "Bad command matcher not failed"
    except BadUpdateOrCreateMatcher:
        test_result = True

    assert test_result is True


def test_remove_prepare_script():
    first_section = {"name": "first", "command": ["/bin/bash", "-c", "first_script"]}
    second_section = {"name": "second", "command": ["/bin/bash", "-c", "second_script"]}
    thrid_section = {"name": "third", "command": ["/bin/bash", "-c", "third_script"]}

    service = create_service({"initContainers": [first_section, second_section, thrid_section]})

    assert_idempotent(service, lambda: remove_prepare_script(service, lambda name, _: name == 'first'))
    assert_idempotent(service, lambda: remove_prepare_script(service, lambda _, command: command == 'second_script'))

    assert '{}'.format(service.runtime_attrs.diff) == \
           "{update: {'instance_spec': {update: {'initContainers': {delete: [1, 0]}}}}}"


def test_find_container_script():
    first_section = {"name": "first", "command": ["/bin/bash", "-c", "first_script"]}
    second_section = {"name": "second", "command": ["/bin/bash", "-c", "second_script"]}

    service = create_service({"containers": [first_section, second_section]})

    assert find_container_script(service, lambda name, _: name == 'first') == first_section
    assert find_container_script(service, lambda _, command: command == '/bin/bash -c first_script') == first_section

    assert find_container_script(service, lambda name, _: name == 'second') == second_section
    assert find_container_script(service, lambda _, command: command == '/bin/bash -c second_script') == second_section


def test_update_container_script():
    first_section = {"name": "first", "command": ["/bin/bash", "-c", "first_script"]}
    second_section = {"name": "second", "command": ["/bin/bash", "-c", "second_script"]}

    service = create_service({"containers": [first_section, second_section]})

    assert_idempotent(service, lambda: update_container_script(
        service, lambda name, _: name == 'first', 'not_so_first', ["/bin/bash", "-c", "updated_script"]
    ))

    assert_idempotent(service, lambda: update_container_script(
        service, lambda _, command: command == '/bin/bash -c second_script', 'updated_name', ["/bin/bash", "-c", "updatet_script"]
    ))

    assert_idempotent(service, lambda: update_container_script(
        service, lambda name, _: name == 'not existant', 'never', ["/bin/bash", "-c", "never"]
    ))

    diff = '{}'.format(service.runtime_attrs.diff)

    assert diff == \
           "{update: {'instance_spec': {update: {'containers': {" \
           "0: {update: {'command': {delete: [2], insert: [(2, 'updated_script')]}, 'name': 'not_so_first'}}, " \
           "1: {update: {'command': {delete: [2], insert: [(2, 'updatet_script')]}, 'name': 'updated_name'}}}}}}}" \
           or diff == \
           "{update: {'instance_spec': {update: {'containers': {" \
           "0: {update: {'command': {insert: [(2, 'updated_script')], delete: [2]}, 'name': 'not_so_first'}}, " \
           "1: {update: {'command': {insert: [(2, 'updatet_script')], delete: [2]}, 'name': 'updated_name'}}}}}}}"


def test_remove_container_script():
    first_section = {"name": "first", "command": ["/bin/bash", "-c", "first_script"]}
    second_section = {"name": "second", "command": ["/bin/bash", "-c", "second_script"]}

    service = create_service({"containers": [first_section, second_section]})

    assert_idempotent(service, lambda: remove_container_script(
        service, lambda name, _: name == 'first'
    ))

    assert '{}'.format(service.runtime_attrs.diff) == \
           "{update: {'instance_spec': {update: {'containers': {delete: [0]}}}}}"
