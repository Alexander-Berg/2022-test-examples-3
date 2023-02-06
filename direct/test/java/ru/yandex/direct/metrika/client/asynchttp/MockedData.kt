package ru.yandex.direct.metrika.client.asynchttp

const val GET_BYTIME_STAT_E2E_ANALYTICS_WITH_GOAL_REQUEST =
    "GET:/stat/v1/data/bytime?accuracy=1&id=79204741&group=day&attribution=CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK&currency=RUB&dimensions=ym%3Aev%3A%3Cattribution%3EExpenseSource&metrics=ym%3Aev%3AexpenseClicks%2Cym%3Aev%3Agoal%3Cgoal_id%3Evisits%2Cym%3Aev%3Aexpenses%3Ccurrency%3E%2Cym%3Aev%3Agoal%3Cgoal_id%3Eecommerce%3Ccurrency%3EConvertedRevenue&goal_id=186218983&date1=2021-05-10&date2=2021-05-11"
const val GET_BYTIME_STAT_E2E_ANALYTICS_SKIPPED_GOAL_DATA =
    "GET:/stat/v1/data/bytime?accuracy=1&id=79204741&group=day&attribution=CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK&currency=RUB&dimensions=ym%3Aev%3A%3Cattribution%3EExpenseSource&metrics=ym%3Aev%3AexpenseClicks%2Cym%3Aev%3Aexpenses%3Ccurrency%3E%2Cym%3Aev%3Aecommerce%3Ccurrency%3EConvertedRevenue&date1=2021-05-10&date2=2021-05-11"
const val GET_BYTIME_STAT_E2E_ANALYTICS_WITH_CHIEF_LOGIN_REQUEST =
    "GET:/stat/v1/data/bytime?accuracy=1&id=79204741&group=day&attribution=CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK&currency=RUB&dimensions=ym%3Aev%3A%3Cattribution%3EExpenseSource&metrics=ym%3Aev%3AexpenseClicks%2Cym%3Aev%3Aexpenses%3Ccurrency%3E%2Cym%3Aev%3Aecommerce%3Ccurrency%3EConvertedRevenue&date1=2021-05-10&date2=2021-05-11&direct_client_logins=chief_login"
const val GET_BYTIME_STAT_E2E_ANALYTICS_WITHOUT_REVENUE =
    "GET:/stat/v1/data/bytime?accuracy=1&id=79204741&group=day&attribution=CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK&currency=RUB&dimensions=ym%3Aev%3A%3Cattribution%3EExpenseSource&metrics=ym%3Aev%3AexpenseClicks%2Cym%3Aev%3Aexpenses%3Ccurrency%3E&date1=2021-05-10&date2=2021-05-11"
const val GET_BYTIME_STAT_E2E_ANALYTICS_WITH_CONVERSION_RATE_REQUEST =
    "GET:/stat/v1/data/bytime?accuracy=1&id=79204741&group=day&attribution=CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK&currency=RUB&dimensions=ym%3Aev%3A%3Cattribution%3EExpenseSource&metrics=ym%3Aev%3AexpenseClicks%2Cym%3Aev%3AanyGoalConversionRate%2Cym%3Aev%3Aexpenses%3Ccurrency%3E%2Cym%3Aev%3Aecommerce%3Ccurrency%3EConvertedRevenue&date1=2021-05-10&date2=2021-05-11"

const val GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_REQUEST =
    "GET:/stat/v1/data/bytime?accuracy=1&id=16705159&group=day&attribution=CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK&currency=BYN&dimensions=ym%3As%3A%3Cattribution%3ETrafficSource%2Cym%3As%3A%3Cattribution%3ESourceEngine&metrics=ym%3As%3Avisits%2Cym%3As%3Agoal%3Cgoal_id%3Evisits%2Cym%3As%3Agoal%3Cgoal_id%3Eecommerce%3Ccurrency%3EConvertedRevenue&goal_id=1820101&date1=2022-01-17&date2=2022-01-17"
