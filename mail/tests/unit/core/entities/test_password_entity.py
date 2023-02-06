import pytest

from hamcrest import assert_that, equal_to, not_

from mail.ipa.ipa.core.entities.password import Password


def test_eq():
    assert_that(Password.from_plain('123456123456'), equal_to(Password.from_plain('123456123456')))


def test_neq():
    assert_that(Password.from_plain('123456123456'), not_(equal_to(Password.from_plain('123456123457'))))


@pytest.mark.parametrize('alien', ('123456123456', 123456123456, None))
def test_eq_alien(alien):
    assert_that(Password.from_plain('123456123456'), not_(equal_to(alien)))


def test_encrypt_decrypt():
    password = Password.from_plain('123456123456')
    encrypted = password.encrypted()
    assert_that(Password(encrypted), equal_to(password))


def test_value():
    password = '123456123456'
    assert_that(Password.from_plain(password).value(), equal_to(password))


def test_str():
    password = Password.from_plain('123456123456')
    assert str(password).find('123456123456') < 0
