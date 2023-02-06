import market.dynamic_pricing.deprecated.autostrategy_stats_raw.lib.get_stats_raw as tested_module
from nile.api.v1.record import Record
import pytest
import datetime


def test_select_stock_table_by_revision():
    assert 'some_path/1d/2019-02-01' == tested_module.select_stock_table_by_revision(
        h_revision=1,
        d_revision=2,
        h_path='some_path/1h/2019-02-01T19:00:00',
        d_path='some_path/1d/2019-02-01',
        yesterday='2019-02-01',
        max_hourly_stock_table_name_ts=datetime.datetime.strptime('2019-07-03T15:56:13', '%Y-%m-%dT%H:%M:%S')
    )
    with pytest.raises(Exception) as excinfo:
        tested_module.select_stock_table_by_revision(
            h_revision=1,
            d_revision=2,
            h_path='some_path/1h/2019-02-01T17:00:00',
            d_path='some_path/1d/2019-01-31',
            yesterday='2019-02-01',
            max_hourly_stock_table_name_ts=datetime.datetime.strptime('2019-07-03T15:56:13', '%Y-%m-%dT%H:%M:%S')
        )
    assert excinfo.value.args[0] == "The stock data is too old"

    assert 'some_path/1h/2019-02-01T19:00:00' == tested_module.select_stock_table_by_revision(
        h_revision=2,
        d_revision=1,
        h_path='some_path/1h/2019-02-01T19:00:00',
        d_path='some_path/1d/2019-02-01',
        yesterday='2019-02-01',
        max_hourly_stock_table_name_ts=datetime.datetime.strptime('2019-02-01T15:56:13', '%Y-%m-%dT%H:%M:%S')
    )
    with pytest.raises(Exception) as excinfo:
        tested_module.select_stock_table_by_revision(
            h_revision=2,
            d_revision=1,
            h_path='some_path/1h/2019-02-01T17:00:00',
            d_path='some_path/1d/2019-01-31',
            yesterday='2019-02-01',
            max_hourly_stock_table_name_ts=datetime.datetime.strptime('2019-02-01T17:56:13', '%Y-%m-%dT%H:%M:%S')
        )
    assert excinfo.value.args[0] == "The stock data is too old"


def test_transit_reducer():
    input_data = {Record(market_sku=1): [Record(date="2019-09-26",
                                                in_transit=4,
                                                ),
                                         Record(date="2019-09-28",
                                                in_transit=8,
                                                ),
                                         Record(date="2019-09-30",
                                                in_transit=110,
                                                )
                                         ]
                  }
    res = []
    for t in tested_module.transit_reducer(input_data.items()):
        res.append(t)
    assert [
        Record(in_transit={'2019-09-26': 4,
                           '2019-09-28': 8,
                           '2019-09-30': 110
                           },
               market_sku=1)
    ] == res


def test_get_default_paths():
    prod_paths_latest = {
        'kvi_path': '//home/market/production/monetize/dynamic_pricing/kvi/latest',
        'config_path': '//home/market/production/monetize/dynamic_pricing/config/latest',
        'sku_list_path': '//home/market/production/monetize/dynamic_pricing/groups/latest',
        'exp_schedule_path': '//home/market/production/monetize/dynamic_pricing/experiment_plan/latest',
        'last_curr_price_path': '//home/market/production/monetize/dynamic_pricing/autostrategy_current_prices/sku_price/latest',
        'replenishment_suppliers_path': '//home/market/production/replenishment/order_planning/latest/intermediate/suppliers',
        'replenishment_transits_path': '//home/market/production/replenishment/order_planning/latest/outputs/transits',
        'replenishment_recommendations_path': '//home/market/production/replenishment/order_planning/latest/outputs/recommendations',
        'abc_path': '//home/market/production/replenishment/abc/latest',
        'dco_path': '//home/market/production/mstat/analyst/regular/mbi/blue_prices/latest_dco_upload_table',
        'blue_assort_path': '//home/market/production/mstat/analyst/regular/blue-assortment/latest',
        'axapta_disc_path': '//home/market/production/mstat/analyst/regular/discounts_from_axapta/latest',
        'max_price_path': '//home/market/production/mstat/analyst/regular/mbi/blue_prices/max_blue_prices/latest',
        'axapta_price_rules_log_path': '//home/market/production/mstat/dictionaries/axapta_price_rules_log/latest',
        'sku_path': '//home/market/production/mstat/dictionaries/dynamic_pricing/assortment_ssku/latest',
        'vat_path': '//home/market/production/mstat/dictionaries/axapta_product_category/latest',
        'deadstock_sales_path': '//home/market/production/monetize/dynamic_pricing/deadstock_sales/deadstock_sale_prices/latest'
    }
    prod_paths_default = tested_module.get_default_paths(cur_ts='2019-09-02T01:23:45', use_latest=True)
    # Test keys' lists equivalence:
    assert sorted(prod_paths_latest.keys()) == sorted(prod_paths_default.keys())
    for key in prod_paths_latest.keys():
        assert prod_paths_latest[key] == prod_paths_default[key]

    prod_paths_default = tested_module.get_default_paths(cur_ts='2132-02-29T23:45:00', use_latest=True)
    # Test keys' lists equivalence:
    assert sorted(prod_paths_latest.keys()) == sorted(prod_paths_default.keys())
    for key in prod_paths_latest.keys():
        assert prod_paths_latest[key] == prod_paths_default[key]

    with pytest.raises(Exception) as excinfo:
        tested_module.get_default_paths(cur_ts='2019-09-02T16:19:59', use_latest=False)
    assert excinfo.value.args[0] == "Cannot determine the paths"
