from copy import copy

import pytest

from hamcrest import assert_that, contains

from mail.ciao.ciao.core.entities.enums import FrameName
from mail.ciao.ciao.core.entities.scenario_response import IRRELEVANT_RESPONSE, ScenarioResponse
from mail.ciao.ciao.core.entities.scenario_result import ScenarioResult
from mail.ciao.ciao.core.entities.state_stack import StateStack, StateStackItem
from mail.ciao.ciao.core.scenario_runner import ScenarioRunner
from mail.ciao.ciao.tests.utils import dummy_async_function

ALL_SCENARIO_RESULTS = {
    'irrelevant_response_result': ScenarioResult(
        response=ScenarioResponse(irrelevant=True),
    ),
    'relevant_response_result': ScenarioResult(
        response=ScenarioResponse(text='some text'),
    ),
    'call_result': ScenarioResult(
        call=(1, 2)
    ),
    'value_response_result': ScenarioResult(
        value='some value',
        response=ScenarioResponse(text='some text'),
    ),
    'value_result': ScenarioResult(
        value='some value',
    ),
}


def get_handled_parametrize(handled_ids):
    return pytest.mark.parametrize('scenario_result_id', handled_ids)


def get_not_handled_parametrize(handled_ids):
    return pytest.mark.parametrize(
        'scenario_result_id',
        set(ALL_SCENARIO_RESULTS).difference(handled_ids),
    )


@pytest.fixture
def scenario_result(scenario_result_id):
    return ALL_SCENARIO_RESULTS[scenario_result_id]


@pytest.fixture
def scenario_cls(mocker):
    async def dummy_run():
        pass

    mock = mocker.Mock()
    mock.scenario_name = 'top_scenario_name'
    mock.return_value.run = mocker.Mock(side_effect=dummy_run)
    return mock


@pytest.fixture
def state_stack(scenario_cls):
    return StateStack(stack_items=[
        StateStackItem(
            scenario_name=scenario_cls.scenario_name,
            params={},
        )
    ])


@pytest.fixture(autouse=True)
def setup(mocker, state_stack, scenario_cls):
    mocker.patch.object(
        ScenarioRunner,
        'state_stack',
        property(fget=lambda self: state_stack)
    )
    mocker.patch(
        'mail.ciao.ciao.core.scenario_runner.SCENARIO_BY_NAME',
        {
            scenario_cls.scenario_name: scenario_cls,
        }
    )


@pytest.fixture
def frame_name():
    return FrameName.UNKNOWN_FRAME


@pytest.fixture
def slots():
    return {'slot_name': 'slot_value', 'weird slot name': 'it\'s value'}


@pytest.fixture
def commit():
    return False


@pytest.fixture
def scenario_runner(frame_name, slots, commit):
    return ScenarioRunner(frame_name=frame_name, slots=slots, commit=commit)


@pytest.mark.asyncio
class TestRunTopScenario:
    @pytest.mark.parametrize('state_stack', (StateStack(),))
    async def test_empty_stack(self, scenario_runner):
        with pytest.raises(AssertionError):
            await scenario_runner._run_top_scenario()

    async def test_scenario_not_found(self, state_stack, scenario_runner):
        state_stack.top.scenario_name = 'nonexistent scenario'
        scenario_result = await scenario_runner._run_top_scenario()
        assert scenario_result.response is not None \
            and scenario_result.response.error == str(KeyError)

    async def test_scenario_error(self, scenario_cls, scenario_runner):
        async def error_run():
            raise ValueError

        scenario_cls.return_value.run.side_effect = error_run
        scenario_result = await scenario_runner._run_top_scenario()
        assert scenario_result.response is not None \
            and scenario_result.response.error == str(ValueError)

    @pytest.mark.parametrize('pass_params', (True, False))
    @pytest.mark.parametrize('commit', (True, False))
    async def test_instance_params(self, scenario_cls, state_stack, scenario_runner, frame_name, slots, commit,
                                   pass_params):
        params = state_stack.top.params = {'some_valid_identifier': 'value', 'key': 123}
        await scenario_runner._run_top_scenario(pass_params=pass_params)
        scenario_cls.assert_called_once_with(
            **params,
            frame_name=frame_name if pass_params else None,
            slots=slots if pass_params else None,
            commit=commit if pass_params else False,
        )

    async def test_result(self, rands, scenario_cls, scenario_runner):
        data = rands()

        async def return_run():
            return data

        scenario_cls.return_value.run.side_effect = return_run
        assert (await scenario_runner._run_top_scenario()) == data

    async def test_updates_params(self, state_stack, scenario_cls, scenario_runner):
        params = scenario_cls.return_value.get_params.return_value = 'new_params'
        await scenario_runner._run_top_scenario()
        assert state_stack.top.params == params


