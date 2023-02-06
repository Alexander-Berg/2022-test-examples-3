# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement, create_suburban_zone
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement import settlement_suburban_zone_json


class TestSuburbanZoneJson(TestCase):
    def test_settlement_with_suburban_zone(self):
        suburban_zone_settlement_id = 999
        suburban_zone_settlement = create_settlement(id=suburban_zone_settlement_id)

        suburban_zone_id = 1000
        suburban_zone = create_suburban_zone(
            id=suburban_zone_id,
            settlement=suburban_zone_settlement,
            code='zone code',
        )

        settlement = create_settlement(suburban_zone=suburban_zone)

        assert settlement_suburban_zone_json(settlement) == {
            'id': suburban_zone_id,
            'settlement': {
                'id': suburban_zone_settlement_id
            }
        }

    def test_settlement_without_suburban_zone(self):
        settlement = create_settlement()
        assert settlement_suburban_zone_json(settlement) is None
