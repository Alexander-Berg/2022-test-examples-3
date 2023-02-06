# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import mock
import pytest

from common.test_utils.workflow import SomeDocument, DEFAULT_NAMESPACE, create_process, use_registry
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.logging import AddContextFilter
from common.workflow import registry
from common.workflow.process import StateAction, ProcessEvent
from common.workflow.reviver import revive
from common.workflow.scheme import load_scheme

LOCK_ALIVE_TIME = 100


class OkAction(StateAction):
    def do(self, data, *args, **kwargs):
        return 'ok'


SCHEME = load_scheme({
    'states': {
        'initial': {
            'do': OkAction,
            'transitions': {
                'ok': 'some_state'
            }
        },
        'some_state': {
            'do': OkAction,
            'transitions': {
                'ok': 'ok'
            }
        },
        'ok': {}
    },
    'lock_alive_time': LOCK_ALIVE_TIME,
})

NOW = datetime(2010, 2, 3, 15, 30, 0)
LOCK_EXPIRED = NOW - timedelta(seconds=LOCK_ALIVE_TIME + 1)
LOCK_NOT_EXPIRED = NOW - timedelta(seconds=LOCK_ALIVE_TIME - 1)
NOW_UTC = NOW - timedelta(hours=3)
NO_PROCESS = object()


@pytest.mark.mongouser
@use_registry
@pytest.mark.parametrize('process_data, lock_time, should_be_revived', [
    # только создали, нужно подхватить
    (NO_PROCESS, LOCK_NOT_EXPIRED, True),

    # процесс уже завершен
    ({'state': 'ok'}, LOCK_NOT_EXPIRED, False),

    # незавершенный процесс в начальной стадии
    (None, LOCK_NOT_EXPIRED, False),

    # незавершенный процесс
    ({'state': 'some_state'}, LOCK_NOT_EXPIRED, False),

    # новый документ, процесс даже не запускался
    (NO_PROCESS, LOCK_EXPIRED, True),

    # давно завершенный процесс, не воскрешать
    ({'state': 'ok'}, LOCK_EXPIRED, False),

    # незавершенный процесс в начальной стадии
    (None, LOCK_EXPIRED, True),

    # незавершенный процесс
    ({'state': 'some_state'}, LOCK_EXPIRED, True),
    # незавершенный процесс в ожидании
    ({'state': 'some_state', 'wait_till': NOW_UTC + timedelta(seconds=9)}, LOCK_EXPIRED, False),
    # незавершенный процесс, ожидание окончено
    ({'state': 'some_state', 'wait_till': NOW_UTC - timedelta(seconds=9)}, LOCK_EXPIRED, True),

    # новые внешние события
    ({'state': 'ok', 'external_events': [ProcessEvent('ee')], 'external_events_count': 1}, LOCK_EXPIRED, True),
    ({'state': 'ok', 'external_events': [], 'external_events_count': 0}, LOCK_EXPIRED, False),
])
def test_revive(process_data, lock_time, should_be_revived):
    with replace_setting('ENVIRONMENT_NOW', lock_time):
        if process_data == NO_PROCESS:
            document = SomeDocument.objects.create()
        else:
            document = create_process(process_data, scheme=SCHEME).document

    registry.add_process('myprocess', SCHEME, SomeDocument, namespace=DEFAULT_NAMESPACE)
    with replace_setting('ENVIRONMENT_NOW', NOW), \
            mock.patch.object(registry.run_process, 'apply_async') as m_apply_async:
        revive('myprocess')
    if should_be_revived:
        assert m_apply_async.called
        assert m_apply_async.call_args_list == [mock.call(['myprocess', str(document.id), {}])]
    else:
        assert not m_apply_async.called


@pytest.mark.mongouser
@use_registry
def test_log_context(caplog):
    with replace_setting('ENVIRONMENT_NOW', LOCK_NOT_EXPIRED):
        document = SomeDocument.objects.create()

    registry.add_process('myprocess', SCHEME, SomeDocument, namespace=DEFAULT_NAMESPACE)
    with replace_setting('ENVIRONMENT_NOW', NOW), \
            mock.patch.object(registry.run_process, 'apply_async') as m_apply_async:
        caplog.handler.filters.append(AddContextFilter(as_dict=True))
        revive('myprocess', log_context_getter=lambda o: {'_id': str(o['_id'])})

    expected_log_context = {'_id': str(document.id)}
    assert m_apply_async.called
    assert m_apply_async.call_args_list == [mock.call(['myprocess', str(document.id), expected_log_context])]
    assert any(getattr(r, 'context', None) == expected_log_context for r in caplog.records)
