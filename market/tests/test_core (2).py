# coding: utf-8

import pytest
from collections import namedtuple
from market.idx.admin.mi_agent.lib.core.core import Core

Check = namedtuple('Check', ['check', 'name'])
check_true = Check(check=lambda: True, name='true')
check_false = Check(check=lambda: False, name='false')


@pytest.mark.parametrize("test_input, expected", [
    (  # хотя бы один сенсор сработал + условия позволяют - есть действия
        {
            'sensors': [check_false, check_true],
            'conditions': [check_true, check_true]
        },
        True
    ),
    (  # оба сенсора в норме - не зовем действия
        {
            'sensors': [check_true, check_true],
            'conditions': [check_true, check_true]
        },
        False
    ),
    (  # сработал сенсор, но хотя бы одно условие не соблюдается - нет действия
        {
            'sensors': [check_false, check_true],
            'conditions': [check_false, check_true]
        },
        False
    ),
    (
        {
            'sensors': [],
            'conditions': []
        },
        False
    ),
    (  # сработал сенсор, в порядке условия, но dry_run - нет действия
        {
            'sensors': [check_true],
            'conditions': [check_true, check_true],
            'dry_run': True
        },
        False
    ),
])
def test_core(test_input, expected):
    sensors = test_input['sensors']
    conditions = test_input['conditions']
    dry_run = bool(test_input.get('dry_run'))

    action_called = [False]

    def call_action(called):
        called[0] = True

    Action = namedtuple('SwitchMaster', ['execute', 'name'])
    actions = [Action(execute=lambda: call_action(action_called), name='')]

    core = Core(sensors, conditions, actions, None)
    core.run_agent(dry_run)

    assert action_called[0] == expected
