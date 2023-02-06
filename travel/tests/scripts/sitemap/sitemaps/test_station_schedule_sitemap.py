# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from django.contrib.sites.models import Site

from hamcrest import has_entries, assert_that, contains_inanyorder

from common.data_api.baris.test_helpers import mock_baris_response
from common.tester.factories import create_station, create_station_terminal
from common.models.transport import TransportType

from travel.rasp.admin.scripts.sitemap.sitemaps.station import StationScheduleSitemap


@pytest.mark.dbuser
def test_station_schedule_sitemap():
    create_station(
        id=204, t_type=TransportType.TRAIN_ID, type_choices=''
    )
    create_station(
        id=205, t_type=TransportType.TRAIN_ID, type_choices='train'
    )
    create_station(
        id=206, t_type=TransportType.TRAIN_ID, type_choices='suburban'
    )
    create_station(
        id=207, t_type=TransportType.TRAIN_ID, type_choices='train,suburban'
    )

    create_station(
        id=210, t_type=TransportType.PLANE_ID, type_choices=''
    )
    create_station(
        id=211, t_type=TransportType.PLANE_ID, type_choices='tablo'
    )
    airport = create_station(
        id=212, t_type=TransportType.PLANE_ID, type_choices='tablo'
    )
    create_station_terminal(station=airport, name='A')
    create_station_terminal(station=airport, name='B')

    create_station(
        id=221, t_type=TransportType.BUS_ID, type_choices='schedule'
    )

    with mock_baris_response({
        "flights": [
            {'departureStation': 211, 'arrivalStation': 212, 'flightsCount': 1, 'totalFlightsCount': 1}
        ]}
    ):
        sitemap = StationScheduleSitemap()
        urls = sitemap.get_urls(site=Site(domain='t.rasp.yandex.ru'))

        assert len(urls) == 15
        assert_that(urls, contains_inanyorder(
            has_entries({'location': 'https://t.rasp.yandex.ru/station/205/'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/205/?event=arrival'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/206/'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/207/'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/207/?event=arrival'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/207/suburban/'}),

            has_entries({'location': 'https://t.rasp.yandex.ru/station/211/'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/211/?event=arrival'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/212/'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/212/?event=arrival'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/212/?terminal=A'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/212/?event=arrival&terminal=A'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/212/?terminal=B'}),
            has_entries({'location': 'https://t.rasp.yandex.ru/station/212/?event=arrival&terminal=B'}),

            has_entries({'location': 'https://t.rasp.yandex.ru/station/221/'}),
        ))
