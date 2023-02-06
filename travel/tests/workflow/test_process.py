# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import copy
from datetime import datetime, timedelta

import mock
import pytest
from decorator import contextmanager
from hamcrest import assert_that, has_entry, contains, has_entries
from hamcrest.library.object import has_properties
from mongoengine import Document
from mongoengine.fields import DynamicField
from pymongo.collection import Collection
from pymongo.errors import ConnectionFailure
from pymongo.write_concern import DEFAULT_WRITE_CONCERN

from common.db.switcher import switcher
from common.test_utils.workflow import (
    SCHEME, Sleep, SomeDocument, msk_as_utc, create_process, get_process_doc, assert_process_attr, get_same_process,
)
from common.tester.utils.datetime import replace_now
from common.workflow.errors import UnableToUpdate
from common.workflow.process import Process, StateAction, ProcessEvent
from common.workflow.scheme import load_scheme


pytestmark = pytest.mark.mongouser


class UnhandledError(Exception):
    pass


class TestProcess(object):
    def test_collection(self):
        doc = SomeDocument()
        assert Process(SCHEME, doc).collection == doc.__class__._get_collection()

    def test_process(self):
        process = create_process(process_dict={'aaa': 1}, acquire_lock=False)
        assert process.process == {'aaa': 1, 'history': [], 'data': {}, 'external_events': []}

    def test_state(self):
        process = create_process(process_dict={'state': 'wow such state'})
        assert process.state == 'wow such state'

    def test_state_config(self):
        process = create_process(process_dict={'state': Process.EXCEPTION_STATE})
        assert process.state_config == {}
        process = create_process(process_dict={'state': 'some_state'})
        assert process.state_config == {
            'do': {
                'action': Sleep,
                'args': [2]
            },
            'transitions': {
                'some_state_event': 'some_state_event_state_to',
                'inter_to_ok': 'ok',
            },
        }

    def test_get_transition_state(self):
        process = create_process(process_dict={'state': 'some_state'})
        assert process._get_transition_state(ProcessEvent('some_state_event')) == 'some_state_event_state_to'
        assert process._get_transition_state(Process.EXCEPTION_EVENT) == Process.EXCEPTION_STATE
        assert process._get_transition_state(ProcessEvent('invalid_event')) is None

    def test_get_transition_state_exception_state(self):
        process = create_process(process_dict={'state': Process.EXCEPTION_STATE})
        assert process._get_transition_state(ProcessEvent('some_state_event')) == Process.EXCEPTION_STATE

    def test_get_namespace(self):
        process = create_process(process_dict={'data': {'so data': 'much doge'}})
        assert process.get_namespace('') == process.process
        assert process.get_namespace('data') == {'so data': 'much doge'}

    def test_terminal_states(self):
        process = create_process()
        assert set(process.terminal_states) == {'ok', 'interrupted', Process.EXCEPTION_STATE,
                                                'some_state_event_state_to'}

        scheme = copy.deepcopy(SCHEME)
        scheme['states'][Process.EXCEPTION_STATE] = {'do': lambda: 'noproblem'}
        scheme['states']['some_state']['transitions']['ok'] = Process.EXCEPTION_STATE
        process = create_process(scheme=scheme)
        assert set(process.terminal_states) == {'ok', 'interrupted', 'some_state_event_state_to'}

    def test_init_process(self):
        process = create_process(acquire_lock=False)
        assert not process.process

        process.document_locker._acquire_lock()
        process.init_process()

        # проверяем правильное состояние самого объекта + данных в базе
        assert_process_attr(process, 'state', 'initial')

        # повторный init_process не должен ничего менять
        with mock.patch.object(Process, '_update') as m_update:
            process.init_process()

            assert_process_attr(process, 'state', 'initial')
            assert len(m_update.call_args_list) == 0

    def test_init_process_data(self):
        process = create_process(acquire_lock=False, process_dict={'state': 'start_sate'})
        process.document_locker._acquire_lock()
        process.update({'$unset': {'data': 1}})
        assert 'data' not in process.process

        process.init_process()

        assert_process_attr(process, 'state', 'start_sate')
        assert_process_attr(process, 'data', {})

    @pytest.mark.parametrize('event', [
        ProcessEvent('some_ext_event', doc_update={'set__value2': 42}, is_external=False),
        ProcessEvent('some_ext_event', doc_update={'set__value2': 42}, is_external=True),
        ProcessEvent('some_ext_event', doc_update={'set__value2': 42}, is_external=True, params='some event params'),
    ])
    @replace_now(datetime(2010, 2, 3, 15, 30, 42))
    def test_receive_event(self, event):
        process = create_process({
            'state': 'initial',
            'data': {'v': 1},
            'history': [{'msg': 1}]
        })

        with mock.patch.object(process, '_log_history') as m_log:
            process._receive_event(event)
            assert process.state == 'some_state'
            assert process.document.value2 == 42
            assert_process_attr(process, 'external_event', None, default=None)
            assert_process_attr(process, 'data', {})
            assert_process_attr(process, 'event_params', event.params, default=None)

            history_expected = {
                'state': 'initial',
                'event': event.name,
                'state_to': 'some_state',
                'dt': msk_as_utc(datetime(2010, 2, 3, 15, 30, 42)),
                'lock_uid': process.lock_uid,
                'data': {'v': 1},
            }
            if event.is_external:
                history_expected.update({'external_event': event.is_external})

            assert len(process.process['history']) == 1, 'В history должно быть только то, что было раньше'
            assert m_log.called_once_with(history_expected)

    def test_receive_event_bad(self):
        process = create_process()
        with pytest.raises(ValueError):
            process._receive_event(ProcessEvent('unknown_event'))
        assert process.state == 'initial'

    def test_fetch_external_event(self):
        process = create_process()
        event = process._get_next_processable_event()
        assert event is None

        process = create_process({'external_events': [{'name': 'event_after_ok'}], 'state': 'ok'})
        event = process._get_next_processable_event()
        assert event.name == 'event_after_ok'
        assert event.params is None
        assert event.doc_update is None