class TestHandleIrrelevantResponse:
    HANDLED_IDS = ('irrelevant_response_result',)
    HANDLED_PARAMETRIZE = get_handled_parametrize(HANDLED_IDS)
    NOT_HANDLED_PARAMETRIZE = get_not_handled_parametrize(HANDLED_IDS)

    @pytest.fixture(autouse=True)
    def stack_pop_mock(self, mocker, state_stack):
        return mocker.patch.object(state_stack, 'pop', mocker.Mock())

    @pytest.fixture
    def returned_func(self, scenario_runner, scenario_result):
        async def _inner():
            return await scenario_runner._handle_irrelevant_response(scenario_result)

        return _inner

    @HANDLED_PARAMETRIZE
    def test_handled_result(self, returned):
        assert returned

    @HANDLED_PARAMETRIZE
    def test_handled_side_effect(self, returned, stack_pop_mock):
        stack_pop_mock.assert_called_once()

    @NOT_HANDLED_PARAMETRIZE
    def test_not_handled_result(self, returned):
        assert not returned

    @NOT_HANDLED_PARAMETRIZE
    def test_not_handled_side_effect(self, returned, stack_pop_mock):
        stack_pop_mock.assert_not_called()


class TestHandleCall:
    HANDLED_IDS = ('call_result',)
    HANDLED_PARAMETRIZE = get_handled_parametrize(HANDLED_IDS)
    NOT_HANDLED_PARAMETRIZE = get_not_handled_parametrize(HANDLED_IDS)

    @pytest.fixture(autouse=True)
    def stack_append_mock(self, mocker, state_stack):
        return mocker.patch.object(state_stack, 'append', mocker.Mock())

    @pytest.fixture
    def returned_func(self, scenario_runner, scenario_result):
        async def _inner():
            return await scenario_runner._handle_call(scenario_result)

        return _inner

    @HANDLED_PARAMETRIZE
    def test_handled_result(self, returned):
        assert returned

    @HANDLED_PARAMETRIZE
    def test_handled_side_effect(self, scenario_result, returned, stack_append_mock):
        stack_append_mock.assert_called_once_with(
            scenario=scenario_result.call[0],
            arg_name=scenario_result.call[1],
        )

    @NOT_HANDLED_PARAMETRIZE
    def test_not_handled_result(self, returned):
        assert not returned

    @NOT_HANDLED_PARAMETRIZE
    def test_not_handled_side_effect(self, returned, stack_append_mock):
        stack_append_mock.assert_not_called()


