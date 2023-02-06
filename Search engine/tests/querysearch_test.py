import pytest

from pytest_factor.factortypes.query import Query


def test_ctor():
    assert Query(text='text', lr=1).text == 'text'
    assert Query(cgi={'text': 'text', 'lr': 1}).text == 'text'
    assert Query(text='text', lr=1).lr == 1
    assert Query(cgi={'text': 'text', 'lr': 1}).lr == 1


def test_ctor_assert_less():
    with pytest.raises(AssertionError):
        Query(text='text')


def test_ctor_assert_more():
    Query(text='text', lr=1, param='param')
