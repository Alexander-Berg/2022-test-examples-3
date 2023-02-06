# coding: utf-8

import pytest
from pymdb import fnv1

parametrize = pytest.mark.parametrize


@parametrize(('char', 'value'), [
    (u't', 116),
    (u'\u044f', 53647),
])
def test_ascii(char, value):
    assert fnv1.ascii(char) == value


@parametrize(('string', 'value'), [
    (u'', 14695981039346656037),
    (u'Your receipt No.180061923943', 57719951403834279),
    (u'Без темы', 17048815841131914618),
    (u'不知火 舞', 14428919260234711724),
])
def test_ora_fnv1(string, value):
    assert fnv1.ora_fnv1(string) == value


@parametrize(('string', 'value'), [
    (u'', 14695981039346656037),
    (u'test', 10090666253179731817),
    (u'\u0422\u0435\u0441\u0442', 11329441945975021271),
])
def test_fnv1(string, value):
    assert fnv1.fnv1(string) == value
