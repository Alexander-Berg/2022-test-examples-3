import pytest

from sendr_utils import without_none

from hamcrest import assert_that, has_properties

from mail.ciao.ciao.core.entities.enums import FrameName, YesNo
from mail.ciao.ciao.core.scenarios.delete_event import DeleteEventScenario, DeleteEventScenarioState
from mail.ciao.ciao.interactions.calendar.exceptions import EventNotFoundCalendarError
from mail.ciao.ciao.tests.utils import dummy_async_function


@pytest.fixture
def event(create_event_entity):
    return create_event_entity()


@pytest.fixture
def state():
    return None


@pytest.fixture
def slots():
    return None


@pytest.fixture
def commit():
    return None


@pytest.fixture
def scenario(state, event, slots, commit):
    return DeleteEventScenario(**without_none(dict(
        state=state,
        event=event,
        slots=slots,
        commit=commit,
    )))


@pytest.mark.parametrize('state,expected_state', (
    (None, DeleteEventScenarioState.INITIAL),
    (DeleteEventScenarioState.INITIAL, DeleteEventScenarioState.INITIAL),
    (DeleteEventScenarioState.AWAITING_CONFIRMATION, DeleteEventScenarioState.AWAITING_CONFIRMATION),
))
def test_get_params(event, scenario, expected_state):
    assert scenario.get_params() == {
        'state': expected_state,
        'event': event,
    }


@pytest.mark.parametrize('state', (None, DeleteEventScenarioState.INITIAL))
class TestHandleInitial:
    @pytest.fixture
    def returned_func(self, scenario):
        return scenario._handle_initial

    def test_state(self, scenario, returned):
        assert scenario.get_params()['state'] == DeleteEventScenarioState.AWAITING_CONFIRMATION

    def test_returned(self, event, returned):
        assert_that(
            returned,
            has_properties({
                'response': has_properties({
                    'text': f'Удалить событие "{event.name}"?',
                    'requested_slot': ('delete_event_confirmation', YesNo),
                    'frame_name': FrameName.DELETE_EVENT,
                    'expected_frames': [FrameName.DELETE_EVENT],
                }),
            })
        )


@pytest.mark.parametrize('state', (DeleteEventScenarioState.AWAITING_CONFIRMATION,))
class TestHandleAwaitingConfirmation:
    @pytest.fixture(autouse=True)
    def delete_event_mock(self, mocker):
        return mocker.patch(
            'mail.ciao.ciao.interactions.calendar.base.BaseCalendarClient.delete_event',
            mocker.Mock(side_effect=dummy_async_function()),
        )

    @pytest.fixture
    def returned_func(self, setup_user, scenario):
        return scenario._handle_awaiting_confirmation

    class TestNo:
        @pytest.fixture
        def slots(self):
            return {'delete_event_confirmation': YesNo.NO}

        def test_no__returned(self, returned):
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': 'Хорошо.',
                        'commit': False,
                    }),
                    'value': None,
                }),
            )

        def test_no__delete_event_call(self, delete_event_mock, returned):
            delete_event_mock.assert_not_called()

    class TestYes:
        @pytest.fixture
        def slots(self):
            return {'delete_event_confirmation': YesNo.YES}

        @pytest.mark.parametrize('commit', (False, True))
        def test_yes__returned(self, returned):
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': 'Событие удалено.',
                        'commit': True,
                    }),
                    'value': None,
                }),
            )

        @pytest.mark.parametrize('commit', (False, True))
        def test_yes__delete_event_call(self, mocker, user, event, commit, delete_event_mock, returned):
            assert delete_event_mock.mock_calls == [] if not commit else [
                mocker.call(
                    uid=user.uid,
                    user_ticket=user.user_ticket,
                    event_id=event.event_id,
                )
            ]

        @pytest.mark.asyncio
        @pytest.mark.parametrize('commit', (True,))
        async def test_yes__delete_event_not_found(self, returned_func, delete_event_mock):
            delete_event_mock.side_effect = dummy_async_function(exc=EventNotFoundCalendarError(
                status_code=200,
                response_status=None,
                service=None,
                method=None,
            ))
            await returned_func()
            delete_event_mock.assert_called_once()


class TestHandle:
    @pytest.fixture
    def result(self, mocker):
        return mocker.Mock()

    @pytest.fixture(autouse=True)
    def handle_initial_mock(self, mocker, scenario, result):
        mocker.patch.object(
            scenario,
            '_handle_initial',
            mocker.Mock(side_effect=dummy_async_function(result))
        )

    @pytest.fixture(autouse=True)
    def handle_awaiting_confirmation_mock(self, mocker, scenario, result):
        mocker.patch.object(
            scenario,
            '_handle_awaiting_confirmation',
            mocker.Mock(side_effect=dummy_async_function(result))
        )

    @pytest.fixture
    def returned_func(self, scenario):
        async def _inner():
            return await scenario.handle()

        return _inner

    @pytest.mark.parametrize('state', (None, DeleteEventScenarioState.INITIAL,))
    def test_initial(self, mocker, scenario, result, returned):
        assert all((
            scenario._handle_initial.mock_calls == [mocker.call()],
            scenario._handle_awaiting_confirmation.mock_calls == [],
            returned == result,
        ))

    @pytest.mark.parametrize('state', (DeleteEventScenarioState.AWAITING_CONFIRMATION,))
    def test_awaiting_confirmation(self, mocker, scenario, result, returned):
        assert all((
            scenario._handle_initial.mock_calls == [],
            scenario._handle_awaiting_confirmation.mock_calls == [mocker.call()],
            returned == result,
        ))
