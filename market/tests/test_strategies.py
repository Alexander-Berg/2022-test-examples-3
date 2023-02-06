from optimal_assortment_strategy import get_optimal_assortment
import yatest.common


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path

_env = __YaTestEnv()

records = [
    {
        'market_sku': 1,
        'offer_id': 1,
        'new_price': 1000,
        'new_price_demand': 10,
        'current_price': 1099,
        'current_price_demand': 9,
        'discount_max_absolute': 1000,
        'discount_max_percent': 100,
        'discount_min_absolute': 0,
        'discount_min_percent': 0,
    },
    {
        'market_sku': 2,
        'offer_id': 1,
        'new_price': 1000,
        'new_price_demand': 10,
        'current_price': 1100,
        'current_price_demand': 1,
        'discount_max_absolute': 1000,
        'discount_max_percent': 100,
        'discount_min_absolute': 0,
        'discount_min_percent': 0,
    },
    {
        'market_sku': 2,
        'offer_id': 2,
        'new_price': 1000,
        'new_price_demand': 10,
        'current_price': 1100,
        'current_price_demand': 1,
        'discount_max_absolute': 1000,
        'discount_max_percent': 100,
        'discount_min_absolute': 0,
        'discount_min_percent': 0,
    },
]


def test_max_gmv():
    promos = get_optimal_assortment(records, 1100, 'max_gmv')
    promos = list(promos)
    print(promos)

    assert len(promos) == 2
    assert promos[0]['new_price_demand'] == 5
    assert promos[0]['current_price_demand'] == 0.5
    assert promos[0]['market_sku'] == 2
    assert promos[0]['subsidy_per_sku'] == 100
    assert promos[0]['subsidy_on_demand'] == 100*10/2
    assert promos[0]['spent_budget'] == 1000
    assert promos[1]['market_sku'] == 2
    assert promos[1]['subsidy_per_sku'] == 100
    assert promos[1]['subsidy_on_demand'] == 100*10/2
    assert promos[1]['spent_budget'] == 1000


def max_minref_sales():
    promos = get_optimal_assortment(records, 1100, 'max_minref_sales')
    promos = list(promos)
    print(promos)

    assert len(promos) == 1
    assert promos[0]['market_sku'] == 1
    assert promos[0]['subsidy_per_sku'] == 99
    assert promos[0]['subsidy_on_demand'] == 99*10
    assert promos[0]['spent_budget'] == 990
