# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.utils import wait_until

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.pylibrary.proto_utils import message_from_data


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

time_points = {
    'past_time': past_time,
    'current_time': current_time,
    'future_time': future_time,
}

OFFERS = [
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestPartnerContentOriginalUpdate',
            'warehouse_id': 42,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestPartnerContentOriginalTermsUpdate',
            'warehouse_id': 42,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestPartnerContentActualUpdate',
            'warehouse_id': 42,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'business_id': 7,
            'offer_id': 'offer-with-invalid-string-in-original-terms',
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
            'scope': DTC.BASIC,
        },
        'content': {
            'partner': {
                'original': {
                    'name': {
                        'value': 'name\0',
                        'meta': {
                            'timestamp': NOW_UTC.strftime(time_pattern),
                        },
                    },
                    'description': {
                        'value': "description",
                        'meta': {
                            'timestamp': NOW_UTC.strftime(time_pattern),
                        },
                    },
                    'barcode': {
                        'value': [
                            'value',
                            '\0',
                        ],
                        'meta': {
                            'timestamp': NOW_UTC.strftime(time_pattern),
                        },
                    },
                    'country_of_origin': {
                        'value': [
                            '1',
                            '2',
                        ],
                        'meta': {
                            'timestamp': NOW_UTC.strftime(time_pattern),
                        },
                    },
                },
            },
        },
    },
]


def service_content_partner_original(time_point):
    return {
        'supplier_info': {
            'ogrn': time_point + '_ogrn',
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'pricelabs_params': {
            'params': {
                'param': time_point + '_value',
            },
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
    }


def service_content_partner_actual(time_point, value=None):
    return {
        'sales_notes': {
            'value': time_point + '_sales_notes',
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'quantity': {
            'min': value,
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'installment_options': {
            'options_groups': [
                {
                    'group_name': time_point + '_group',
                },
            ],
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
    }


def service_content_partner_original_terms(time_point, value=None):
    return {
        'sales_notes': {
            'value': time_point + '_sales_notes',
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'quantity': {
            'min': value,
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'supply_quantity': {
            'min': value,
            'step': value,
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'supply_plan': {
            'value': value,
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'transport_unit_size': {
            'value': value,
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'supply_weekdays': {
            'days': [
                value,
            ],
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
        'partner_delivery_time': {
            'value': value,
            'meta': {
                'timestamp': time_points[time_point],
            },
        },
    }


@pytest.fixture(scope='module')
def offers():
    return [message_from_data(offer, DTC.Offer()) for offer in OFFERS]


@pytest.fixture(
    scope='module',
    params=[
        # partner content original
        {
            'offer_id': 'TestPartnerContentOriginalUpdate',
            'parent_field_name': 'content',
            'field_name': 'partner',
            'update_null_data': {
                'original': service_content_partner_original('current_time'),
            },
            'update_by_old_data': {
                'original': service_content_partner_original('past_time'),
            },
            'update_by_new_data': {
                'original': service_content_partner_original('future_time'),
            },
            'expected_after_update_null_service': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentOriginalUpdate',
                },
                'content': {
                    'partner': {
                        'original': service_content_partner_original('current_time'),
                    }
                },
            },
            'expected_after_update_null_actual_service': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'warehouse_id': 42,
                    'offer_id': 'TestPartnerContentOriginalUpdate',
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentOriginalUpdate',
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
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentOriginalUpdate',
                },
                'content': {
                    'partner': {
                        'original': service_content_partner_original('future_time'),
                    }
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'warehouse_id': 42,
                    'offer_id': 'TestPartnerContentOriginalUpdate'
                },
            }
        },
        # partner content original terms
        {
            'offer_id': 'TestPartnerContentOriginalTermsUpdate',
            'parent_field_name': 'content',
            'field_name': 'partner',
            'update_null_data': {
                'original_terms': service_content_partner_original_terms('current_time', 2),
            },
            'update_by_old_data': {
                'original_terms': service_content_partner_original_terms('past_time', 1),
            },
            'update_by_new_data': {
                'original_terms': service_content_partner_original_terms('future_time', 3),
            },
            'expected_after_update_null_service': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentOriginalTermsUpdate',
                },
                'content': {
                    'partner': {
                        'original_terms': service_content_partner_original_terms('current_time', 2),
                    }
                },
            },
            'expected_after_update_null_actual_service': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'warehouse_id': 42,
                    'offer_id': 'TestPartnerContentOriginalTermsUpdate',
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentOriginalTermsUpdate',
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
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentOriginalTermsUpdate',
                },
                'content': {
                    'partner': {
                        'original_terms': service_content_partner_original_terms('future_time', 3),
                    }
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'warehouse_id': 42,
                    'offer_id': 'TestPartnerContentOriginalTermsUpdate'
                },
            },
        },
        # partner content actual
        {
            'offer_id': 'TestPartnerContentActualUpdate',
            'parent_field_name': 'content',
            'field_name': 'partner',
            'update_null_data': {
                'actual': service_content_partner_actual('current_time', 2),
            },
            'update_by_old_data': {
                'actual': service_content_partner_actual('past_time', 1),
            },
            'update_by_new_data': {
                'actual': service_content_partner_actual('future_time', 3),
            },
            'expected_after_update_null_service': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentActualUpdate',
                },
                'content': {
                    'partner': {
                        'actual': service_content_partner_actual('current_time', 2),
                    }
                },
            },
            'expected_after_update_null_actual_service': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'warehouse_id': 42,
                    'offer_id': 'TestPartnerContentActualUpdate',
                },
            },
            'expected_price_after_update_null': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentActualUpdate',
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
                    'shop_id': 1,
                    'business_id': 1,
                    'offer_id': 'TestPartnerContentActualUpdate',
                },
                'content': {
                    'partner': {
                        'actual': service_content_partner_actual('future_time', 3),
                    }
                },
            },
            'expected_after_update_by_new_actual': {
                'identifiers': {
                    'shop_id': 1,
                    'business_id': 1,
                    'warehouse_id': 42,
                    'offer_id': 'TestPartnerContentActualUpdate',
                },
            },
        },
    ],
    ids=[
        'test_partner_content_original_update',
        'test_partner_content_original_terms_update',
        'test_partner_content_actual_update',
    ]
)
def gen_service_data(request):
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
        },
        'features': {
            'enable_offer_format_validator': True,
            'enable_original_partner_content_validation': False,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


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


def create_update_part(business_id, offer_id, case, gen_service_data, warehouse_id=None, shop_id=None):
    identifiers = {
        'business_id': business_id,
        'offer_id': offer_id,
    }
    if warehouse_id:
        identifiers['warehouse_id'] = warehouse_id
    if shop_id:
        identifiers['shop_id'] = shop_id

    if gen_service_data['parent_field_name']:
        update_part = {
            'identifiers': identifiers,
            gen_service_data['parent_field_name']: {
                gen_service_data['field_name']: gen_service_data[case],
            },
        }
    else:
        update_part = {
            'identifiers': identifiers,
            gen_service_data['field_name']: gen_service_data[case]
        }

    return update_part


def test_strings_with_nulls_are_valid_when_original_partner_content_validation_is_off(inserter, piper):
    assert_that(piper.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 7,
                'offer_id': 'offer-with-invalid-string-in-original-terms',
            },
            'content': {
                'partner': {
                    'original': {
                        'name': {
                            'value': 'name\0',
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern),
                            },
                        },
                        'description': {
                            'value': "description",
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern),
                            },
                        },
                        'barcode': {
                            'value': [
                                'value',
                                '\0',
                            ],
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern),
                            },
                        },
                        'country_of_origin': {
                            'value': [
                                '1',
                                '2',
                            ],
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern),
                            },
                        },
                    },
                },
            },
        }, DTC.Offer())
    ]))


