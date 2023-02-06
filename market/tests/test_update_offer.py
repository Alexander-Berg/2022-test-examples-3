# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import HIDDEN, AVAILABLE, PUSH_PARTNER_FEED, BLUE
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
PAST_UTC = NOW_UTC - timedelta(minutes=45)
FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

past_time = PAST_UTC.strftime(time_pattern)
past_ts = create_timestamp_from_json(past_time)

future_time = FUTURE_UTC.strftime(time_pattern)
future_ts = create_timestamp_from_json(future_time)

OFFERS = [
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': '1',
            'warehouse_id': 42,
            'business_id': 1,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': '2',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                },
            ],
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'pic+flags',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                },
            ],
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestBindingUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        # для всех случаев, когда проверяется обновление поля - не price,
        # поле price служит индикатором того, что меняются лишь нужные поля
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestMarketContentUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestPartnerPicturesUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestPartnerActualPicturesUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestMarketPicturesUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestDeliverySpecificUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestDeliveryCalculatorUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            }
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestDeliveryInfoUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestPartnerInfoUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestStockInfoMarketStocksUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestStockInfoPartnerStocksUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestPriceUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestEnableAutoDiscountsUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': 'TestPartnerContentUpdate',
            'warehouse_id': 42,
            'business_id': 2,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': PUSH_PARTNER_FEED,
                },
            },
        },
    },
]

SERVICE_OFFERS_TABLE_DATA = [
    offer_to_service_row(message_from_data({
        'identifiers': {
            'shop_id': offer['identifiers']['shop_id'],
            'offer_id': offer['identifiers']['offer_id'],
            'business_id': offer['identifiers']['business_id'],
        },
        'meta': {
            'rgb': BLUE
        },
    }, DatacampOffer())) for offer in OFFERS
]

BASIC_OFFERS_TABLE_DATA = [
    offer_to_basic_row(message_from_data({
        'identifiers': {
            'offer_id': offer['identifiers']['offer_id'],
            'business_id': offer['identifiers']['business_id'],
        },
    }, DatacampOffer())) for offer in OFFERS
]

ACTUAL_SERVICE_OFFERS_TABLE_DATA = [
    offer_to_service_row(message_from_data({
        'identifiers': {
            'shop_id': offer['identifiers']['shop_id'],
            'offer_id': offer['identifiers']['offer_id'],
            'business_id': offer['identifiers']['business_id'],
            'warehouse_id': offer['identifiers']['warehouse_id'],
        },
        'meta': {
            'rgb': BLUE
        },
    }, DatacampOffer())) for offer in OFFERS
]


@pytest.fixture(scope='module')
def offers():
    return [message_from_data(offer, DatacampOffer()) for offer in OFFERS]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_offers_tablepath, data=BASIC_OFFERS_TABLE_DATA)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_offers_tablepath, data=ACTUAL_SERVICE_OFFERS_TABLE_DATA)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_offers_tablepath, data=SERVICE_OFFERS_TABLE_DATA)