const val GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_WITH_ROW_IDS_REQUEST =
    "GET:/stat/v1/data/bytime?accuracy=1&id=16705159&group=day&attribution=CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK&currency=BYN&dimensions=ym%3As%3A%3Cattribution%3ETrafficSource%2Cym%3As%3A%3Cattribution%3ESourceEngine&metrics=ym%3As%3Avisits%2Cym%3As%3Agoal%3Cgoal_id%3Evisits%2Cym%3As%3Agoal%3Cgoal_id%3Eecommerce%3Ccurrency%3EConvertedRevenue&goal_id=1820101&date1=2022-01-17&date2=2022-01-17&row_ids=%5B%5B%22ad%22%2C%22ad.Google%20Ads%22%5D%2C%5B%22organic%22%5D%5D"

const val GET_AVAILABLE_SOURCES_REQUEST =
    "GET:/stat/v1/data?id=16705159&attribution=CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK&dimensions=ym%3As%3A%3Cattribution%3ETrafficSource%2Cym%3As%3A%3Cattribution%3ESourceEngine&metrics=ym%3As%3Avisits&filters=ym%3As%3A%3Cattribution%3ETrafficSource%3D.%28%27ad%27%2C%27social%27%29%20OR%20ym%3As%3A%3Cattribution%3ESourceEngine%3D.%28%27messenger.telegram%27%29&limit=5&date1=2022-01-17&date2=2022-01-17"

const val GET_BYTIME_STAT_E2E_ANALYTICS_RESPONSE = """
{
    "query": {
        "ids": [
            79204741
        ],
        "dimensions": [
            "ym:ev:CROSS_DEVICE_LAST_SIGNIFICANTExpenseSource"
        ],
        "metrics": [
            "ym:ev:expenseClicks",
            "ym:ev:goal186218983visits",
            "ym:ev:expensesRUB",
            "ym:ev:goal186218983ecommerceRUBConvertedRevenue"
        ],
        "sort": [
            "-ym:ev:expenseClicks"
        ],
        "date1": "2021-05-10",
        "date2": "2021-05-11",
        "goal_id": "186218983",
        "attribution": "CROSS_DEVICE_LAST_SIGNIFICANT",
        "group": "day",
        "auto_group_size": "1",
        "attr_name": "",
        "quantile": "50",
        "offline_window": "21",
        "currency": "RUB",
        "adfox_event_id": "0",
        "auto_group_type": "day"
    },
    "data": [
        {
            "dimensions": [
                {
                    "name": "google",
                    "id": "1.google"
                }
            ],
            "metrics": [
                [
                    10.0,
                    20.0
                ],
                [
                    5.0,
                    6.0
                ],
                [
                    152621.0,
                    150927.0
                ],
                [
                    152621.0,
                    150927.0
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "name": "other",
                    "id": "1.other"
                }
            ],
            "metrics": [
                [
                    100.0,
                    110.0
                ],
                [
                    50.0,
                    51.0
                ],
                [
                    8878.0,
                    8942.0
                ],
                [
                    8878.0,
                    8942.0
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "name": "yandex",
                    "id": "1.yandex"
                }
            ],
            "metrics": [
                [
                    1000.0,
                    1010.0
                ],
                [
                    100.0,
                    101.0
                ],
                [
                    125361.0,
                    89432.0
                ],
                [
                    125361.0,
                    89432.0
                ]
            ]
        }
    ],
    "meta": {},
    "total_rows": 2,
    "total_rows_rounded": false,
    "sampled": false,
    "sampleable": false,
    "contains_sensitive_data": false,
    "sample_share": 1.0,
    "max_sample_share": 1.0,
    "sample_size": 0,
    "sample_space": 0,
    "data_lag": 177,
    "totals": [
        [
            1110.0,
            1140.0
        ],
        [
            155.0,
            158.0
        ],
        [
            286860.0,
            249301.0
        ],
        [
            286860.0,
            249301.0
        ]
    ],
    "time_intervals": [
        [
            "2021-05-10",
            "2021-05-10"
        ],
        [
            "2021-05-11",
            "2021-05-11"
        ]
    ],
    "_profile": {
    }
}
"""

