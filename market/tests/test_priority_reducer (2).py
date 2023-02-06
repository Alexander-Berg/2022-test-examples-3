from pytest import approx
from market.dynamic_pricing.pricing.dynamic_pricing.bounds_calculator_in_memory.bounds import (
    priority_reducer,
    get_winner,
)
import pandas as pd

template_get_winner = {'market_sku': 1, 'shop_sku': 'ssku_1', 'bound_price_type': 'sell', 'priority': 1001}
template_priority_reducer = {'market_sku': 1, 'shop_sku': '1', 'bound_type': 'lower', 'bound_price_type': 'sell'}


def test_get_winner_all_strong():
    input_raw = [
        {'bound_type': 'lower', 'bound': 1, 'bound_strength': None},
        {'bound_type': 'lower', 'bound': 2, 'bound_strength': None},
        {'bound_type': 'lower', 'bound': 3, 'bound_strength': None},
        {'bound_type': 'upper', 'bound': 1, 'bound_strength': None},
        {'bound_type': 'upper', 'bound': 2, 'bound_strength': None},
        {'bound_type': 'upper', 'bound': 3, 'bound_strength': None},
    ]
    [x.update(template_get_winner) for x in input_raw]
    result = get_winner(pd.DataFrame(input_raw), inside_priority=False).loc[
        :, ['bound', 'bound_strength', 'bound_type']
    ]
    expected = pd.DataFrame(
        [
            {'bound': 3, 'bound_strength': None, 'bound_type': 'lower'},
            {'bound': 1, 'bound_strength': None, 'bound_type': 'upper'},
        ]
    )

    assert result.reset_index(drop=True).equals(expected.reset_index(drop=True))


def test_get_winner_all_weak():
    input_raw = [
        {'bound_type': 'lower', 'bound': 1, 'bound_strength': 'weak'},
        {'bound_type': 'lower', 'bound': 2, 'bound_strength': 'weak'},
        {'bound_type': 'lower', 'bound': 3, 'bound_strength': 'weak'},
        {'bound_type': 'upper', 'bound': 1, 'bound_strength': 'weak'},
        {'bound_type': 'upper', 'bound': 2, 'bound_strength': 'weak'},
        {'bound_type': 'upper', 'bound': 3, 'bound_strength': 'weak'},
    ]
    [x.update(template_get_winner) for x in input_raw]
    result = get_winner(pd.DataFrame(input_raw), inside_priority=False).loc[
        :, ['bound', 'bound_strength', 'bound_type']
    ]
    expected = pd.DataFrame(
        [
            {'bound': 1, 'bound_strength': 'weak', 'bound_type': 'lower'},
            {'bound': 3, 'bound_strength': 'weak', 'bound_type': 'upper'},
        ]
    )
    assert result.reset_index(drop=True).equals(expected.reset_index(drop=True))


def test_get_winner_both():
    input_raw = [
        {'bound': 1, 'bound_strength': 'weak', 'bound_type': 'lower'},
        {'bound': 2, 'bound_strength': 'weak', 'bound_type': 'lower'},
        {'bound': 3, 'bound_strength': None, 'bound_type': 'lower'},
        {'bound': 4, 'bound_strength': None, 'bound_type': 'lower'},
        {'bound': 1, 'bound_strength': 'weak', 'bound_type': 'upper'},
        {'bound': 2, 'bound_strength': 'weak', 'bound_type': 'upper'},
        {'bound': 3, 'bound_strength': None, 'bound_type': 'upper'},
        {'bound': 4, 'bound_strength': None, 'bound_type': 'upper'},
    ]
    [x.update(template_get_winner) for x in input_raw]
    result = get_winner(pd.DataFrame(input_raw), inside_priority=False).loc[
        :, ['bound', 'bound_strength', 'bound_type']
    ]
    expected = pd.DataFrame(
        [
            {'bound': 4, 'bound_strength': None, 'bound_type': 'lower'},
            {'bound': 3, 'bound_strength': None, 'bound_type': 'upper'},
        ]
    )
    assert result.reset_index(drop=True).equals(expected.reset_index(drop=True))