@pytest.fixture(
    scope='module',
    params=[
        # binding
        {
            'offer_id': 'TestBindingUpdate',
            'parent_field_name': 'content',
            'field_name': 'binding',
            'update_null_data': {
                'partner': {
                    'meta': {
                        'timestamp': current_time,
                    },
                    'market_category_id': 12345,
                },
                'smb_partner': {
                    'meta': {
                        'timestamp': current_time,
                    },
                    'market_category_id': 12345,
                },
                'approved': {
                    'meta': {
                        'timestamp': current_time,
                    },
                    'market_category_id': 12345,
                },
                'uc_mapping': {
                    'meta': {
                        'timestamp': current_time,
                    },
                    'market_category_id': 12345,
                },
            },
            'update_by_old_data': {
                'partner': {
                    'meta': {
                        'timestamp': past_time,
                    },
                    'market_category_id': 67890,
                },
                'smb_partner': {
                    'meta': {
                        'timestamp': past_time,
                    },
                    'market_category_id': 67890,
                },
                'approved': {
                    'meta': {
                        'timestamp': past_time,
                    },
                    'market_category_id': 67890,
                },
                'uc_mapping': {
                    'meta': {
                        'timestamp': past_time,
                    },
                    'market_category_id': 67890,
                },
            },
            'update_by_new_data': {
                'partner': {
                    'meta': {
                        'timestamp': future_time,
                    },
                    'market_category_id': 54321,
                },
                'smb_partner': {
                    'meta': {
                        'timestamp': future_time,
                    },
                    'market_category_id': 54321,
                },
                'approved': {
                    'meta': {
                        'timestamp': future_time,
                    },
                    'market_category_id': 54321,
                },
                'uc_mapping': {
                    'meta': {
                        'timestamp': future_time,
                    },
                    'market_category_id': 54321,
                },
            },
            'expected_after_update_null_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestBindingUpdate',
                    'business_id': 2,
                },
                'content': {
                    'binding': {
                        'smb_partner': {
                            'meta': {
                                'timestamp': current_time,
                            },
                            'market_category_id': 12345,
                        },
                    }
                },
            },
            'expected_after_update_null_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestBindingUpdate',
                    'business_id': 2,
                    'warehouse_id': 42
                },
                'content': {
                    'binding': {
                        'partner': {
                            'meta': {
                                'timestamp': current_time,
                            },
                            'market_category_id': 12345,
                        },
                        'approved': {
                            'meta': {
                                'timestamp': current_time,
                            },
                            'market_category_id': 12345,
                        },
                        'uc_mapping': {
                            'meta': {
                                'timestamp': current_time,
                            },
                            'market_category_id': 12345,
                        },
                    }
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestBindingUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                }
            },
            'expected_after_update_by_new_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestBindingUpdate',
                    'business_id': 2,
                },
                'content': {
                    'binding': {
                        'smb_partner': {
                            'meta': {
                                'timestamp': future_time,
                            },
                            'market_category_id': 54321,
                        },
                    },
                }
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestBindingUpdate',
                    'business_id': 2,
                },
                'content': {
                    'binding': {
                        'partner': {
                            'meta': {
                                'timestamp': future_time,
                            },
                            'market_category_id': 54321,
                        },
                        'approved': {
                            'meta': {
                                'timestamp': future_time,
                            },
                            'market_category_id': 54321,
                        },
                        'uc_mapping': {
                            'meta': {
                                'timestamp': future_time,
                            },
                            'market_category_id': 54321,
                        },
                    },
                }
            },
        },
        # market content
        {
            'offer_id': 'TestMarketContentUpdate',
            'parent_field_name': 'content',
            'field_name': 'market',
            'update_null_data': {
                'dimensions': {
                    'weight': 1,
                },
            },
            'update_by_old_data': {
                'dimensions': {
                    'weight': 2,
                },
            },
            'update_by_new_data': {
                'dimensions': {
                    'weight': 3,
                },
            },
            'expected_after_update_null_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestMarketContentUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'content': {
                    'market': {
                        'dimensions': {
                            'weight': 1,
                        },
                        'meta': {
                            'timestamp': current_time,
                        },
                    }
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestMarketContentUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestMarketContentUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'content': {
                    'market': {
                        'dimensions': {
                            'weight': 3,
                        },
                        'meta': {
                            'timestamp': future_time,
                        },
                    }
                },
            },
        },
        # partner pictures
        {
            'offer_id': 'TestPartnerPicturesUpdate',
            'parent_field_name': 'pictures',
            'field_name': 'partner',
            'update_null_data': {
                'original': {
                    'source': [{'url': 'url01'}],
                    'meta': {
                        'timestamp': current_time,
                    },
                },
            },
            'update_by_old_data': {
                'original': {
                    'source': [{'url': 'url02'}],
                    'meta': {
                        'timestamp': past_time,
                    },
                },
            },
            'update_by_new_data': {
                'original': {
                    'source': [{'url': 'url03'}],
                    'meta': {
                        'timestamp': future_time,
                    },
                },
            },
            'expected_after_update_null_basic': {
                'identifiers': {
                    'offer_id': 'TestPartnerPicturesUpdate',
                    'business_id': 2,
                },
                'pictures': {
                    'partner': {
                        'original': {
                            'source': [
                                {
                                    'url': 'url01'
                                }
                            ],
                            'meta': {
                                'timestamp': current_time
                            }
                        }
                    }
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPartnerPicturesUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_basic': {
                'identifiers': {
                    'offer_id': 'TestPartnerPicturesUpdate',
                    'business_id': 2,
                },
                'pictures': {
                    'partner': {
                        'original': {
                            'source': [
                                {
                                    'url': 'url03'
                                }
                            ],
                            'meta': {
                                'timestamp': future_time
                            }
                        }
                    }
                },
            },
        },
        # partner actual pictures
        {
            'offer_id': 'TestPartnerActualPicturesUpdate',
            'parent_field_name': 'pictures',
            'field_name': 'partner',
            'update_null_data': {
                'original': {
                    'source': [
                        {'url': 'picurl'}
                    ],
                    'meta': {
                        'timestamp': current_time
                    }
                },
                'actual': {
                    'picurl': {
                        'id': 'url01',
                        'meta': {
                            'timestamp': current_time,
                        },
                    }
                },
            },
            'update_by_old_data': {
                'original': {
                    'source': [
                        {'url': 'picurl'}
                    ],
                    'meta': {
                        'timestamp': past_time
                    }
                },
                'actual': {
                    'picurl': {
                        'id': 'url02',
                        'meta': {
                            'timestamp': past_time,
                        },
                    }
                }
            },
            'update_by_new_data': {
                'original': {
                    'source': [
                        {'url': 'picurl'}
                    ],
                    'meta': {
                        'timestamp': future_time
                    }
                },
                'actual': {
                    'picurl': {
                        'id': 'url03',
                        'meta': {
                            'timestamp': future_time,
                        },
                    }
                }
            },
            'expected_after_update_null_basic': {
                'identifiers': {
                    'offer_id': 'TestPartnerActualPicturesUpdate',
                    'business_id': 2,
                },
                'pictures': {
                    'partner': {
                        'original': {
                            'source': [
                                {'url': 'picurl'}
                            ],
                            'meta': {
                                'timestamp': current_time
                            }
                        },
                        'actual': {
                            'picurl': {
                                'id': 'url01',
                                'meta': {
                                    'timestamp': current_time
                                }
                            }
                        }
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'offer_id': 'TestPartnerActualPicturesUpdate',
                    'shop_id': 2,
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_basic': {
                'identifiers': {
                    'offer_id': 'TestPartnerActualPicturesUpdate',
                    'business_id': 2,
                },
                'pictures': {
                    'partner': {
                        'original': {
                            'source': [
                                {'url': 'picurl'}
                            ],
                            'meta': {
                                'timestamp': future_time
                            }
                        },
                        'actual': {
                            'picurl': {
                                'id': 'url03',
                                'meta': {
                                    'timestamp': future_time
                                }
                            }
                        },
                    }
                },
            },
        },
        # market pictures
        {
            'offer_id': 'TestMarketPicturesUpdate',
            'parent_field_name': 'pictures',
            'field_name': 'market',
            'update_null_data': {
                'product_pictures': [
                    {'id': 'url01'}
                ]
            },
            'update_by_old_data': {
                'product_pictures': [
                    {'id': 'url02'}
                ]
            },
            'update_by_new_data': {
                'product_pictures': [
                    {'id': 'url03'}
                ]
            },
            'expected_after_update_null_basic': {
                'identifiers': {
                    'offer_id': 'TestMarketPicturesUpdate',
                    'business_id': 2,
                },
                'pictures': {
                    'market': {
                        'product_pictures': [
                            {'id': 'url01'}
                        ],
                        'meta': {
                            'timestamp': current_time,
                        },
                    }
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'offer_id': 'TestMarketPicturesUpdate',
                    'business_id': 2,
                    'shop_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_basic': {
                'identifiers': {
                    'offer_id': 'TestMarketPicturesUpdate',
                    'business_id': 2,
                },
                'pictures': {
                    'market': {
                        'product_pictures': [
                            {'id': 'url03'}
                        ],
                        'meta': {
                            'timestamp': future_time,
                        },
                    }
                },
            },
        },
        # delivery specific
        {
            'offer_id': 'TestDeliverySpecificUpdate',
            'parent_field_name': 'delivery',
            'field_name': 'specific',
            'update_null_data': {
                'delivery_currency': 'a',
            },
            'update_by_old_data': {
                'delivery_currency': 'b',
            },
            'update_by_new_data': {
                'delivery_currency': 'c',
            },
            'expected_after_update_null_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliverySpecificUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'delivery': {
                    'specific': {
                        'delivery_currency': 'a',
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliverySpecificUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliverySpecificUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'delivery': {
                    'specific': {
                        'delivery_currency': 'c',
                        'meta': {
                            'timestamp': future_time,
                        },
                    },
                },
            },
        },
        # delivery calculator
        {
            'offer_id': 'TestDeliveryCalculatorUpdate',
            'parent_field_name': 'delivery',
            'field_name': 'calculator',
            'update_null_data': {
                'delivery_calc_generation': 1,
            },
            'update_by_old_data': {
                'delivery_calc_generation': 2,
            },
            'update_by_new_data': {
                'delivery_calc_generation': 3,
            },
            'expected_after_update_null_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliveryCalculatorUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'delivery': {
                    'calculator': {
                        'delivery_calc_generation': 1,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliveryCalculatorUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliveryCalculatorUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'delivery': {
                    'calculator': {
                        'delivery_calc_generation': 3,
                        'meta': {
                            'timestamp': future_time,
                        },
                    },
                },
            },
        },
        # delivery info
        {
            'offer_id': 'TestDeliveryInfoUpdate',
            'parent_field_name': 'delivery',
            'field_name': 'delivery_info',
            'update_null_data': {
                'delivery_currency': 'RUR',
            },
            'update_by_old_data': {
                'delivery_currency': 'USD',
            },
            'update_by_new_data': {
                'delivery_currency': 'EUR',
            },
            'expected_after_update_null_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliveryInfoUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'delivery': {
                    'delivery_info': {
                        'delivery_currency': 'RUR',
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliveryInfoUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestDeliveryInfoUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'delivery': {
                    'delivery_info': {
                        'delivery_currency': 'EUR',
                        'meta': {
                            'timestamp': future_time,
                        },
                    },
                },
            },
        },
        # partner info
        {
            'offer_id': 'TestPartnerInfoUpdate',
            'parent_field_name': None,
            'field_name': 'partner_info',
            'update_null_data': {
                'shop_name': 'shop1',
            },
            'update_by_old_data': {
                'shop_name': 'shop2',
            },
            'update_by_new_data': {
                'shop_name': 'shop3',
            },
            'expected_after_update_null_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPartnerInfoUpdate',
                    'business_id': 2,
                    'warehouse_id': 42
                },
                'partner_info': {
                    'shop_name': 'shop1',
                    'meta': {
                        'timestamp': current_time,
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPartnerInfoUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPartnerInfoUpdate',
                    'business_id': 2,
                    'warehouse_id': 42
                },
                'partner_info': {
                    'shop_name': 'shop3',
                    'meta': {
                        'timestamp': future_time,
                    },
                },
            },
        },
        # stock info market stocks
        {
            'offer_id': 'TestStockInfoMarketStocksUpdate',
            'parent_field_name': 'stock_info',
            'field_name': 'market_stocks',
            'update_null_data': {
                'count': 1,
            },
            'update_by_old_data': {
                'count': 2,
            },
            'update_by_new_data': {
                'count': 3,
            },
            'expected_after_update_null_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestStockInfoMarketStocksUpdate',
                    'warehouse_id': 42,
                    'business_id': 2,
                },
                'stock_info': {
                    'market_stocks': {
                        'count': 1,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestStockInfoMarketStocksUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestStockInfoMarketStocksUpdate',
                    'business_id': 2,
                },
                'stock_info': {
                    'market_stocks': {
                        'count': 3,
                        'meta': {
                            'timestamp': future_time,
                        },
                    },
                },
            },
        },
        # stock info partner stocks
        {
            'offer_id': 'TestStockInfoPartnerStocksUpdate',
            'parent_field_name': 'stock_info',
            'field_name': 'partner_stocks_default',
            'update_null_data': {
                'count': 1,
            },
            'update_by_old_data': {
                'count': 2,
            },
            'update_by_new_data': {
                'count': 3,
            },
            'expected_after_update_null_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestStockInfoPartnerStocksUpdate',
                    'business_id': 2,
                },
                'stock_info': {
                    'partner_stocks_default': {
                        'count': 1,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestStockInfoPartnerStocksUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestStockInfoPartnerStocksUpdate',
                    'business_id': 2,
                },
                'stock_info': {
                    'partner_stocks_default': {
                        'count': 3,
                        'meta': {
                            'timestamp': future_time,
                        },
                    },
                },
            },
        },
        # price
        {
            'offer_id': 'TestPriceUpdate',
            'parent_field_name': 'price',
            'field_name': 'basic',
            'update_null_data': {
                'binary_price': {
                    'price': 10,
                },
            },
            'update_by_old_data': {
                'binary_price': {
                    'price': 20,
                },
            },
            'update_by_new_data': {
                'binary_price': {
                    'price': 30,
                },
            },
            'expected_after_update_null_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPriceUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'binary_price': {
                            'price': 10,
                        },
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPriceUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'binary_price': {
                            'price': 10,
                        },
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPriceUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'binary_price': {
                            'price': 30,
                        },
                        'meta': {
                            'timestamp': future_time,
                        },
                    },
                },
            },
        },
        # enable auto discounts
        {
            'offer_id': 'TestEnableAutoDiscountsUpdate',
            'parent_field_name': 'price',
            'field_name': 'enable_auto_discounts',
            'update_null_data': {
                'flag': True,
            },
            'update_by_old_data': {
                'flag': False,
            },
            'update_by_new_data': {
                'flag': True,
            },
            'expected_after_update_null_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestEnableAutoDiscountsUpdate',
                    'business_id': 2,
                },
                'price': {
                    'enable_auto_discounts': {
                        'flag': True,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestEnableAutoDiscountsUpdate',
                    'business_id': 2,
                },
                'price': {
                    'enable_auto_discounts': {
                        'flag': True,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestEnableAutoDiscountsUpdate',
                    'business_id': 2,
                },
                'price': {
                    'enable_auto_discounts': {
                        'flag': True,
                        'meta': {
                            'timestamp': future_time,
                        },
                    },
                },
            },
        },
        # partner content
        {
            'offer_id': 'TestPartnerContentUpdate',
            'parent_field_name': 'content',
            'field_name': 'partner',
            'update_null_data': {
                'partner_content_desc': {
                    'title': 'title1',
                    'meta': {
                        'timestamp': current_time,
                    },
                },
            },
            'update_by_old_data': {
                'partner_content_desc': {
                    'title': 'title2',
                    'meta': {
                        'timestamp': past_time,
                    },
                },
            },
            'update_by_new_data': {
                'partner_content_desc': {
                    'title': 'title3',
                    'meta': {
                        'timestamp': future_time,
                    },
                },
            },
            'expected_after_update_null_original': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPartnerContentUpdate',
                    'business_id': 2,
                },
                'content': {
                    'partner': {
                        'partner_content_desc': {
                            'title': 'title1',
                            'meta': {
                                'timestamp': current_time,
                            },
                        },
                    }
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 2,
                    'offer_id': 'TestPartnerContentUpdate',
                    'business_id': 2,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time,
                        },
                    },
                },
            },
            'expected_after_update_by_new_original': {
                'content': {
                    'partner': {
                        'partner_content_desc': {
                            'title': 'title3',
                            'meta': {
                                'timestamp': future_time,
                            },
                        },
                    }
                },
            },
        },
    ],
    ids=[
        'test_binding_update',
        'test_market_content_update',
        'test_partner_pictures_update',
        'test_partner_actual_pictures_update',
        'test_market_pictures_update',
        'test_delivery_specific_update',
        'test_delivery_calculator_update',
        'test_delivery_info_update',
        'test_partner_info_update',
        'test_stock_info_market_stocks_update',
        'test_stock_info_partner_stocks_update',
        'test_price_update',
        'test_enable_auto_discounts_update',
        'test_partner_content_update',
    ]
)
def gen_data(request):
    return request.param


