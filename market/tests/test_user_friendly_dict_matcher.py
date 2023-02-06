# coding: utf-8

import pytest
from hamcrest import (
    assert_that,
    is_not,
)

from market.idx.yatf.matchers.user_friendly_dict_matcher import user_friendly_dict_equal_to


def make_nested_dict(id, name, message):
    return {
        'id': 1,
        'uno': {
            'dos': {
                'tres': ':)',
                'numbers': {
                    'int': 10,
                    'float': 3.14,
                },
                'strings': {
                    'str': 'string',
                    'unicode': u'string',
                    'bytes': b'string',
                },
                'lists': {
                    'list': [{'a': 10, 'b': {'bb': None}, 'c': [{'d': []}]}]
                },
                'ids': {
                    'id': id,
                    'name': name,
                    'title': message,
                }
            },
        },
    }


@pytest.mark.parametrize('name', ['T800', u'T800', b'T800'])
def test_equal_dicts(name):
    """
    Проверяем, что равные словари равны
    """
    actual = make_nested_dict(1, name, 'Hasta la vista')
    expected = make_nested_dict(1, name, 'Hasta la vista')
    assert_that(
        actual,
        user_friendly_dict_equal_to(expected),
        "result mismatch"
    )


def test_unequal_dicts():
    """
    Проверяем, что неравные словари не равны
    """
    actual = make_nested_dict(1, 'T800', 'Hasta la vista')
    expected = make_nested_dict(1, 'T1000', 'Hasta la vista')
    assert_that(
        actual,
        is_not(user_friendly_dict_equal_to(expected)),
        "result mismatch"
    )


@pytest.mark.parametrize('message', ['А́ста ла ви́ста, бе́йби', u'А́ста ла ви́ста, бе́йби'])
def test_unicode_equal_dicts(message):
    """
    Проверяем, что матчер работает с юникодом, когда словари равны
    """
    actual = make_nested_dict(1, 'T800', message)
    expected = make_nested_dict(1, 'T800', message)
    assert_that(
        actual,
        user_friendly_dict_equal_to(expected, strict_encoding=False),
        "result mismatch"
    )


@pytest.mark.parametrize('message', ['А́ста ла ви́ста, бе́йби', u'А́ста ла ви́ста, бе́йби'])
def test_unicode_unequal_dicts(message):
    """
    Проверяем, что матчер работает с юникодом, когда словари не равны
    """
    actual = make_nested_dict(1, 'T800', message)
    expected = make_nested_dict(1, 'T800', 'Асталависта, бэби')
    assert_that(
        actual,
        is_not(user_friendly_dict_equal_to(expected, strict_encoding=False)),
        "result mismatch"
    )


def test_two_none():
    """
    Проверяем, что матчер возвращает равно для двух None
    """
    assert_that(
        None,
        user_friendly_dict_equal_to(None),
        "result mismatch"
    )


def test_two_empty_dicts():
    """
    Проверяем, что матчер возвращает равно для двух пустых словарей
    """
    assert_that(
        dict(),
        user_friendly_dict_equal_to(dict()),
        "result mismatch"
    )


@pytest.mark.parametrize('not_dict_argument', [None, 1, 'string', [1, 2]])
@pytest.mark.parametrize('is_swap', [True, False])
def test_not_dict_argument(not_dict_argument, is_swap):
    """
    Проверяем, что матчер бросает AssertionError, если один из аргументов не dict()
    """
    first, second = dict(), not_dict_argument
    if is_swap:
        first, second = second, first

    with pytest.raises(AssertionError):
        assert_that(
            first,
            user_friendly_dict_equal_to(second),
            "result mismatch"
        )
