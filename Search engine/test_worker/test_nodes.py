import typing

from search.beholder.src.worker.nodes.abc_nodes import AbstractNode, NodeInput, NodeOutput, NodeState, NodeArguments
from search.beholder.src.worker.nodes.base import LongToShortNode, DropGraphStateNode
from search.beholder.src.worker.nodes.logic import AnyNode


class NodeTestInput(dict):
    def __getitem__(self, item):
        if item not in self:
            return None
        return super().__getitem__(item)

    def __iter__(self):
        return iter(self.values())


class NodeConfig(typing.NamedTuple):
    input: NodeInput
    state: NodeState
    arguments: NodeArguments


class NodeTestState(typing.NamedTuple):
    output: NodeOutput
    state: NodeState


class NodeTest(typing.NamedTuple):
    config: NodeConfig
    expect: NodeTestState


class NodesTester:
    def __init__(self, node_type: typing.Type[AbstractNode]):
        self.node_type = node_type

    def check(self, node_input=None, state=None, arguments=None, expected_output_diff=None, expected_state_diff=None):
        test = NodeTest(
            config=NodeConfig(
                input=node_input or {},
                arguments=arguments or {},
                state=state or {},
            ),
            expect=NodeTestState(
                output=expected_output_diff or {},
                state=expected_state_diff or {},
            )
        )

        node = self.create_node(test)
        output_diff, state_diff = node.update()

        assert output_diff == test.expect.output
        assert state_diff == test.expect.state

    def create_node(self, test: NodeTest):
        node = self.node_type('foo', 'bar', test.config.arguments)
        node.input = NodeTestInput(test.config.input)
        node.state.storage.states['bar', 'foo'] = test.config.state
        return node


def test_long_to_short():
    arguments = {
        'equal_function': '\"x[2] == y[2]\"',
    }

    test = NodesTester(LongToShortNode)

    test.check(
        arguments=arguments,
        node_input={0: ('host', 'service', 'OK', 'func_id')},
        expected_output_diff={0: ('host', 'service', 'OK', 'func_id')},
        expected_state_diff={'current': ('host', 'service', 'OK', 'func_id')},
    )

    test.check(
        arguments=arguments,
        node_input={0: ('host', 'service', 'OK', 'func_id')},
        state={'current': ('host', 'service', 'OK', 'func_id')},
        expected_output_diff={},
        expected_state_diff={},
    )


def test_any_node():
    test = NodesTester(AnyNode)

    test.check(
        expected_output_diff={}
    )
    test.check(
        node_input={0: 'foo'},
        expected_output_diff={0: 'foo'},
    )
    test.check(
        node_input={1: 'foo', 2: 'bar'},
        expected_output_diff={0: 'foo'},
    )


def test_drop_graph_state_node():
    test = NodesTester(DropGraphStateNode)

    test.check(
        expected_output_diff={}
    )
    test.check(
        node_input={0: 'foo'},
        expected_output_diff={0: True},
    )
