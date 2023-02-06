import pytest
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


def test_sku_in_groups():
    full_config = {
        "groups": [
            {
                "group_id": 1,
                "active_dates": [],
                "filters": [
                    {"sku_in": [1, 2, 3]}
                ]
            },
            {
                "group_id": 2,
                "active_dates": [],
                "filters": [
                    {'category_in': [123]}
                ]
            },
        ]
    }
    expected_result = {
        1: [[sf.one_of("market_sku", [1, 2, 3])]],
        2: [[sf.one_of("category", [123])]]
    }
    _, result_filters, _ = parse(full_config, dict)
    assert len(result_filters) == len(expected_result)
    for item in result_filters:
        assert item in expected_result


def test_experiment_plan():
    full_config = {
        "groups": [
            {
                "group_id": 1,
                "filters": [],
                "active_dates": [
                    {
                        "start": "2019-01-01",
                        "duration": 3
                    },
                    {
                        "start": "2019-01-10",
                        "duration": 2
                    },
                ]
            },
            {
                "group_id": 2,
                "filters": [],
                "active_dates": [
                    {
                        "start": "2019-02-01",
                        "duration": 1,
                        "repeat_every": 2,
                        "repeat_count": 3,
                    },
                    {
                        "start": "2019-02-10",
                        "duration": 1,
                        "repeat_every": 2,
                        "end": "2019-02-15",
                    },
                ]
            },
        ]
    }
    expected_result = [
        {
            "group_id": 1,
            "date": "2019-01-01",
        },
        {
            "group_id": 1,
            "date": "2019-01-02",
        },
        {
            "group_id": 1,
            "date": "2019-01-03",
        },
        {
            "group_id": 1,
            "date": "2019-01-10",
        },
        {
            "group_id": 1,
            "date": "2019-01-11",
        },
        {
            "group_id": 2,
            "date": "2019-02-01",
        },
        {
            "group_id": 2,
            "date": "2019-02-03",
        },
        {
            "group_id": 2,
            "date": "2019-02-05",
        },
        {
            "group_id": 2,
            "date": "2019-02-10",
        },
        {
            "group_id": 2,
            "date": "2019-02-12",
        },
        {
            "group_id": 2,
            "date": "2019-02-14",
        },
    ]
    _, _, result_dates = parse(full_config, dict)
    assert len(result_dates) == len(expected_result)
    for item in result_dates:
        assert item in expected_result


def test_configs():
    full_config = {
        "groups": [
            {
                "group_id": 1,
                "name": "Group #1",
                "active_dates": [],
                "filters": [],
                "config": "#1",
                "margin": 0,
            },
            {
                "group_id": 2,
                "active_dates": [],
                "filters": [],
                "config": "#1",
                "margin": 10,
            },
            {
                "group_id": 3,
                "active_dates": [],
                "filters": [],
                "config": "#2",
                "margin": 100,
            },
            {
                "group_id": 4,
                "active_dates": [],
                "filters": [],
                "margin": 100,
            },
        ],
        "configs": {
            "#1": {
                "margin_adj_alg": "trivial",
                "margin_adj_config": {},
                "pricing_alg": "margin.v1",
                "pricing_config": {},
                "checker_alg": "dummy",
                "checker_config": {},
                "stock_controller_alg": "unlimited",
                "stock_controller_config": {},
            },
            "#2": {
                "margin_adj_alg": "not_trivial",
                "margin_adj_config": {},
                "pricing_alg": "margin.v100500",
                "pricing_config": {},
                "checker_alg": "dummy",
                "checker_config": {},
                "stock_controller_alg": "unlimited",
                "stock_controller_config": {},
            },
        }
    }
    expected_result = [
        {
            "group_id": 1,
            "name": "Group #1",
            "margin": 0,
            "margin_adj_alg": "trivial",
            "margin_adj_config": "{}",
            "pricing_alg": "margin.v1",
            "pricing_config": "{}",
            "checker_alg": "dummy",
            "checker_config": "{}",
            "stock_controller_alg": "unlimited",
            "stock_controller_config": "{}",
        },
        {
            "group_id": 2,
            "name": "Unknown",
            "margin": 10,
            "margin_adj_alg": "trivial",
            "margin_adj_config": "{}",
            "pricing_alg": "margin.v1",
            "pricing_config": "{}",
            "checker_alg": "dummy",
            "checker_config": "{}",
            "stock_controller_alg": "unlimited",
            "stock_controller_config": "{}",
        },
        {
            "group_id": 3,
            "name": "Unknown",
            "margin": 100,
            "margin_adj_alg": "not_trivial",
            "margin_adj_config": "{}",
            "pricing_alg": "margin.v100500",
            "pricing_config": "{}",
            "checker_alg": "dummy",
            "checker_config": "{}",
            "stock_controller_alg": "unlimited",
            "stock_controller_config": "{}",
        },
    ]
    result_config, _, _ = parse(full_config, dict)
    assert len(result_config) == len(expected_result)
    for item in result_config:
        assert item in expected_result

def test_duplicate_groups():
    full_config = {
        "groups": [
            {
                "group_id": 1,
                "name": "Group #1",
                "active_dates": [],
                "filters": [],
                "config": "#1",
                "margin": 0,
            },
            {
                "group_id": 1,
                "active_dates": [],
                "filters": [],
                "config": "#1",
                "margin": 10,
            },
            {
                "group_id": 3,
                "active_dates": [],
                "filters": [],
                "config": "#2",
                "margin": 100,
            },
            {
                "group_id": 4,
                "active_dates": [],
                "filters": [],
                "margin": 100,
            },
        ],
        "configs": {
            "#1": {
                "margin_adj_alg": "trivial",
                "margin_adj_config": {},
                "pricing_alg": "margin.v1",
                "pricing_config": {},
                "checker_alg": "dummy",
                "checker_config": {},
                "stock_controller_alg": "unlimited",
                "stock_controller_config": {},
            },
            "#2": {
                "margin_adj_alg": "not_trivial",
                "margin_adj_config": {},
                "pricing_alg": "margin.v100500",
                "pricing_config": {},
                "checker_alg": "dummy",
                "checker_config": {},
                "stock_controller_alg": "unlimited",
                "stock_controller_config": {},
            },
        }
    }
    with yatest.common.pytest.raises(RuntimeError):
        result_config, _, _ = parse(full_config, dict)

def test_real_config():
    path = yatest.common.source_path('market/dynamic_pricing/deprecated/config_generator/config.json')
    with open(path) as config_file:
        full_config = json.loads(config_file.read())
    # Just test that it will not fail
    _, _, _ = parse(full_config, dict)