const val GET_BYTIME_STAT_E2E_ANALYTICS_SKIPPED_GOAL_DATA_RESPONSE = """
{
    "query": {
        "ids": [
            79204741
        ],
        "dimensions": [
            "ym:ev:CROSS_DEVICE_LAST_SIGNIFICANTExpenseSource"
        ],
        "metrics": [
            "ym:ev:expenseClicks",
            "ym:ev:expensesRUB",
            "ym:ev:ecommerceRUBConvertedRevenue"
        ],
        "sort": [
            "-ym:ev:expenseClicks"
        ],
        "date1": "2021-05-10",
        "date2": "2021-05-11",
        "attribution": "CROSS_DEVICE_LAST_SIGNIFICANT",
        "group": "day",
        "auto_group_size": "1",
        "attr_name": "",
        "quantile": "50",
        "offline_window": "21",
        "currency": "RUB",
        "adfox_event_id": "0",
        "auto_group_type": "day"
    },
    "data": [
        {
            "dimensions": [
                {
                    "name": "google",
                    "id": "1.google"
                }
            ],
            "metrics": [
                [
                    10.0,
                    20.0
                ],
                [
                    152621.0,
                    150927.0
                ],
                [
                    152621.0,
                    150927.0
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "name": "other",
                    "id": "1.other"
                }
            ],
            "metrics": [
                [
                    100.0,
                    110.0
                ],
                [
                    8878.0,
                    8942.0
                ],
                [
                    8878.0,
                    8942.0
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "name": "yandex",
                    "id": "1.yandex"
                }
            ],
            "metrics": [
                [
                    1000.0,
                    1010.0
                ],
                [
                    125361.0,
                    89432.0
                ],
                [
                    125361.0,
                    89432.0
                ]
            ]
        }
    ],
    "meta": {},
    "total_rows": 2,
    "total_rows_rounded": false,
    "sampled": false,
    "sampleable": false,
    "contains_sensitive_data": false,
    "sample_share": 1.0,
    "max_sample_share": 1.0,
    "sample_size": 0,
    "sample_space": 0,
    "data_lag": 177,
    "totals": [
        [
            1110.0,
            1140.0
        ],
        [
            286860.0,
            249301.0
        ],
        [
            286860.0,
            249301.0
        ]
    ],
    "time_intervals": [
        [
            "2021-05-10",
            "2021-05-10"
        ],
        [
            "2021-05-11",
            "2021-05-11"
        ]
    ],
    "_profile": {
    }
}
"""

const val GET_BYTIME_STAT_E2E_ANALYTICS_WITH_ANY_GOAL_CONVERSION_RATE_RESPONSE = """
{
    "query": {
        "ids": [
            79204741
        ],
        "dimensions": [
            "ym:ev:CROSS_DEVICE_LAST_SIGNIFICANTExpenseSource"
        ],
        "metrics": [
            "ym:ev:expenseClicks",
            "ym:ev:anyGoalConversionRate",
            "ym:ev:expensesRUB",
            "ym:ev:ecommerceRUBConvertedRevenue"
        ],
        "sort": [
            "-ym:ev:expenseClicks"
        ],
        "date1": "2021-05-10",
        "date2": "2021-05-11",
        "attribution": "CROSS_DEVICE_LAST_SIGNIFICANT",
        "group": "day",
        "auto_group_size": "1",
        "attr_name": "",
        "quantile": "50",
        "offline_window": "21",
        "currency": "RUB",
        "adfox_event_id": "0",
        "auto_group_type": "day"
    },
    "data": [
        {
            "dimensions": [
                {
                    "name": "google",
                    "id": "1.google"
                }
            ],
            "metrics": [
                [
                    10.0,
                    10.0
                ],
                [
                    null,
                    20.0
                ],
                [
                    152621.0,
                    150927.0
                ],
                [
                    152621.0,
                    150927.0
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "name": "other",
                    "id": "1.other"
                }
            ],
            "metrics": [
                [
                    100.0,
                    110.0
                ],
                [
                    31.0,
                    10.0
                ],
                [
                    8878.0,
                    8942.0
                ],
                [
                    8878.0,
                    8942.0
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "name": "yandex",
                    "id": "1.yandex"
                }
            ],
            "metrics": [
                [
                    1000.0,
                    1010.0
                ],
                [
                    10.0,
                    10.0
                ],
                [
                    125361.0,
                    89432.0
                ],
                [
                    125361.0,
                    89432.0
                ]
            ]
        }
    ],
    "total_rows": 2,
    "total_rows_rounded": false,
    "sampled": false,
    "sampleable": false,
    "contains_sensitive_data": false,
    "sample_share": 1.0,
    "max_sample_share": 1.0,
    "sample_size": 0,
    "sample_space": 0,
    "data_lag": 177,
    "totals": [
        [
            1110.0,
            1140.0
        ],
        [
            22.52083779,
            19.37829147
        ],
        [
            286860.0,
            249301.0
        ],
        [
            286860.0,
            249301.0
        ]
    ],
    "time_intervals": [
        [
            "2021-05-10",
            "2021-05-10"
        ],
        [
            "2021-05-11",
            "2021-05-11"
        ]
    ],
    "_profile": {
    }
}
"""

