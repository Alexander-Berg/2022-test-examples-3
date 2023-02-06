from datetime import datetime, timedelta, timezone

import pytest
import ujson
from marshmallow import fields

from hamcrest import assert_that, contains, has_properties

from alice.megamind.protos.scenarios.request_pb2 import TScenarioRunRequest
from alice.megamind.protos.scenarios.response_pb2 import TScenarioRunResponse
from mail.ciao.ciao.api.schemas.base import BaseSchema
from mail.ciao.ciao.api.schemas.megamind.state import StateStackItemSchema
from mail.ciao.ciao.core.entities.enums import FrameName
from mail.ciao.ciao.core.entities.scenario_response import ScenarioResponse
from mail.ciao.ciao.core.entities.state import State
from mail.ciao.ciao.core.entities.state_stack import StateStack, StateStackItem
from mail.ciao.ciao.core.scenarios.base import BaseScenario
from mail.ciao.ciao.utils.datetime import UserDate
from mail.ciao.protos.state_pb2 import TState


class DummyScenario:
    scenario_name = 'dummy_scenario'


class DummyScenarioParamsSchema(BaseSchema):
    param1 = fields.Field()
    param2 = fields.Field()


@pytest.fixture(autouse=True)
def setup_dummy_scenario(mocker):
    mocker.patch.object(
        StateStackItemSchema,
        '_PARAMS_SCHEMAS',
        {DummyScenario.scenario_name: DummyScenarioParamsSchema()},
    )


@pytest.fixture
def app_id(rands, settings):
    app_id = rands()
    settings.ALLOWED_APP_IDS = (app_id,)
    return app_id


@pytest.fixture
def uuid(rands):
    return rands()


class BaseTestRunMegamindHandler:
    @pytest.fixture
    def tvm_default_uid(self, user):
        return user.uid

    @pytest.fixture(autouse=True)
    def now(self, mocker):
        now = datetime(2019, 2, 3, 13, 22, 10, tzinfo=timezone.utc)
        mocker.patch('mail.ciao.ciao.core.entities.state.utcnow', mocker.Mock(return_value=now))
        return now

    @pytest.fixture
    def _store(self):
        return {}

    @pytest.fixture
    def get_state(self, _store):
        def _inner():
            return _store.get('state')

        return _inner

    @pytest.fixture
    def get_user(self, _store):
        def _inner():
            return _store.get('user')

        return _inner

    @pytest.fixture
    async def response(self, request_func):
        return await request_func()

    @pytest.fixture(params=(
        pytest.param(False, id='not_expired'),
        pytest.param(True, id='expired'),
    ))
    def state_expired(self, request):
        return request.param

    @pytest.fixture
    def state(self, now, state_expired):
        expired_multiplier = -1 if state_expired else 1
        return State(
            hard_expire=now + expired_multiplier * timedelta(seconds=1),
            state_stack=StateStack(stack_items=[
                StateStackItem(
                    scenario_name=DummyScenario.scenario_name,
                    params={'param1': 'value1', 'param2': 'value2'},
                ),
                StateStackItem(
                    scenario_name=DummyScenario.scenario_name,
                    params={'param2': 'value2'},
                    arg_name='arg2',
                ),
            ]),
        )

    @pytest.fixture
    def frame(self):
        return {
            'name': FrameName.UNKNOWN_FRAME.value,
            'slots': [
                {
                    'name': 'slot1',
                    'value': '{"days":1,"months":2,"years":2020}',
                    'type': 'sys.date',
                    'accepted_types': ['sys.date'],
                },
                {'name': 'slot2', 'value': 'yes', 'type': 'YesNo', 'accepted_types': ['!!']},
            ]
        }

    @pytest.fixture
    def parsed_frame(self):
        return {
            'name': FrameName.UNKNOWN_FRAME,
            'slots': {
                'slot1': UserDate(year=2020, month=2, day=1),
            }
        }

    @pytest.fixture
    def scenario_response(self):
        return ScenarioResponse(text='scenario_response_text')

    @pytest.fixture(autouse=True)
    def scenario_runner_mock(self, mocker, _store, scenario_response):
        async def dummy_run():
            _store['state'] = BaseScenario.context.state
            _store['user'] = BaseScenario.context.user
            return scenario_response

        mock = mocker.patch('mail.ciao.ciao.api.handlers.megamind.base.ScenarioRunner')
        mock.return_value.run.side_effect = dummy_run
        return mock

    def test_response_200(self, response):
        assert response.status == 200

    @pytest.mark.asyncio
    @pytest.mark.parametrize('state_expired', (pytest.param(True, id='expired'),))
    async def test_resets_expired_state(self, state, now, request_func, get_state):
        state.hard_expire = now
        await request_func()
        assert get_state() == State()

    @pytest.mark.parametrize('state_expired', (pytest.param(False, id='not_expired'),))
    def test_reuses_state(self, state, response, get_state):
        assert get_state() == state

    def test_sets_user(self, user, get_user, response):
        assert get_user() == user

    def test_scenario_runner_constructor_call(self, parsed_frame, response, scenario_runner_mock):
        scenario_runner_mock.assert_called_once_with(
            frame_name=parsed_frame['name'],
            slots=parsed_frame['slots'],
            commit=False,
        )

    def test_scenario_runner_run_called(self, response, scenario_runner_mock):
        scenario_runner_mock.return_value.run.assert_called_once()

    def test_response(self):
        raise NotImplementedError

    class TestDenyApp:
        @pytest.fixture
        def app_id(self, rands):
            return rands()

        def test_irrelevant(self, scenario_runner_mock):
            scenario_runner_mock.assert_not_called()