class TestEventQueue(object):
    @replace_now(datetime(2018, 7, 25, 8, 30, 00))
    def test_send_external_event(self):
        process = create_process()
        ext_process = get_same_process(process)
        assert_process_attr(ext_process, 'external_events', [])

        ext_process.send_external_event('some_ext_event', params='external_event_params')
        process = get_same_process(process)
        assert_process_attr(ext_process, 'external_events', contains(has_entries({
            'name': 'some_ext_event', 'params': 'external_event_params', 'doc_update': None,
            'is_external': True, 'created_at': msk_as_utc(datetime(2018, 7, 25, 8, 30, 00)),
        })))
        assert_process_attr(process, 'external_events', contains(has_entries({
            'name': 'some_ext_event', 'params': 'external_event_params', 'doc_update': None,
            'is_external': True, 'created_at': msk_as_utc(datetime(2018, 7, 25, 8, 30, 00)),
        })))

    def test_double_send_external_event(self):
        process = create_process()
        process.send_external_event('some_ext_event')
        assert_process_attr(process, 'external_events', contains(has_entries({
            'name': 'some_ext_event', 'params': None, 'doc_update': None,
            'is_external': True
        })))

        process.send_external_event('some_ext_event', {'tickets': [0, 1]})
        assert_process_attr(process, 'external_events', contains(
            has_entries({'name': 'some_ext_event', 'params': None, 'doc_update': None,
                         'is_external': True}),
            has_entries({'name': 'some_ext_event', 'params': {'tickets': [0, 1]}, 'doc_update': None,
                         'is_external': True})
        ))

        process.send_external_event('some_event')
        assert_process_attr(process, 'external_events', contains(
            has_entries({'name': 'some_ext_event', 'params': None, 'doc_update': None,
                         'is_external': True}),
            has_entries({'name': 'some_ext_event', 'params': {'tickets': [0, 1]}, 'doc_update': None,
                         'is_external': True}),
            has_entries({'name': 'some_event', 'params': None, 'doc_update': None,
                         'is_external': True})
        ))

        process.send_external_event('some_event')
        assert_process_attr(process, 'external_events', contains(
            has_entries({'name': 'some_ext_event', 'params': None, 'is_external': True}),
            has_entries({'name': 'some_ext_event', 'params': {'tickets': [0, 1]},
                         'is_external': True}),
            has_entries({'name': 'some_event', 'params': None, 'doc_update': None,
                         'is_external': True}),
            has_entries({'name': 'some_event', 'params': None, 'doc_update': None,
                         'is_external': True})
        ))

    def test_simple_process_external_event(self):
        process = create_process(acquire_lock=False)
        with process.document_locker():
            process.init_process()
            process.send_external_event('some_other_event')
            process.send_external_event('some_ext_event')
            assert_that(process.process['external_events'], contains(has_entry('name', 'some_other_event'),
                                                                     has_entry('name', 'some_ext_event')))

            process._process_state()

        assert process.state == 'some_state'
        assert_that(process.process['external_events'], contains(has_entry('name', 'some_other_event')))
        assert_that(process.process['received_event'], has_entry('name', 'some_ext_event'))

    def test_process_external_event_remain(self):
        process = create_process(acquire_lock=False)
        with process.document_locker():
            process.init_process()
            process.send_external_event('some_other_event')
            assert_that(process.process['external_events'], contains(has_entry('name', 'some_other_event')))

            process._process_state()

        assert process.state == 'ok'
        assert_that(process.process['external_events'], contains(has_entry('name', 'some_other_event')))
        assert_that(process.process['received_event'], has_entries({'name': 'ok', 'is_external': False}))

    def test_clear_events_in_terminal_state(self):
        process = create_process(acquire_lock=False)
        with process.document_locker():
            process.init_process()
            process.update({'$set': {'state': 'ok'}})
            process.send_external_event('some_other_event')
            process._process_state()

        assert process.state == 'ok'
        assert process.process['external_events'] == []

    @pytest.mark.parametrize('process_dict, allow_send_to_empty_process', [
        ({'data': {}}, True),
        ({'data': {}}, False),
        ({'state': None}, True),
        ({'state': None}, False),
    ])
    def test_send_event_to_empty_process(self, process_dict, allow_send_to_empty_process):
        process = create_process(process_dict=process_dict)
        process.send_external_event('some_ext_event', allow_send_to_empty_process=allow_send_to_empty_process)

        process = get_same_process(process)
        if allow_send_to_empty_process:
            assert process.process.get('state') is None
        else:
            assert process.state == process.EXCEPTION_STATE
        assert_process_attr(process, 'external_events', contains(has_entries({
            'name': 'some_ext_event', 'is_external': True,
        })))


