# coding: utf-8

from mock import patch
from market.idx.devtools.dukalis.offer_diagnostics.lib.analyzer.getters.getters import DataCampInfoGetter
from market.idx.devtools.dukalis.library.tasks.engine import TasksEngine
from market.idx.devtools.dukalis.library.tasks.tasks import TaskResponse
from market.idx.devtools.dukalis.library.analyzer.common.analyzer_params import AnalyzerParams

service_offer_1234 = {
    "price": {
        "basic": {
            "binary_price": {
                "price": 352400000000
            },
            "meta": {
                "source": "PUSH_PARTNER_FEED",
                "timestamp": {
                    "seconds": 1626181932
                }
            },
        }
    },
    "status": {
        "result": "NOT_PUBLISHED_CHECKING",
    }
}

warehouse_1234_1 = {
    "delivery": {
        "market": {
            "calculator": {
                "delivery_bucket_ids": [
                    9815067,
                    9815068,
                ],
                "pickup_bucket_ids": []
            },
        },
    },
    "status": {
        "disabled": [
            {
                "flag": True,
                "meta": {
                    "source": "MARKET_IDX",
                }
            },
            {
                "flag": False,
                "meta": {
                    "source": "MARKET_STOCK",
                }
            }
        ],
        "publish": "HIDDEN",
    }
}

handler_response = {
    "limit": 10,
    "offers": [
        {
            "actual": [
                {
                    "key": 1234,
                    "value": {
                        "warehouse": [
                            {
                                "key": 1,
                                "value": warehouse_1234_1
                            }
                        ]
                    }
                },
            ],
            "basic": {
                "content": {
                    "status": {
                        "result": {
                            "card_status": "NO_CARD_PROCESSING_CHANGES",
                        }
                    }
                }
            },
            "service": [
                {
                    "key": 1234,
                    "value": service_offer_1234
                }
            ]
        }
    ],
    "offset": 0,
    "total": 1
}


def mock_fetch(cls, *args, **kwargs):
    return TaskResponse(True, handler_response, None, None)


def test_sample_datacamp_info_getter():
    # супер-простой пример теста для проверки DataCampInfoGetter
    # копируя реальные офферы/их части в handler_response, можно убедиться в корректности проходов по структуре оффера
    with patch.object(TasksEngine, 'fetch', new=mock_fetch):
        # используются в запросе, но не нужны для проверок внутренностей геттера
        business_id, offer_id, mi_type = 1, 2, 3

        shop_id = 1234
        whid = 1
        params = AnalyzerParams()
        getter = DataCampInfoGetter(params, business_id, shop_id, offer_id, whid, mi_type)

        assert getter.get_basic()
        assert getter.get_service()
        assert getter.get_warehouse()
        assert getter.get_price()
        assert getter.get_status()
        assert getter.get_delivery_status()
        assert getter.get_card_status()
        assert getter.get_integral_status()

        assert getter.get_service() == service_offer_1234
        assert getter.get_warehouse() == warehouse_1234_1
        assert getter.get_price() == (352400000000, 'PUSH_PARTNER_FEED', '2021-07-13 13:12:12')
        assert getter.get_delivery_status() == (True, False, False)

        assert getter.get_card_status() == "NO_CARD_PROCESSING_CHANGES"
        assert getter.get_integral_status() == "NOT_PUBLISHED_CHECKING"

        # это выглядит странно (по MARKET_STOCK flag=false), в коде пометка, что будет переделано
        assert getter.get_status() == ('HIDDEN', ['MARKET_IDX', 'MARKET_STOCK'])
