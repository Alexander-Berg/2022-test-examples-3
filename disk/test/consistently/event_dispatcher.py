# -*- coding: utf-8 -*-
from collections import defaultdict

from test.base_suit import BaseTestCase
from mpfs.common.errors import EventError
from mpfs.engine.process import event_dispatcher
from mpfs.core.event_dispatcher.events import Event, subscribe


class CallCounter(object):
    def __init__(self):
        self.calls = 0

    def __call__(self, *args, **kwargs):
        self.calls += 1


class Test1Event(Event):
    required_fields = {'test_field_1': str}


class Test1SubEvent(Test1Event):
    required_fields = {'sub_field': int}


class Test1ReloadEvent(Test1Event):
    required_fields = {'test_field_1': int}


class Test2Event(Event):
    required_fields = ()


class BullshitEvent(Event):
    required_fields = ()


class EventsTestCase(BaseTestCase):
    def test_create_event(self):
        """Проверяет, что созданный ивент имеет пустые данные."""
        event = Test2Event()
        assert event.data == {}

    def test_required_fields(self):
        self.assertRaises(EventError, Test1Event)
        self.assertRaises(EventError, Test1Event, wrong_field='1')
        self.assertRaises(EventError, Test1Event, test_field_1=1)
        event = Test1Event(test_field_1='str', another_filed=5)
        assert event.data == {'test_field_1': 'str'}

    def test_required_fields_inheritance(self):
        assert Test1Event.required_fields == {'test_field_1': str}
        assert Test1SubEvent.required_fields == {'test_field_1': str, 'sub_field': int}

    def test_required_fields_reload(self):
        assert Test1Event.required_fields == {'test_field_1': str}
        assert Test1ReloadEvent.required_fields == {'test_field_1': int}


class EventDispatcherTestCase(BaseTestCase):
    def setup_method(self, method):
        event_dispatcher().__init__([Event, Test1Event, Test2Event, BullshitEvent, Test1SubEvent])

    def test_common_dispatch(self):
        cc = CallCounter()
        # подписываемся на событие
        event_dispatcher().subscribe(Test2Event, cc)

        event = Test2Event()
        # шлем события
        event.send()
        assert cc.calls == 1
        Test2Event().send()
        assert cc.calls == 2
        event.send()
        assert cc.calls == 3
        BullshitEvent().send()
        assert cc.calls == 3

    def test_several_observers(self):
        # несколько подписчиков слушают одно событие
        cc_1 = CallCounter()
        cc_2 = CallCounter()

        event_dispatcher().subscribe(Test2Event, cc_1)
        event_dispatcher().subscribe(Test2Event, cc_2)

        Test2Event().send()
        assert cc_1.calls == cc_2.calls == 1
        Test2Event().send()
        assert cc_1.calls == cc_2.calls == 2
        Test2Event().send()
        assert cc_1.calls == cc_2.calls == 3
        # шлем другое событие
        Test1Event(test_field_1='1').send()
        assert cc_1.calls == cc_2.calls == 3

    def test_subscribe_decorator(self):
        # тестируем подписку через декоратор
        class ActivateHandler(Exception):
            pass

        @subscribe(event_class=Test2Event)
        def callback(event):
            assert isinstance(event, Test2Event)
            raise ActivateHandler

        self.assertRaises(ActivateHandler, Test2Event().send)

    def test_cascade_subsribtion(self):
        # тестируем вызов обработчика, подписанного на событие-родителя
        cc_1, cc_2, cc_3 = CallCounter(), CallCounter(), CallCounter()

        event_dispatcher().subscribe(Event, cc_1)
        event_dispatcher().subscribe(Test1Event, cc_2)
        event_dispatcher().subscribe(Test1SubEvent, cc_3)

        event_1 = Event()
        event_2 = Test1Event(test_field_1='')
        event_3 = Test1SubEvent(test_field_1='', sub_field=1)

        assert cc_1.calls == cc_2.calls == cc_3.calls == 0
        event_3.send()
        assert cc_1.calls == 1
        assert cc_2.calls == 1
        assert cc_3.calls == 1
        event_2.send()
        assert cc_1.calls == cc_2.calls == 2
        assert cc_3.calls == 1
        event_1.send()
        assert cc_1.calls == 3
        assert cc_2.calls == 2
        assert cc_3.calls == 1
