# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.library.python.rasp_vault import cli


def test_empty():
    assert cli.build_tag_expresion(None)(['tag'])
    assert cli.build_tag_expresion('')(['tag'])


@pytest.mark.parametrize('tag_name', [
    'a-b',
    'm'
])
def test_single_tag(tag_name):
    assert cli.build_tag_expresion(tag_name)([tag_name])
    assert not cli.build_tag_expresion(tag_name)([])
    assert not cli.build_tag_expresion(tag_name)(['other-tag'])


def test_and_expr():
    checker = cli.build_tag_expresion('a and b-c')
    assert checker(['a', 'b-c'])
    assert checker(['a', 'b_c'])
    assert not checker(['c'])
    assert not checker([])
    assert not checker(['a'])
    assert not checker(['b-c'])


def test_or_expr():
    checker = cli.build_tag_expresion('a or b-c')
    assert checker(['a', 'b-c'])
    assert checker(['a'])
    assert checker(['b-c'])
    assert not checker(['c'])
    assert not checker([])


def test_not_expr():
    checker = cli.build_tag_expresion('not a')
    assert not checker(['a', 'b-c'])
    assert not checker(['a'])
    assert checker(['b-c'])
    assert checker(['c'])
    assert checker([])


def test_parenthesis_expr():
    checker = cli.build_tag_expresion('(not a) and (b or c)')
    assert checker(['b'])
    assert checker(['c'])
    assert checker(['b', 'c', 'y'])
    assert not checker([])
    assert not checker(['d'])
    assert not checker(['a'])
    assert not checker(['a', 'c'])
