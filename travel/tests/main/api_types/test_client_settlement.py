# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from travel.avia.library.python.common.models.geo import Settlement

from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestClientSettlementHandler(TestApiHandler):
    def setUp(self):
        super(TestClientSettlementHandler, self).setUp()
        self.settlement = Settlement.objects.get(id=Settlement.MOSCOW_ID)
        self.foreign_settlement = Settlement.objects.get(id=Settlement.KIEV_ID)

        # подкручиваем москву
        self.msk = Settlement.objects.get(id=Settlement.MOSCOW_ID)
        self.msk._geo_id = Settlement.MOSCOW_ID
        self.msk.save()

    def test_our_city(self):
        geo_id = self.send_request(geo_id=self.settlement._geo_id)
        assert geo_id == self.settlement._geo_id

    def test_unknown_city(self):
        geo_id = self.send_request(geo_id=42)
        assert geo_id == Settlement.MOSCOW_ID

    def test_foreign_city(self):
        geo_id = self.send_request(geo_id=self.foreign_settlement._geo_id)
        assert geo_id == Settlement.MOSCOW_ID

    def send_request(self, geo_id, root_domain='ru'):
        payload = {
            'name': 'clientSettlement',
            'params': {
                'geoId': geo_id,
                'rootDomain': root_domain
            },
            'fields': ['geoId']
        }

        return self.api_data(payload)['data'][0]['geoId']
