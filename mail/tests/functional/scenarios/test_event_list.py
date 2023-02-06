import re
from datetime import datetime

import pytest
import ujson

from hamcrest import assert_that, contains, contains_inanyorder, empty, has_entries, has_properties, is_

from mail.ciao.ciao.core.entities.analytcs import Intent
from mail.ciao.ciao.core.entities.enums import FrameName
from mail.ciao.ciao.tests.data.calendar_responses import *  # noqa

from .utils import assert_requests_slot, create_frame


@pytest.fixture(autouse=True)
def mock_get_events(aioresponses_mocker, settings, get_events_response):
    aioresponses_mocker.get(
        re.compile(f'^{settings.PUBLIC_CALENDAR_API_URL}/internal/get-events.*$'),
        payload=get_events_response,
        repeat=False,
    )


@pytest.fixture(autouse=True)
def mock_delete_event(aioresponses_mocker, settings, delete_event_response):
    return aioresponses_mocker.post(
        re.compile(f'^{settings.PUBLIC_CALENDAR_API_URL}/internal/delete-event.*$'),
        payload=delete_event_response,
        repeat=False,
    )


@pytest.fixture(autouse=True)
def mock_update_event(aioresponses_mocker, settings, delete_event_response):
    return aioresponses_mocker.post(
        re.compile(f'^{settings.PUBLIC_CALENDAR_API_URL}/internal/update-event.*$'),
        payload={},
        repeat=False,
    )


@pytest.fixture
def starter_frame():
    frame = create_frame(FrameName.EVENT_LIST.value)
    slot = frame.Slots.add()
    slot.Name = 'event_list_day'
    slot.Type = 'sys.date'
    slot.Value = '{"years":2020,"months":2,"days":18}'
    return frame


@pytest.fixture
def yes_no_answer():
    return 'yes'


@pytest.fixture
def yes_no_frame(yes_no_answer):
    frame = create_frame(FrameName.EVENT_LIST_SUBSEQUENT.value, [
        ('event_list_yes_no_answer', 'yes_no', yes_no_answer)
    ])
    return frame


@pytest.fixture
def awaiting_action_frames(starter_frame):
    return [
        starter_frame,
        create_frame(FrameName.EVENT_LIST_SUBSEQUENT.value, [('event_list_yes_no_answer', 'yes_no', 'yes')])
    ]


class BaseTestEventList:
    def test_analytics(self, returned):
        assert returned.ResponseBody.AnalyticsInfo.Intent == Intent.EVENT_LIST.value


class TestStart(BaseTestEventList):
    @pytest.fixture
    def returned_func(self, run_scenario, starter_frame):
        async def _inner():
            return await run_scenario([starter_frame])

        return _inner

    @pytest.mark.asyncio
    @pytest.mark.parametrize('with_slots', (True, False))
    async def test_requests_yes_no(self, starter_frame, returned_func, with_slots):
        if not with_slots:
            starter_frame.Slots.pop()
        returned = await returned_func()
        assert_that(
            returned.ResponseBody.SemanticFrame,
            has_properties({
                'Name': 'alice.mail_ciao_event_list_subsequent',
                'Slots': contains(has_properties({
                    'Name': 'event_list_yes_no_answer',
                    'AcceptedTypes': contains('yes_no'),
                    'IsRequested': True,
                }))
            })
        )
        assert_that(
            returned.ResponseBody,
            has_properties({
                'Entities': contains(
                    has_properties({
                        'Name': 'yes_no',
                        'Items': has_entries({
                            'yes': has_properties({
                                'Instances': contains_inanyorder(
                                    has_properties({'Language': 1, 'Phrase': 'да'}),
                                    has_properties({'Language': 1, 'Phrase': 'ага'}),
                                    has_properties({'Language': 1, 'Phrase': 'давай'}),
                                    has_properties({'Language': 1, 'Phrase': 'конечно'}),
                                    has_properties({'Language': 1, 'Phrase': 'точно'}),
                                ),
                            }),
                            'no': has_properties({
                                'Instances': contains_inanyorder(
                                    has_properties({'Language': 1, 'Phrase': 'нет'}),
                                    has_properties({'Language': 1, 'Phrase': 'не-а'}),
                                    has_properties({'Language': 1, 'Phrase': 'не надо'}),
                                ),
                            })
                        })
                    })
                ),
            })
        )

    def test_contains_sensitive_data(self, returned):
        assert returned.ResponseBody.Layout.ContainsSensitiveData


