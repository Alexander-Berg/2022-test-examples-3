# coding: utf-8

from hamcrest import assert_that, not_none, equal_to
import logging
import os
import pytest

import yt.wrapper as yt

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import DeliveryDiffEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.yatf.common import get_source_path
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampYtUnitedOffersRows
from market.idx.yatf.utils.utils import rows_as_table

# Warehouse 172 is used in the data/nordstream.pb.sn test file
WAREHOUSE_ID = 172


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'yt_home': '//home/datacamp/united',
            },
            'delivery_diff': {
                'enable': True,
                'yt_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'output_dir': 'delivery_diff',
                'geobase_xml_path': os.path.join(get_source_path(), 'market', 'idx', 'datacamp', 'routines', 'tests', 'data', 'geobase.xml'),
                'geo2_c2p_path': os.path.join(get_source_path(), 'market', 'idx', 'datacamp', 'routines', 'tests', 'data', 'geo2.c2p'),
                'nordstream_path': os.path.join(get_source_path(), 'market', 'idx', 'datacamp', 'routines', 'tests', 'data', 'nordstream.pbuf.sn'),
            }
        })
    return config


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': business_id * 10,
            'status': 'publish',
            'mbi': {
                'shop_id': business_id * 10,
                'business_id': business_id,
                'datafeed_id': 1000,
                'is_push_partner': True,
                'warehouse_id': WAREHOUSE_ID,
                'is_enabled': True
            }
        } for business_id in range(1, 5)
    ]


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'T100',
                'feed_id': 1000,
            },
            'content': {
                'master_data': {
                    'dimensions': {
                        'height_mkm': 120000,
                        'length_mkm': 220000,
                        'width_mkm': 10000,
                    },
                    'weight_gross': {
                        'value_mg': 2600,
                    }
                },
                'status': {
                    'content_system_status': {
                        'status_content_version': {
                            'counter': 11,
                        }
                    }
                }
            },
            'status': {
                'version': {
                    'actual_content_version': {
                        'counter': 11,
                    }
                }
            }
        } for business_id in range(1, 5)
    ] + [
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T600',
            }
        }
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'T100',
                'shop_id': business_id * 10,
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.BLUE,
            },
            'content': {
                'status': {
                    'content_system_status': {
                        'service_offer_state': DTC.CONTENT_STATE_READY,
                    }
                }
            },
            'status': {
                'version': {
                    'original_partner_data_version': {
                        'counter': 22,
                    }
                }
            }

        } for business_id in range(1, 5)
    ] + [
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T600',
                'shop_id': 60 + i,
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.BLUE,
            }
        } for i in range(0, 2)
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'T100',
                'warehouse_id': WAREHOUSE_ID,
                'shop_id': business_id * 10,
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.BLUE,
            },
            'status': {
                'ready_for_publication': {
                    'value': DTC.ReadinessForPublicationStatus.READY,
                },
                'publication': {
                    'value': DTC.PublicationStatus.PUBLISHED,
                },
            },
            'stock_info': {
                'market_stocks': {
                    'count': 10,
                },
            },
            'tech_info': {
                'last_mining': {
                    'original_partner_data_version': {
                        'counter': 22,
                    },
                },
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': DTC.MARKET_NORDSTREAM,
                    },
                    'verdict': [
                        {
                            'results': [{
                                'messages': [{
                                    'code': '39B',
                                }],
                            }],
                        }
                    ]
                }],
            },
        } for business_id in range(1, 5)
    ] + [
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T600',
                'warehouse_id': i,
                'shop_id': 60,
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.BLUE,
            },
        } for i in range(0, 2)
    ] + [
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T600',
                'warehouse_id': i,
                'shop_id': 61,
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.BLUE,
            }
        } for i in range(0, 2)
    ] + [
        {
            'identifiers': {
                'business_id': 7,
                'offer_id': 'T700',
                'warehouse_id': WAREHOUSE_ID,
                'shop_id': 63,
                'feed_id': 1000,
            },
            'delivery': {
                'partner': {
                    'actual': {
                        'delivery_options': {
                            'options': [
                                {
                                    'Cost': 10,
                                    'DaysMin': 5,
                                    'OrderBeforeHour': 7,
                                }
                            ]
                        }
                    }
                }
            },
            'meta': {
                'rgb': DTC.BLUE,
            }
        }
    ]


@pytest.yield_fixture(scope='module')
def dumper(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        partners_table
):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
        'config': config,
    }
    with DeliveryDiffEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_dumper(yt_server, config, dumper, service_offers_table):
    yt_client = yt_server.get_yt_client()
    tables = yt_client.list(config.delivery_diff_output_dir)
    assert_that(len(tables), equal_to(2))
    assert_that(len(tables) > 0)

    yt_path = yt.ypath_join(config.delivery_diff_output_dir, 'recent')
    delivery_diff = list(yt_client.read_table(yt_path))

    logging.debug("!!! " + yt_path)
    logging.debug("!!! \n" + rows_as_table(delivery_diff,
                                           column_to_proto_type_dict={'offer': DTC.Offer},
                                           column_widths_dict={'offer': 50},
                                           column_alignment_dict={'offer': 'l'}))

    # Skips 2 offers with warehouse_id = 0
    assert_that(len(delivery_diff), equal_to(4))
    assert_that(delivery_diff, HasDatacampYtUnitedOffersRows(
        [
            {
                "business_id": business_id,
                "offer_id": offer_id,
                "shop_id":  shop_id,
                "warehouse_id":  warehouse_id,
                "offer": not_none(),
            } for business_id, shop_id, offer_id, warehouse_id in [
                (1, 10, "T100", 172),
                (2, 20, "T100", 172),
                (3, 30, "T100", 172),
                (4, 40, "T100", 172)
            ]
        ]
    ))
