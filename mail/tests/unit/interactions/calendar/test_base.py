import re
from datetime import date, datetime

import pytest
import pytz

from sendr_utils import alist, without_none

from hamcrest import assert_that, has_entries, has_properties, is_

from mail.ciao.ciao.interactions.calendar.base import BaseCalendarClient
from mail.ciao.ciao.interactions.calendar.exceptions import EventNotFoundCalendarError
from mail.ciao.ciao.tests.data.calendar_responses import *  # noqa


class FinalCalendarClient(BaseCalendarClient):
    SERVICE = 'final_calendar'
    BASE_URL = 'https://final-calendar-client'
    TVM_ID = 1234


@pytest.fixture
async def calendar_client(create_client):
    client = create_client(FinalCalendarClient)
    yield client
    await client.CONNECTOR.close()


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture(params=(
    pytest.param(False, id='empty_user_ticket'),
    pytest.param(True, id='not_empty_user_ticket'),
))
def user_ticket(request, rands):
    return f'user-ticket-{rands()}' if request.param else None


@pytest.fixture
def expected_tickets(user_ticket):
    tickets = {'X-Ya-Service-Ticket': is_(str)}
    if user_ticket is not None:
        tickets['X-Ya-User-Ticket'] = user_ticket
    return tickets


@pytest.fixture
def timezone():
    return pytz.timezone('Europe/Moscow')


class TestMapEvent:
    @pytest.fixture
    def event_data(self):
        return {
            'actions': {
                'accept': False,
                'attach': False,
                'changeOrganizer': False,
                'delete': True,
                'detach': False,
                'edit': True,
                'invite': True,
                'move': True,
                'reject': False
            },
            'attendees': [],
            'availability': 'busy',
            'canAdminAllResources': False,
            'decision': 'yes',
            'description': '',
            'descriptionHtml': '',
            'endTs': '2020-02-09T13:00:00+03:00',
            'externalId': 'Prs5J9oAyandex.ru',
            'id': 1952202,
            'instanceStartTs': '2020-02-09T09:00:00',
            'isAllDay': False,
            'isRecurrence': False,
            'layerId': 2271,
            'location': '',
            'locationHtml': '',
            'name': 'Без названия',
            'notifications': [],
            'organizerLetToEditAnyMeeting': False,
            'othersCanView': False,
            'participantsCanEdit': False,
            'participantsCanInvite': False,
            'repetitionNeedsConfirmation': False,
            'resources': [],
            'sequence': 0,
            'startTs': '2020-02-09T12:00:00+03:00',
            'subscribers': [],
            'totalAttendees': 0,
            'type': 'user'
        }

    def test_result(self, event_data):
        assert_that(
            BaseCalendarClient.map_event(event_data),
            has_properties({
                'event_id': event_data['id'],
                'external_id': event_data['externalId'],
                'name': event_data['name'],
                'description': event_data['description'],
                'start_ts': datetime.fromisoformat(event_data['startTs']),
                'end_ts': datetime.fromisoformat(event_data['endTs']),
                'others_can_view': event_data['othersCanView'],
                'sequence': event_data['sequence'],
                'all_day': event_data['isAllDay'],
            })
        )


class TestCountEvents:
    @pytest.fixture(autouse=True)
    def count_events_mock(self, mocker, aioresponses_mocker, calendar_client, count_events_response):
        return aioresponses_mocker.get(
            re.compile(f'^{calendar_client.BASE_URL}/internal/count-events.*$'),
            payload=count_events_response,
        )

    @pytest.fixture
    def from_date(self):
        return date(2020, 2, 10)

    @pytest.fixture
    def to_date(self):
        return date(2020, 2, 20)

    @pytest.fixture
    def returned_func(self, calendar_client, uid, user_ticket, from_date, to_date, timezone):
        async def _inner():
            return await calendar_client.count_events(
                uid=uid,
                user_ticket=user_ticket,
                from_date=from_date,
                to_date=to_date,
                timezone=timezone,
            )

        return _inner

    def test_called_once(self, returned, count_events_mock):
        count_events_mock.assert_called_once()

    def test_params(self, uid, from_date, to_date, timezone, returned, count_events_mock):
        assert count_events_mock.call_args[1]['params'] == {
            'uid': uid,
            'from': from_date.isoformat(),
            'to': to_date.isoformat(),
            'tz': timezone.zone,
        }

    def test_headers(self, expected_tickets, returned, count_events_mock):
        assert_that(count_events_mock.call_args[1]['headers'], has_entries(expected_tickets))

    @pytest.mark.parametrize('count_events_response,expected', (
        (
            {
                'layers': [{'id': 1, 'color': '2', 'counts': {'2020-02-10': 1, '2020-02-11': 2}}],
            },
            {
                date(2020, 2, 10): 1,
                date(2020, 2, 11): 2,
            },
        ),
        (
            {
                'layers': [
                    {'id': 1, 'color': '2', 'counts': {'2020-02-10': 1, '2020-02-11': 2}},
                    {'id': 2, 'color': '3', 'counts': {'2020-02-10': 3, '2020-02-12': 5}},
                ],
            },
            {
                date(2020, 2, 10): 4,
                date(2020, 2, 11): 2,
                date(2020, 2, 12): 5,
            },
        ),
    ))
    def test_response(self, returned, expected):
        assert returned == expected