const val GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_RESPONSE = """
{
    "query": {
        "ids": [
            16705159
        ],
        "dimensions": [
            "ym:s:CROSS_DEVICE_LAST_SIGNIFICANTTrafficSource"
        ],
        "metrics": [
            "ym:s:visits",
            "ym:s:goal1820101visits",
            "ym:s:goal1820101ecommerceBYNConvertedRevenue"
        ],
        "sort": [
            "-ym:s:visits"
        ],
        "date1": "2022-01-17",
        "date2": "2022-01-17",
        "goal_id": "1820101",
        "attribution": "CROSS_DEVICE_LAST_SIGNIFICANT",
        "currency": "BYN",
        "group": "day",
        "auto_group_size": "1",
        "attr_name": "",
        "quantile": "50",
        "offline_window": "21",
        "adfox_event_id": "0",
        "auto_group_type": "day"
    },
    "data": [
        {
            "dimensions": [
                {
                    "icon_id": "3",
                    "icon_type": "traffic-source",
                    "name": "Ad traffic",
                    "id": "ad"
                }
            ],
            "metrics": [
                [
                    103424.0
                ],
                [
                    263421.0
                ],
                [
                    553590.1
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "icon_id": "2",
                    "icon_type": "traffic-source",
                    "name": "Search engine traffic",
                    "id": "organic"
                }
            ],
            "metrics": [
                [
                    69000.0
                ],
                [
                    137352.0
                ],
                [
                    552283.339998
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "icon_id": "0",
                    "icon_type": "traffic-source",
                    "name": "Direct traffic",
                    "id": "direct"
                }
            ],
            "metrics": [
                [
                    6599.0
                ],
                [
                    7013.0
                ],
                [
                    71226.019999
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "icon_id": "-1",
                    "icon_type": "traffic-source",
                    "name": "Internal traffic",
                    "id": "internal"
                }
            ],
            "metrics": [
                [
                    3197.0
                ],
                [
                    6252.0
                ],
                [
                    49577.279999
                ]
            ]
        }
    ],
    "total_rows": 1,
    "total_rows_rounded": false,
    "sampled": false,
    "contains_sensitive_data": false,
    "sample_share": 1.0,
    "sample_size": 185839,
    "sample_space": 185839,
    "data_lag": 168,
    "totals": [
        [
            185832.0
        ],
        [
            418983.0
        ],
        [
            1276255.039996
        ]
    ],
    "time_intervals": [
        [
            "2022-01-17",
            "2022-01-17"
        ]
    ],
    "_profile": {
    }
}
"""

