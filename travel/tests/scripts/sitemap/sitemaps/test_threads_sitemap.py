# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.contrib.sites.models import Site

from common.tester.factories import create_thread
from travel.rasp.admin.scripts.sitemap.sitemaps.thread import ThreadsSitemap


@pytest.mark.dbuser
def test_threads_sitemap():
    create_thread(uid='uid1', canonical_uid='R_1')
    create_thread(uid='uid2', canonical_uid='R_2')
    create_thread(uid='uid3', canonical_uid='T_1')

    sitemap = ThreadsSitemap()
    urls = sitemap.get_urls(site=Site(domain='rasp.yandex.ru'))

    assert len(urls) == 2
    assert urls[0]['location'] == 'https://rasp.yandex.ru/thread/R_1'
    assert urls[1]['location'] == 'https://rasp.yandex.ru/thread/R_2'
