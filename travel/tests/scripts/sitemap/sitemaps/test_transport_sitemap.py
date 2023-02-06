# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from django.contrib.sites.models import Site

from travel.rasp.admin.scripts.sitemap.sitemaps.transport import TransportSitemap


FULL_TRANSPORT_DATA = {
    'train': {
        'RU': {'main_city': 'moscow_slug'},
        'KZ': {'main_city': 'nur-sultan_slug'}
    },
    'bus': {
        'RU': {'main_city': 'moscow_slug'}
    }
}


def test_transport_sitemap():
    with mock.patch(
        'travel.rasp.admin.scripts.sitemap.sitemaps.transport.get_full_transport_data',
        return_value=FULL_TRANSPORT_DATA
    ):
        sitemap = TransportSitemap()
        urls = sitemap.get_urls(site=Site(domain='rasp.yandex.ru'))

        assert len(urls) == 2
        assert {url['location'] for url in urls} == {
            'https://rasp.yandex.ru/train',
            'https://rasp.yandex.ru/bus',
        }