def test_lower_same_prior_strong():

    input_data = [
        {
            'ref_min_price': 120,
            'purchase_price': 110,
            'rule_id': 1,
            'priority': 1,
            'rule_base': 'ref_min_price',
            'rule_action': 'markup',
            'action_coeff': 0,
            'bound_strength': None,
        },
        {
            'ref_min_price': 120,
            'purchase_price': 110,
            'rule_id': 2,
            'priority': 1,
            'rule_base': 'purchase_price',
            'rule_action': 'markup',
            'action_coeff': 0.1,
            'bound_strength': None,
        },
    ]
    [x.update(template_priority_reducer) for x in input_data]
    rec = priority_reducer(pd.DataFrame(input_data)).iloc[0, :]
    assert (rec.rule_id, rec.bound, rec.priority) == (2, approx(121.0), 1)


def test_lower_non_equal_prior_strong():
    input_data = [
        {
            'ref_min_price': 120,
            'purchase_price': 110,
            'rule_id': 1,
            'priority': 1,
            'rule_base': 'ref_min_price',
            'rule_action': 'markup',
            'action_coeff': 0,
            'bound_strength': None,
        },
        {
            'ref_min_price': 120,
            'purchase_price': 110,
            'rule_id': 2,
            'priority': 2,
            'rule_base': 'purchase_price',
            'rule_action': 'markup',
            'action_coeff': 0.1,
            'bound_strength': None,
        },
    ]
    [x.update(template_priority_reducer) for x in input_data]
    rec = priority_reducer(pd.DataFrame(input_data)).iloc[0, :]
    assert (rec.rule_id, rec.bound, rec.priority) == (1, approx(120.0), 1)


def test_lower_with_missing():
    input_data = [
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 1,
            'priority': 1,
            'rule_base': 'ref_min_price',
            'rule_action': 'markup',
            'action_coeff': 0,
            'bound_strength': None,
        },
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 2,
            'priority': 2,
            'rule_base': 'purchase_price',
            'rule_action': 'markup',
            'action_coeff': 0.1,
            'bound_strength': None,
        },
    ]
    [x.update(template_priority_reducer) for x in input_data]
    rec = priority_reducer(pd.DataFrame(input_data)).iloc[0, :]
    assert (rec.rule_id, rec.bound, rec.priority) == (2, approx(121.0), 2)


def test_lower_with_not_full():
    input_data = [
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 1,
            'priority': 1,
            'rule_base': 'ref_min_price',
            'rule_action': 'markup',
            'action_coeff': 0,
            'bound_strength': None,
        },
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 3,
            'priority': 1,
            'rule_base': 'purchase_price',
            'rule_action': 'markup',
            'action_coeff': 0.2,
            'bound_strength': None,
        },
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 2,
            'priority': 2,
            'rule_base': 'purchase_price',
            'rule_action': 'markup',
            'action_coeff': 0.1,
            'bound_strength': None,
        },
    ]
    [x.update(template_priority_reducer) for x in input_data]
    rec = priority_reducer(pd.DataFrame(input_data)).iloc[0, :]
    assert (rec.rule_id, rec.bound, rec.priority) == (3, approx(132.0), 1)


def test_lower_all_not_full():
    input_data = [
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 1,
            'priority': 1,
            'rule_base': 'ref_min_price',
            'rule_action': 'markup',
            'action_coeff': 0,
            'bound_strength': None,
        },
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 3,
            'priority': 1,
            'rule_base': 'purchase_price',
            'rule_action': 'markup',
            'action_coeff': 0.0,
            'bound_strength': None,
        },
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 4,
            'priority': 2,
            'rule_base': 'ref_min_price',
            'rule_action': 'markup',
            'action_coeff': 0,
            'bound_strength': None,
        },
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 2,
            'priority': 2,
            'rule_base': 'purchase_price',
            'rule_action': 'markup',
            'action_coeff': 0.1,
            'bound_strength': None,
        },
    ]
    [x.update(template_priority_reducer) for x in input_data]
    rec = priority_reducer(pd.DataFrame(input_data)).iloc[0, :]
    assert (rec.rule_id, rec.bound, rec.priority) == (2, approx(121.0), 2)


def test_lower_all_missing():
    input_data = [
        {
            'ref_min_price': None,
            'purchase_price': 110,
            'rule_id': 1,
            'priority': 1,
            'rule_base': 'ref_min_price',
            'rule_action': 'markup',
            'action_coeff': 0,
            'bound_strength': None,
        }
    ]
    [x.update(template_priority_reducer) for x in input_data]
    rec = priority_reducer(pd.DataFrame(input_data)).iloc[0, :]
    assert (rec.rule_id, rec.bound, rec.priority) == (1, None, 1)