class TestHandleValue:
    HANDLED_IDS = ('value_result', 'value_response_result')
    HANDLED_PARAMETRIZE = get_handled_parametrize(HANDLED_IDS)
    NOT_HANDLED_PARAMETRIZE = get_not_handled_parametrize(HANDLED_IDS)

    @pytest.fixture
    def returned_func(self, scenario_runner, scenario_result):
        async def _inner():
            return await scenario_runner._handle_value(scenario_result)

        return _inner

    @NOT_HANDLED_PARAMETRIZE
    def test_not_handled_result(self, returned):
        assert not returned

    @HANDLED_PARAMETRIZE
    class TestLastScenarioOnStack:
        @pytest.fixture
        def state_stack(self):
            return StateStack(stack_items=[StateStackItem(scenario_name='some name', params={})])

        def test_last_scenario_on_stack__result(self, returned):
            assert not returned

        def test_last_scenario_on_stack__pops(self, returned, state_stack):
            assert state_stack.empty

    @HANDLED_PARAMETRIZE
    class TestNotLastScenarioOnStack:
        @pytest.fixture
        def previous_scenario_params(self):
            return {'key': 'value'}

        @pytest.fixture
        def current_scenario_arg_name(self):
            return None

        @pytest.fixture
        def stack_items(self, previous_scenario_params, current_scenario_arg_name):
            return [
                StateStackItem(scenario_name='previous scenario', params=copy(previous_scenario_params)),
                StateStackItem(scenario_name='current scenario', params={}, arg_name=current_scenario_arg_name),
            ]

        @pytest.fixture
        def state_stack(self, stack_items):
            return StateStack(stack_items=copy(stack_items))

        def test_not_last_scenario_on_stack__result(self, returned):
            assert returned

        def test_not_last_scenario_on_stack__pops(self, returned, state_stack, stack_items):
            assert state_stack.stack_items == tuple(stack_items[:-1])

        @pytest.mark.parametrize('current_scenario_arg_name', (None,))
        def test_not_last_scenario_on_stack__no_result_passed(self, returned, state_stack, previous_scenario_params):
            assert state_stack.top.params == previous_scenario_params

        @pytest.mark.parametrize('current_scenario_arg_name', ('some_valid_identifier',))
        def test_not_last_scenario_on_stack__result_passed(self, returned, scenario_result, state_stack,
                                                           previous_scenario_params, current_scenario_arg_name):
            assert state_stack.top.params == {**previous_scenario_params, current_scenario_arg_name: scenario_result}


class TestHandleRelevantResponse:
    HANDLED_IDS = ('relevant_response_result', 'value_response_result')
    HANDLED_PARAMETRIZE = get_handled_parametrize(HANDLED_IDS)
    NOT_HANDLED_PARAMETRIZE = get_not_handled_parametrize(HANDLED_IDS)

    @pytest.fixture
    def returned_func(self, scenario_runner, scenario_result):
        async def _inner():
            return await scenario_runner._handle_relevant_response(scenario_result)

        return _inner

    @HANDLED_PARAMETRIZE
    def test_handled_result(self, returned):
        assert returned

    @NOT_HANDLED_PARAMETRIZE
    def test_not_handled_result(self, returned):
        assert not returned


@pytest.mark.asyncio
class Test_Run:
    @pytest.fixture
    def max_iterations(self):
        return 10

    @pytest.fixture(autouse=True)
    def setup_max_iterations(self, mocker, max_iterations):
        mocker.patch.object(ScenarioRunner, '_MAX_ITERATIONS', max_iterations)

    @pytest.fixture
    def handle_methods(self):
        return (
            '_handle_irrelevant_response',
            '_handle_call',
            '_handle_value',
            '_handle_relevant_response',
        )

    @pytest.fixture
    def run_top_scenario_result(self, mocker):
        return mocker.Mock()

    @pytest.fixture(autouse=True)
    def run_top_scenairo_mock(self, mocker, scenario_runner, run_top_scenario_result):
        return mocker.patch.object(
            scenario_runner,
            '_run_top_scenario',
            mocker.Mock(side_effect=dummy_async_function(run_top_scenario_result)),
        )

    @pytest.fixture
    def handle_result(self, handle_methods):
        return {
            method: False
            for method in handle_methods
        }

    @pytest.fixture(autouse=True)
    def handle_mocks(self,
                     mocker,
                     scenario_runner,
                     handle_methods,
                     handle_result,
                     ):
        def dummy_handle_builder(method):
            async def dummy_handle(*args, **kwargs):
                nonlocal handle_result, method
                return handle_result[method]

            return dummy_handle

        for method in handle_methods:
            mocker.patch.object(
                scenario_runner,
                method,
                mocker.Mock(side_effect=dummy_handle_builder(method))
            )

    @pytest.fixture(params=(
        None,
        '_handle_irrelevant_response',
        '_handle_call',
        '_handle_value',
        '_handle_relevant_response',
    ))
    def handle_method_that_handled(self, request):
        return request.param

    @pytest.fixture
    def expected_called_methods(self, handle_methods, handle_method_that_handled):
        if handle_method_that_handled is None:
            return set(handle_methods)
        else:
            return set(handle_methods[:handle_methods.index(handle_method_that_handled) + 1])

    async def test_not_handled(self, scenario_runner):
        assert await scenario_runner._run() is IRRELEVANT_RESPONSE

    @pytest.mark.parametrize('max_iterations', (1,))
    async def test_handle_calls(self, mocker, scenario_runner, handle_methods, handle_result,
                                handle_method_that_handled, expected_called_methods, run_top_scenario_result):
        handle_result[handle_method_that_handled] = True
        await scenario_runner._run()
        assert_that(
            [getattr(scenario_runner, method).call_args_list for method in handle_methods],
            contains(*[
                [mocker.call(run_top_scenario_result)] if method in expected_called_methods else []
                for method in handle_methods
            ])
        )

    async def test_returns_response(self, scenario_runner, handle_result, run_top_scenario_result):
        handle_result['_handle_relevant_response'] = True
        assert await scenario_runner._run() is run_top_scenario_result.response