class TestProcessRun(object):
    """ tests for Process.run """

    def test_plain_run(self):
        process = create_process(process_dict={}, acquire_lock=False)
        with mock.patch.object(process.document_locker, '_acquire_lock',
                               wraps=process.document_locker._acquire_lock) as m_acquire_lock, \
                mock.patch.object(process.document_locker, '_update_lock',
                                  wraps=process.document_locker._update_lock):

            assert len(m_acquire_lock.call_args_list) == 0

            assert not process.document_locker.is_locked()
            process.run()
            assert len(m_acquire_lock.call_args_list) == 1
            m_acquire_lock.assert_called_once_with()
            assert not process.document_locker.is_locked()
            assert process.state == 'ok'

    def test_external_event(self):
        process = create_process(process_dict={'state': 'some_state', 'history': [], 'data': {}}, acquire_lock=False)
        assert process.state == 'some_state'
        process.send_external_event('inter_to_ok')
        process.run()
        assert process.state == 'ok'

    @replace_now(datetime(2018, 7, 25, 8, 30, 00))
    def test_run_till_wait_action(self):
        scheme = copy.deepcopy(SCHEME)
        scheme['states']['initial']['do']['kwargs'] = {'event': 'wait_some_event'}
        process = create_process(process_dict={}, acquire_lock=False, scheme=scheme)
        process.run()
        assert process.state == 'some_state'
        assert process.process['wait_till'] == datetime(2018, 7, 25, 5, 30, 1)

    def test_restart_process(self):
        scheme = copy.deepcopy(SCHEME)
        scheme['states']['initial']['do']['kwargs'] = {'event': 'wait_some_event'}
        process = create_process(process_dict={}, acquire_lock=False, scheme=scheme,
                                 max_lifetime=timedelta(seconds=0))
        state = process.run()
        assert state == Process.NEED_RESCHEDULE_STATE
        assert process.state == 'wait_for_some_state'