def test_offer_partly_update(inserter, offers, piper, lbk_topic, gen_service_data):
    offer_id = gen_service_data['offer_id']

    # Шаг 1 : запишем данные для офера, в котором отсутствует обновляемое поле
    update_part = create_update_part(1, offer_id, 'update_null_data', gen_service_data, 42, 1)

    offers_processed = piper.united_offers_processed
    lbk_topic.write(message_from_data(update_part, DTC.Offer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    # Шаг 2 : проверим, что изменение появилось в хранилище
    assert_that(piper.service_offers_table.data, HasOffers([
        message_from_data(gen_service_data['expected_after_update_null_service'], DTC.Offer())
    ]))
    assert_that(piper.actual_service_offers_table.data, HasOffers([
        message_from_data(gen_service_data['expected_after_update_null_actual_service'], DTC.Offer())
    ]))

    # Шаг 3 : проверим, что другие данные не изменились
    assert_that(piper.service_offers_table.data, HasOffers([
        message_from_data(gen_service_data['expected_price_after_update_null'], DTC.Offer())
    ]))

    # Шаг 4 : попробуем записать в хранилище старые данные
    if not (gen_service_data.get('parent_field_name') == 'content' and gen_service_data.get('field_name') == 'partner'):
        gen_service_data['update_by_old_data']['meta'] = {'timestamp': past_time}
    update_part = create_update_part(1, offer_id, 'update_by_old_data', gen_service_data, 42, 1)

    lbk_topic.write(message_from_data(update_part, DTC.Offer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 2)

    # Шаг 5 : проверим, что изменение НЕ появилось в хранилище
    assert_that(piper.service_offers_table.data, HasOffers([
        message_from_data(gen_service_data['expected_after_update_null_service'], DTC.Offer())
    ]))
    assert_that(piper.actual_service_offers_table.data, HasOffers([
        message_from_data(gen_service_data['expected_after_update_null_actual_service'], DTC.Offer())
    ]))

    # Шаг 6 : попробуем записать в хранилище новые данные
    if not (gen_service_data.get('parent_field_name') == 'content' and gen_service_data.get('field_name') == 'partner'):
        gen_service_data['update_by_new_data']['meta'] = {'timestamp': future_time}
    update_part = create_update_part(1, offer_id, 'update_by_new_data', gen_service_data, 42, 1)

    lbk_topic.write(message_from_data(update_part, DTC.Offer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 3)

    # Шаг 7 : проверим, что изменение появилось в хранилище
    assert_that(piper.service_offers_table.data, HasOffers([
        message_from_data(gen_service_data['expected_after_update_by_new_original'], DTC.Offer())
    ]))
    assert_that(piper.actual_service_offers_table.data, HasOffers([
        message_from_data(gen_service_data['expected_after_update_by_new_actual'], DTC.Offer())
    ]))
