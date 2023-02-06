package routes

import (
	"reflect"
	"testing"

	"github.com/stretchr/testify/require"

	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestSerializeRouteString(t *testing.T) {
	tree := make(map[string]any)
	err := json.Unmarshal([]byte(jsonRouteString), &tree)
	require.NoError(t, err)

	var route *cr.DeliveryRoute
	traverseTypedJSONTree(reflect.TypeOf(route), reflect.ValueOf(tree))

	var bytes []byte
	bytes, err = json.Marshal(&tree)
	require.NoError(t, err)
	require.Equal(t, jsonRouteStringResult, string(bytes))
}

var jsonRouteString = `{
    "Error": "",
    "delivery_dates": {
        "last_warehouse_offset": {
            "offset": -1,
            "warehouse_position": 0
        },
        "packaging_time": 142255,
        "reception_by_warehouse": "2022-06-07T21:00:00Z",
        "shipment_by_supplier": "2022-06-07T21:00:00Z",
        "shipment_date": "2022-06-07T21:00:00Z",
        "shipment_day": 0,
        "supplier_delivery_list": []
    },
    "delivery_subtype": "ORDINARY",
    "offers": [
        {
            "available_count": 1,
            "cargo_types": [
                750,
                780
            ],
            "category_ids": [],
            "delivery_options": [],
            "feed_id": 0,
            "partner_id": "172",
            "shop_id": 10411915,
            "shop_sku": "protbatonshoko"
        }
    ],
    "packing_boxes": [
        {
            "dimensions": [
                1,
                3,
                8
            ],
            "weight": 60
        }
    ],
    "promise": "",
    "route": {
        "airship_delivery": false,
        "cost": 0,
        "cost_for_shop": 0,
        "date_from": {
            "day": 10,
            "month": 6,
            "year": 2022
        },
        "date_to": {
            "day": 10,
            "month": 6,
            "year": 2022
        },
        "delivery_type": "PICKUP",
        "is_dsbs_to_market_outlet": false,
        "is_external_logistics": false,
        "is_market_courier": true,
        "paths": [
            {
                "point_from": 0,
                "point_to": 1
            },
            {
                "point_from": 1,
                "point_to": 2
            },
            {
                "point_from": 2,
                "point_to": 3
            },
            {
                "point_from": 3,
                "point_to": 4
            },
            {
                "point_from": 4,
                "point_to": 5
            }
        ],
        "points": [
            {
                "ids": {
                    "dsbs_point_id": "",
                    "gps_coords": null,
                    "logistic_point_id": "10000004403",
                    "partner_id": "172",
                    "post_code": 0,
                    "region_id": 120013
                },
                "partner_name": "Яндекс Маркет Софьино",
                "partner_type": "FULFILLMENT",
                "segment_id": "736270",
                "segment_type": "WAREHOUSE",
                "services": [
                    {
                        "code": "CONSOLIDATION",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "3398836",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 8,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "1970-01-01T00:00:00Z",
                        "schedule_start_time": "1970-01-01T00:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-09T00:30:55Z",
                        "type": "INTERNAL",
                        "tz_offset": 0,
                        "working_schedule": []
                    },
                    {
                        "code": "PROCESSING",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "10800s",
                        "duration_delta": 0,
                        "id": "3398834",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 8,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-09T20:59:59Z",
                        "schedule_start_time": "2022-06-08T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-09T00:30:55Z",
                        "type": "INTERNAL",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "SHIPMENT",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "3398835",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 8,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-09T20:59:59Z",
                        "schedule_start_time": "2022-06-08T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-09T03:30:55Z",
                        "type": "OUTBOUND",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            {
                "ids": {
                    "dsbs_point_id": "",
                    "gps_coords": null,
                    "logistic_point_id": "0",
                    "partner_id": "63119",
                    "post_code": 0,
                    "region_id": 193
                },
                "partner_name": "СЦ МК Сестрица Сорока",
                "partner_type": "SORTING_CENTER",
                "segment_id": "831744",
                "segment_type": "MOVEMENT",
                "services": [
                    {
                        "code": "INBOUND",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "3600s",
                        "duration_delta": 0,
                        "id": "9651432",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 8,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-09T20:59:59Z",
                        "schedule_start_time": "2022-06-08T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-09T03:30:55Z",
                        "type": "INBOUND",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "MOVEMENT",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "28800s",
                        "duration_delta": 0,
                        "id": "9651431",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 8,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-09T20:59:59Z",
                        "schedule_start_time": "2022-06-08T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-09T04:30:55Z",
                        "type": "INTERNAL",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "SHIPMENT",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651433",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 9,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-09T20:59:59Z",
                        "schedule_start_time": "2022-06-08T21:00:00Z",
                        "service_meta": [
                            {
                                "key": "START_AT_RIGHT_BORDER",
                                "value": "1"
                            }
                        ],
                        "start_time": "2022-06-09T12:30:55Z",
                        "type": "OUTBOUND",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            {
                "ids": {
                    "dsbs_point_id": "",
                    "gps_coords": null,
                    "logistic_point_id": "10001073009",
                    "partner_id": "63119",
                    "post_code": 0,
                    "region_id": 193
                },
                "partner_name": "СЦ МК Сестрица Сорока",
                "partner_type": "SORTING_CENTER",
                "segment_id": "831743",
                "segment_type": "warehouse",
                "services": [
                    {
                        "code": "PROCESSING",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "7200s",
                        "duration_delta": 0,
                        "id": "9651427",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 9,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T20:59:59Z",
                        "schedule_start_time": "2022-06-09T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-10T00:25:55Z",
                        "type": "INTERNAL",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "SORT",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "7200s",
                        "duration_delta": 0,
                        "id": "9651428",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 9,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T20:59:59Z",
                        "schedule_start_time": "2022-06-09T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-10T02:25:55Z",
                        "type": "INTERNAL",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "SHIPMENT",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "7200s",
                        "duration_delta": 0,
                        "id": "9651430",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 9,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T20:59:59Z",
                        "schedule_start_time": "2022-06-09T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-10T04:25:55Z",
                        "type": "OUTBOUND",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            {
                "ids": {
                    "dsbs_point_id": "",
                    "gps_coords": null,
                    "logistic_point_id": "0",
                    "partner_id": "63132",
                    "post_code": 0,
                    "region_id": 0
                },
                "partner_name": "МК Сестрица Сорока",
                "partner_type": "DELIVERY",
                "segment_id": "831774",
                "segment_type": "movement",
                "services": [
                    {
                        "code": "INBOUND",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651498",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": null,
                        "schedule_start_time": null,
                        "service_meta": [],
                        "start_time": "2022-06-10T06:25:55Z",
                        "type": "INBOUND",
                        "tz_offset": 0,
                        "working_schedule": []
                    },
                    {
                        "code": "MOVEMENT",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651508",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T20:59:59Z",
                        "schedule_start_time": "2022-06-09T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-10T06:25:55Z",
                        "type": "INTERNAL",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "SHIPMENT",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651499",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T20:59:59Z",
                        "schedule_start_time": "2022-06-09T21:00:00Z",
                        "service_meta": [
                            {
                                "key": "START_AT_RIGHT_BORDER",
                                "value": "1"
                            }
                        ],
                        "start_time": "2022-06-10T06:25:55Z",
                        "type": "OUTBOUND",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            {
                "ids": {
                    "dsbs_point_id": "",
                    "gps_coords": null,
                    "logistic_point_id": "0",
                    "partner_id": "63132",
                    "post_code": 0,
                    "region_id": 225
                },
                "partner_name": "МК Сестрица Сорока",
                "partner_type": "DELIVERY",
                "segment_id": "831779",
                "segment_type": "linehaul",
                "services": [
                    {
                        "code": "DELIVERY",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651509",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T20:59:59Z",
                        "schedule_start_time": "2022-06-09T21:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-10T06:25:55Z",
                        "type": "INTERNAL",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 86399,
                                        "start_time": 0
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "LAST_MILE",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651510",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": null,
                        "schedule_start_time": null,
                        "service_meta": [],
                        "start_time": "2022-06-10T06:25:55Z",
                        "type": "INTERNAL",
                        "tz_offset": 0,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 39600,
                                        "start_time": 39600
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "SHIPMENT",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651511",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T09:00:00Z",
                        "schedule_start_time": "2022-06-10T05:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-10T06:25:55Z",
                        "type": "OUTBOUND",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 43200,
                                        "start_time": 28800
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            {
                "ids": {
                    "dsbs_point_id": "",
                    "gps_coords": null,
                    "logistic_point_id": "10001073004",
                    "partner_id": "63112",
                    "post_code": 394026,
                    "region_id": 193
                },
                "partner_name": "Партнёрский ПВЗ ООО Белый и Черный",
                "partner_type": "DELIVERY",
                "segment_id": "831732",
                "segment_type": "pickup",
                "services": [
                    {
                        "code": "INBOUND",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651404",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T19:00:00Z",
                        "schedule_start_time": "2022-06-10T07:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-10T07:00:00Z",
                        "type": "INBOUND",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 79200,
                                        "start_time": 36000
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "code": "RETURN_ALLOWED",
                        "cost": 0,
                        "delivery_intervals": [],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651403",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": null,
                        "schedule_start_time": null,
                        "service_meta": [],
                        "start_time": "2022-06-10T07:00:00Z",
                        "type": "INTERNAL",
                        "tz_offset": 0,
                        "working_schedule": []
                    },
                    {
                        "code": "HANDING",
                        "cost": 0,
                        "delivery_intervals": [
                            {
                                "from": {
                                    "hour": 10,
                                    "minute": 0
                                },
                                "to": {
                                    "hour": 12,
                                    "minute": 0
                                }
                            }
                        ],
                        "disabled_dates": [],
                        "duration": "0s",
                        "duration_delta": 0,
                        "id": "9651399",
                        "items": [
                            {
                                "item_index": 0,
                                "quantity": 1
                            }
                        ],
                        "logistic_date": {
                            "day": 10,
                            "month": 6,
                            "year": 2022
                        },
                        "schedule_end_time": "2022-06-10T19:00:00Z",
                        "schedule_start_time": "2022-06-10T07:00:00Z",
                        "service_meta": [],
                        "start_time": "2022-06-10T07:00:00Z",
                        "type": "OUTBOUND",
                        "tz_offset": 10800,
                        "working_schedule": [
                            {
                                "days_of_week": [
                                    0,
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6
                                ],
                                "time_windows": [
                                    {
                                        "end_time": 79200,
                                        "start_time": 36000
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        ],
        "shipment_warehouse_id": "63119",
        "tariff_id": "100721"
    },
    "route_debug_messages": [],
    "route_id": "1d4b53c9-e72b-11ec-8db8-be1c9c73c41c",
    "string_delivery_dates": {
        "last_warehouse_offset": {
            "offset": -1,
            "warehouse_position": 0
        },
        "packaging_time": "PT39H1855M",
        "reception_by_warehouse": "2022-06-08T00:00:00+03:00",
        "shipment_by_supplier": "2022-06-08T00:00:00+03:00",
        "shipment_date": "2022-06-08",
        "supplier_delivery_list": []
    },
    "virtual_box": {
        "dimensions": [
            1,
            3,
            8
        ],
        "weight": 60
    }
}`

var jsonRouteStringResult = `{"Error":"","delivery_dates":{"last_warehouse_offset":{"offset":-1,"warehouse_position":0},"packaging_time":142255,"reception_by_warehouse":{"nanos":0,"seconds":1654635600},"shipment_by_supplier":{"nanos":0,"seconds":1654635600},"shipment_date":{"nanos":0,"seconds":1654635600},"shipment_day":0,"supplier_delivery_list":[]},"delivery_subtype":"ORDINARY","offers":[{"available_count":1,"cargo_types":[750,780],"category_ids":[],"delivery_options":[],"feed_id":0,"partner_id":172,"shop_id":10411915,"shop_sku":"protbatonshoko"}],"packing_boxes":[{"dimensions":[1,3,8],"weight":60}],"promise":"","route":{"airship_delivery":false,"cost":0,"cost_for_shop":0,"date_from":{"day":10,"month":6,"year":2022},"date_to":{"day":10,"month":6,"year":2022},"delivery_type":"PICKUP","is_dsbs_to_market_outlet":false,"is_external_logistics":false,"is_market_courier":true,"paths":[{"point_from":0,"point_to":1},{"point_from":1,"point_to":2},{"point_from":2,"point_to":3},{"point_from":3,"point_to":4},{"point_from":4,"point_to":5}],"points":[{"ids":{"dsbs_point_id":"","gps_coords":null,"logistic_point_id":10000004403,"partner_id":172,"post_code":0,"region_id":120013},"partner_name":"Яндекс Маркет Софьино","partner_type":"FULFILLMENT","segment_id":736270,"segment_type":"WAREHOUSE","services":[{"code":"CONSOLIDATION","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":3398836,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":8,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":0},"schedule_start_time":{"nanos":0,"seconds":0},"service_meta":[],"start_time":{"nanos":0,"seconds":1654734655},"type":"INTERNAL","tz_offset":0,"working_schedule":[]},{"code":"PROCESSING","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":10800},"duration_delta":0,"id":3398834,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":8,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654808399},"schedule_start_time":{"nanos":0,"seconds":1654722000},"service_meta":[],"start_time":{"nanos":0,"seconds":1654734655},"type":"INTERNAL","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]},{"code":"SHIPMENT","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":3398835,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":8,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654808399},"schedule_start_time":{"nanos":0,"seconds":1654722000},"service_meta":[],"start_time":{"nanos":0,"seconds":1654745455},"type":"OUTBOUND","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]}]},{"ids":{"dsbs_point_id":"","gps_coords":null,"logistic_point_id":0,"partner_id":63119,"post_code":0,"region_id":193},"partner_name":"СЦ МК Сестрица Сорока","partner_type":"SORTING_CENTER","segment_id":831744,"segment_type":"MOVEMENT","services":[{"code":"INBOUND","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":3600},"duration_delta":0,"id":9651432,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":8,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654808399},"schedule_start_time":{"nanos":0,"seconds":1654722000},"service_meta":[],"start_time":{"nanos":0,"seconds":1654745455},"type":"INBOUND","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]},{"code":"MOVEMENT","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":28800},"duration_delta":0,"id":9651431,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":8,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654808399},"schedule_start_time":{"nanos":0,"seconds":1654722000},"service_meta":[],"start_time":{"nanos":0,"seconds":1654749055},"type":"INTERNAL","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]},{"code":"SHIPMENT","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651433,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":9,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654808399},"schedule_start_time":{"nanos":0,"seconds":1654722000},"service_meta":[{"key":"START_AT_RIGHT_BORDER","value":"1"}],"start_time":{"nanos":0,"seconds":1654777855},"type":"OUTBOUND","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]}]},{"ids":{"dsbs_point_id":"","gps_coords":null,"logistic_point_id":10001073009,"partner_id":63119,"post_code":0,"region_id":193},"partner_name":"СЦ МК Сестрица Сорока","partner_type":"SORTING_CENTER","segment_id":831743,"segment_type":"warehouse","services":[{"code":"PROCESSING","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":7200},"duration_delta":0,"id":9651427,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":9,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654894799},"schedule_start_time":{"nanos":0,"seconds":1654808400},"service_meta":[],"start_time":{"nanos":0,"seconds":1654820755},"type":"INTERNAL","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]},{"code":"SORT","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":7200},"duration_delta":0,"id":9651428,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":9,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654894799},"schedule_start_time":{"nanos":0,"seconds":1654808400},"service_meta":[],"start_time":{"nanos":0,"seconds":1654827955},"type":"INTERNAL","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]},{"code":"SHIPMENT","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":7200},"duration_delta":0,"id":9651430,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":9,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654894799},"schedule_start_time":{"nanos":0,"seconds":1654808400},"service_meta":[],"start_time":{"nanos":0,"seconds":1654835155},"type":"OUTBOUND","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]}]},{"ids":{"dsbs_point_id":"","gps_coords":null,"logistic_point_id":0,"partner_id":63132,"post_code":0,"region_id":0},"partner_name":"МК Сестрица Сорока","partner_type":"DELIVERY","segment_id":831774,"segment_type":"movement","services":[{"code":"INBOUND","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651498,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":0},"schedule_start_time":{"nanos":0,"seconds":0},"service_meta":[],"start_time":{"nanos":0,"seconds":1654842355},"type":"INBOUND","tz_offset":0,"working_schedule":[]},{"code":"MOVEMENT","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651508,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654894799},"schedule_start_time":{"nanos":0,"seconds":1654808400},"service_meta":[],"start_time":{"nanos":0,"seconds":1654842355},"type":"INTERNAL","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]},{"code":"SHIPMENT","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651499,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654894799},"schedule_start_time":{"nanos":0,"seconds":1654808400},"service_meta":[{"key":"START_AT_RIGHT_BORDER","value":"1"}],"start_time":{"nanos":0,"seconds":1654842355},"type":"OUTBOUND","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]}]},{"ids":{"dsbs_point_id":"","gps_coords":null,"logistic_point_id":0,"partner_id":63132,"post_code":0,"region_id":225},"partner_name":"МК Сестрица Сорока","partner_type":"DELIVERY","segment_id":831779,"segment_type":"linehaul","services":[{"code":"DELIVERY","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651509,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654894799},"schedule_start_time":{"nanos":0,"seconds":1654808400},"service_meta":[],"start_time":{"nanos":0,"seconds":1654842355},"type":"INTERNAL","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":86399,"start_time":0}]}]},{"code":"LAST_MILE","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651510,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":0},"schedule_start_time":{"nanos":0,"seconds":0},"service_meta":[],"start_time":{"nanos":0,"seconds":1654842355},"type":"INTERNAL","tz_offset":0,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":39600,"start_time":39600}]}]},{"code":"SHIPMENT","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651511,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654851600},"schedule_start_time":{"nanos":0,"seconds":1654837200},"service_meta":[],"start_time":{"nanos":0,"seconds":1654842355},"type":"OUTBOUND","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":43200,"start_time":28800}]}]}]},{"ids":{"dsbs_point_id":"","gps_coords":null,"logistic_point_id":10001073004,"partner_id":63112,"post_code":394026,"region_id":193},"partner_name":"Партнёрский ПВЗ ООО Белый и Черный","partner_type":"DELIVERY","segment_id":831732,"segment_type":"pickup","services":[{"code":"INBOUND","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651404,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654887600},"schedule_start_time":{"nanos":0,"seconds":1654844400},"service_meta":[],"start_time":{"nanos":0,"seconds":1654844400},"type":"INBOUND","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":79200,"start_time":36000}]}]},{"code":"RETURN_ALLOWED","cost":0,"delivery_intervals":[],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651403,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":0},"schedule_start_time":{"nanos":0,"seconds":0},"service_meta":[],"start_time":{"nanos":0,"seconds":1654844400},"type":"INTERNAL","tz_offset":0,"working_schedule":[]},{"code":"HANDING","cost":0,"delivery_intervals":[{"from":{"hour":10,"minute":0},"to":{"hour":12,"minute":0}}],"disabled_dates":[],"duration":{"nanos":0,"seconds":0},"duration_delta":0,"id":9651399,"items":[{"item_index":0,"quantity":1}],"logistic_date":{"day":10,"month":6,"year":2022},"schedule_end_time":{"nanos":0,"seconds":1654887600},"schedule_start_time":{"nanos":0,"seconds":1654844400},"service_meta":[],"start_time":{"nanos":0,"seconds":1654844400},"type":"OUTBOUND","tz_offset":10800,"working_schedule":[{"days_of_week":[0,1,2,3,4,5,6],"time_windows":[{"end_time":79200,"start_time":36000}]}]}]}],"shipment_warehouse_id":63119,"tariff_id":100721},"route_debug_messages":[],"route_id":"1d4b53c9-e72b-11ec-8db8-be1c9c73c41c","string_delivery_dates":{"last_warehouse_offset":{"offset":-1,"warehouse_position":0},"packaging_time":"PT39H1855M","reception_by_warehouse":"2022-06-08T00:00:00+03:00","shipment_by_supplier":"2022-06-08T00:00:00+03:00","shipment_date":"2022-06-08","supplier_delivery_list":[]},"virtual_box":{"dimensions":[1,3,8],"weight":60}}`
