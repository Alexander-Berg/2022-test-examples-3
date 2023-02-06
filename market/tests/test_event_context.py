# coding: utf-8

import json
import tempfile
import unittest

from market.idx.pylibrary.metrics.event import Event
from market.idx.pylibrary.metrics.event_context import EventContext


class EventContextTestCase(unittest.TestCase):

    def test_write_on_exit(self):
        with tempfile.NamedTemporaryFile('w+') as log_file:
            event = Event('test-event')
            event_context = EventContext(log_file=log_file, close_on_exit=False)

            with event_context:
                with event_context.write_event(event):
                    pass

                event_context.flush()
                events = list(read_events(log_file.name))
                self.assertEqual(len(events), 0)

            event_context.flush()
            events = list(read_events(log_file.name))
            self.assertDictEqual(events[0].to_dict(), event.to_dict())

    def test_write_immediately(self):
        with tempfile.NamedTemporaryFile('w+') as log_file:
            event = Event('test-event')
            event_context = EventContext(log_file=log_file, close_on_exit=False)

            with event_context:
                with event_context.write_event(event, write_immediately=True):
                    pass

                event_context.flush()
                events = list(read_events(log_file.name))
                self.assertDictEqual(events[0].to_dict(), event.to_dict())

            event_context.flush()
            events = list(read_events(log_file.name))
            self.assertDictEqual(events[0].to_dict(), event.to_dict())

    def test_write_context_additional_fields(self):
        with tempfile.NamedTemporaryFile('w+') as log_file:
            event = Event('test-event')
            fields = {'abc': 'test_field', 'qwer': 123}
            event_context = EventContext(log_file=log_file, close_on_exit=False, **fields)

            with event_context:
                with event_context.write_event(event):
                    pass
                event_context.new_attr = 'test_val'
                fields['new_attr'] = 'test_val'

            event_context.flush()
            expected = event.to_dict()
            expected.update(fields)
            events = list(read_events(log_file.name))
            self.assertDictEqual(events[0].to_dict(), expected)

    def test_write_event_additional_fields(self):
        with tempfile.NamedTemporaryFile('w+') as log_file:
            event1 = Event('test-event1')
            event2 = Event('test-event2')
            event_context = EventContext(log_file=log_file, close_on_exit=False)

            with event_context:
                with event_context.write_event(event1) as e1:
                    e1.new_attr1 = 'test_val1'
                with event_context.write_event(event2) as e2:
                    e2.new_attr2 = 'test_val2'

            event_context.flush()
            events = list(read_events(log_file.name))

            self.assertIn('new_attr1', events[0].to_dict())
            self.assertEqual(events[0].new_attr1, 'test_val1')
            self.assertNotIn('new_attr2', events[0].to_dict())

            self.assertIn('new_attr2', events[1].to_dict())
            self.assertEqual(events[1].new_attr2, 'test_val2')
            self.assertNotIn('new_attr1', events[1].to_dict())


def read_events(path):
    with open(path) as file_obj:
        for line in file_obj:
            if line:
                event_fields = json.loads(line)
                event_fields.pop('duration', None)
                yield Event(**event_fields)