class TestProcessUpdate(object):
    """ tests for Process.update """

    def test_update(self):
        process = create_process({'a': 1})
        doc_id = process.document.id
        process.update(
            {'$set': {'a': 42}},
            {'set__value': '100500', 'push_all__some_list': [1, 2, 3]}
        )

        assert_process_attr(process, 'a', 42)
        assert process.document.id == doc_id
        assert process.document.value == '100500'
        assert process.document.value2 == 11
        assert process.document.some_list == [1, 2, 3]

        doc = get_process_doc(process)
        assert doc.value == '100500'
        assert doc.some_list == [1, 2, 3]

    def test_namespace_changed(self):
        process = create_process({'a': 1})
        process.update(
            {'$set': {'a': 42}},
            {'set__value': '100500', 'push_all__some_list': [1, 2, 3]},
            namespace='wow.namespace'
        )

        assert_process_attr(process, 'a', 1)
        assert_process_attr(process, 'wow.namespace.a', 42)
        assert process.document.value == '100500'

    def test_error_handling(self):
        process = create_process(acquire_lock=False)
        with pytest.raises(UnableToUpdate):
            process.update({'$set': {'a': 1}})

        process.document.delete()
        with pytest.raises(UnableToUpdate):
            process.update({'$set': {'a': 1}})

    @mock.patch('common.utils.try_hard.sleep')
    def test_retry(self, m_sleep):
        process = create_process(acquire_lock=False, max_retries=3)
        with mock.patch.object(Collection, 'find_one_and_update', auto_spec=True) as m_find_one_and_update:
            m_find_one_and_update.side_effect = [
                ConnectionFailure('Waaagh!'), ConnectionFailure('Waaagh!'), {'_id': 42}
            ]
            process.update({'$set': {'a': 1}})
            assert process.document.id == 42
            assert m_find_one_and_update.call_count == 3