const val GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_DETAILED_RESPONSE = """
{
    "query": {
        "ids": [
            16705159
        ],
        "dimensions": [
            "ym:s:CROSS_DEVICE_LAST_SIGNIFICANTTrafficSource",
            "ym:s:CROSS_DEVICE_LAST_SIGNIFICANTSourceEngine"
        ],
        "metrics": [
            "ym:s:visits",
            "ym:s:goal1820101visits",
            "ym:s:goal1820101ecommerceBYNConvertedRevenue"
        ],
        "sort": [
            "-ym:s:visits"
        ],
        "date1": "2022-01-17",
        "date2": "2022-01-17",
        "goal_id": "1820101",
        "attribution": "CROSS_DEVICE_LAST_SIGNIFICANT",
        "currency": "BYN",
        "group": "day",
        "auto_group_size": "1",
        "attr_name": "",
        "quantile": "50",
        "offline_window": "21",
        "adfox_event_id": "0",
        "auto_group_type": "day"
    },
    "data": [
        {
            "dimensions": [
                {
                    "icon_id": "3",
                    "icon_type": "traffic-source",
                    "name": "Ad traffic",
                    "id": "ad"
                },
                {
                    "name": "Google Ads",
                    "id": "ad.Google Ads",
                    "favicon": "google.com",
                    "url": null
                }
            ],
            "metrics": [
                [
                    103424.0
                ],
                [
                    263421.0
                ],
                [
                    553590.1
                ]
            ]
        },
        {
            "dimensions": [
                {
                    "icon_id": "2",
                    "icon_type": "traffic-source",
                    "name": "Search engine traffic",
                    "id": "organic"
                }
            ],
            "metrics": [
                [
                    69000.0
                ],
                [
                    137352.0
                ],
                [
                    552283.339998
                ]
            ]
        }
    ],
    "total_rows": 1,
    "total_rows_rounded": false,
    "sampled": false,
    "contains_sensitive_data": false,
    "sample_share": 1.0,
    "sample_size": 185839,
    "sample_space": 185839,
    "data_lag": 168,
    "totals": [
        [
            185832.0
        ],
        [
            418983.0
        ],
        [
            1276255.039996
        ]
    ],
    "time_intervals": [
        [
            "2022-01-17",
            "2022-01-17"
        ]
    ],
    "_profile": {
    }
}
"""

const val GET_AVAILABLE_SOURCES_RESPONSE = """
{
    "query": {
        "ids": [
            16705159
        ],
        "dimensions": [
            "ym:s:CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICKTrafficSource",
            "ym:s:CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICKSourceEngine"
        ],
        "metrics": [
            "ym:s:visits"
        ],
        "sort": [
            "-ym:s:visits"
        ],
        "humanized_filter": "Traffic source among (Ad traffic,Social network traffic) or Traffic source (detailed) = 'Telegram'",
        "date1": "2022-01-17",
        "date2": "2022-01-17",
        "filters": "ym:s:CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICKTrafficSource=.('ad','social') OR ym:s:CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICKSourceEngine=.('messenger.telegram')",
        "limit": 10,
        "offset": 1,
        "attribution": "CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK",
        "group": "Week",
        "auto_group_size": "1",
        "attr_name": "",
        "quantile": "50",
        "offline_window": "21",
        "currency": "BYN",
        "adfox_event_id": "0"
    },
    "data": [
        {
            "dimensions": [
                {
                    "icon_id": "3",
                    "icon_type": "traffic-source",
                    "name": "Ad traffic",
                    "id": "ad"
                },
                {
                    "name": "Yandex: Direct",
                    "id": "ad.Яндекс: Директ",
                    "favicon": "direct.yandex.ru",
                    "url": null
                }
            ],
            "metrics": [
                50594.0
            ]
        },
        {
            "dimensions": [
                {
                    "icon_id": "8",
                    "icon_type": "traffic-source",
                    "name": "Social network traffic",
                    "id": "social"
                },
                {
                    "name": "instagram.com",
                    "id": "social.instagram",
                    "favicon": "instagram.com",
                    "url": null
                }
            ],
            "metrics": [
                388.0
            ]
        },
        {
            "dimensions": [
                {
                    "icon_id": "10",
                    "icon_type": "traffic-source",
                    "name": "Messenger traffic",
                    "id": "messenger"
                },
                {
                    "name": "Telegram",
                    "id": "messenger.telegram",
                    "favicon": "Telegram",
                    "url": null
                }
            ],
            "metrics": [
                69.0
            ]
        }
    ],
    "total_rows": 3,
    "total_rows_rounded": false,
    "sampled": false,
    "contains_sensitive_data": false,
    "sample_share": 1.0,
    "sample_size": 1000000,
    "sample_space": 1000000,
    "data_lag": 0,
    "totals": [
        120720.0
    ],
    "min": [
        788.0
    ],
    "max": [
        50594.0
    ],
    "_profile": {
    }
}
"""
