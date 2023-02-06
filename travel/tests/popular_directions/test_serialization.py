# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import pytest

from common.models.currency import Price
from common.models.geo import Settlement
from travel.rasp.train_api.popular_directions.serialization import ArrivalSettlementSchema


@pytest.mark.dbuser
class TestArrivalSettlementSchema(object):
    def test_dump_best_offer(self):
        settlement, other_settlement = Settlement(id=1), Settlement(id=2)
        best_offer = {
            'departure_date': date(2000, 1, 1),
            'number': '123Ж',
            'price': Price(100),
        }

        assert 'bestOffer' not in ArrivalSettlementSchema(
            strict=True
        ).dump(settlement).data

        assert 'bestOffer' not in ArrivalSettlementSchema(
            strict=True, context={'best_offers': {other_settlement: best_offer}}
        ).dump(settlement).data

        assert ArrivalSettlementSchema(
            strict=True, context={'best_offers': {settlement: best_offer}}
        ).dump(settlement).data['bestOffer'] == {
            'departureDate': '2000-01-01',
            'number': '123Ж',
            'price': {'value': 100, 'currency': 'RUR'},
        }

    def test_dump_image_url(self):
        settlement = Settlement(id=1)

        assert 'imageUrl' not in ArrivalSettlementSchema(
            strict=True
        ).dump(settlement).data

        assert ArrivalSettlementSchema(
            strict=True, context={'image_urls': {settlement: 'image_url'}}
        ).dump(settlement).data['imageUrl'] == 'image_url'