class TestRunMegamindHandler__json(BaseTestRunMegamindHandler):
    @pytest.fixture
    def request_func(self, user, app, state, frame, app_id, uuid):
        async def _inner():
            return await app.post(
                '/megamind/run',
                headers={  # Middleware requires tickets, mocked tvm always responds with ok
                    'X-Ya-Service-Ticket': 'a',
                    'X-Ya-User-Ticket': user.user_ticket,
                },
                json={
                    'base_request': {
                        'state': {
                            'hard_expire': state.hard_expire.isoformat(),
                            'soft_expire': state.soft_expire.isoformat(),
                            'state_stack': {
                                'stack_items': [
                                    {
                                        'scenario_name': item.scenario_name,
                                        'params': item.params,
                                        'arg_name': item.arg_name,
                                    }
                                    for item in state.state_stack.stack_items
                                ],
                            }
                        },
                        'client_info': {
                            'timezone': str(user.timezone),
                            'app_id': app_id,
                            'uuid': uuid,
                        },
                    },
                    'input': {
                        'semantic_frames': [
                            {
                                'name': frame['name'],
                                'slots': frame['slots'],
                            }
                        ],
                    }
                }
            )

        return _inner

    @pytest.mark.asyncio
    async def test_response(self, scenario_response, response, get_state):
        response_data = await response.json()
        state = get_state()

        assert response_data == {
            'response_body': {
                'layout': {
                    'cards': [{'text': scenario_response.text}],
                    'output_speech': scenario_response.text,
                    'should_listen': False,
                    'suggest_buttons': [],
                },
                'state': {
                    'hard_expire': state.hard_expire.isoformat(),
                    'soft_expire': state.soft_expire.isoformat(),
                    'state_stack': {
                        'stack_items': [
                            {
                                'scenario_name': item.scenario_name,
                                'params': item.params,
                                'arg_name': item.arg_name,
                            }
                            for item in state.state_stack.stack_items
                        ]
                    },
                },
                'frame_actions': {},
                'entities': []
            }
        }


class TestRunMegamindHandler__protobuf(BaseTestRunMegamindHandler):
    @pytest.fixture
    def request_func(self, user, app, state, app_id, uuid, frame):
        async def _inner():
            proto = TScenarioRunRequest()

            proto.BaseRequest.ClientInfo.Timezone = str(user.timezone)
            proto.BaseRequest.ClientInfo.AppId = app_id
            proto.BaseRequest.ClientInfo.Uuid = uuid

            # frame
            frame_proto = proto.Input.SemanticFrames.add()
            frame_proto.Name = frame['name']
            for slot in frame['slots']:
                slot_proto = frame_proto.Slots.add()
                slot_proto.Name = slot['name']
                slot_proto.Value = slot['value']
                slot_proto.Type = slot['type']
                slot_proto.AcceptedTypes.extend(slot['accepted_types'])

            # state
            state_proto = TState()
            state_proto.JsonData = ujson.dumps({
                'hard_expire': state.hard_expire.isoformat(),
                'soft_expire': state.soft_expire.isoformat(),
                'state_stack': {
                    'stack_items': [
                        {
                            'scenario_name': item.scenario_name,
                            'params': item.params,
                            'arg_name': item.arg_name,
                        }
                        for item in state.state_stack.stack_items
                    ]
                }
            })
            proto.BaseRequest.State.Pack(state_proto)

            return await app.post(
                '/megamind/run',
                headers={  # Middleware requires tickets, mocked tvm always responds with ok
                    'Content-Type': 'application/protobuf',
                    'X-Ya-Service-Ticket': 'a',
                    'X-Ya-User-Ticket': user.user_ticket,
                },
                data=proto.SerializeToString(),
            )

        return _inner

    @pytest.fixture
    async def protobuf_response(self, response):
        raw_data = await response.read()
        data = TScenarioRunResponse()
        data.ParseFromString(raw_data)
        return data

    def test_response(self, scenario_response, protobuf_response):
        assert_that(protobuf_response, has_properties({
            'ResponseBody': has_properties({
                'Layout': has_properties({
                    'Cards': contains(has_properties({'Text': scenario_response.text})),
                    'OutputSpeech': scenario_response.text,
                }),
                'State': has_properties({
                    'type_url': 'type.googleapis.com/NMail.NCiao.TState',
                }),
            }),
        }))

    def test_response_state_value(self, protobuf_response, get_state):
        state_proto = TState()
        protobuf_response.ResponseBody.State.Unpack(state_proto)
        state = get_state()
        assert ujson.loads(state_proto.JsonData) == {
            'hard_expire': state.hard_expire.isoformat(),
            'soft_expire': state.soft_expire.isoformat(),
            'state_stack': {
                'stack_items': [
                    {
                        'scenario_name': item.scenario_name,
                        'params': item.params,
                        'arg_name': item.arg_name,
                    }
                    for item in state.state_stack.stack_items
                ],
            }
        }
