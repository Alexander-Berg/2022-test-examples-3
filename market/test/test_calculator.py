from nile.api.v1 import Record
from market.dynamic_pricing.pricing.regional_calculator.bin.calculator import (
    regional_mapper_wrapper,
    rules_reducer,
)
from market.dynamic_pricing.pricing.library.types import PriceType


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common

        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


def test_regional_mapper():
    regions_cast_dict = {
        1: [1],  # Регион в регион
        10: [1, 2, 3, 4, 5],  # Страна в регионы
        100: [1],  # Город в регион
    }
    records = [
        {"regions_in": "[1]", "regions_out": "[]"},  # 1
        {"regions_in": "[10]", "regions_out": "[]"},  # 2
        {"regions_in": "[10]", "regions_out": "[1]"},  # 3
        {"regions_in": "[100]", "regions_out": "[]"},  # 4
    ]
    expected = [
        1,  # 1
        1,  # 2
        2,
        3,
        4,
        5,
        2,  # 3
        3,
        4,
        5,
        1,  # 4
    ]
    regional_mapper = regional_mapper_wrapper(regions_cast_dict)

    for i, r in enumerate(regional_mapper(records)):
        assert (
            r["region_id"] == expected[i]
        ), f"Expected {expected[i]}, got {r['region_id']}"


def test_rules_reducer():
    gropus = {
        Record(k='g1'): [
            {
                "lower_bound": 50,
                "upper_bound": 100,
                "purchase_price": 10,
                "rule_base": "dbs_min_price",
                "rule_action": "markup",
                "action_coeff": 0,
                "dbs_min_price": None,
                "price_type": PriceType.SELL
            },
            {
                "lower_bound": 50,
                "upper_bound": 100,
                "purchase_price": 10,
                "rule_base": "market_min_3p_price",
                "rule_action": "markup",
                "action_coeff": 0,
                "market_min_3p_price": 60,
                "price_type": PriceType.SELL
            },
        ],
        Record(k='g2'): [
            {
                "lower_bound": 50,
                "upper_bound": 100,
                "purchase_price": 10,
                "rule_base": "market_min_3p_price",
                "rule_action": "markup",
                "action_coeff": 0,
                "market_min_3p_price": 150,
                "price_type": PriceType.SELL
            },
        ],
        Record(k='g3'): [
            {
                "lower_bound": 50,
                "upper_bound": 100,
                "purchase_price": 10,
                "rule_base": "market_min_3p_price",
                "rule_action": "markup",
                "action_coeff": 0,
                "market_min_3p_price": 80,
                "price_type": PriceType.SELL
            },
        ],
        Record(k='g4'): [
            {
                "lower_bound": 50,
                "upper_bound": 100,
                "purchase_price": 10,
                "rule_base": "market_min_3p_price",
                "rule_action": "markup",
                "action_coeff": 0,
                "market_min_3p_price": 10,
                "price_type": PriceType.SELL
            },
        ],
        Record(k='g5'): [
            {
                "lower_bound": 50,
                "upper_bound": 100,
                "purchase_price": 10,
                "rule_base": "market_min_3p_price",
                "rule_action": "markup",
                "action_coeff": 0,
                "market_min_3p_price": None,
                "price_type": PriceType.SELL
            },
        ],
    }
    expected = [
        60,  # Первой статистики нет, поэтому будет использовано второе по приоритету правило,
        100,  # Цена больше верхней границы, обрезаем
        80,  # Цена из статистики подходит
        50,  # Цена образана по нижней границе
        None,  # Не удалось сформировать региональную цену
    ]
    for i, r in enumerate(rules_reducer(gropus.items())):
        assert expected[i] == r['new_price'], f"Expected {expected[i]}, got {r['new_price']}"
