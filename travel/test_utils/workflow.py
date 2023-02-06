# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import gevent
import pytz
from hamcrest import assert_that, equal_to
from hamcrest.core.base_matcher import BaseMatcher
from mongoengine import (
    StringField, IntField, ListField, EmbeddedDocumentField, EmbeddedDocument, DynamicField, MapField
)

from common.db.mongo.base import RaspDocument
from travel.rasp.library.python.common23.utils.code_utils import ContextManagerAsDecorator
from common.utils.date import MSK_TZ
from common.workflow import registry
from common.workflow.process import StateAction, Process
from common.workflow.scheme import load_scheme
from common.workflow.sleep import WaitAction
from common.workflow.utils import nothing, get_by_dotted_path


class Sleep(StateAction):
    def do(self, data, t, event='ok', *args, **kwargs):
        gevent.sleep(t)
        return event


LOCK_UPDATE_INTERVAL = 0.1

SCHEME = load_scheme({
    'states': {
        'initial': {
            'do': {
                'action': Sleep,
                'args': [0.1]
            },
            'transitions': {
                'ok': 'ok',
                'some_event': 'some_state',
                'some_ext_event': 'some_state',
                'wait_some_event': 'wait_for_some_state',
                'inter_event': 'interrupted',
            },
            'interruptable_by': 'inter_event',
        },
        'wait_for_some_state': {
            'do': {
                'action': WaitAction,
                'args': (timedelta(seconds=1),)
            },
            'transitions': {
                'ok': 'some_state',
            },
        },
        'some_state': {
            'do': {
                'action': Sleep,
                'args': [2]
            },
            'transitions': {
                'some_state_event': 'some_state_event_state_to',
                'inter_to_ok': 'ok',
            },
            'interruptable_by': 'inter_to_ok',
        },
        'ok': {
            'transitions': {
                'back': 'initial',
                'event_after_ok': 'some_state',
            },
            'interruptable_by': '*',
        },
        'interrupted': {},
        'some_state_event_state_to': {},
    },
    'lock_alive_time': 120,
    'lock_update_interval': LOCK_UPDATE_INTERVAL
})


def msk_as_utc(dt):
    return MSK_TZ.localize(dt).astimezone(pytz.UTC).replace(tzinfo=None)


DEFAULT_NAMESPACE = 'subdocs.1.process'


class SomeSubDocument(EmbeddedDocument):
    process = DynamicField()


class SomeDocument(RaspDocument):
    value = StringField()
    value2 = IntField(default=11)
    some_list = ListField(IntField())
    subdocs = MapField(EmbeddedDocumentField(SomeSubDocument))


def create_process(process_dict=None, acquire_lock=True, doc=None, scheme=SCHEME, max_retries=None, **kwargs):
    """ Шорткат для создания процесса для тестов"""

    # Специально делаем вложенность, чтобы всегда проверять работу механизма namespace-ов.
    # Соответствует DEFAULT_NAMESPACE
    subdoc = SomeSubDocument()
    if process_dict:
        subdoc.process = process_dict
        process_dict.setdefault('history', [])
        process_dict.setdefault('data', {})
        process_dict.setdefault('external_events', [])

    if not doc:
        doc = SomeDocument.objects.create(value='fortytwo', subdocs={
            '0': SomeSubDocument(),
            '1': subdoc,
            '2': SomeSubDocument()
        })
        doc.save()

    scheme = load_scheme(scheme)
    process = Process(scheme, document=doc, namespace=DEFAULT_NAMESPACE, max_retries=max_retries, **kwargs)
    if acquire_lock:
        process.document_locker._acquire_lock()

        if not process_dict:
            process.init_process()

    return process


def get_process_doc(process):
    """ Шорткат для получения копии документа процесса из базы """
    return SomeDocument.objects.get(id=process.document.id)


def assert_process_attr(process, path, obj, namespace=DEFAULT_NAMESPACE, default=nothing):
    """ Проверяем, что атрибут процесса установлен правильно - как на объекте в памяти, так и в базе. """
    assert_that(get_by_dotted_path(process.process, path, default),
                obj if isinstance(obj, BaseMatcher) else equal_to(obj))

    doc = get_process_doc(process)
    assert_that(get_by_dotted_path(doc, namespace + '.' + path, default),
                obj if isinstance(obj, BaseMatcher) else equal_to(obj))


def get_same_process(process):
    doc = get_process_doc(process)
    return Process(process.scheme, document=doc, namespace=process.namespace)


def create_process_document_locker(process=None):
    if process is None:
        process = create_process(acquire_lock=False)

    return process.document_locker


class UseRegistry(ContextManagerAsDecorator):
    registry_prev_state = None

    def __enter__(self):
        self.registry_prev_state = {}
        for name in registry.get_processes_names():
            self.registry_prev_state[name] = registry.get_process_params(name)

        registry.clear()

    def __exit__(self, exc_type, exc_val, exc_tb):
        registry.clear()
        for name, params in self.registry_prev_state.items():
            registry.add_process(name, model=params['model'], **params['kwargs'])


use_registry = UseRegistry()
