import pytest


@pytest.fixture
def count_events_response():
    return {
        'layers': [
            {'id': 2271, 'color': '#4887e1', 'counts': {'2020-02-09': 1}}
        ]
    }


@pytest.fixture
def create_event_response():
    return {
        'status': 'ok',
        'sequence': 0,
        'showEventId': 1963473,
        'showDate': '2020-02-10',
        'endTs': '2020-02-10T00:00:00',
        'externalIds': ['qZ13RaX9yandex.ru']
    }


@pytest.fixture
def delete_event_response():
    return {'status': 'ok'}


@pytest.fixture
def get_events_response():
    return {
        'events': [
            {
                'actions': {
                    'accept': False,
                    'attach': False,
                    'changeOrganizer': False,
                    'delete': True,
                    'detach': False,
                    'edit': True,
                    'invite': True,
                    'move': True,
                    'reject': False,
                },
                'attendees': [],
                'availability': 'busy',
                'canAdminAllResources': False,
                'decision': 'yes',
                'description': '',
                'descriptionHtml': '',
                'endTs': '2020-02-10T17:00:00+03:00',
                'externalId': 'qZ13RaX9yandex.ru',
                'id': 1963473,
                'instanceStartTs': '2020-02-10T13:00:00',
                'isAllDay': False,
                'isRecurrence': False,
                'layerId': 2271,
                'location': '',
                'locationHtml': '',
                'name': 'Без названия',
                'notifications': [],
                'organizerLetToEditAnyMeeting': False,
                'othersCanView': True,
                'participantsCanEdit': False,
                'participantsCanInvite': False,
                'repetitionNeedsConfirmation': False,
                'resources': [],
                'sequence': 0,
                'startTs': '2020-02-10T16:00:00+03:00',
                'subscribers': [],
                'totalAttendees': 0,
                'type': 'user',
            },
            {
                'actions': {
                    'accept': False,
                    'attach': False,
                    'changeOrganizer': False,
                    'delete': True,
                    'detach': False,
                    'edit': True,
                    'invite': True,
                    'move': True,
                    'reject': False,
                },
                'attendees': [],
                'availability': 'busy',
                'canAdminAllResources': False,
                'decision': 'yes',
                'description': 'debug debug 123',
                'descriptionHtml': 'debug debug 123',
                'endTs': '2020-02-10T17:00:00+03:00',
                'externalId': 'Ju7NHLheyandex.ru',
                'id': 1963472,
                'instanceStartTs': '2020-02-10T13:00:00',
                'isAllDay': False,
                'isRecurrence': False,
                'layerId': 2271,
                'location': '',
                'locationHtml': '',
                'name': 'debug1',
                'notifications': [],
                'organizerLetToEditAnyMeeting': False,
                'othersCanView': True,
                'participantsCanEdit': False,
                'participantsCanInvite': False,
                'repetitionNeedsConfirmation': False,
                'resources': [],
                'sequence': 0,
                'startTs': '2020-02-10T16:00:00+03:00',
                'subscribers': [],
                'totalAttendees': 0,
                'type': 'user',
            }
        ],
        'lastUpdateTs': 1581351905440
    }
