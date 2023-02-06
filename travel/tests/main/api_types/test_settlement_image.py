# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from travel.avia.library.python.tester.factories import create_settlement
from travel.avia.library.python.tester.factories import create_settlement_image

from travel.avia.backend.main.api_types.settlement_image import get_default_images
from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestSettlementImageHandler(TestApiHandler):
    def setUp(self):
        super(TestSettlementImageHandler, self).setUp()

        self.some_settlement = create_settlement()
        self.another_settlement = create_settlement()

        self.default_image = create_settlement_image()
        self.another_image = create_settlement_image()

        get_default_images.cache_clear()

    def test_settlement_image(self):
        some_image = create_settlement_image(settlement=self.some_settlement)
        another_image = create_settlement_image(settlement=self.another_settlement)

        payload = [{
            'name': 'settlementImage',
            'params': {
                'settlementId': self.some_settlement.id
            },
            'fields': ['id', 'url2']
        }, {
            'name': 'settlementImage',
            'params': {
                'settlementId': self.another_settlement.id
            },
            'fields': ['id', 'url2']
        }]

        data = self.api_data(payload)

        assert data == {
            'status': 'success',
            'data': [{
                'id': some_image.id,
                'url2': some_image.url2
            }, {
                'id': another_image.id,
                'url2': another_image.url2
            }]
        }

    def test_check_default_fields(self):
        some_image = create_settlement_image(settlement=self.some_settlement)

        payload = [{
            'name': 'settlementImage',
            'params': {
                'settlementId': self.some_settlement.id
            }
        }]

        data = self.api_data(payload)

        assert data == {
            'status': 'success',
            'data': [{
                'url2': some_image.url2
            }]
        }

    def test_check_default_images(self):
        payload = [{
            'name': 'settlementImage',
            'params': {
                'settlementId': self.some_settlement.id
            },
            'fields': ['id']
        }]

        data = self.api_data(payload)

        assert data['status'] == 'success'
        assert data['data'][0]['id'] in set([self.default_image.id, self.another_image.id])
