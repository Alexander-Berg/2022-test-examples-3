# coding: utf-8

import ujson

from django.test import Client, TestCase
from martylib.core.date_utils import get_datetime
from martylib.core.decorators import convenient_decorator

from timeline.models import Story, Tag


@convenient_decorator
def with_ticket(method, queue='WEB'):
    """
    Adds `ticket_id` argument to a :param:`method`.
    Ticket ID is unique for each test.

    :type method: callable
    :type queue: str
    :rtype: callable
    """
    def _wrapped(instance):
        ticket_id = '{}-{}'.format(queue, instance.latest_ticket_number)
        instance.latest_ticket_number += 1
        return method(instance, ticket_id=ticket_id)
    return _wrapped


class TestNannyTicketsImport(TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.client = Client()
        cls.latest_ticket_number = 100

    def make_request(
        self, ticket_id, event_id=1, service_id='ydl', snapshot_id='b9730be38e3aa13be2360fe5f4328f911b7e',
        author='roboslone', created_at=1234567999, comment='comment', event_type='NEW', data=None,
        expected_response_code=200, expected_response_content=b'{"ok": true}'
    ):
        """
        Generates webhook payload, makes HTTP request, checks response status code and content,
        ensures that story with given :param:`ticket_id` was created.

        :type ticket_id: str
        :type event_id: int | str
        :type service_id: str
        :type snapshot_id: str
        :type author: str
        :type created_at: int
        :type comment: str
        :type event_type: str
        :type data: dict
        :type expected_response_code: int
        :type expected_response_content: bytes
        :rtype: django.http.HttpResponse
        """
        webhook_payload = ujson.dumps({
            'id': str(event_id),
            'service_id': service_id,
            'snapshot_id': snapshot_id,
            'ticket_id': ticket_id,
            'author': author,
            'created_at': created_at,
            'comment': comment,
            'type': event_type,
            'data': data or {}
        })

        response = self.client.post('/webhooks/nanny-ticket/', data=webhook_payload, content_type='application/json')

        # Response test.
        self.assertEqual(response.status_code, expected_response_code)
        self.assertEqual(response.content, expected_response_content)

        # Lookup test.
        qs = Story.objects.search(intersect_tags=[
            Tag.get('type:nanny-ticket-update'),
            Tag.get(f'nanny-ticket:{ticket_id}'),
        ])
        self.assertEqual(qs.count(), 1)

        return response

    def check_imported_story(
        self, ticket_id, comment='comment', expected_events_count=1, expected_tags=None,
        expected_start=None, expected_end=None
    ):
        """
        Searches for imported story and checks it's tags, event count, start and end.

        :type ticket_id: str
        :type comment: str
        :type expected_events_count: int
        :type expected_tags: List[str]
        :type expected_start: datetime.datetime
        :type expected_end: datetime.datetime
        :rtype: Story
        """
        story = Story.objects.get(name=f'{ticket_id}: {comment}')

        self.assertEqual(story.events.count(), expected_events_count)
        for tag in expected_tags or []:
            self.assertIn(Tag.get(tag), story.tags.all())

        if expected_start is not None:
            self.assertEqual(story.start, expected_start)
        if expected_end is not None:
            self.assertEqual(story.end, expected_end)

        return story

    def test_invalid_method(self):
        response = self.client.patch('/webhooks/nanny-ticket/', data={}, content_type='application/json')
        self.assertEqual(response.status_code, 405)
        self.assertEqual(
            response.content,
            b'{"error": "method \\"PATCH\\" not allowed"}'
        )

    def test_invalid_schema(self):
        response = self.client.post('/webhooks/nanny-ticket/', data={}, content_type='application/json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(
            response.content,
            b'{"error": "webhook payload validation failed: \'id\' is a required property"}'
        )

    @with_ticket
    def test_valid_payload(self, ticket_id):
        self.make_request(ticket_id, event_type='NEW')
        self.check_imported_story(ticket_id, expected_tags=['source:webhook'])

    @with_ticket
    def test_update_existing(self, ticket_id):
        self.make_request(ticket_id, event_type='NEW', created_at=1234567890)
        self.check_imported_story(ticket_id, expected_events_count=1)

        self.make_request(ticket_id, event_type='STATUS_CHANGE', created_at=1234567900, data={
            'status': 'ON_HOLD',
            'comment': '',
        })
        story = self.check_imported_story(ticket_id, expected_events_count=2, expected_start=get_datetime(1234567890))
        self.assertEqual(story.end, None)

    @with_ticket
    def test_story_ending(self, ticket_id):
        self.make_request(ticket_id, event_type='NEW', created_at=1234567890)
        self.check_imported_story(ticket_id, expected_events_count=1)

        self.make_request(ticket_id, event_type='STATUS_CHANGE', created_at=1234567900, data={
            'status': 'DEPLOY_SUCCESS',
            'comment': '',
        })
        self.check_imported_story(
            ticket_id, expected_events_count=2,
            expected_start=get_datetime(1234567890), expected_end=get_datetime(1234567900)
        )

    @with_ticket(queue='IMGS')
    def test_tags_on_ticket_creation(self, ticket_id):
        self.make_request(ticket_id)
        self.check_imported_story(ticket_id, expected_tags=[
            'nanny-ticket-queue:IMGS',
            f'nanny-ticket:{ticket_id}',
            'user:roboslone',
        ])

    @with_ticket(queue='IMGS')
    def test_tags_on_status_update(self, ticket_id):
        self.make_request(ticket_id, event_type='STATUS_CHANGE', data={
            'status': 'ON_HOLD',
            'comment': '',
        })
        self.check_imported_story(ticket_id, expected_tags=[
            'nanny-ticket-queue:IMGS',
            f'nanny-ticket:{ticket_id}',
            'user:roboslone',
            'nanny-ticket-status:ON_HOLD',
        ])

    @with_ticket
    def test_tags_on_snapshot_activation(self, ticket_id):
        self.make_request(ticket_id, event_type='ACTIVATE_SERVICE_SNAPSHOT', data={
            'activate_recipe': 'default',
            'tracked_tickets': {
                'tickets': [
                    {'ticket_id': 'WEB-200'},
                    {'ticket_id': 'WEB-201'},
                ],
                'startrek_tickets': [
                    'SEARCHPRODINCIDENTS-100',
                ]
            },
        })
        self.check_imported_story(ticket_id, expected_tags=[
            'nanny-ticket-queue:WEB',
            f'nanny-ticket:{ticket_id}',
            'nanny-ticket:WEB-200',
            'nanny-ticket:WEB-201',
            'startrek:SEARCHPRODINCIDENTS-100',
            'user:roboslone',
        ])

    @with_ticket
    def test_service_and_snapshot_tags(self, ticket_id):
        self.make_request(ticket_id, service_id='mikey', snapshot_id='12345')
        self.check_imported_story(ticket_id, expected_tags=[
            'nanny-service:mikey',
            'nanny-snapshot:mikey:12345'
        ])

    @with_ticket
    def test_story_ending_on_ticket_cancel(self, ticket_id):
        self.make_request(ticket_id, created_at=1234567890)
        self.check_imported_story(ticket_id, expected_events_count=1, expected_start=get_datetime(1234567890))

        self.make_request(ticket_id, event_type='STATUS_CHANGE', created_at=1234567900, data={
            'status': 'CANCELLED',
            'comment': '',
        })
        self.check_imported_story(
            ticket_id, expected_events_count=2,
            expected_start=get_datetime(1234567890), expected_end=get_datetime(1234567900)
        )
