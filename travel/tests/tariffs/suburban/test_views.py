# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

from django.test import Client
from hamcrest import assert_that, has_entries, anything, contains

from common.models.factories import create_tariff_type, create_aeroex_tariff, create_tariff_group
from common.models.tariffs import TariffType
from common.tester.factories import create_station
from common.tester.testcase import TestCase


class TestSettlementDirections(TestCase):
    def setUp(self):
        self.client = Client()
        self.station_from = create_station()
        self.station_to = create_station()
        self.group = create_tariff_group(title='Group title', title_uk='Group title (uk)')
        self.tariff_type = create_tariff_type(
            code='xxx', title='TariffType title', order=100500, is_main=True,
            link='TariffType link', category=TariffType.USUAL_CATEGORY,
            title_uk='TariffType title (uk)', description_uk='TariffType description (uk)',
            __=dict(tariff_groups=[self.group])
        )
        create_aeroex_tariff(
            station_from=self.station_from, station_to=self.station_to,
            type=self.tariff_type, precalc=False,
            tariff=10, suburban_search=True, reverse=False
        )

    def test_suburban_tariffs(self):
        response = self.client.get('/uk/tariffs/suburban/?pointFrom={}&pointTo={}'.format(self.station_from.point_key,
                                                                                          self.station_to.point_key))
        assert response.status_code == 200
        data = json.loads(response.content)
        assert len(data['tariffs']) == len(data['groups']) == 1

        has_our_tariff = has_entries(
            order=self.tariff_type.order,
            isMain=self.tariff_type.is_main,
            title=self.tariff_type.L_title(lang='uk'),
            description=self.tariff_type.L_description(lang='uk'),
            url=self.tariff_type.link,
            price=has_entries(
                value=10,
                currency='RUR'
            )
        )

        assert_that(data['tariffs'][0], has_entries(
            classes=has_entries(suburban=has_our_tariff),
            key=anything(),
            suburbanCategories=has_entries({
                TariffType.USUAL_CATEGORY: contains(has_our_tariff)
            })
        ))

        assert_that(data['groups'][0], has_entries(
            stationFromId=self.station_from.id,
            stationToId=self.station_to.id,
            id=self.group.id,
            title=self.group.L_title(lang='uk'),
            categories=has_entries({
                TariffType.USUAL_CATEGORY: contains(has_our_tariff)
            })
        ))
