# -*- coding: utf-8 -*-
from market.dynamic_pricing.pricing.common.checks.solomon_sender.price_report_checker import GroupInfo
from market.dynamic_pricing.pricing.common.checks.solomon_sender.report_api import ReportApi
from market.dynamic_pricing.pricing.library.errors import error_codes_dict, ReportWarningCode
from market.dynamic_pricing.pricing.library.types import PriceGroupType

import json
import logging
import sys
import pytest


POSITIVE_MSKU = 100607784828
POSITIVE_SSKU = "Й000017.1134210"
GROUP_ID = 2000
AGGREGATIONS_COUNT = 3  # all_ssku all_msku all_ssku_crossed
logging.basicConfig(stream=sys.stdout, level=logging.DEBUG, format='%(levelname)s %(asctime)s %(message)s')


class MockReportApi(ReportApi):

    def request_sku(self, sku_list):
        sku = sku_list[0] # пока тестируем 1 ску
        if int(sku) != POSITIVE_MSKU:
            return json.loads('{"search": { "results": [ ]}}')
        content = '''{{
                "search": {{
                    "totalOffers": 1,
                    "results":
                    [
                        {{
                            "supplier": {{
                                "id": 465852
                            }},
                            "marketSku": "{msku}",
                            "sku": "{msku}",
                            "supplierSku": "ЙЙ000017.1134210",
                            "shopSku": "{ssku}",
                            "prices": {{
                                "currency": "RUR",
                                "value": "10890",
                                "rawValue": "10890",
                                "discount": {{
                                    "oldMin": "10000"
                                }}
                            }}
                        }}
                    ]
                }}
            }}'''.format(msku=POSITIVE_MSKU, ssku=POSITIVE_SSKU)
        return json.loads(content)


@pytest.fixture
def report():
    return MockReportApi()


@pytest.fixture
def group_info():
    return GroupInfo(GROUP_ID)


def test_ssku_no_errors(report, group_info):
    group_info.check_msku_report(report, [(POSITIVE_MSKU, POSITIVE_SSKU, 10890, GROUP_ID, PriceGroupType.DCO)])
    assert POSITIVE_MSKU in group_info.report_msku_info
    assert len(group_info.metrics) == len(error_codes_dict) + AGGREGATIONS_COUNT
    assert all([v == 0 for k, v in group_info.metrics.items()]), ' '.join([k for k, v in group_info.metrics.items() if v!=0])


def test_ssku_no_errors_crossed(report, group_info):
    group_info.check_msku_report(report, [(POSITIVE_MSKU, POSITIVE_SSKU, 10000, GROUP_ID, PriceGroupType.DCO_CROSSED)])
    assert POSITIVE_MSKU in group_info.report_msku_info
    assert len(group_info.metrics) == len(error_codes_dict) + AGGREGATIONS_COUNT
    assert all([v == 0 for k, v in group_info.metrics.items()])


def test_ssku_not_on_report(report, group_info):
    '''проверка что мску нет на репорте '''
    group_info.check_msku_report(
        report, [(-1, POSITIVE_SSKU, 10000, GROUP_ID, PriceGroupType.DCO)]
    )
    assert POSITIVE_MSKU not in group_info.report_msku_info
    assert len(group_info.metrics) == len(error_codes_dict) + AGGREGATIONS_COUNT
    assert 1 == group_info.metrics[ReportWarningCode.OUT_OFF_STOCK_REPORT.error_name]


def test_ssku_not_on_report_crossed(report, group_info):
    '''проверка что мску нет на репорте, но цена зачеркнутая'''
    group_info.check_msku_report(
        report, [(1, POSITIVE_SSKU, 10000, GROUP_ID, PriceGroupType.DCO_CROSSED)]
    )
    assert POSITIVE_MSKU not in group_info.report_msku_info
    assert len(group_info.metrics) == len(error_codes_dict) + AGGREGATIONS_COUNT
    assert 1 == group_info.metrics[ReportWarningCode.OUT_OFF_STOCK_REPORT.error_name]


def test_ssku_price_unexpected(report, group_info):
    '''проверка цена на репорте не такая как у нас'''
    group_info.check_msku_report(
        report, [(POSITIVE_MSKU, POSITIVE_SSKU, -1, GROUP_ID, PriceGroupType.DCO)]
    )
    assert POSITIVE_MSKU in group_info.report_msku_info
    assert len(group_info.metrics) == len(error_codes_dict) + AGGREGATIONS_COUNT
    assert 1 == group_info.metrics[ReportWarningCode.UNEXPECTED_PRICE.error_name]


def test_ssku_price_unexpected_crossed(report, group_info):
    '''проверка перечеркнутая цена на репорте не такая как у нас'''
    group_info.check_msku_report(
        report, [(POSITIVE_MSKU, POSITIVE_SSKU, -1, GROUP_ID, PriceGroupType.DCO_CROSSED)]
    )
    assert POSITIVE_MSKU in group_info.report_msku_info
    assert len(group_info.metrics) == len(error_codes_dict) + AGGREGATIONS_COUNT
    assert 1 == group_info.metrics[ReportWarningCode.UNEXPECTED_PRICE_CROSSED.error_name]
