import pytest
from marshmallow import ValidationError

from common.workflow.process import StateAction
from common.workflow.scheme import load_scheme


class Action(StateAction):
    def do(self, data, *args, **kwargs):
        return 'ok'


@pytest.fixture
def scheme():
    return {
        'initial_state': 'start',
        'states': {
            'start': {
                'do': Action,
                'transitions': {
                    'ok': 'done',
                    'not_ok': 'failed',
                }
            },
            'done': {
                'do': {
                    'action': Action,
                    'args': [1, 2, 3],
                    'kwargs': {'a': 42}
                }
            },
            'failed': {}
        }
    }


def test_valid_logic(scheme):
    with pytest.raises(ValidationError):
        load_scheme({})

    assert load_scheme(scheme) == {
        'initial_state': 'start',
        'states': {
            'start': {
                'do': {
                    'action': Action,
                },
                'transitions': {
                    'ok': 'done',
                    'not_ok': 'failed',
                },
            },
            'done': {
                'do': {
                    'action': Action,
                    'args': [1, 2, 3],
                    'kwargs': {'a': 42}
                },
                'transitions': {},
            },
            'failed': {'transitions': {}}
        },
        'lock_alive_time': 20.0,
        'lock_update_interval': 5.0
    }


def test_undefined_state(scheme):
    scheme['states'].pop('failed')
    with pytest.raises(ValidationError) as ex:
        load_scheme(scheme)
    assert 'failed' in str(ex) and 'undefined' in str(ex)


def test_unreachable_state(scheme):
    scheme['states']['start']['transitions'].pop('ok')
    with pytest.raises(ValidationError) as ex:
        load_scheme(scheme)
    assert 'done' in str(ex) and 'unreachable' in str(ex)
