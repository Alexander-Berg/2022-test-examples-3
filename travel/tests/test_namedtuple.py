# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.utils.namedtuple import namedtuple_with_defaults


def test_namedtuple_behaviour():
    tuple_class = namedtuple_with_defaults('MyClass', 'a,b', defaults={})
    instance = tuple_class(a=1, b=2)

    assert instance[0] == 1 == instance.a
    assert instance[1] == 2 == instance.b


@pytest.mark.parametrize(
    'defaults, args, kwargs, expected', [
        ({'b': 2}, (), {'a': 1}, (1, 2)),
        ({'b': 2}, (1,), {}, (1, 2)),
        ({'b': 2}, (1, 0), {}, (1, 0)),
        ({'b': 2}, (), {'a': 1, 'b': 0}, (1, 0)),
        ({'b': 2}, (1,), {'b': 0}, (1, 0)),
        ({'b': 2}, (), {'b': 0}, (None, 0)),

        ({'a': 1}, (), {'b': 2}, (1, 2)),
        ({'a': 1}, (0,), {}, (0, None)),
        ({'a': 1}, (0, 2), {}, (0, 2)),
        ({'a': 1}, (), {'a': 0, 'b': 2}, (0, 2)),
        ({'a': 1}, (0,), {'b': 2}, (0, 2)),

        ({'a': 1, 'b': 2}, (), {}, (1, 2)),
        ({'a': 0, 'b': 0}, (), {}, (0, 0)),
    ]
)
def test_namedtuple_defaults(defaults, args, kwargs, expected):
    tuple_class = namedtuple_with_defaults('MyClass', 'a,b', defaults=defaults)
    assert tuple_class(*args, **kwargs) == expected
