from pytest import approx
from market.dynamic_pricing.pricing.dynamic_pricing.bounds_calculator.bounds import (
    priority_reducer, get_winner
)


def test_get_winner_all_strong():
    input_raw = [
        {'bound': 1, 'bound_strength': None},
        {'bound': 2, 'bound_strength': None},
        {'bound': 3, 'bound_strength': None}
    ]
    assert get_winner(input_raw, 'lower') == {'bound': 3, 'bound_strength': None}
    assert get_winner(input_raw, 'upper') == {'bound': 1, 'bound_strength': None}


def test_get_winner_all_weak():
    input_raw = [
        {'bound': 1, 'bound_strength': 'weak'},
        {'bound': 2, 'bound_strength': 'weak'},
        {'bound': 3, 'bound_strength': 'weak'}
    ]
    assert get_winner(input_raw, 'lower') == {'bound': 1, 'bound_strength': 'weak'}
    assert get_winner(input_raw, 'upper') == {'bound': 3, 'bound_strength': 'weak'}


def test_get_winner_both():
    input_raw = [
        {'bound': 1, 'bound_strength': 'weak'},
        {'bound': 2, 'bound_strength': 'weak'},
        {'bound': 3, 'bound_strength': None},
        {'bound': 4, 'bound_strength': None}
    ]
    assert get_winner(input_raw, 'lower') == {'bound': 4, 'bound_strength': None}
    assert get_winner(input_raw, 'upper') == {'bound': 3, 'bound_strength': None}


def test_lower_same_prior_strong():
    input_data = [
        (
            {
                'market_sku': 1,
                'shop_sku': '1',
                'bound_type': 'lower',
            },
            [
                {
                    'ref_min_price': 120,
                    'purchase_price': 110,
                    'rule_id': 1,
                    'priority': 1,
                    'rule_base': 'ref_min_price',
                    'rule_action': 'markup',
                    'action_coeff': 0,
                    'bound_strength': None
                },
                {
                    'ref_min_price': 120,
                    'purchase_price': 110,
                    'rule_id': 2,
                    'priority': 1,
                    'rule_base': 'purchase_price',
                    'rule_action': 'markup',
                    'action_coeff': 0.1,
                    'bound_strength': None
                }
            ]
        )
    ]
    rec = list(priority_reducer(input_data))[0]
    assert (rec.rule_id, rec.bound, rec.priority) == (2, approx(121.0), 1)


def test_lower_non_equal_prior_strong():
    input_data = [
        (
            {
                'market_sku': 1,
                'shop_sku': '1',
                'bound_type': 'lower',
            },
            [
                {
                    'ref_min_price': 120,
                    'purchase_price': 110,
                    'rule_id': 1,
                    'priority': 1,
                    'rule_base': 'ref_min_price',
                    'rule_action': 'markup',
                    'action_coeff': 0,
                    'bound_strength': None
                },
                {
                    'ref_min_price': 120,
                    'purchase_price': 110,
                    'rule_id': 2,
                    'priority': 2,
                    'rule_base': 'purchase_price',
                    'rule_action': 'markup',
                    'action_coeff': 0.1,
                    'bound_strength': None
                }
            ]
        )
    ]
    rec = list(priority_reducer(input_data))[0]
    assert (rec.rule_id, rec.bound, rec.priority) == (1, approx(120.0), 1)


def test_lower_with_missing():
    input_data = [
        (
            {
                'market_sku': 1,
                'shop_sku': '1',
                'bound_type': 'lower',
            },
            [
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 1,
                    'priority': 1,
                    'rule_base': 'ref_min_price',
                    'rule_action': 'markup',
                    'action_coeff': 0,
                    'bound_strength': None
                },
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 2,
                    'priority': 2,
                    'rule_base': 'purchase_price',
                    'rule_action': 'markup',
                    'action_coeff': 0.1,
                    'bound_strength': None
                }
            ]
        )
    ]
    rec = list(priority_reducer(input_data))[0]
    assert (rec.rule_id, rec.bound, rec.priority) == (2, approx(121.0), 2)


def test_lower_with_not_full():
    input_data = [
        (
            {
                'market_sku': 1,
                'shop_sku': '1',
                'bound_type': 'lower',
            },
            [
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 1,
                    'priority': 1,
                    'rule_base': 'ref_min_price',
                    'rule_action': 'markup',
                    'action_coeff': 0,
                    'bound_strength': None
                },
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 3,
                    'priority': 1,
                    'rule_base': 'purchase_price',
                    'rule_action': 'markup',
                    'action_coeff': 0.2,
                    'bound_strength': None
                },
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 2,
                    'priority': 2,
                    'rule_base': 'purchase_price',
                    'rule_action': 'markup',
                    'action_coeff': 0.1,
                    'bound_strength': None
                }
            ]
        )
    ]
    rec = list(priority_reducer(input_data))[0]
    assert (rec.rule_id, rec.bound, rec.priority) == (3, approx(132.0), 1)


def test_lower_all_not_full():
    input_data = [
        (
            {
                'market_sku': 1,
                'shop_sku': '1',
                'bound_type': 'lower',
            },
            [
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 1,
                    'priority': 1,
                    'rule_base': 'ref_min_price',
                    'rule_action': 'markup',
                    'action_coeff': 0,
                    'bound_strength': None
                },
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 3,
                    'priority': 1,
                    'rule_base': 'purchase_price',
                    'rule_action': 'markup',
                    'action_coeff': 0.0,
                    'bound_strength': None
                },
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 4,
                    'priority': 2,
                    'rule_base': 'ref_min_price',
                    'rule_action': 'markup',
                    'action_coeff': 0,
                    'bound_strength': None
                },
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 2,
                    'priority': 2,
                    'rule_base': 'purchase_price',
                    'rule_action': 'markup',
                    'action_coeff': 0.1,
                    'bound_strength': None
                }
            ]
        )
    ]
    rec = list(priority_reducer(input_data))[0]
    assert (rec.rule_id, rec.bound, rec.priority) == (2, approx(121.0), 2)


def test_lower_all_missing():
    input_data = [
        (
            {
                'market_sku': 1,
                'shop_sku': '1',
                'bound_type': 'lower',
            },
            [
                {
                    'ref_min_price': None,
                    'purchase_price': 110,
                    'rule_id': 1,
                    'priority': 1,
                    'rule_base': 'ref_min_price',
                    'rule_action': 'markup',
                    'action_coeff': 0,
                    'bound_strength': None
                }
            ]
        )
    ]
    rec = list(priority_reducer(input_data))[0]
    assert (rec.rule_id, rec.bound, rec.priority) == (1, None, 1)