class TestCreateEvent:
    @pytest.fixture
    def timezone(self):
        return pytz.timezone('Europe/Moscow')

    @pytest.fixture
    def start_datetime(self, timezone):
        return timezone.localize(datetime(2020, 2, 10, 18, 54, 30, microsecond=11))

    @pytest.fixture
    def end_datetime(self, timezone):
        return timezone.localize(datetime(2020, 2, 10, 19, 52, 11, microsecond=22))

    @pytest.fixture
    def external_id(self):
        return 'event-external_id'

    @pytest.fixture
    def name(self):
        return 'event-name'

    @pytest.fixture
    def description(self):
        return 'event-description'

    @pytest.fixture
    def all_day(self):
        return None

    @pytest.fixture(autouse=True)
    def create_event_mock(self, aioresponses_mocker, calendar_client, create_event_response):
        return aioresponses_mocker.post(
            re.compile(f'^{calendar_client.BASE_URL}/internal/create-event.*$'),
            payload=create_event_response,
        )

    @pytest.fixture
    def returned_func(self, calendar_client, uid, user_ticket, start_datetime, end_datetime, external_id, name,
                      description, all_day):
        async def _inner():
            return await calendar_client.create_event(
                uid=uid,
                user_ticket=user_ticket,
                start_datetime=start_datetime,
                end_datetime=end_datetime,
                external_id=external_id,
                name=name,
                description=description,
                all_day=all_day,
            )

        return _inner

    def test_called_once(self, returned, create_event_mock):
        create_event_mock.assert_called_once()

    @pytest.mark.parametrize('external_id,name,description,all_day', (
        pytest.param('event-external_id', 'event-name', 'event-description', None, id='default'),
        pytest.param(None, 'event-name', 'event-description', None, id='empty_external_id'),
        pytest.param('event-external_id', None, 'event-description', None, id='empty_name'),
        pytest.param('event-external_id', 'event-name', None, None, id='empty_description'),
        pytest.param('event-external_id', 'event-name', 'event-description', True, id='all_day'),
    ))
    def test_request(self, uid, start_datetime, end_datetime, external_id, name, description, returned,
                     create_event_mock, expected_tickets, all_day):
        assert_that(
            create_event_mock.call_args[1],
            has_entries({
                'headers': has_entries(expected_tickets),
                'params': {'uid': uid},
                'json': without_none({
                    'startTs': start_datetime.strftime('%Y-%m-%dT%H:%M:%S%z'),
                    'endTs': end_datetime.strftime('%Y-%m-%dT%H:%M:%S%z'),
                    'externalId': external_id,
                    'name': name,
                    'description': description,
                    'type': 'user',
                    'isAllDay': all_day,
                }),
            })
        )

    def test_result(self, create_event_response, returned):
        assert returned == create_event_response['showEventId']


