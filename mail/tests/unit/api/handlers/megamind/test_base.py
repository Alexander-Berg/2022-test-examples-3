from datetime import datetime, timezone

import pytest
import pytz
import ujson

from sendr_utils import temp_set

from mail.ciao.ciao.api.handlers.megamind.base import BaseMegamindHandler
from mail.ciao.ciao.core.entities.enums import FrameName
from mail.ciao.ciao.core.entities.scenario_response import ScenarioResponse
from mail.ciao.ciao.core.entities.state import State
from mail.ciao.ciao.core.entities.state_stack import StateStack, StateStackItem
from mail.ciao.ciao.core.scenarios.base import BaseScenario


class BaseTestUsesGetFrame:
    @pytest.fixture
    def frame(self):
        return {}

    @pytest.fixture(autouse=True)
    def get_frame_mock(self, mocker, frame):
        return mocker.patch.object(
            BaseMegamindHandler,
            'get_frame',
            mocker.Mock(return_value=frame),
        )


class BaseTestUsesIsProtobuf:
    @pytest.fixture(params=(
        pytest.param(True, id='protobuf'),
        pytest.param(False, id='not_protobuf'),
    ))
    def is_protobuf(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def is_protobuf_mock(self, mocker, is_protobuf):
        mocker.patch.object(
            BaseMegamindHandler,
            'is_protobuf',
            property(fget=lambda self: is_protobuf),
        )


class TestBaseMegamindHandler:
    @pytest.fixture
    def request_mock(self, mocker):
        return mocker.MagicMock()

    @pytest.fixture
    def handler(self, request_mock):
        return BaseMegamindHandler(request=request_mock)

    @pytest.mark.parametrize('content_type,is_protobuf', (
        ('application/protobuf', True),
        ('application/json', False),
    ))
    def test_is_protobuf(self, request_mock, handler, content_type, is_protobuf):
        request_mock.content_type = content_type
        assert handler.is_protobuf == is_protobuf

    @pytest.mark.parametrize('data,is_voice', (
        ({}, False),
        ({'input': {}}, False),
        ({'input': {'text': {}}}, False),
        ({'input': {'voice': {}}}, True),
    ))
    def test_is_voice(self, data, is_voice):
        assert BaseMegamindHandler.is_voice(data) == is_voice

    @pytest.mark.parametrize('data,timezone', (
        ({'base_request': {'client_info': {'timezone': 'UTC'}}}, pytz.timezone('UTC')),
        ({'base_request': {'client_info': {'timezone': 'Europe/Moscow'}}}, pytz.timezone('Europe/Moscow')),
    ))
    def test_get_timezone(self, data, timezone):
        assert BaseMegamindHandler.get_timezone(data) == timezone

    @pytest.mark.parametrize('data,request_id', (
        ({}, None),
        ({'base_request': {}}, None),
        ({'base_request': {'request_id': 'some_request_id'}}, 'some_request_id'),
    ))
    def test_get_request_id(self, data, request_id):
        assert BaseMegamindHandler.get_request_id(data) == request_id

    def test_get_uuid(self, rands):
        uuid = rands()
        assert BaseMegamindHandler.get_uuid({
            'base_request': {
                'client_info': {
                    'uuid': uuid,
                },
            },
        }) == uuid

    @pytest.mark.parametrize('data,app_id', (
        ({}, None),
        ({'base_request': {}}, None),
        ({'base_request': {'client_info': {}}}, None),
        ({'base_request': {'client_info': {'app_id': 'some_app_id'}}}, 'some_app_id'),
    ))
    def test_get_app_id(self, data, app_id):
        assert BaseMegamindHandler.get_app_id(data) == app_id

    @pytest.mark.parametrize('data,text', (
        ({}, None),
        ({'input': {}}, None),
        ({'input': {'voice': {}}}, None),
        ({'input': {'voice': {'utterance': 'some voice'}}}, 'some voice'),
        ({'input': {'text': {}}}, None),
        ({'input': {'text': {'utterance': 'some text'}}}, 'some text'),
    ))
    def test_get_text(self, data, text):
        assert BaseMegamindHandler.get_text(data) == text

    class TestRequestResponseSchema(BaseTestUsesIsProtobuf):
        def test_request_schema__result(self, handler, is_protobuf):
            handler.PROTOBUF_SCHEMAS = (1, 2)
            handler.JSON_SCHEMAS = (3, 4)
            if is_protobuf:
                schemas = handler.PROTOBUF_SCHEMAS
            else:
                schemas = handler.JSON_SCHEMAS
            assert (handler.request_schema, handler.response_schema) == schemas

    class TestGetState:
        @pytest.fixture
        def data(self):
            return {
                'base_request': {
                    'state': {
                        'soft_expire': datetime(3000, 1, 2, tzinfo=timezone.utc),
                        'hard_expire': datetime(4000, 3, 4, tzinfo=timezone.utc),
                        'state_stack': {
                            'stack_items': [
                                {
                                    'scenario_name': 'some scenario',
                                    'params': {},
                                    'arg_name': 'some arg name',
                                },
                                {
                                    'scenario_name': 'some scenario2',
                                    'params': {'x': 'y'},
                                },
                            ],
                        },
                    },
                },
            }

        @pytest.fixture(autouse=True)
        def now(self, mocker):
            now = datetime(2020, 1, 31, 18, 9, 10, tzinfo=timezone.utc)
            mocker.patch('mail.ciao.ciao.core.entities.state.utcnow', mocker.Mock(return_value=now))
            return now

        @pytest.mark.parametrize('data', (
            {},
            {'base_request': {}},
            {'base_request': {'state': {}}},
        ))
        def test_get_state__empty_data(self, data):
            assert BaseMegamindHandler.get_state(data) == State()

        def test_get_state__expired_data(self, mocker, data):
            mocker.patch.object(State, 'expired', property(fget=lambda self: True))
            assert BaseMegamindHandler.get_state(data) == State()

        def test_get_state__loads_stack(self, data):
            BaseMegamindHandler.get_state(data).state_stack == StateStack(stack_items=[
                StateStackItem(
                    scenario_name=stack_item['scenario_name'],
                    params=stack_item['params'],
                    arg_name=stack_item.get('arg_name'),
                )
                for stack_item in data['base_request']['state']['state_stack']['stack_items']
            ])

    class TestGetFrameName:
        @pytest.mark.parametrize('frame,frame_name', (
            ({}, None),
            ({'name': 'some name'}, 'some name'),
        ))
        def test_get_frame_name__result(self, frame, frame_name):
            assert BaseMegamindHandler.get_frame_name(frame) == FrameName.from_string(frame_name)

    class TestGetSlots:
        @pytest.mark.parametrize('frame_data,slots', (
            ({}, {}),
            ({'slots': []}, {}),
            ({'slots': [{'name': 'x', 'parsed': 'y'}]}, {'x': 'y'}),
            (
                {'slots': [{'name': 'a', 'parsed': 'b'}, {'name': 'c', 'parsed': 'd'}]},
                {'a': 'b', 'c': 'd'},
            ),
        ))
        def test_get_slots__result(self, frame_data, slots):
            assert BaseMegamindHandler.get_slots(frame_data) == slots

    class TestMakeResponseData:
        @pytest.fixture
        def state(self):
            state = object()
            with temp_set(BaseScenario.context, 'state', state):
                yield state

        @pytest.fixture
        def response_body(self, state):
            return {
                'response_body': {'state': state}
            }

        @pytest.fixture
        def frame_name(self):
            return FrameName.UNKNOWN_FRAME

        @pytest.fixture(autouse=True)
        def get_frames_data_mock(self, mocker, rands):
            return mocker.patch.object(
                BaseMegamindHandler,
                'get_frames_data',
                mocker.Mock(return_value=rands()),
            )

        def test_make_response_data__empty(self, frame_name, state):
            assert BaseMegamindHandler.make_response_data(ScenarioResponse(), frame_name) == {
                'response_body': {
                    'state': state,
                    'frame_actions': {},
                    'layout': {'should_listen': False},
                },
            }

        def test_make_response_data__commit(self, mocker, rands, frame_name, state, response_body,
                                            get_frames_data_mock):
            request_data = rands()
            assert all((
                BaseMegamindHandler.make_response_data(ScenarioResponse(commit=True), frame_name, request_data) == {
                    'commit_candidate': {
                        'response_body': {
                            'state': state,
                            'frame_actions': {},
                            'layout': {'should_listen': False},
                        },
                        'arguments': {
                            'semantic_frames': get_frames_data_mock.return_value,
                        },
                    },
                },
                get_frames_data_mock.mock_calls == [mocker.call(request_data)]
            ))

        def test_make_response_data__text(self, rands, frame_name, state, response_body):
            text = rands()
            assert BaseMegamindHandler.make_response_data(ScenarioResponse(text=text), frame_name) == {
                'response_body': {
                    'state': state,
                    'frame_actions': {},
                    'layout': {
                        'output_speech': text,
                        'cards': [{'text': text}],
                        'should_listen': False,
                        'suggest_buttons': {},
                    },
                },
            }

        def test_make_response_data__irrelevant(self, frame_name, state):
            assert BaseMegamindHandler.make_response_data(ScenarioResponse(irrelevant=True), frame_name) == {
                'response_body': {
                    'state': state,
                    'frame_actions': {},
                    'layout': {'should_listen': False},
                },
                'features': {'is_irrelevant': True},
            }

        @pytest.mark.parametrize('input_key', ('voice', 'text'))
        def test_make_response_data__requested_slot_text(self, frame_name, state, input_key):
            slot_name = 'slot_name'
            slot_type = 'slot_type'
            assert BaseMegamindHandler.make_response_data(
                scenario_response=ScenarioResponse(requested_slot=(slot_name, slot_type)),
                frame_name=frame_name,
                request_data={'input': {input_key: {}}},
            ) == {
                'response_body': {
                    'state': state,
                    'frame_actions': {},
                    'semantic_frame': {
                        'name': frame_name.value,
                        'slots': [
                            {'name': slot_name, 'accepted_types': [slot_type], 'is_requested': True},
                        ],
                    },
                    'layout': {
                        'should_listen': input_key == 'voice',
                    },
                },
            }

        def test_make_response_data__sensitive(self, rands, frame_name, state):
            text = rands()
            assert BaseMegamindHandler.make_response_data(
                ScenarioResponse(text=text, contains_sensitive_data=True),
                frame_name,
            ) == {
                'response_body': {
                    'state': state,
                    'frame_actions': {},
                    'layout': {
                        'cards': [{'text': text}],
                        'output_speech': text,
                        'contains_sensitive_data': True,
                        'should_listen': False,
                        'suggest_buttons': {},
                    },
                },
            }

        def test_make_response_data__buttons_expected_frames(self, rands, frame_name, state):
            text = rands()
            buttons = [rands(), rands()]
            expected_frames = [rands(), rands()]
            suggests = [rands(), rands()]
            assert BaseMegamindHandler.make_response_data(ScenarioResponse(
                text=text,
                buttons=buttons,
                expected_frames=expected_frames,
                suggests=suggests,
            )) == {
                'response_body': {
                    'state': state,
                    'frame_actions': {
                        **{
                            f'button_{i}': button
                            for i, button in enumerate(buttons)
                        },
                        **{
                            f'expected_frame_{i}': expected_frame
                            for i, expected_frame in enumerate(expected_frames)
                        },
                        **{
                            f'suggest_{i}': suggest
                            for i, suggest in enumerate(suggests)
                        },
                    },
                    'layout': {
                        'cards': [{
                            'text_with_buttons': {
                                'text': text,
                                'buttons': {
                                    f'button_{i}': button
                                    for i, button in enumerate(buttons)
                                },
                            },
                        }],
                        'output_speech': text,
                        'should_listen': False,
                        'suggest_buttons': {
                            f'suggest_{i}': suggest
                            for i, suggest in enumerate(suggests)
                        },
                    },
                },
            }

    @pytest.mark.asyncio
    class TestGetData(BaseTestUsesIsProtobuf):
        @pytest.fixture
        def read_value(self, rands):
            return rands()

        @pytest.fixture
        def json_value(self, rands):
            return rands()

        @pytest.fixture(autouse=True)
        def setup_request(self, request_mock, read_value, json_value):
            async def dummy_read():
                return read_value

            async def dummy_json():
                return json_value

            request_mock.read.side_effect = dummy_read
            request_mock.json.side_effect = dummy_json

        @pytest.fixture
        def parsed_data(self, rands):
            return rands()

        @pytest.fixture(autouse=True)
        def request_schema_mock(self, mocker, parsed_data):
            mock = mocker.Mock()
            mock.load = mocker.Mock(return_value=(parsed_data, ...))
            mocker.patch.object(BaseMegamindHandler, 'request_schema', property(fget=lambda self: mock))

        async def test_get_data__load_call(self, read_value, json_value, handler, is_protobuf, ):
            await handler.get_data()
            data = read_value if is_protobuf else json_value
            handler.request_schema.load.assert_called_once_with(data)

        async def test_get_data__result(self, request, handler, parsed_data):
            assert await handler.get_data() == parsed_data

    class TestMakeResponse(BaseTestUsesIsProtobuf):
        @pytest.fixture
        def scenario_response(self, rands):
            return rands()

        @pytest.fixture
        def response_data(self, rands):
            return rands()

        @pytest.fixture
        def dump_result(self, rands):
            return {rands(): rands()}

        @pytest.fixture(autouse=True)
        def make_response_data_mock(self, mocker, response_data):
            return mocker.patch.object(
                BaseMegamindHandler,
                'make_response_data',
                mocker.Mock(return_value=response_data)
            )

        @pytest.fixture(autouse=True)
        def response_schema_mock(self, mocker, dump_result):
            mock = mocker.Mock()
            mock.dump.return_value = (dump_result, ...)
            mocker.patch.object(BaseMegamindHandler, 'response_schema', property(fget=lambda self: mock))
            return mock

        @pytest.fixture(autouse=True)
        def web_response_mock(self, mocker):
            return mocker.patch('mail.ciao.ciao.api.handlers.megamind.base.web.Response', mocker.Mock(side_effect=dict))

        @pytest.fixture
        def frame_name(self):
            return FrameName.UNKNOWN_FRAME

        @pytest.fixture
        def request_data(self, mocker):
            return mocker.Mock()

        @pytest.fixture
        def returned(self, frame_name, request_data, handler, scenario_response):
            return handler.make_response(scenario_response, frame_name, request_data)

        def test_make_response__make_response_data_call(self, frame_name, request_data, scenario_response,
                                                        make_response_data_mock, returned):
            make_response_data_mock.assert_called_once_with(scenario_response, frame_name, request_data)

        def test_make_response__response_schema_dump_call(self, response_data, response_schema_mock, returned):
            response_schema_mock.dump.assert_called_once_with(response_data)

        @pytest.mark.parametrize('is_protobuf', (True,))
        def test_make_response__response_call_protobuf(self, dump_result, web_response_mock, returned):
            web_response_mock.assert_called_once_with(body=dump_result, content_type='application/protobuf')

        @pytest.mark.parametrize('is_protobuf', (False,))
        def test_make_response__response_call_json(self, dump_result, web_response_mock, returned):
            web_response_mock.assert_called_once_with(text=ujson.dumps(dump_result), content_type='application/json')

        @pytest.mark.parametrize('is_protobuf', (True,))
        def test_make_response__result_protobuf(self, dump_result, web_response_mock, returned):
            assert returned == {'body': dump_result, 'content_type': 'application/protobuf'}

        @pytest.mark.parametrize('is_protobuf', (False,))
        def test_make_response__result_json(self, dump_result, web_response_mock, returned):
            assert returned == {'text': ujson.dumps(dump_result), 'content_type': 'application/json'}
