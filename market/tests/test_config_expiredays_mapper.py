from market.dynamic_pricing.pricing.expiring_goods.expiring_assortment.tables.wms import (
    map_config_expiredays_wrapper
)


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()

BAD_CONFIG = [
    {
        "expiredays_min": 1,
        "expiredays_max": 400,
        "start_sale_before": 0.5,
        "start_sale_before_units": "percent_of_expiredays"
    },
    {
        "expiredays_min": 301,
        "start_sale_before": 100,
        "start_sale_before_units": "days_to_osg_exit"
    }
]


GOOD_CONFIG = [
    {
        "expiredays_min": 1,
        "expiredays_max": 10,
        "start_sale_before": 0.5,
        "start_sale_before_units": "percent_of_expiredays"
    },
    {
        "expiredays_min": 11,
        "start_sale_before": 2,
        "start_sale_before_units": "days_to_osg_exit"
    }
]


def test_bad_config():
    map_config_expiredays = map_config_expiredays_wrapper(BAD_CONFIG)
    input_records = []
    input_records.append({
        'market_sku': 1,
        'sg_end_date': '2021-11-20',
        'sg': 350
    })
    try:
        res = list(map_config_expiredays(input_records))
    except Exception as e:
        res = e
    assert res.args[0] == "One msku in two days intervals {'market_sku': 1, 'sg_end_date': '2021-11-20', 'sg': 350}"


def test_units_days():
    map_config_expiredays = map_config_expiredays_wrapper(GOOD_CONFIG)
    input_records = []
    input_records.append({
        'market_sku': 1,
        'stop_sale_date': '2021-11-20',
        'sg': 11
    })
    res = list(map_config_expiredays(input_records))[0]
    assert res['sale_start_date'] == '2021-11-18'


def test_units_percent():
    map_config_expiredays = map_config_expiredays_wrapper(GOOD_CONFIG)
    input_records = []
    input_records.append({
        'market_sku': 1,
        'sg_end_date': '2021-11-20',
        'sg': 10
    })
    res = list(map_config_expiredays(input_records))[0]
    assert res['sale_start_date'] == '2021-11-15'
