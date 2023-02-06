# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.tester.factories import create_settlement, create_station
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.tariffs.train.segment_builder.helpers.sales_depth import (
    digits_only, SalesDepthManager, SalesDepthRule
)


class SegmentStub(object):
    def __init__(self, number, start_station, end_station, is_suburban=False):
        self.number = number
        self.start_station = start_station
        self.end_station = end_station
        self.is_suburban = is_suburban


@pytest.mark.parametrize('number, expected_digits', (
    ('001А', '001'),
    ('201МЩ', '201'),
    ('301М1', '301'),
    ('strange_number', ''),
    ('strange_number22', ''),
    ('', ''),
))
def test_digits_only(number, expected_digits):
    assert digits_only(number) == expected_digits


@pytest.mark.dbuser
def test_sales_depth():
    station_a = create_station(settlement=create_settlement())
    station_b = create_station(settlement=create_settlement())
    station_c = create_station(settlement=create_settlement())
    station_d = create_station(settlement=create_settlement())
    no_rule_station = create_station(settlement=create_settlement())
    no_rule_number = '100500КР'

    sales_depth_manager = SalesDepthManager(sales_depth_rules=(
        SalesDepthRule(station_a.settlement_id, station_b.settlement_id, ('120',), 120),
        SalesDepthRule(station_c.settlement_id, station_d.settlement_id, ('030',), 30, both_directions=False),
    ), suburban_sales_depth=10)

    with replace_dynamic_setting('TRAIN_PURCHASE_SOLD_OUT_DEFAULT_SALES_DEPTH', 90):
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', station_a, station_b)) == 120
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', station_b, station_a)) == 120
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', station_a, no_rule_station)) == 90
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', no_rule_station, station_b)) == 90
        assert sales_depth_manager.get_sales_depth(SegmentStub(no_rule_number, station_a, station_b)) == 90
        assert sales_depth_manager.get_sales_depth(SegmentStub(no_rule_number, station_b, station_a)) == 90

        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', station_a, station_b, is_suburban=True)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', station_b, station_a, is_suburban=True)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', station_a, no_rule_station, is_suburban=True)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', no_rule_station, station_b, is_suburban=True)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub(no_rule_number, station_a, station_b, is_suburban=True)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub(no_rule_number, station_b, station_a, is_suburban=True)) == 10

        assert sales_depth_manager.get_sales_depth(SegmentStub('030Ж', station_c, station_d)) == 30
        assert sales_depth_manager.get_sales_depth(SegmentStub('030Ж', station_d, station_c)) == 90

        assert sales_depth_manager.get_sales_depth(SegmentStub('811Я', station_a, station_b, is_suburban=False)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('812Я', station_b, station_a, is_suburban=False)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('820Я', station_a, no_rule_station, is_suburban=False)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('834Я', no_rule_station, station_b, is_suburban=False)) == 10

        assert sales_depth_manager.get_sales_depth(SegmentStub('7011Я', station_a, station_b, is_suburban=False)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('7012Я', station_b, station_a, is_suburban=False)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('7020Я', station_a, no_rule_station, is_suburban=False)) == 10
        assert sales_depth_manager.get_sales_depth(SegmentStub('7034Я', no_rule_station, station_b, is_suburban=False)) == 10

    with replace_dynamic_setting('TRAIN_PURCHASE_SOLD_OUT_DEFAULT_SALES_DEPTH', 45):
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', station_a, no_rule_station)) == 45
        assert sales_depth_manager.get_sales_depth(SegmentStub('120Я', no_rule_station, station_b)) == 45
        assert sales_depth_manager.get_sales_depth(SegmentStub(no_rule_number, station_a, station_b)) == 45
        assert sales_depth_manager.get_sales_depth(SegmentStub(no_rule_number, station_b, station_a)) == 45
