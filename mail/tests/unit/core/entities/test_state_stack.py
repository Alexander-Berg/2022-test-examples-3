from copy import copy

import pytest

from mail.ciao.ciao.core.entities.state_stack import StateStack, StateStackItem


class TestStateStack:
    @pytest.fixture
    def empty_stack(self):
        return StateStack()

    @pytest.fixture
    def not_empty_stack(self):
        return StateStack(stack_items=[
            StateStackItem(scenario_name='scenario1', params={'some': 'data'}, arg_name='arg_name1'),
            StateStackItem(scenario_name='scenario2', params={}),
            StateStackItem(scenario_name='scenario3', params={}),
        ])

    @pytest.fixture(params=(
        pytest.param(0, id='empty_stack'),
        pytest.param(1, id='not_empty_stack'),
    ))
    def stack(self, request, empty_stack, not_empty_stack):
        return (empty_stack, not_empty_stack)[request.param]

    def test_top_empty(self, empty_stack):
        assert empty_stack.top is None

    def test_top_not_empty(self, not_empty_stack):
        assert not_empty_stack.top is not_empty_stack._stack_items[-1]

    def test_empty(self, empty_stack, not_empty_stack):
        assert empty_stack.empty and not not_empty_stack.empty

    def test_append(self, mocker, stack):
        scenario = mocker.Mock()
        scenario_name = scenario.scenario_name = 'some_scenario_name'
        params = scenario.get_params.return_value = {'key': 'value'}
        arg_name = 'some_arg_name'
        stack.append(scenario, arg_name)
        assert all((
            stack.top.scenario_name == scenario_name,
            stack.top.params == params,
            stack.top.arg_name == arg_name,
        ))

    def test_pop_empty(self, empty_stack):
        with pytest.raises(AssertionError):
            empty_stack.pop()

    def test_pop_not_empty(self, not_empty_stack):
        items = copy(not_empty_stack._stack_items)
        returned = not_empty_stack.pop()
        assert returned == items[-1] and not_empty_stack._stack_items == items[:-1]