class TestDeleteEvent:
    @pytest.fixture
    def event_id(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    def delete_event_mock(self, aioresponses_mocker, calendar_client, delete_event_response):
        return aioresponses_mocker.post(
            re.compile(f'^{calendar_client.BASE_URL}/internal/delete-event.*$'),
            payload=delete_event_response,
        )

    @pytest.fixture
    def returned_func(self, calendar_client, uid, user_ticket, event_id):
        async def _inner():
            return await calendar_client.delete_event(uid=uid, user_ticket=user_ticket, event_id=event_id)

        return _inner

    def test_called_once(self, returned, delete_event_mock):
        delete_event_mock.assert_called_once()

    def test_request(self, uid, expected_tickets, event_id, returned, delete_event_mock):
        assert_that(
            delete_event_mock.call_args[1],
            has_entries({
                'headers': has_entries(expected_tickets),
                'params': {
                    'uid': uid,
                    'id': event_id,
                },
            })
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('delete_event_response', (
        (
            {
                'invocationInfo': {
                    'req-id': 'L9e1zNJI',
                    'hostname': 'sas1-d0b1479d9dd8.qloud-c.yandex.net',
                    'exec-duration-millis': 30,
                    'action': 'deleteEvent',
                    'app-name': 'web',
                    'app-version': '6351359'
                },
                'error': {
                    'name': 'event-not-found',
                    'message': 'event not found by id 1963471',
                    'stackTrace': '...'
                }
            },
        )
    ))
    async def test_event_not_found(self, returned_func):
        with pytest.raises(EventNotFoundCalendarError):
            await returned_func()


class TestGetEvents:
    @pytest.fixture
    def from_datetime(self, timezone):
        return timezone.localize(datetime(2020, 2, 11, 13, 32, 20))

    @pytest.fixture
    def to_datetime(self, timezone):
        return timezone.localize(datetime(2020, 2, 11, 15, 34, 22))

    @pytest.fixture(autouse=True)
    def get_events_mock(self, aioresponses_mocker, calendar_client, get_events_response):
        return aioresponses_mocker.get(
            re.compile(f'^{calendar_client.BASE_URL}/internal/get-events.*$'),
            payload=get_events_response,
        )

    @pytest.fixture
    def returned_func(self, calendar_client, uid, user_ticket, from_datetime, to_datetime):
        async def _inner():
            return await alist(calendar_client.get_events(
                uid=uid,
                user_ticket=user_ticket,
                from_datetime=from_datetime,
                to_datetime=to_datetime,
            ))

        return _inner

    def test_called_once(self, returned, get_events_mock):
        get_events_mock.assert_called_once()

    def test_request(self, uid, expected_tickets, from_datetime, to_datetime, returned, get_events_mock):
        assert_that(
            get_events_mock.call_args[1],
            has_entries({
                'headers': has_entries(expected_tickets),
                'params': {
                    'uid': uid,
                    'from': from_datetime.strftime('%Y-%m-%dT%H:%M:%S%z'),
                    'to': to_datetime.strftime('%Y-%m-%dT%H:%M:%S%z'),
                    'dateFormat': 'zoned',
                }
            })
        )

    def test_response(self, get_events_response, returned):
        assert returned == [
            BaseCalendarClient.map_event(event_data)
            for event_data in get_events_response['events']
        ]

    @pytest.mark.xfail
    class TestOthersCanView:
        @pytest.fixture
        def get_events_response(self, get_events_response):
            get_events_response['events'] = [
                {**event, 'othersCanView': False} for event in get_events_response['events']
            ]
            return get_events_response

        def test_response(self, returned):
            assert returned == []


class TestUpdateEvent:
    @pytest.fixture
    def event_id(self, randn):
        return randn()

    @pytest.fixture
    def start_ts(self, timezone):
        return timezone.localize(datetime(2020, 4, 20, 20, 25))

    @pytest.fixture
    def end_ts(self, timezone):
        return timezone.localize(datetime(2020, 4, 21, 22, 30))

    @pytest.fixture
    def all_day(self):
        return True

    @pytest.fixture(autouse=True)
    def update_event_mock(self, aioresponses_mocker, calendar_client):
        return aioresponses_mocker.post(
            re.compile(f'^{calendar_client.BASE_URL}/internal/update-event.*$'),
            payload={},  # not parsing response at the moment
        )

    @pytest.fixture
    def returned_func(self, calendar_client, uid, user_ticket, timezone, event_id, start_ts, end_ts, all_day):
        async def _inner():
            return await calendar_client.update_event(
                uid=uid,
                user_ticket=user_ticket,
                event_id=event_id,
                start_ts=start_ts,
                end_ts=end_ts,
                all_day=all_day,
            )

        return _inner

    def test_called_once(self, update_event_mock, returned):
        update_event_mock.assert_called_once()

    def test_request(self, expected_tickets, uid, event_id, start_ts, end_ts, all_day, update_event_mock, returned):
        assert_that(
            update_event_mock.call_args[1],
            has_entries({
                'headers': has_entries(expected_tickets),
                'params': has_entries({
                    'uid': uid,
                    'id': event_id,
                }),
                'json': has_entries({
                    'startTs': start_ts.strftime('%Y-%m-%dT%H:%M:%S%z'),
                    'endTs': end_ts.strftime('%Y-%m-%dT%H:%M:%S%z'),
                    'isAllDay': all_day,
                }),
            })
        )
