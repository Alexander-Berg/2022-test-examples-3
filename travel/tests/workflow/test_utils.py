# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.workflow.utils import get_by_dotted_path, set_by_dotted_path, get_update_with_prefix, merge_updates


class A(object):
    def __init__(self, **kwargs):
        for k, v in kwargs.items():
            setattr(self, k, v)


def test_get_by_dotted_path():
    a = A(b=A(
        c={'foo': A(
            l=[None, None, A(boo={'k': {'l2': [{}, {'d': 42}]}}), None]
        )}
    ))

    path = 'b.c.foo.l.2.boo.k.l2.'
    assert get_by_dotted_path(a, path + '1.d') == 42
    assert get_by_dotted_path(a, path + '1') == {'d': 42}

    assert get_by_dotted_path(a, path + '1.e', 43) == 43

    with pytest.raises(AttributeError):
        assert get_by_dotted_path(a, path + '1.e')

    with pytest.raises(IndexError):
        assert get_by_dotted_path(a, path + '2.d')

    a = A(l=[1, 42, 3])
    assert get_by_dotted_path(a, 'l.1') == 42

    a = [1, 2, A(v=42)]
    assert get_by_dotted_path(a, '2.v') == 42


def test_set_by_dotted_path():
    a = A(b=A(
        c={'foo': A(
            l=[None, None, A(boo={'k': {'l2': [{}, {'d': 42}]}}), None]
        )}
    ))

    path = 'b.c.foo.l.2.boo.k.l2.'
    assert get_by_dotted_path(a, path + '1.d') == 42
    set_by_dotted_path(a, path + '1.d', 44)
    assert get_by_dotted_path(a, path + '1.d') == 44

    a = A(l=[1, 2, 3])
    set_by_dotted_path(a, 'l.2', 42)
    assert get_by_dotted_path(a, 'l.2') == 42


def test_get_update_with_prefix():
    upd = {'$set': {'a': 1}, '$unset': {'b.c.d': '3'}}
    result = get_update_with_prefix(upd, 'some.prefix.0.42.wow')
    assert result == {'$set': {'some.prefix.0.42.wow.a': 1}, '$unset': {'some.prefix.0.42.wow.b.c.d': '3'}}


def test_merge_updates():
    upd_from = {'$set': {'a': 1}}
    upd_to = {'$set': {'b': 2}}
    merge_updates(upd_from, upd_to)
    assert upd_from == {'$set': {'a': 1, 'b': 2}}
    assert upd_to == {'$set': {'b': 2}}

    merge_updates(upd_from, {'$unset': {'c': 3}})
    assert upd_from == {'$set': {'a': 1, 'b': 2}, '$unset': {'c': 3}}
