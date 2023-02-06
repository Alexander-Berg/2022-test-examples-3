# coding: utf8

import pytest

from url_mapper import RaspToTouch


DISABLED_PATHS = [
    'suggests',
    'trains_on_map',
    'flights_in_air',
    'map',
    'widgets',
    'map',
    'o14',
    'buy',
    'O14',
    '014',
    'order',
    'print',
    'nearest',
    'time',
    'search/validate',
    'tablo',
    'storeurl',
    'city/prefill',
    'station/esr',
    'station/express',
]


@pytest.mark.parametrize("path", DISABLED_PATHS)
def test_rasp_to_touch_has_no_mapping(path):
    mapper = RaspToTouch(domain='rasp.yandex.net', schema='https')
    path = '/{}/whatever'.format(path)
    assert mapper.has_mapping(path) is False


@pytest.mark.parametrize("qs", DISABLED_PATHS)
def test_rasp_to_touch_has_mapping_if_disabled_string_in_end_of_path(qs):
    mapper = RaspToTouch(domain='rasp.yandex.net', schema='https')
    path = '/search/whatever_{}'.format(qs)
    assert mapper.has_mapping(path) is True


def test_rasp_to_touch_informers():
    """На самом деле мы проверяем есть ли в пути informers, но так же мы проверяем, содержит ли путь info"""
    mapper = RaspToTouch(domain='rasp.yandex.net', schema='https')
    assert mapper.has_mapping('/informers/') is False
    assert mapper.has_mapping('/whatever/informers') is False
    assert mapper.has_mapping('/whatever/info') is False