@pytest.fixture(scope='module')
def lbk_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, lbk_topic):
    cfg = {
        'logbroker': {
            'offers_topic': lbk_topic.topic,
        },
        'general': {
            'color': 'blue',
        }
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, lbk_topic):
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(offers, piper, lbk_topic):
    for offer in offers:
        lbk_topic.write(offer.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(offers))


def test_offer_publish_status_updated(inserter, offers, piper):
    """Проверяет, что если в топике PQ был флаг disabled, то после обновления publish status обновиться"""
    def _get_offer_disables(offer):
        return offer['status']['disabled'] if 'status' in offer and 'disabled' in offer['status'] else []

    def _is_offer_disabled(offer):
        disables = _get_offer_disables(offer)
        for disable in disables:
            if disable['flag']:
                return True
        return False

    for offer in OFFERS:
        if 'status' in offer and len(offer['status']) > 0:
            assert_that(piper.actual_service_offers_table.data,
                        HasOffers([message_from_data({
                            'identifiers': {
                                'shop_id': offer['identifiers']['shop_id'],
                                'offer_id': offer['identifiers']['offer_id'],
                                'business_id': offer['identifiers']['business_id'],
                            },
                            'status': {
                                'publish': HIDDEN if _is_offer_disabled(offer) else AVAILABLE,
                                'disabled': _get_offer_disables(offer)
                            },
                        }, DatacampOffer())]),
                        'Publish status is incorrect')


