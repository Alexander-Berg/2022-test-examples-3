import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.entities.user_info import UserInfo


@pytest.mark.parametrize('login, expected', (
    ('foo', 'foo'),
    ('Bar', 'bar'),
    (' baz\t', 'baz'),
))
def test_normalize_login(login, expected):
    user_info = UserInfo(login=login, password=None, src_login=None)
    assert_that(
        user_info.login,
        equal_to(expected),
    )


@pytest.mark.parametrize('value, expected', (
    ('value', 'value'),
    ('v a l u e', 'v a l u e'),
    (' value\t', 'value'),
    (None, None),
    ('  ', None),
))
def test_normalize_value(value, expected):
    assert_that(UserInfo._normalize_value(value), equal_to(expected))
