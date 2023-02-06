import pytest

from edera.exceptions import CyclicDependencyError


def test_linearizer_performs_topological_ordering(valid_graph, linearizer):
    linearization = linearizer.linearize(valid_graph)
    assert set(linearization) == set(valid_graph)
    for index, item in enumerate(linearization):
        assert valid_graph[item].parents <= set(linearization[:index])


def test_linearizer_reports_about_cycle_dependencies(invalid_graph, linearizer):
    with pytest.raises(CyclicDependencyError) as info:
        linearizer.linearize(invalid_graph)
    assert tuple(info.value.cycle) in {(2, 5, 7), (5, 7, 2), (7, 2, 5)}
