# -*- coding: utf-8 -*-
from __future__ import print_function, unicode_literals

from collections import OrderedDict

from enum import Enum
from pytest import raises

from travel.library.python.schematized import Schematized
from travel.library.python.schematized.fields import Enumeration, Float, String


class StringEnum(Enum):
    M1 = 'v1'
    M2 = 'v2'


class C(Schematized):
    __fields__ = OrderedDict([
        ('f1', String()),
        ('f2', Float()),
        ('f3', Enumeration(StringEnum, String)),
    ])


def test_inheritance():
    class Level11(Schematized):
        __fields__ = OrderedDict(field11=String(optional=True))

    class Level21(Level11):
        __fields__ = OrderedDict(field21=String(optional=True))

    class Level22(Level11):
        __fields__ = OrderedDict(field22=String(optional=True))

    class Level31(Level21, Level22):
        __fields__ = OrderedDict(field31=String(optional=True))

    class Level41(Level31):
        __fields__ = OrderedDict(field41=String(optional=True))

    assert [Level41, Level31, Level22, Level11, Level21, Level11] == Level41().get_inheritance_chain()

    fields = list(Level41().__fields__.keys())
    assert 'field11' == fields[0]
    assert 'field21' == fields[1]
    assert 'field22' == fields[2]
    assert 'field31' == fields[3]
    assert 'field41' == fields[4]


def test_from_dict():
    d = {'f1': b'hello', 'f2': None}
    o = C.from_dict(d, convert_type=True)
    assert 'hello' == o.f1
    assert o.f2 is None

    d = {'f1': b'hello', 'f2': '0.5', 'f3': 'v2'}
    o = C.from_dict(d, convert_type=True)
    assert 'hello' == o.f1
    assert 0.5 == o.f2
    assert StringEnum.M2 == o.f3

    with raises(TypeError):
        d = {'f1': b'hello', 'f2': '0.5'}
        C.from_dict(d)

    d = {'f1': 'привет', 'f2': 0.5}
    o = C.from_dict(d)
    assert 'привет' == o.f1
    assert 0.5 == o.f2


def test_as_dict():
    o = C()
    o.f1 = 'привет'
    o.f2 = 0.5
    o.f3 = StringEnum.M1
    assert {'f1': 'привет', 'f2': 0.5, 'f3': 'v1'} == o.as_dict()


def test_diff():
    o1 = C()
    o2 = C()

    o1.f1 = 'a'
    o2.f1 = 'a'
    assert [] == Schematized.diff(o1, o2)

    o1.f1 = 'a'
    o2.f1 = 'b'
    assert [('f1', 'a', 'b')] == Schematized.diff(o1, o2)

    o1.f1 = 'a'
    o2.f1 = 'b'
    o1.f2 = 1.
    o2.f2 = 2.
    assert [('f1', 'a', 'b'), ('f2', 1., 2.)] == Schematized.diff(o1, o2)
