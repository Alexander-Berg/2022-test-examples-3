# coding: utf8

from contextlib import contextmanager

import mock

from common.tester.utils.replace_setting import replace_setting

from travel.rasp.suburban_tasks.suburban_tasks.scripts import update_suburban_events
from travel.rasp.suburban_tasks.suburban_tasks.scripts.update_suburban_events import (
    read_rows_from_sqs, send_rows_to_remote_server)
from travel.rasp.suburban_tasks.tests.scripts.update_suburban_events.factories import create_row, create_rzd_query


@contextmanager
def mock_queue(queue):
    def queue_get():
        return [(q, 'token') for q in queue]

    def queue_pop(*args, **kwargs):
        return queue.pop()

    with mock.patch('travel.rasp.library.python.sqs.queue.FIFOQueue.push', side_effect=queue.append), \
         mock.patch('travel.rasp.library.python.sqs.queue.FIFOQueue.receive_messages', side_effect=queue_get), \
         mock.patch('travel.rasp.library.python.sqs.queue.FIFOQueue.delete_message', side_effect=queue_pop):
        yield


@replace_setting('SUBURBAN_EVENTS_SEND_TO_SQS', True)
@replace_setting('SUBURBAN_EVENTS_QUEUE_MAX_BATCH_SIZE', 3)
def test_read_rows_from_sqs_by_batches():
    queue = []
    events = []

    with mock_queue(queue), mock.patch.object(update_suburban_events, 'save_events_to_feed',
                                              side_effect=events.extend) as m_save_to_feed:
        rows = [create_row(i)[1] for i in range(10)]
        send_rows_to_remote_server(rows)
        assert len(queue) == 4

        read_rows_from_sqs()
        assert m_save_to_feed.call_count == 1
        assert len(queue) == 0
        assert len(events) == 10

        for i in range(10):
            event = rows[i].to_dict()
            event.pop('id', None)
            event.pop('query', None)

            assert event == events[i]


@replace_setting('SUBURBAN_EVENTS_SEND_TO_SQS', True)
def test_read_rows_from_several_queries():
    event_groups = []

    with mock_queue([]), mock.patch.object(update_suburban_events, 'save_events_to_feed',
                                           side_effect=event_groups.append):

        # send first message with 3 events
        rows = [create_row(i)[1] for i in range(3)]
        query = create_rzd_query()
        for row in rows:
            row.query = query

        send_rows_to_remote_server(rows)

        # send second message with 1 event
        query2 = create_rzd_query()
        row2 = create_row(4)[1]
        row2.query = query2
        send_rows_to_remote_server([row2])

        # read messages
        read_rows_from_sqs()
        assert len(event_groups) == 2

        events = event_groups[0]
        assert len(events) == 3
        for event in events:
            assert event['query'].id == query.id

        events = event_groups[1]
        assert len(events) == 1
        assert events[0]['query'].id == query2.id


def test_no_messages_in_sqs():
    with mock_queue([]), mock.patch.object(update_suburban_events, 'save_events_to_feed') as m_save_to_feed:
        read_rows_from_sqs()
        m_save_to_feed.assert_not_called()
