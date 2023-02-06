import pytest
import copy
from nile.api.v1 import Record
from market.dynamic_pricing.pricing.common.checks.checker.checker import map_and_check
from market.dynamic_pricing.pricing.library.errors import CheckerErrorCode

def test_checker_factory():
    data = []
    record1 = Record(
        base_rule_name="autostrategy",
        checker_alg="combined",
        checker_config="{\"checkers\": [{\"config\": {\"margin_threshold\": -0.5, \"ignore_fixed\": true}, \"name\": \"low_margin_sku\"}, {\"config\": {\"margin_threshold\": 0.33, \"ignore_fixed\": true}, \"name\": \"high_margin_sku\"}, {\"config\": {\"coefficient\": 0.8, \"ignore_fixed\": true}, \"name\": \"cheap_against_white_sku\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"out_off_stock_sku\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"wrong_bounds_sku\"}, {\"config\": {\"coefficient\": 0.5, \"ignore_fixed\": false}, \"name\": \"purchase_price_warning\"}, {\"config\": {\"coefficient\": 0.5, \"ignore_fixed\": false}, \"name\": \"cost_price_warning\"}, {\"config\": {\"demand_threshold\": 0.0, \"ignore_fixed\": false}, \"name\": \"demand_zero_warning\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"empty_bounds_sku\"}, {\"config\": {\"threshold\": 5.0, \"ignore_fixed\": false}, \"name\": \"cost_purchase_warning\"}]}",
        demand=0,
        group_id=2000,
        hid=13314877,
        lower_bound=169.14,
        market_sku=1,
        new_price=187.51,
        purchase_price=153.76,
        shop_sku="000109.61002555-7",
        stock=6,
        upper_bound=238.32,
        abc_status='A')
    record2 = Record(
        base_rule_name="autostrategy",
        checker_alg="combined",
        checker_config="{\"checkers\": [{\"config\": {\"margin_threshold\": -0.5, \"ignore_fixed\": true}, \"name\": \"low_margin_sku\"}, {\"config\": {\"margin_threshold\": 0.33, \"ignore_fixed\": true}, \"name\": \"high_margin_sku\"}, {\"config\": {\"coefficient\": 0.8, \"ignore_fixed\": true}, \"name\": \"cheap_against_white_sku\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"out_off_stock_sku\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"wrong_bounds_sku\"}, {\"config\": {\"coefficient\": 0.5, \"ignore_fixed\": false}, \"name\": \"purchase_price_warning\"}, {\"config\": {\"coefficient\": 0.5, \"ignore_fixed\": false}, \"name\": \"cost_price_warning\"}, {\"config\": {\"demand_threshold\": 0.0, \"ignore_fixed\": false}, \"name\": \"demand_zero_warning\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"empty_bounds_sku\"}, {\"config\": {\"threshold\": 5.0, \"ignore_fixed\": false}, \"name\": \"cost_purchase_warning\"}]}",
        demand=1,
        group_id=2000,
        hid=13314877,
        lower_bound=169.14,
        market_sku=2,
        new_price=187.51,
        purchase_price=153.76,
        shop_sku="000109.61002555-7",
        stock=6,
        upper_bound=238.32)
    record3 = Record(
        base_rule_name="autostrategy",
        checker_alg="combined",
        checker_config="{\"checkers\": [{\"config\": {\"margin_threshold\": -0.5, \"ignore_fixed\": true}, \"name\": \"low_margin_sku\"}, {\"config\": {\"margin_threshold\": 0.33, \"ignore_fixed\": true}, \"name\": \"high_margin_sku\"}, {\"config\": {\"coefficient\": 0.8, \"ignore_fixed\": true}, \"name\": \"cheap_against_white_sku\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"out_off_stock_sku\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"wrong_bounds_sku\"}, {\"config\": {\"coefficient\": 0.5, \"ignore_fixed\": false}, \"name\": \"purchase_price_warning\"}, {\"config\": {\"coefficient\": 0.5, \"ignore_fixed\": false}, \"name\": \"cost_price_warning\"}, {\"config\": {\"demand_threshold\": 0.0, \"ignore_fixed\": false}, \"name\": \"demand_zero_warning\"}, {\"config\": {\"ignore_fixed\": false}, \"name\": \"empty_bounds_sku\"}, {\"config\": {\"threshold\": 5.0, \"ignore_fixed\": false}, \"name\": \"cost_purchase_warning\"}]}",
        demand=0,
        group_id=2000,
        hid=13314877,
        lower_bound=169.14,
        market_sku=3,
        new_price=187.51,
        purchase_price=153.76,
        shop_sku="000109.61002555-7",
        stock=6,
        upper_bound=238.32,
        abc_status='D')

    data.append(record1)
    data.append(record2)
    data.append(record3)
    assert len(list(data)) == 3

    result = list(map_and_check(data))
    assert result[0].error_code == CheckerErrorCode.DEMAND_ZERO_WARNING.error_code
    assert len(result) == 1
