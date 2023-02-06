# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from urlparse import urlparse, parse_qsl

import pytest

from travel.avia.ticket_daemon.ticket_daemon.lib.http import url_complement_missing


def test_url_complement_missing():
    params = {'utm_source': 'yandex', 'deepParam': 'deeepVal'}
    url = url_complement_missing(
        'https://booking.yandex.ru/Search.aspx?s=2',
        params
    )
    for key in params:
        assert key in url


params_variants = [
    {},
    {'key': 'value'},
    {'key1': 'value1', 'key2': 'value2'},
]


@pytest.mark.parametrize('ext_params', params_variants)
def test_add_params__to_pure_url(faker, ext_params):
    url = url_complement_missing(faker.url(), ext_params)

    assert ext_params == _extract_params(url)


@pytest.mark.parametrize('params', params_variants)
@pytest.mark.parametrize('ext_params', [
    {},
    {'ext_key': 'value'},
    {'ext_key1': 'ext_value1', 'ext_key2': 'value2'},
])
def test_add_params__to_url_with_params(faker, params, ext_params):
    url = url_complement_missing(faker.url(), params)
    new_url = url_complement_missing(url, ext_params)

    params.update(ext_params)

    assert params == _extract_params(new_url)


def _extract_params(url):
    return dict(parse_qsl(urlparse(url).query))
