import pytest

from mail.beagle.beagle.utils.graph import CycleFoundError, expand_acyclic_graph


@pytest.mark.parametrize('graph,expected', (
    ({}, {}),
    ({1: set(), 2: set()}, {1: set(), 2: set()}),
    (
        {
            1: {2},
            2: {3},
        },
        {
            1: {2, 3},
            2: {3},
            3: set(),
        }
    ),
    (
        {
            1: {2, 3},
            2: {4},
            4: {3},
        },
        {
            1: {2, 3, 4},
            2: {3, 4},
            3: set(),
            4: {3},
        }
    ),
))
def test_expands(graph, expected):
    assert expand_acyclic_graph(graph) == expected


@pytest.mark.parametrize('graph', (
    {1: {1}},
    {1: {2}, 2: {1}},
    {1: {2}, 2: {3}, 3: {1}},
    {
        1: {2, 3, 4},
        3: {5},
        5: {6, 7, 8},
        7: {1},
    },
))
def test_raises_cycle_error(graph):
    with pytest.raises(CycleFoundError):
        expand_acyclic_graph(graph)
