# -*- coding: utf-8 -*-
from __future__ import print_function, unicode_literals

from enum import Enum
from pytest import raises

from travel.library.python.schematized.fields import Float, UInt64, String, Enumeration


def test_float():
    f = Float()
    assert 'VT_DOUBLE' == f.get_logfeller_type_name()
    assert 'double' == f.get_yt_type_name()

    f.check(5.)

    with raises(TypeError):
        f.check('3')


def test_integer():
    f = UInt64()
    assert 'VT_UINT64' == f.get_logfeller_type_name()
    assert 'uint64' == f.get_yt_type_name()

    f.check(5)

    with raises(TypeError):
        f.check(3.)


def test_string():
    f = String()
    assert 'VT_STRING' == f.get_logfeller_type_name()
    assert 'string' == f.get_yt_type_name()

    f.check('5')

    with raises(TypeError):
        f.check(3)


def test_enum():
    class UInt64EnumClass(Enum):
        M1 = 1
        M2 = 2

    class StringEnumClass(Enum):
        M1 = 'value1'
        M2 = 'value2'

    f = Enumeration(StringEnumClass, String)
    f.check(StringEnumClass.M1)

    f.check(f.get_underlying_type()('value2'))

    f = Enumeration(UInt64EnumClass, UInt64, default=UInt64EnumClass.M2)

    with raises(TypeError):
        f.check(StringEnumClass.M1)

    with raises(TypeError):
        Enumeration(UInt64EnumClass, String)