class TestYesNo(BaseTestEventList):
    @pytest.fixture
    def returned_func(self, run_scenario, starter_frame, yes_no_frame):
        async def _inner():
            return await run_scenario([starter_frame, yes_no_frame])

        return _inner

    @pytest.mark.parametrize('yes_no_answer', ('yes',))
    def test_yes_text_present(self, returned, get_events_response):
        assert_that(
            returned.ResponseBody.Layout,
            has_properties({
                'Cards': contains(has_properties({
                    'Text': is_(str),
                })),
                'OutputSpeech': is_(str),
            })
        )

    @pytest.mark.parametrize('yes_no_answer', ('yes',))
    def test_yes_text(self, returned, get_events_response):
        text = returned.ResponseBody.Layout.Cards[0].TextWithButtons.Text
        output_speech = returned.ResponseBody.Layout.OutputSpeech
        lines = text.split('\n')
        event_names = [event['name'] for event in get_events_response['events']]
        assert all((
            text == output_speech,
            len(lines) == len(event_names),
            *[  # checking that event names are present in output
                event_name in event_line
                for event_line, event_name in zip(lines, event_names)
            ]
        ))

    @pytest.mark.parametrize('yes_no_answer', ('yes',))
    def test_yes_nlu_hint(self, returned):
        assert_that(
            returned,
            has_properties({
                'ResponseBody': has_properties({
                    'FrameActions': has_entries({
                        'expected_frame_0': has_properties({
                            'NluHint': has_properties({
                                'FrameName': 'alice.mail_ciao_delete_event',
                            })
                        })
                    }),
                }),
            }),
        )

    @pytest.mark.parametrize('yes_no_answer', ('yes',))
    def test_open_calendar_button(self, returned):
        assert_that(
            returned,
            has_properties({
                'ResponseBody': has_properties({
                    'Layout': has_properties({
                        'Cards': contains(has_properties({
                            'TextWithButtons': has_properties({
                                'Buttons': contains(has_properties({
                                    'Title': is_(str),
                                    'ActionId': 'button_0',
                                })),
                            })
                        })),
                    }),
                    'FrameActions': has_entries({
                        'button_0': has_properties({
                            'Directives': has_properties({
                                'List': contains(has_properties({
                                    'OpenUriDirective': has_properties({
                                        'Uri': is_(str),
                                    })
                                }))
                            })
                        })
                    }),
                }),
            }),
        )

    @pytest.mark.parametrize('yes_no_answer', ('no',))
    def test_no_text_empty(self, returned):
        assert_that(
            returned.ResponseBody.Layout,
            has_properties({
                'Cards': empty(),
                'OutputSpeech': '',
            })
        )

    @pytest.mark.parametrize('yes_no_answer,contains_sensitive_data', (
        ('yes', True),
        ('no', False),
    ))
    def test_contains_sensitive_data(self, yes_no_answer, returned, contains_sensitive_data):
        assert returned.ResponseBody.Layout.ContainsSensitiveData == contains_sensitive_data


@pytest.mark.asyncio
async def test_delete_expects_find_frame(run_scenario, awaiting_action_frames):
    result = await run_scenario([
        *awaiting_action_frames,
        create_frame(FrameName.DELETE_EVENT.value),
    ])
    assert_that(
        result.ResponseBody,
        has_properties({
            'FrameActions': has_entries({
                'expected_frame_0': has_properties({
                    'NluHint': has_properties({
                        'FrameName': 'alice.mail_ciao_find_event',
                    })
                })
            })
        }),
    )


class BaseTestAction:
    @pytest.fixture
    def event_data(self, get_events_response):
        return get_events_response['events'][0]

    @pytest.fixture(params=('name', 'time_start'))
    def filter_slot(self, user, request, event_data):
        if request.param == 'name':
            return ('find_event_name', 'string', event_data['name'])
        elif request.param == 'time_start':
            start_ts = datetime.fromisoformat(event_data['startTs']).astimezone(user.timezone)
            return ('find_event_time_start', 'sys.time', ujson.dumps({
                'hours': start_ts.hour,
                'minutes': start_ts.minute,
            }))
        else:
            raise NotImplementedError

    @pytest.fixture
    def extra_frames(self):
        return []

    @pytest.fixture
    def action_frame_name(self):
        raise NotImplementedError

    @pytest.fixture(params=(False, True))
    def frames(self, awaiting_action_frames, extra_frames, filter_slot, action_frame_name, request):
        if request.param:
            filter_frames = [
                create_frame(action_frame_name, [filter_slot]),
            ]
        else:
            filter_frames = [
                create_frame(action_frame_name),
                create_frame(FrameName.FIND_EVENT.value, [filter_slot]),
            ]
        return awaiting_action_frames + filter_frames + extra_frames

    @pytest.fixture
    async def returned_func(self, run_scenario, frames):
        async def _inner():
            return await run_scenario(frames)

        return _inner


