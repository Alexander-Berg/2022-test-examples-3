# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import datetime
import json
import six

import yatest.common
from argparse import Namespace
from psycopg2.extras import RealDictRow
from travel.cpa.collectors.buses.atlasbus import AtlasbusBusesCollector
from travel.cpa.collectors.buses.busfor import BusforBusesCollector
from travel.cpa.collectors.buses.ecolines import EcolinesBusesCollector
from travel.cpa.collectors.buses.etraffic import EtrafficBusesCollector
from travel.cpa.collectors.buses.noy import NoyBusesCollector
from travel.cpa.collectors.buses.ok import OkBusesCollector
from travel.cpa.collectors.buses.rates import BusesRates
from travel.cpa.collectors.buses.ruset import RusetBusesCollector
from travel.cpa.collectors.buses.sks import SksBusesCollector
from travel.cpa.collectors.buses.unitiki_new import UnitikiNewBusesCollector
from travel.cpa.collectors.buses.yugavtotrans import YugavtotransBusesCollector


ORDERS_PATH = 'travel/cpa/tests/ut/data/collectors/buses/orders.json'
BUSFOR_ORDERS_PATH = 'travel/cpa/tests/ut/data/collectors/buses/orders_busfor.json'
EXPECTED_SNAPSHOTS_PATH = 'travel/cpa/tests/ut/data/collectors/buses/expected_snapshots.json'
EXPECTED_SNAPSHOTS_BUSFOR_PATH = 'travel/cpa/tests/ut/data/collectors/buses/expected_snapshots_busfor.json'


collectors = [
    AtlasbusBusesCollector,
    BusforBusesCollector,
    EcolinesBusesCollector,
    EtrafficBusesCollector,
    NoyBusesCollector,
    OkBusesCollector,
    RusetBusesCollector,
    SksBusesCollector,
    UnitikiNewBusesCollector,
    YugavtotransBusesCollector
]


class BillingProvider:
    PARTNER_INFO = {
        'rates': {
            'yandex-fee': 4.0, 'revenue': 2.0
        }
    }

    PARTNER_INFO_WITH_HISTORY = {
        'rates': {
            'yandex-fee': 4.0, 'revenue': 2.0
        },
        'history': {
            'revenue': {
                datetime.date(2019, 5, 10): 0.0,
                datetime.date(2020, 5, 19): 1.0,
                datetime.date(2019, 6, 10): 0.0,
            }
        }
    }

    @staticmethod
    def get_billing():
        return {
            'partners': {test_collector.PARTNER_NAME: BillingProvider.PARTNER_INFO for test_collector in collectors}
        }

    @staticmethod
    def get_billing_with_history():
        return {
            'partners':
                {test_collector.PARTNER_NAME: BillingProvider.PARTNER_INFO_WITH_HISTORY for test_collector in collectors}
        }


class MockBusesRates(BusesRates):
    def __init__(self, bus_billing):
        super(MockBusesRates, self).__init__()
        self.bus_billing = bus_billing


def _get_expected_list(snapshots_fn, partner_name, agency_fee_amount):
    expected_list = []
    with open(yatest.common.source_path(snapshots_fn), 'r') as snapshots_file:
        snapshots = json.load(snapshots_file)
        for snapshot in snapshots:
            expected = snapshot.copy()
            expected['partner_name'] = partner_name
            expected['travel_order_id'] = ':'.join([partner_name, snapshot['partner_order_id']])
            expected['total_agency_fee_amount'] = agency_fee_amount
            expected['profit_amount'] = 30.0 + agency_fee_amount
            expected_list.append(expected)
        return expected_list


def _check_snapshots(actual_list, expected_list):
    assert len(actual_list) == len(expected_list)
    for expected, actual in zip(expected_list, actual_list):
        actual_dict = actual.as_dict()
        checked_actual_dict = {k: v for k, v in six.iteritems(actual_dict) if k in expected}
        assert checked_actual_dict == expected


def get_mocked_collector_cls(cls, orders_fn):
    class MockedBusCollector(cls):
        ORDERS_PATH = orders_fn

        def __init__(self):
            options = Namespace(updated_from='2000-01-01', updated_to='2000-01-03',
                                buses_environment='testing', vault_token='no_token')
            super(MockedBusCollector, self).__init__(options)

        def _load_orders(self):
            with open(yatest.common.source_path(self.ORDERS_PATH), 'r') as orders_file:
                dict_orders = json.load(orders_file)
                orders = []
                for order in dict_orders:
                    order['creation_ts'] = datetime.datetime.utcfromtimestamp(order['creation_ts'])
                    orders.append(RealDictRow(**order))
            return orders
    return MockedBusCollector


def test_buses_collector():
    for collector in collectors:
        mocked_collector = get_mocked_collector_cls(collector, ORDERS_PATH)()
        mocked_collector.bus_rates = MockBusesRates(BillingProvider.get_billing())
        actual_list = list(mocked_collector._get_snapshots())
        expected_list = _get_expected_list(EXPECTED_SNAPSHOTS_PATH, collector.PARTNER_NAME, 33.83)
        _check_snapshots(actual_list, expected_list)
        # test with billing history
        mocked_collector.bus_rates = MockBusesRates(BillingProvider.get_billing_with_history())
        actual_list = list(mocked_collector._get_snapshots())
        expected_list = _get_expected_list(EXPECTED_SNAPSHOTS_PATH, collector.PARTNER_NAME, 16.93)
        _check_snapshots(actual_list, expected_list)


def test_busfor_collector_revenue():
    # no revenue calculate in collector, calculate partner_commission only, see OrderProcessor
    mocked_collector = get_mocked_collector_cls(BusforBusesCollector, BUSFOR_ORDERS_PATH)()
    mocked_collector.bus_rates = MockBusesRates(BillingProvider.get_billing())
    actual_list = list(mocked_collector._get_snapshots())
    expected_list = _get_expected_list(EXPECTED_SNAPSHOTS_BUSFOR_PATH, mocked_collector.PARTNER_NAME, 33.83)
    _check_snapshots(actual_list, expected_list)
