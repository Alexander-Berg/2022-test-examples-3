# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from django.contrib.sites.models import Site
from hamcrest import assert_that, contains_inanyorder

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.transport import TransportType, TransportSubtype
from common.tester.factories import create_settlement, create_station, create_thread, create_transport_subtype
from common.tester.testcase import TestCase

from travel.rasp.admin.scripts.sitemap.sitemaps.search import LastochkaSitemap


class TestLastochkaSiteMap(TestCase):
    def _create_thread(self, station_from, station_to, t_subtype, t_type=TransportType.TRAIN_ID):
        return create_thread(
            __={'calculate_noderoute': True},
            t_type=t_type,
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ]
        )

    def get_urls(self):
        sitemap = LastochkaSitemap(use_cache=False)
        urls = sitemap.get_urls(site=Site(domain='rasp.yandex.ru'))

        return [url['location'] for url in urls]

    def assert_sitemap_contains(self, expected_urls):
        with mock_baris_response({'flights': []}):
            actual_urls = self.get_urls()
        assert_that(actual_urls, contains_inanyorder(*expected_urls))

    def test_can_create_map(self):
        from_station = create_station(slug='from_station', settlement=create_settlement(slug='from_city'))
        to_station = create_station(slug='to_station', settlement=create_settlement(slug='to_city'))

        t_subtype = create_transport_subtype(
            id=TransportSubtype.LAST_SUBURBAN_ID, t_type=TransportType.SUBURBAN_ID, code='lastochka')

        self._create_thread(from_station, to_station, t_subtype, t_type=TransportType.SUBURBAN_ID)

        self.assert_sitemap_contains([
            'https://rasp.yandex.ru/lastochka/from_station--to_station'
        ])

    def test_contains_no_duplicates_with_different_t_types(self):
        from_station = create_station(slug='from_station', settlement=create_settlement(slug='from_city'))
        to_station = create_station(slug='to_station', settlement=create_settlement(slug='to_city'))

        t_sub_suburban = create_transport_subtype(
            id=TransportSubtype.LAST_SUBURBAN_ID, t_type=TransportType.SUBURBAN_ID, code='lastochka')

        t_sub_train = create_transport_subtype(
            id=TransportSubtype.LASTDAL_ID, t_type=TransportType.TRAIN_ID, code='lastochka_dal')

        self._create_thread(from_station, to_station, t_sub_suburban, t_type=TransportType.SUBURBAN_ID)
        self._create_thread(from_station, to_station, t_sub_train, t_type=TransportType.TRAIN_ID)

        self.assert_sitemap_contains([
            'https://rasp.yandex.ru/lastochka/from_station--to_station',
            'https://rasp.yandex.ru/lastochka/from_city--to_city'
        ])

    def test_two_routes_one_city(self):
        city_from = create_settlement(slug='from_city')
        city_to = create_settlement(slug='to_city')

        from_station = create_station(slug='from_station', settlement=city_from)
        to_station = create_station(slug='to_station', settlement=city_to)
        from_station2 = create_station(slug='from_station2', settlement=city_from)
        to_station2 = create_station(slug='to_station2', settlement=city_to)

        t_subtype = create_transport_subtype(
            id=TransportSubtype.LAST_SUBURBAN_ID, t_type=TransportType.SUBURBAN_ID, code='lastochka')

        self._create_thread(from_station, to_station, t_subtype, t_type=TransportType.SUBURBAN_ID)
        self._create_thread(from_station2, to_station2, t_subtype, t_type=TransportType.SUBURBAN_ID)

        self.assert_sitemap_contains([
            'https://rasp.yandex.ru/lastochka/from_station--to_station',
            'https://rasp.yandex.ru/lastochka/from_station2--to_station2',
            'https://rasp.yandex.ru/lastochka/from_city--to_city',
        ])

    def test_can_ignore_another_subtypes(self):
        from_station = create_station(slug='from_station', settlement=create_settlement(slug='from_city'))
        to_station = create_station(slug='to_station', settlement=create_settlement(slug='to_city'))

        t_subtype = create_transport_subtype(id=777, t_type=TransportType.SUBURBAN_ID, code='sapsan')

        self._create_thread(from_station, to_station, t_subtype, t_type=TransportType.SUBURBAN_ID)

        self.assert_sitemap_contains([])