class TestDelete(BaseTestAction):
    @pytest.fixture
    def action_frame_name(self):
        return FrameName.DELETE_EVENT.value

    def test_requests_confirmation(self, returned):
        assert_requests_slot(
            response=returned,
            frame=FrameName.DELETE_EVENT.value,
            slot_name='delete_event_confirmation',
            accepted_types=['yes_no'],
            contains_sensitive_data=False,
        )

    class TestDeletion:
        @pytest.fixture
        def extra_frames(self, confirmation):
            return [
                create_frame(
                    FrameName.DELETE_EVENT.value,
                    [('delete_event_confirmation', 'yes_no', confirmation)],
                )
            ]

        @pytest.mark.parametrize('confirmation', ('yes',))
        def test_deletion__confirmed(self, user, mock_delete_event, event_data, returned):
            assert_that(
                mock_delete_event.call_args[1],
                has_entries({
                    'headers': has_entries({
                        'X-Ya-Service-Ticket': is_(str),
                        'X-Ya-User-Ticket': user.user_ticket,
                    }),
                    'params': {
                        'uid': user.uid,
                        'id': event_data['id'],
                    },
                }),
            )

        @pytest.mark.parametrize('confirmation', ('no',))
        def test_deletion__cancelled(self, user, mock_delete_event, event_data, returned):
            mock_delete_event.assert_not_called()


class TestReschedule(BaseTestAction):
    @pytest.fixture
    def action_frame_name(self):
        return FrameName.RESCHEDULE_EVENT.value

    def test_requests_new_date_start(self, returned):
        assert_requests_slot(
            response=returned,
            frame=FrameName.RESCHEDULE_EVENT.value,
            slot_name='reschedule_event_new_date_start',
            accepted_types=['sys.date'],
            contains_sensitive_data=False,
        )

    @pytest.mark.parametrize('extra_frames', (
        [
            create_frame(
                FrameName.RESCHEDULE_EVENT.value,
                [('reschedule_event_new_date_start', 'sys.date', '{"days":1,"days_relative":true}')],
            ),
        ],
    ))
    def test_requests_new_time_start(self, returned):
        assert_requests_slot(
            response=returned,
            frame=FrameName.RESCHEDULE_EVENT.value,
            slot_name='reschedule_event_new_time_start',
            accepted_types=['sys.time'],
            contains_sensitive_data=False,
        )

    class TestRescheduling:
        @pytest.fixture
        def extra_frames(self, confirmation):
            return [
                create_frame(
                    FrameName.RESCHEDULE_EVENT.value,
                    [
                        ('reschedule_event_new_date_start', 'sys.date', '{"days":1}'),
                        ('reschedule_event_new_time_start', 'sys.time', '{"hours":1}'),
                    ]
                ),
                create_frame(
                    FrameName.RESCHEDULE_EVENT.value,
                    [
                        ('reschedule_event_confirmation', 'yes_no', confirmation),
                    ],
                )
            ]

        @pytest.mark.parametrize('confirmation', ('yes',))
        def test_rescheduling__confirmed(self, mock_update_event, user, event_data, returned):
            assert_that(
                mock_update_event.call_args[1],
                has_entries({
                    'headers': has_entries({
                        'X-Ya-Service-Ticket': is_(str),
                        'X-Ya-User-Ticket': user.user_ticket,
                    }),
                    'params': {
                        'uid': user.uid,
                        'id': event_data['id']
                    },
                    'json': has_entries({
                        'startTs': is_(str),
                        'endTs': is_(str),
                    }),
                })
            )

        @pytest.mark.parametrize('confirmation', ('no',))
        def test_rescheduling__cancelled(self, mock_update_event, returned):
            mock_update_event.assert_not_called()