def test_shop_sku_for_blue_offers_is_set_up(inserter, offers, piper):
    """Проверяем, что shop sku для синих офферов проставляется"""
    wait_until(lambda: piper.united_offers_processed >= len(offers))
    for offer in OFFERS:
        assert_that(
            piper.basic_offers_table.data,
            HasOffers([message_from_data({
                'identifiers': {
                    'business_id': offer['identifiers']['business_id'],
                    'offer_id': offer['identifiers']['offer_id']
                }
            }, DatacampOffer())])
        )
        assert_that(
            piper.service_offers_table.data,
            HasOffers([message_from_data({
                'identifiers': {
                    'business_id': offer['identifiers']['business_id'],
                    'offer_id': offer['identifiers']['offer_id'],
                    'shop_id': offer['identifiers']['shop_id'],
                    'extra': {
                        'shop_sku': offer['identifiers']['offer_id']
                    }
                }
            }, DatacampOffer())])
        )


def create_update_part(business_id, shop_id, offer_id, warehouse_id, case, gen_data):
    identifiers = {
        'business_id': business_id,
        'shop_id': shop_id,
        'offer_id': offer_id,
        'warehouse_id': warehouse_id,
    }

    if gen_data['parent_field_name']:
        update_part = {
            'identifiers': identifiers,
            gen_data['parent_field_name']: {
                gen_data['field_name']: gen_data[case],
            },
        }
    else:
        update_part = {
            'identifiers': identifiers,
            gen_data['field_name']: gen_data[case]
        }

    return update_part


