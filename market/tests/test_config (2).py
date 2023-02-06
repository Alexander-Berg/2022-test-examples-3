from qb2.api.v1 import filters as sf
import json

import yatest.common

from parse import parse


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


def test_msku_transitions():
    transitions_dict = {1: 4}
    full_config = {
        "groups": [
            {
                "group_id": 1,
                "filters": [
                    {"sku_in": [1, 2]}
                ],
                "config": "default",
            },
        ],
        "configs": {
            "default": {}
        }
    }
    _, result_filters = parse(full_config, dict, transitions_dict)
    f = result_filters[1][0][0]
    assert f({'market_sku': 4})
    assert f({'market_sku': 2})
    assert not f({'market_sku': 1})


def test_sku_in_groups():
    transitions_dict = {}
    full_config = {
        "groups": [
            {
                "group_id": 1,
                "filters": [
                    {"sku_in": [1, 2, 3]}
                ],
                "config": "default",
            },
            {
                "group_id": 2,
                "filters": [
                    {'category_in': [123]}
                ],
            },
        ],
        "configs": {
            "default": {}
        }
    }
    expected_result = {
        1: [[sf.one_of("market_sku", [1, 2, 3])]],
        2: [[sf.one_of("category", [123])]]
    }
    _, result_filters = parse(full_config, dict, transitions_dict)
    assert len(result_filters) == len(expected_result)
    for item in result_filters:
        print(item)
        assert item in expected_result


def test_configs():
    transitions_dict = {}
    full_config = {
        "groups": [
            {
                "group_id": 1,
                "name": "Group #1",
                "filters": [],
                "config": "default",
            },
            {
                "group_id": 2,
                "filters": [],
                "margin": 0.2,
                "config": "default",
            },
        ],
        "configs": {
            "default": {}
        }
    }
    expected_result = [
        {
            "group_id": 1,
            "name": "Group #1",
            "margin": None,
            "is_autostrategy": False,
            "margin_adj_alg": None,
            "margin_adj_config": "{}",
            "pricing_alg": None,
            "pricing_config": "{}",
            "checker_alg": None,
            "checker_config": "{}"
        },
        {
            "group_id": 2,
            "name": "Unknown",
            "margin": 0.2,
            "is_autostrategy": False,
            "margin_adj_alg": None,
            "margin_adj_config": "{}",
            "pricing_alg": None,
            "pricing_config": "{}",
            "checker_alg": None,
            "checker_config": "{}"
        }
    ]
    result_config, _,  = parse(full_config, dict, transitions_dict)
    assert len(result_config) == len(expected_result)
    for item in result_config:
        assert item in expected_result


def test_duplicate_groups():
    transitions_dict = {}
    full_config = {
        "groups": [
            {
                "group_id": 1,
                "name": "Group #1",
                "filters": [],
                "config": "default"
            },
            {
                "group_id": 1,
                "filters": [],
                "config": "default",
            },
            {
                "group_id": 3,
                "filters": [],
                "config": "default",
            },
            {
                "group_id": 4,
                "filters": [],
                "config": "default",
            }
        ],
        "configs": {
            "default": {}
        }
    }
    with yatest.common.pytest.raises(RuntimeError):
        result_config, _ = parse(full_config, dict, transitions_dict)


def test_real_bounds_config():
    transitions_dict = {1: 2}
    path = yatest.common.source_path('market/dynamic_pricing/pricing/dynamic_pricing/config_generator/bounds_config.json')
    with open(path) as config_file:
        full_config = json.loads(config_file.read())
    # Just test that it will not fail
    _, _ = parse(full_config, dict, transitions_dict)


def test_real_pricing_config():
    transitions_dict = {1: 2}
    path = yatest.common.source_path('market/dynamic_pricing/pricing/dynamic_pricing/config_generator/pricing_config.json')
    with open(path) as config_file:
        full_config = json.loads(config_file.read())
    # Just test that it will not fail
    _, _ = parse(full_config, dict, transitions_dict)