class TestProcessStateActionRunner(object):
    """ tests for Process.run_state_action """

    def test_complex_action_format(self):
        m_action = mock.MagicMock(wraps=StateAction, autospec=StateAction)
        scheme = {'states': {'initial': {'do': {
            'action': m_action,
            'args': [6, 5],
            'kwargs': {'foo': 'boo'}
        }}}}

        m_do = mock.MagicMock(return_value='event42')
        with mock.patch.object(StateAction, 'do', m_do):
            process = create_process({'state': 'initial', 'data': {'v': 123}, 'history': []}, scheme=scheme)
            event = process.run_state_action()
            assert_that(event, has_properties({'name': 'event42', 'is_external': False}))
            m_action.assert_called_once_with(process.document, 'data', process)
            m_do.assert_called_once_with({'v': 123}, 6, 5, foo='boo')

    def test_simple_action_fromat(self):
        m_action = mock.MagicMock(wraps=StateAction, autospec=StateAction)
        scheme = {'states': {'initial': {'do': m_action}}}
        m_do = mock.MagicMock(return_value='event43')
        with mock.patch.object(StateAction, 'do', m_do):
            process = create_process({'state': 'initial', 'data': {'v': 123}, 'history': []}, scheme=scheme)
            event = process.run_state_action()
            assert_that(event, has_properties({'name': 'event43', 'is_external': False}))
            m_action.assert_called_once_with(process.document, 'data', process)
            m_do.assert_called_once_with({'v': 123})

    def test_run_with_event_params(self):
        m_action = mock.MagicMock(wraps=StateAction, autospec=StateAction)
        scheme = {'states': {'initial': {'do': m_action}}}
        m_do = mock.MagicMock(return_value='event43')
        with mock.patch.object(StateAction, 'do', m_do):
            process = create_process(
                {'state': 'initial', 'data': {'v': 123}, 'history': [], 'event_params': 'some event params'},
                scheme=scheme
            )
            event = process.run_state_action()
            assert_that(event, has_properties({'name': 'event43', 'is_external': False}))
            m_action.assert_called_once_with(process.document, 'data', process)
            m_do.assert_called_once_with({'v': 123}, event_params='some event params')

    def test_error_handling(self):
        class ErrorAction(StateAction):
            def do(self, data, *args, **kwargs):
                raise UnhandledError

        scheme = {
            'states': {
                'initial': {
                    'do': {
                        'action': ErrorAction,
                        'args': [6, 5],
                        'kwargs': {'foo': 'boo'}
                    }
                }
            }
        }
        process = create_process({'state': 'initial', 'data': {'v': 123}, 'history': []}, scheme=scheme)

        assert process.run_state_action() == Process.EXCEPTION_EVENT


class TestStateAction(object):
    def test_call(self):
        process = create_process({'internal': {'v': 111}})
        action = StateAction(process.document, 'internal', process)

        with mock.patch.object(StateAction, 'do') as m_do:
            action(1, 2, b=42)
            m_do.assert_called_once_with({'v': 111}, 1, 2, b=42)

    def test_update_data(self):
        process = create_process({'internal': {'v': 111, 'w': 222}})
        action = StateAction(process.document, 'internal', process)
        d = {'$set': {'x': 333}, '$unset': {'v': 1}}
        action.update_data(d)
        assert process.process['internal'] == {'x': 333, 'w': 222}

    def test_update_data_idempotent(self):
        class IdempotentAction(StateAction):
            idempotent = True

        process = mock.MagicMock()
        action = IdempotentAction(process.document, 'internal', process)
        action.update_data({'foo': 1})
        assert process.update.call_args_list == [
            mock.call({u'foo': 1}, namespace=u'internal', writeConcern=DEFAULT_WRITE_CONCERN)
        ]

    def test_update_data_not_idempotent(self):
        process = mock.MagicMock()
        action = StateAction(process.document, 'internal', process)
        action.update_data({'foo': 1})
        assert process.update.call_args_list == [
            mock.call({u'foo': 1}, namespace=u'internal')
        ]


def test_sync_db():
    class SomeDocument(Document):
        process = DynamicField(default={})

    doc = SomeDocument.objects.create()

    class SomeAction(StateAction):
        def do(self, data, *args, **kwargs):
            return 'event'

    class Locker(object):
        document = doc

        def build_lock_query(self):
            return {
                '_id': self.document.id
            }

        def is_locked(self):
            return True

        @contextmanager
        def __call__(self):
            yield

        def update_document_raw(self, *args, **kwargs):
            return self.document

    process = Process(load_scheme({
        'initial_state': 'start',
        'states': {
            'start': {
                'do': SomeAction,
                'transitions': {
                    'event': 'middle'
                },
            },
            'middle': {
                'do': SomeAction,
                'transitions': {
                    'event': 'done'
                },
            },
            'done': {}
        }
    }), doc)

    with mock.patch.object(switcher, 'sync_with_lazy_reconnect') as m_sync_reconnect:
        process.run()
        assert m_sync_reconnect.call_count == 2
