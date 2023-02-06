import market.dynamic_pricing.deprecated.utilities.lib.yt_paths as tested_module
import pytest


def test_dict_updating():
    dict1 = {
        'a': 1,
        'b': 1,
        'c': 1,
    }

    tested_module.update_existing_with_kwargs(dict1, a=2, b=2, d=3)
    assert len(dict1) == 3
    assert dict1['a'] == 2
    assert dict1['b'] == 2
    assert dict1['c'] == 1

    dict2 = {
        'a': 3,
        'b': 3,
        'd': 3
    }
    tested_module.update_existing_with_kwargs(dict1, **dict2)
    assert len(dict1) == 3
    assert dict1['a'] == 3
    assert dict1['b'] == 3
    assert dict1['c'] == 1

    tested_module.update_existing(dict1, {'c': 3, 'e': 6})
    assert len(dict1) == 3
    assert dict1['a'] == 3
    assert dict1['b'] == 3
    assert dict1['c'] == 3


def test_autostrategy_paths():
    tested_paths = tested_module.AutostrategyPipelinePaths('testing', True)
    assert tested_paths.paths['config_path'] == '//home/market/testing/monetize/dynamic_pricing/canonic_test/config'

def test_autostrategy_paths_update():
    paths_to_update = {
        'sku_price_path': "test_test",
        'another_path': "test_test"
    }
    tested_paths = tested_module.AutostrategyPipelinePaths('testing', True, **paths_to_update)
    assert tested_paths.paths['sku_price_path'] == 'test_test'
    assert 'another_path' not in tested_paths.paths
