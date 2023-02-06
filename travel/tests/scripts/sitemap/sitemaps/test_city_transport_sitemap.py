# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.contrib.sites.models import Site

from common.models.transport import TransportType
from common.models.geo import Country
from common.tester.factories import create_settlement, create_station, create_thread, create_transport_type

from travel.rasp.admin.scripts.sitemap.sitemaps.settlement_transport import SettlementTransportSitemap


create_thread = create_thread.mutate(__={'calculate_noderoute': True})


@pytest.mark.dbuser
def test_city_transport_sitemap():
    train = create_transport_type(TransportType.TRAIN_ID)
    bus = create_transport_type(TransportType.BUS_ID)
    russia = Country.objects.get(id=Country.RUSSIA_ID)

    ekb = create_settlement(slug='ekb', country=russia)
    create_station(settlement=ekb, t_type=train, majority=1, type_choices='train,suburban')
    create_station(settlement=ekb, t_type=train, majority=4, type_choices='train')
    ekb_related = create_station(t_type=train, majority=1, type_choices='suburban')
    ekb.related_stations.create(station=ekb_related)

    create_station(settlement=ekb, t_type=bus, majority=1, type_choices='schedule')
    create_station(settlement=ekb, t_type=bus, majority=1, type_choices='schedule')

    piter = create_settlement(slug='piter', country=russia)
    create_station(settlement=piter, t_type=train, majority=1, type_choices='train,suburban')
    connected = create_station(t_type=train, majority=2, type_choices='train,suburban')
    piter.station2settlement_set.create(station=connected)

    create_station(settlement=piter, t_type=bus, majority=1, type_choices='schedule')

    sitemap = SettlementTransportSitemap()
    urls = sitemap.get_urls(site=Site(domain='rasp.yandex.ru'))

    assert len(urls) == 4
    assert {url['location'] for url in urls} == {
        'https://rasp.yandex.ru/suburban/ekb',
        'https://rasp.yandex.ru/bus/ekb',
        'https://rasp.yandex.ru/train/piter',
        'https://rasp.yandex.ru/suburban/piter'
    }
