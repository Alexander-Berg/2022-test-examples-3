import pytest

from edera import Graph
from edera.linearizers import DFSLinearizer


@pytest.fixture
def valid_graph():
    result = Graph()
    for item in range(1, 9):
        result.add(item)
    result.link(1, 4)
    result.link(1, 5)
    result.link(2, 5)
    result.link(2, 6)
    result.link(4, 7)
    result.link(5, 7)
    result.link(6, 8)
    return result


@pytest.fixture
def invalid_graph():
    result = Graph()
    for item in range(1, 9):
        result.add(item)
    result.link(1, 4)
    result.link(1, 5)
    result.link(2, 5)
    result.link(2, 6)
    result.link(4, 7)
    result.link(5, 7)
    result.link(6, 8)
    result.link(7, 2)
    return result


@pytest.fixture
def dfs_linearizer():
    return DFSLinearizer()


@pytest.fixture(params=["dfs_linearizer"])
def linearizer(request):
    return request.getfixturevalue(request.param)
