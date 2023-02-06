# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.wizard_lib.views.helpers.service_urls import (
    format_morda_url, format_touch_url, format_trains_url,
    get_morda_host, get_touch_host, get_trains_host
)


@pytest.fixture(autouse=True)
def set_hosts_by_tld():
    with (
        replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru', 'ua': 'rasp.yandex.ua'}),
        replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru', 'ua': 't.rasp.yandex.ua'})
    ):
        yield


@pytest.mark.parametrize('tld, expected', [
    ('ru', 'rasp.yandex.ru'),
    ('ua', 'rasp.yandex.ua'),
    ('com.am', 'rasp.yandex.ru'),
])
def test_get_morda_host(tld, expected):
    assert get_morda_host(tld) == expected


@pytest.mark.parametrize('tld, expected', [
    ('ru', 't.rasp.yandex.ru'),
    ('ua', 't.rasp.yandex.ua'),
    ('com.am', 't.rasp.yandex.ru'),
])
def test_get_touch_host(tld, expected):
    assert get_touch_host(tld) == expected


@pytest.mark.parametrize('tld, expected', [
    ('ru', 'trains.yandex.ru'),
])
def test_get_trains_host(tld, expected):
    assert get_trains_host(tld) == expected


def test_format_morda_url():
    assert format_morda_url('/so_cool/', 'param=42', 'ru') == 'https://rasp.yandex.ru/so_cool/?param=42'


def test_format_touch_url():
    assert format_touch_url('/cool/', 'param=42', 'ru') == 'https://t.rasp.yandex.ru/cool/?param=42'


def test_format_trains_url():
    assert format_trains_url('/more/', 'param=42', 'ru') == 'https://trains.yandex.ru/more/?param=42'
