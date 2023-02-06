import json

import pytest

pytest_plugins = [
    "crypta.lab.rule_estimator.lib.test_helpers.fixtures",
]


@pytest.fixture
def precalculated_table():
    return {
        "path": "//home/sprav/assay/common/owner_verification/yuids_for_rsya/nice_hypo",
        "idKey": "yuid",
        "idType": "yandexuid",
        "updateInterval": 0
    }


@pytest.fixture
def bad_precalculated_table():
    return {
        "path": "//bad",
        "idKey": "yuid",
        "idType": "yandexuid",
        "updateInterval": 0
    }


@pytest.fixture
def rule_conditions(mock_crypta_api, precalculated_table, bad_precalculated_table):
    serialized_precalculated_table = json.dumps(precalculated_table)
    serialized_bad_precalculated_table = json.dumps(bad_precalculated_table)

    mock_crypta_api.rule_conditions = {
        1: {
            "full_values": [
                {
                    "normalized": "yandex.ru",
                    "raw": "yandex.ru",
                    "tags": [],
                },
            ],
            "revision": 1,
            "ruleId": "rule-id",
            "source": "PUBLIC_SITES",
            "state": "APPROVED",
            "timestamps": {
                "created": 1560000000,
                "modified": 1560000000,
            },
            "values": [
                "yandex.ru",
            ],
        },
        2: {
            "full_values": [
                {
                    "normalized": "лаборатория AND (сегмент OR выборка)",
                    "raw": "лаборатория AND (сегмент OR выборка)",
                    "tags": [],
                },
            ],
            "revision": 2,
            "ruleId": "rule-id",
            "source": "BROWSER_TITLES",
            "state": "APPROVED",
            "timestamps": {
                "created": 1560000000,
                "modified": 1560000000,
            },
            "values": [
                "лаборатория AND (сегмент OR выборка)",
            ],
        },
        3: {
            "full_values": [
                {
                    "normalized": serialized_precalculated_table,
                    "raw": serialized_precalculated_table,
                    "tags": [],
                },
            ],
            "revision": 3,
            "ruleId": "rule-id",
            "source": "PRECALCULATED_TABLES",
            "state": "APPROVED",
            "timestamps": {
                "created": 1560000000,
                "modified": 1560000000,
            },
            "values": [
                serialized_precalculated_table,
            ],
        },
        4: {
            "full_values": [
                {
                    "normalized": "com.android.app1",
                    "raw": "com.android.app1",
                    "tags": [],
                },
            ],
            "revision": 4,
            "ruleId": "rule-id",
            "source": "APPS",
            "state": "APPROVED",
            "timestamps": {
                "created": 1560000000,
                "modified": 1560000000,
            },
            "values": [
                "com.android.app1",
            ],
        },
        5: {
            "full_values": [
                {
                    "normalized": "yandex.ru",
                    "raw": "yandex.ru",
                    "tags": [],
                },
            ],
            "revision": 5,
            "ruleId": "rule-id",
            "source": "SEARCH_RESULTS_HOSTS",
            "state": "APPROVED",
            "timestamps": {
                "created": 1560000000,
                "modified": 1560000000,
            },
            "values": [
                "yandex.ru",
            ],
        },
        6: {
            "full_values": [
                {
                    "normalized": serialized_bad_precalculated_table,
                    "raw": serialized_bad_precalculated_table,
                    "tags": [],
                },
            ],
            "revision": 6,
            "ruleId": "rule-id",
            "source": "PRECALCULATED_TABLES",
            "state": "APPROVED",
            "timestamps": {
                "created": 1560000000,
                "modified": 1560000000,
            },
            "values": [
                serialized_bad_precalculated_table,
            ],
        },
    }
    return mock_crypta_api.rule_conditions
