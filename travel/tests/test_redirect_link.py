# -*- coding: utf-8 -*-
from urlparse import urlparse, parse_qs

import pytest
from django.conf import settings

from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.ticket_daemon_api.jsonrpc.complete_results import avia_deep_link, avia_order_link
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches


@pytest.mark.parametrize(
    ('nv', 'lang', 'forward_key', 'backward_key', 'clid'), [
        ('ru', 'uk', 'str fkey', 'str bkey', 100),
        ('ua', 'ru', u'юникодный fkey', None, 0),
        ('ua', 'tr', u'unicode+fkey', u'юникодный bkey', None),
        ('ru', 'ru', u'YK 884.2017-10-07T11:35,\u0414\u0416 105.2017-10-08T08:00', u'', None)
    ]
)
@pytest.mark.dbuser
def test_deeplink(nv, lang, forward_key, backward_key, clid):
    reset_all_caches()

    query = create_query(
        when='2017-09-01',
        return_date='2017-09-02' if backward_key is not None else None,
        national_version=nv,
        lang=lang,
    )

    partner_code = 'mypartner'
    PRICE = 7777.
    CREATED = 1579180053

    parsed = urlparse(
        avia_deep_link(
            query=query,
            v={
                'forward': {'key2': forward_key},
                'backward': {'key2': backward_key} if backward_key is not None else None,
                'partner_code': partner_code,
                'tariff': {'currency': 'RUR', 'value': PRICE},
                'created': CREATED
            },
            clid=clid,
        )
    )

    assert parsed.scheme == 'https'
    if nv == 'ru':
        assert parsed.netloc == settings.TRAVEL_HOST_BY_NATIONAL_VERSION[nv]
        assert parsed.path == '/avia/redirect/'
    else:
        assert parsed.netloc == settings.AVIA_HOST_BY_NATIONAL_VERSION[nv]
        assert parsed.path == '/order/redirect/'
    assert not parsed.params
    assert not parsed.fragment

    params = parse_qs(parsed.query, keep_blank_values=True, strict_parsing=True)

    if clid is not None:
        assert params.pop('clid') == [str(clid)]

    [deep_link_price] = params.pop('tariff_sign')
    assert(str(deep_link_price).startswith(u'7777.0|RUR'))

    def to_str(key):
        return key if isinstance(key, str) else key.encode('utf8')

    if backward_key is not None:
        assert params.pop('backward') == [to_str(backward_key)]

    assert params == {
        'forward': [to_str(forward_key)],
        'lang': [lang],
        'partner': [partner_code],
        'qid': [query.id]
    }


@pytest.mark.parametrize(
    ('national_version', 'lang', 'travel_link_expected'), [
        (None, 'ru', True),
        ('ru', 'ru', True),
        ('ru', 'uk', True),
        ('ua', 'uk', False),
        ('ua', 'ru', False),
        ('tr', 'tr', False),
    ]
)
@pytest.mark.dbuser
def test_order_link(national_version, lang, travel_link_expected):
    reset_all_caches()

    query = create_query(
        when='2021-06-17',
        return_date=None,
        national_version=national_version,
        lang=lang,
    )
    if not national_version:
        query.national_version = national_version

    partner_code = 'mypartner'
    PRICE = 7777.
    CREATED = 1579180053
    forward_key = 'fk'
    backward_key = None

    parsed = urlparse(
        avia_order_link(
            query=query,
            v={
                'forward': {'key2': forward_key},
                'backward': {'key2': backward_key} if backward_key is not None else None,
                'partner_code': partner_code,
                'tariff': {'currency': 'RUR', 'value': PRICE},
                'created': CREATED
            },
            clid=None,
        )
    )

    national_version = national_version or 'ru'
    assert parsed.scheme == 'https'
    if travel_link_expected:
        assert parsed.netloc == settings.TRAVEL_HOST_BY_NATIONAL_VERSION[national_version]
        assert parsed.path == '/avia/order/'
    else:
        assert parsed.netloc == settings.AVIA_HOST_BY_NATIONAL_VERSION[national_version]
        assert parsed.path == '/order/'


@pytest.mark.dbuser
def test_order_link_round_trip():
    reset_all_caches()

    national_version = 'ru'
    lang = 'ru'
    query = create_query(
        when='2021-06-17',
        return_date='2021-06-21',
        national_version=national_version,
        lang=lang,
    )

    partner_code = 'mypartner'
    PRICE = 7777.
    CREATED = 1579180053
    forward_key = 'fk'
    backward_key = 'bk'

    parsed = urlparse(
        avia_order_link(
            query=query,
            v={
                'forward': {'key2': forward_key},
                'backward': {'key2': backward_key},
                'partner_code': partner_code,
                'tariff': {'currency': 'RUR', 'value': PRICE},
                'created': CREATED
            },
            clid=None,
        )
    )

    assert parsed.scheme == 'https'
    assert parsed.netloc == settings.TRAVEL_HOST_BY_NATIONAL_VERSION[national_version]
    assert parsed.path == '/avia/order/'

    params = parse_qs(parsed.query, keep_blank_values=True, strict_parsing=True)
    assert params['forward'] == [forward_key]
    assert params['backward'] == [backward_key]
