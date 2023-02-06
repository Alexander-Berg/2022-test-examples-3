from dataclasses import dataclass
from functools import reduce
from operator import add

from mail.husky.stages.worker.util.map_dataclass import MapDataclass


@dataclass
class Data(MapDataclass):
    x: str
    y: int


def test_map_dataclass():
    init = Data('x', 42)
    result = Data.map(lambda x: x * 2, init)
    assert result.x == 'xx'
    assert result.y == 84


def test_map_multiple_objects():
    args = [
        Data('a', 600),
        Data('b', 60),
        Data('c', 6),
    ]
    result = Data.map(lambda *xs: reduce(add, xs), *args)
    assert result.x == 'abc'
    assert result.y == 666