class TestRun:
    TEST_IRRELEVANT_RESPONSE = ScenarioResponse(irrelevant=True)
    TEST_RELEVANT_RESPONSE = ScenarioResponse()

    SINGLE_RUN_PARAMETRIZE = pytest.mark.parametrize('_run_responses', (
        [TEST_RELEVANT_RESPONSE],
    ))
    DOUBLE_RUN_PARAMETRIZE = pytest.mark.parametrize('_run_responses', (
        [TEST_IRRELEVANT_RESPONSE, TEST_RELEVANT_RESPONSE],
    ))

    @pytest.fixture
    def state_stack(self):
        return StateStack()

    @pytest.fixture(autouse=True)
    def _run_mock(self, mocker, scenario_runner, _run_responses):
        response_iter = iter(_run_responses)

        async def dummy_run():
            return next(response_iter)

        return mocker.patch.object(scenario_runner, '_run', mocker.Mock(side_effect=dummy_run))

    @pytest.fixture
    def returned_func(self, scenario_runner):
        async def _inner():
            return await scenario_runner.run()

        return _inner

    @SINGLE_RUN_PARAMETRIZE
    def test_single_run(self, scenario_runner, _run_responses, returned):
        assert returned is _run_responses[0]

    @DOUBLE_RUN_PARAMETRIZE
    class TestDoubleRunCall:
        @pytest.fixture
        def starter_scenario(self, mocker):
            return mocker.Mock()

        @pytest.fixture(autouse=True)
        def setup_starter_scenario(self, mocker, frame_name, starter_scenario):
            if starter_scenario is not None:
                mocker.patch.object(ScenarioRunner, '_STARTER_SCENARIO_BY_FRAME', {
                    frame_name: starter_scenario,
                })

        @pytest.mark.asyncio
        @pytest.mark.parametrize('state_stack', (
            StateStack(stack_items=[StateStackItem(scenario_name='some_name', params={})]),
        ))
        async def test_asserts_empty_stack_after_first_run(self, scenario_runner):
            with pytest.raises(AssertionError):
                await scenario_runner.run()

        @pytest.mark.parametrize('starter_scenario', (None,))
        def test_starter_scenario_not_found(self, returned):
            assert returned.irrelevant and returned.error is None

        def test_returns_second_run_result(self, returned, _run_responses):
            assert returned is _run_responses[1]

        @pytest.mark.asyncio
        async def test_calls_append_before_run(self, mocker, state_stack, scenario_runner, _run_mock, starter_scenario):
            mock = mocker.Mock()
            mock.attach_mock(
                mocker.patch.object(state_stack, 'append', mocker.Mock()),
                'append',
            )
            mock.attach_mock(_run_mock, '_run')
            await scenario_runner.run()
            assert mock.mock_calls == [
                mocker.call._run(),
                mocker.call.append(scenario=starter_scenario.return_value),
                mocker.call._run(),
            ]

        def test_starter_scenario_constructor_call(self, returned, starter_scenario):
            starter_scenario.assert_called_once_with()
