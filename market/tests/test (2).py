# coding: utf-8

from hamcrest import assert_that, equal_to
from market.idx.pictures.pylibrary.urls import url_to_id


def test_yandex():
    assert_that(
        url_to_id('http://yandex.ru/'),
        equal_to('xguuYx0peCyp6gXFMr-Wwg')
    )


def test_base():
    assert_that(
        url_to_id('http://yandex.ru/'),
        equal_to('xguuYx0peCyp6gXFMr-Wwg')
    )


def test_unicode():
    assert_that(
        url_to_id(u'https:/static-eu.insales.ru/images/products/1/6936/121256728/1_Дед_Мороз_плюш_красный__184_.jpg'),
        equal_to('jKfxvqzd1PCsUkelF8F7ug')
    )
