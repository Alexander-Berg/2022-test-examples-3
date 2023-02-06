import json
import logging

from market.dynamic_pricing.pricing.dynamic_pricing.rules_uploader.uploader import create_dataframe
from market.dynamic_pricing.pricing.library.constants import (
    PRICE_TYPES,
    BOUND_TYPES,
    BASE_PRICE_RULES,
    BASE_BOUND_RULES,
    BASE_VENDOR_RULES,
    RULE_ACTIONS,
    RESERVE_BASE_PRICE_RULES,
    RESERVE_BASE_BOUNDS_RULES,
    REGULAR_PRICE_MIN_PRIORITY,
    REGIONAL_PRICE_STAT_TYPES
)
import pandas as pd
import yatest.common


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


def _get_group_ids(path):
    with open(path, 'r') as json_file:
        config = json.load(json_file)
    return [group_desc['group_id'] for group_desc in config['groups']]


def _test_unique_col_values(df, col_name):
    have_duplicity = len(df[col_name].unique()) != df[col_name].count()
    if have_duplicity:
        ids = df[col_name]
        logging.info('Duplicity groups:')
        logging.info(df[ids.isin(ids[ids.duplicated()])])
    return not have_duplicity


def _test_unique_multi_col_values(df, multi_col_names):
    return df.fillna(1).groupby(multi_col_names).nunique().max().max() == 1


def _test_unique_priority(df, group_id_col_name, type_col_name):
    for group_id in df[group_id_col_name].unique():
        group_df = df[df[group_id_col_name] == group_id]
        for type_in_group in group_df[type_col_name].unique():
            unique_priority_df = group_df[group_df[type_col_name] == type_in_group]
            if not _test_unique_col_values(unique_priority_df, 'priority'):
                return False
    return True


def _test_valid_col_values(df, col_name, valid_values):
    all_valid = df[col_name].isin(valid_values).all()
    if not all_valid:
        invalid_values = df[~df[col_name].isin(valid_values)]
        logging.info('Invalid values in column {}'.format(col_name))
        logging.info(invalid_values)
    return all_valid


def _test_reserve_rule_exists(df, group_id_col_name, rule_base_col_name, reserve_base_rules):
    for group_id in df[group_id_col_name].unique():
        group_df = df[(df[group_id_col_name] == group_id) & (df['priority'] > REGULAR_PRICE_MIN_PRIORITY)]
        if not group_df.empty and not group_df[rule_base_col_name].isin(reserve_base_rules).any():
            return False
    return True


def _test_valid_json_values(df, col_name):
    try:
        df.loc[df[col_name].notna() & (df[col_name].str.strip() != ''), col_name].apply(json.loads)
    except Exception:
        return False
    else:
        return True


def validate_price_rules(price_rules, valid_group_ids):
    assert(_test_unique_col_values(price_rules, 'rule_id'))
    assert(_test_unique_priority(price_rules, 'price_group_id', 'price_type'))
    assert(_test_valid_col_values(price_rules, 'price_type', PRICE_TYPES))
    assert(_test_valid_col_values(price_rules, 'rule_base', BASE_PRICE_RULES))
    assert(_test_valid_col_values(price_rules[price_rules['rule_base'] != 'autostrategy'], 'rule_action', RULE_ACTIONS.keys()))
    assert(_test_reserve_rule_exists(price_rules, 'price_group_id', 'rule_base', RESERVE_BASE_PRICE_RULES))
    assert(_test_valid_col_values(price_rules, 'price_group_id', valid_group_ids))


def validate_regional_rules(regional_rules):
    assert(_test_unique_col_values(regional_rules, 'rule_id'))
    assert(_test_unique_priority(regional_rules, 'price_group_id', 'price_type'))
    assert(_test_valid_col_values(regional_rules, 'rule_base', REGIONAL_PRICE_STAT_TYPES))
    assert(_test_valid_col_values(regional_rules, 'rule_action', RULE_ACTIONS.keys()))
    assert(_test_valid_json_values(regional_rules, 'regions_in'))
    assert(_test_valid_json_values(regional_rules, 'regions_out'))


def validate_bounds_rules(bounds_rules, valid_group_ids):
    assert(_test_unique_col_values(bounds_rules, 'rule_id'))
    assert(_test_valid_col_values(bounds_rules, 'bound_strength', ['weak', 'strong', None]))
    assert(_test_valid_col_values(bounds_rules, 'bound_type', BOUND_TYPES))
    assert(_test_valid_col_values(bounds_rules, 'rule_base', BASE_BOUND_RULES))
    assert(_test_valid_col_values(bounds_rules, 'rule_action', RULE_ACTIONS.keys()))
    assert(_test_reserve_rule_exists(bounds_rules, 'bounds_group_id', 'rule_base', RESERVE_BASE_BOUNDS_RULES))
    assert(_test_valid_col_values(bounds_rules, 'bounds_group_id', valid_group_ids))


def validate_vendors_rules(vendors_rules):
    assert(_test_valid_col_values(vendors_rules, 'rule_base', BASE_VENDOR_RULES))
    assert(_test_valid_col_values(vendors_rules, 'rule_action', RULE_ACTIONS.keys()))
    assert(vendors_rules.empty or _test_unique_multi_col_values(vendors_rules, ['category_id', 'vendor_id']))


def validate_sku_exception_rules(sku_exception_rules, price_rules):
    if not sku_exception_rules.empty:
        assert(_test_unique_multi_col_values(sku_exception_rules, ['shop_sku']))
    assert(_test_unique_priority(pd.concat([price_rules, sku_exception_rules], sort=False), 'price_group_id', 'price_type'))


def test_real_rules():
    '''
    Проверяем, что правила катменов соответствуют здравому смыслу
    '''
    base_path = yatest.common.source_path('market/dynamic_pricing/pricing/dynamic_pricing/rules_uploader')

    price_rules_df = create_dataframe(base_path + '/price_groups.csv')
    bounds_rules_df = create_dataframe(base_path + '/bounds_groups.csv')
    vendors_rules_df = create_dataframe(base_path + '/vendors_rules.csv')
    sku_exception_rules_df = create_dataframe(base_path + '/sku_exception_list.csv')
    regional_rules_df = create_dataframe(base_path + '/regional_price_groups.csv')

    bounds_config_group_ids = _get_group_ids(yatest.common.source_path('market/dynamic_pricing/pricing/dynamic_pricing/config_generator/bounds_config.json'))
    pricing_config_groups_ids = _get_group_ids(yatest.common.source_path('market/dynamic_pricing/pricing/dynamic_pricing/config_generator/pricing_config.json'))
    exeptions_groups_ids = list(set(sku_exception_rules_df['group_id'].to_list()))

    logging.info('bounds: ' + str(bounds_config_group_ids + exeptions_groups_ids))
    logging.info('prices: ' + str(pricing_config_groups_ids + exeptions_groups_ids))

    validate_price_rules(price_rules_df, pricing_config_groups_ids + exeptions_groups_ids)
    validate_bounds_rules(bounds_rules_df, bounds_config_group_ids + exeptions_groups_ids)
    validate_vendors_rules(vendors_rules_df)
    validate_sku_exception_rules(sku_exception_rules_df, price_rules_df)
    validate_regional_rules(regional_rules_df)
