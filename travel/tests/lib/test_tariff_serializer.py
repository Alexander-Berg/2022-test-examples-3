# coding: utf-8

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.ticket_daemon_api.jsonrpc.lib.tariff_serializer import TariffSerializer

PRICE_VALUE = '123'
PRICE_CURRENCY = 'RUR'
PRICE_UNIXTIME = '1579243540'


class TariffSerializerTest(TestCase):
    def setUp(self):
        self.tariff = {
            'value': PRICE_VALUE,
            'currency': PRICE_CURRENCY,
            'price_unixtime': PRICE_UNIXTIME,
        }

    def test_serialize(self):
        price, currency, sign_hash, price_unixtime = TariffSerializer.serialize(
            self.tariff, self.tariff['price_unixtime']
        ).split('|')

        assert PRICE_VALUE == price
        assert PRICE_CURRENCY == currency
        assert PRICE_UNIXTIME == price_unixtime

    def test_deserialize(self):
        tariff = TariffSerializer.deserialize(
            TariffSerializer.serialize(self.tariff, self.tariff['price_unixtime']),
        )

        assert tariff is not None
        self.assertDictEqual(tariff, self.tariff)