def test_offer_partly_update(inserter, offers, piper, lbk_topic, gen_data):
    def should_set_update_meta():
        if gen_data.get('parent_field_name') == 'content' and gen_data.get('field_name') == 'partner':
            return False
        if gen_data.get('parent_field_name') == 'pictures' and gen_data.get('field_name') == 'partner':
            return False
        return True

    offer_id = gen_data['offer_id']

    # Шаг 1 : запишем данные для офера, в котором отсутствует обновляемое поле
    if should_set_update_meta():
        gen_data['update_null_data']['meta'] = {'timestamp': current_time}
    update_part = create_update_part(2, 2, offer_id, 42, 'update_null_data', gen_data)

    offers_processed = piper.united_offers_processed
    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    # Шаг 2 : проверим, что изменение появилось в хранилище
    if 'expected_after_update_null_basic' in gen_data:
        assert_that(
            piper.basic_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_null_basic'], DatacampOffer())]))

    if 'expected_after_update_null_original' in gen_data:
        assert_that(
            piper.service_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_null_original'], DatacampOffer())]))

    if 'expected_after_update_null_actual' in gen_data:
        assert_that(
            piper.actual_service_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_null_actual'], DatacampOffer())]))

    # Шаг 3 : проверим, что другие данные не изменились
    assert_that(
        piper.service_offers_table.data,
        HasOffers([message_from_data(gen_data['expected_price_after_update_null'], DatacampOffer())]))

    # Шаг 4 : попробуем записать в хранилище старые данные
    if should_set_update_meta():
        gen_data['update_by_old_data']['meta'] = {'timestamp': past_time}
    update_part = create_update_part(2, 2, offer_id, 42, 'update_by_old_data', gen_data)

    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 2)

    # Шаг 5 : проверим, что изменение НЕ появилось в хранилище
    if 'expected_after_update_null_basic' in gen_data:
        assert_that(
            piper.basic_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_null_basic'], DatacampOffer())]))

    if 'expected_after_update_null_original' in gen_data:
        assert_that(
            piper.service_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_null_original'], DatacampOffer())]))

    if 'expected_after_update_null_actual' in gen_data:
        assert_that(
            piper.actual_service_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_null_actual'], DatacampOffer())]))

    # Шаг 6 : попробуем записать в хранилище новые данные
    if should_set_update_meta():
        gen_data['update_by_new_data']['meta'] = {'timestamp': future_time}
    update_part = create_update_part(2, 2, offer_id, 42, 'update_by_new_data', gen_data)

    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 3)

    # Шаг 7 : проверим, что изменение появилось в хранилище
    if 'expected_after_update_by_new_basic' in gen_data:
        assert_that(
            piper.basic_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_by_new_basic'], DatacampOffer())]))

    if 'expected_after_update_by_new_original' in gen_data:
        assert_that(
            piper.service_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_by_new_original'], DatacampOffer())]))

    if 'expected_after_update_by_new_actual' in gen_data:
        assert_that(
            piper.actual_service_offers_table.data,
            HasOffers([message_from_data(gen_data['expected_after_update_by_new_actual'], DatacampOffer())]))
