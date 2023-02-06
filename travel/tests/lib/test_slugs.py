# -*- encoding: utf-8 -*-
import unittest

import mock
from parameterized import parameterized


from travel.avia.api_gateway.application.cache.cache_root import CacheRoot
from travel.avia.api_gateway.lib.model_utils import get_settlement_by_code
from travel.avia.library.python.shared_dicts.cache.settlement_cache import SettlementCache
from travel.avia.library.python.shared_dicts.cache.station_cache import StationCache
from travel.avia.library.python.shared_dicts.cache.station_code_cache import StationCodeCache
from travel.proto.dicts.rasp.settlement_pb2 import TSettlement
from travel.proto.dicts.rasp.station_pb2 import TStation


class SlugsTestCase(unittest.TestCase):
    def setUp(self):
        self.settlement_cache = mock.Mock(SettlementCache)
        settlement_by_id = {
            213: TSettlement(Id=213, Slug='moscow', Iata='MOW'),
            239: TSettlement(Id=239, Slug='sochi', Iata='AER'),
            146: TSettlement(Id=146, Slug='simferopol', Iata='SIP'),
            54: TSettlement(Id=54, Slug='yekaterinburg', Iata='SVX'),
            56: TSettlement(Id=56, Slug='chelyabinsk', SirenaId='ЧЛБ'),
            42: TSettlement(Id=42, Slug='saransk'),
            2: TSettlement(Id=2, Iata='LED'),
        }
        settlement_by_iata = {s.Iata: s for s in settlement_by_id.values()}
        settlement_by_sirena = {s.SirenaId: s for s in settlement_by_id.values() if s.SirenaId}
        slug_by_id = {s.Id: s.Slug for s in settlement_by_id.values()}
        settlement_slug_by_id = {}
        self.settlement_cache.get_settlement_by_id.side_effect = settlement_by_id.get
        self.settlement_cache.get_settlement_by_iata.side_effect = settlement_by_iata.get
        self.settlement_cache.get_settlement_by_sirena_id.side_effect = settlement_by_sirena.get
        self.settlement_cache.get_slug_by_id.side_effect = slug_by_id.get
        self.settlement_cache.get_settlement_by_slug.side_effect = settlement_slug_by_id.get

        self.station_cache = mock.Mock(StationCache)
        station_by_id = {
            9600213: TStation(Id=9600213, SettlementId=213),
            9623547: TStation(Id=9623547, SettlementId=239),
            9623339: TStation(Id=9623339, SettlementId=42),
        }
        station_slug_by_id = {}
        self.station_cache.get_station_by_id.side_effect = station_by_id.get
        self.station_cache.get_station_by_slug.side_effect = station_slug_by_id.get

        self.station_code_cache = mock.Mock(StationCodeCache)
        station_id_by_code = {
            'SKX': 9623339,
        }
        self.station_code_cache.get_station_id_by_code.side_effect = station_id_by_code.get

        self.cache_root = CacheRoot(self.station_code_cache, self.settlement_cache,
                                    None, None, None, None, self.station_cache, None, None, None, None, None, None, None,)

    @parameterized.expand(
        [
            ['mow', 213],
            ['skx', 42],
            ['AER', 239],
            ['svx', 54],
            ['члб', 56],
            ['krym', 146],
        ]
    )
    def test_get_settlement_by_code(self, code, expected_id):
        actual = get_settlement_by_code(self.cache_root, code)

        self.assertEqual(expected_id, actual.Id)

    def test_get_settlement_by_unknown_code(self):
        actual = get_settlement_by_code(self.cache_root, 'c123')

        self.assertEqual(None, actual)
