# coding: utf-8

import pytest
import six
import time
from datetime import datetime
from hamcrest import assert_that, equal_to

from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.api.OffersBatch_pb2 import (
    OffersBatchRequest,
    RequestMethod
)
import market.idx.datacamp.proto.api.SyncGetOffer_pb2 as SyncGetOffer
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.test_envs.saas_env import SaasEnv
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

OFFER_IDS_FOR_TEST_CASE_SENSITIVITY = [
    "case_sens_offer_id",
    "CASE_SENS_OFFER_ID",
    "offer_with_slashes/",
    "offer_with_slashes//"
]

DATA = [
    # incomplete offer, business_id = 12345, group = 11111
    {
        'business_id': 12345,
        'offer_id': 'TestOffer01',
        'group_id': 11111,
        'offer': {
            'identifiers': {
                'business_id': 12345,
                'offer_id': 'TestOffer01',
                'shop_id': 123451
            },
            'meta': {
                'ts_created': current_time,
            },
            'content': {
                'partner': {
                    'original': {
                        'group_id': {
                            'value': 11111,
                            'meta': {
                                'timestamp': current_time,
                            },
                        },
                    },
                },
            },
            'status': {
                'united_catalog': {
                    'flag': True
                }
            }
        },
        'expected_offer': {
            'basic': {
                'identifiers': {
                    'business_id': 12345,
                    'offer_id': 'TestOffer01',
                },
                'meta': {
                    'ts_created': {
                        'seconds': current_ts.seconds,
                    },
                },
                'content': {
                    'partner': {
                        'original': {
                            'group_id': {
                                'value': 11111,
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                        'actual': {
                            'title': {
                                'value': 'TestOffer01Title',
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                            'price_from': {  # not mboc field
                                'flag': False,
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            }
                        },
                    },
                },
            },
            'service': IsProtobufMap({
                123451: {
                    'identifiers': {
                        'business_id': 12345,
                        'offer_id': 'TestOffer01',
                        'shop_id': 123451,
                    },
                },
            }),
        },
    },
    # incomplete offer, business_id = 23456, group = 22222
    {
        'business_id': 23456,
        'offer_id': 'TestOffer02',
        'shop_id': 123451,
        'group_id': 22222,
        'offer': {
            'identifiers': {
                'business_id': 23456,
                'offer_id': 'TestOffer02',
                'shop_id': 123451,
            },
            'meta': {
                'ts_created': current_time,
            },
            'content': {
                'partner': {
                    'original': {
                        'group_id': {
                            'value': 22222,
                            'meta': {
                                'timestamp': current_time,
                            },
                        },
                    },
                },
            },
            'status': {
                'united_catalog': {
                    'flag': True
                }
            }
        },
        'expected_offer': {
            'basic': {
                'identifiers': {
                    'business_id': 23456,
                    'offer_id': 'TestOffer02',
                },
                'meta': {
                    'ts_created': {
                        'seconds': current_ts.seconds,
                    },
                },
                'content': {
                    'partner': {
                        'original': {
                            'group_id': {
                                'value': 22222,
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                        'actual': {
                            'title': {
                                'value': 'TestOffer02Title',
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                    },
                },
            },
        }
    },
    # complete offer, business_id = 23456, group = 22222
    {
        'business_id': 23456,
        'offer_id': 'TestOffer03',
        'shop_id': 123451,
        'group_id': 22222,
        'offer': {
            'identifiers': {
                'business_id': 23456,
                'offer_id': 'TestOffer03',
                'shop_id': 123451,
            },
            'meta': {
                'ts_created': current_time,
            },
            'content': {
                'partner': {
                    'original': {
                        'group_id': {
                            'value': 22222,
                            'meta': {
                                'timestamp': current_time,
                            },
                        },
                    },
                },
            },
            'status': {
                'united_catalog': {
                    'flag': True
                }
            }
        },
        'expected_offer': {
            'basic': {
                'identifiers': {
                    'business_id': 23456,
                    'offer_id': 'TestOffer03',
                },
                'meta': {
                    'ts_created': {
                        'seconds': current_ts.seconds,
                    },
                },
                'content': {
                    'partner': {
                        'original': {
                            'group_id': {
                                'value': 22222,
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                        'actual': {
                            'title': {
                                'value': 'TestOffer03Title',
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                    },
                },
            },
        }
    },
    # complete offer, business_id = 23456, group = 33333
    {
        'business_id': 23456,
        'offer_id': 'TestOffer04',
        'shop_id': 123451,
        'group_id': 33333,
        'offer': {
            'identifiers': {
                'business_id': 23456,
                'offer_id': 'TestOffer04',
                'shop_id': 123451,
            },
            'meta': {
                'ts_created': current_time,
            },
            'content': {
                'partner': {
                    'original': {
                        'group_id': {
                            'value': 33333,
                            'meta': {
                                'timestamp': current_time,
                            },
                        },
                    },
                },
            },
            'status': {
                'united_catalog': {
                    'flag': True
                }
            }
        },
        'expected_offer': {
            'basic': {
                'identifiers': {
                    'business_id': 23456,
                    'offer_id': 'TestOffer04',
                },
                'meta': {
                    'ts_created': {
                        'seconds': current_ts.seconds,
                    },
                },
                'content': {
                    'partner': {
                        'original': {
                            'group_id': {
                                'value': 33333,
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                        'actual': {
                            'title': {
                                'value': 'TestOffer04Title',
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                    },
                }
            },
        }
    }
] + [
    # offers for test case sensitivity of offer_id, business_id = 23456, group = 22222 (should get them too)
    {
        'business_id': 23456,
        'offer_id': offer_id,
        'shop_id': 123451,
        'group_id': 22222,
        'offer': {
            'identifiers': {
                'business_id': 23456,
                'offer_id': offer_id,
                'shop_id': 123451,
            },
            'meta': {
                'ts_created': current_time,
            },
            'content': {
                'partner': {
                    'original': {
                        'group_id': {
                            'value': 22222,
                            'meta': {
                                'timestamp': current_time,
                            },
                        },
                    },
                },
            },
            'status': {
                'united_catalog': {
                    'flag': True
                }
            }
        },
        'expected_offer': {
            'basic': {
                'identifiers': {
                    'business_id': 23456,
                    'offer_id': offer_id,
                },
                'meta': {
                    'ts_created': {
                        'seconds': current_ts.seconds,
                    },
                },
                'content': {
                    'partner': {
                        'original': {
                            'group_id': {
                                'value': 22222,
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                        'actual': {
                            'title': {
                                'value': 'OfferTitle{}'.format(i),
                                'meta': {
                                    'timestamp': {
                                        'seconds': current_ts.seconds,
                                    },
                                },
                            },
                        },
                    },
                },
            },
        }
    } for i, offer_id in enumerate(OFFER_IDS_FOR_TEST_CASE_SENSITIVITY)
]


@pytest.fixture(scope='module')
def basic_offers():
    return [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=12345,
            offer_id='TestOffer01'
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                actual=DTC.ProcessedSpecification(
                    title=DTC.StringValue(
                        value='TestOffer01Title',
                        meta=create_update_meta(current_ts.seconds),
                    ),
                    price_from=DTC.Flag(
                        flag=False,
                        meta=create_update_meta(current_ts.seconds),
                    )
                ),
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(
                        value='TestOffer01Title',
                        meta=create_update_meta(current_ts.seconds),
                    ),
                ),
            )),
        meta=create_meta(current_ts.seconds),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=23456,
            offer_id='TestOffer02'
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                actual=DTC.ProcessedSpecification(
                    title=DTC.StringValue(
                        value='TestOffer02Title',
                        meta=create_update_meta(current_ts.seconds)
                    ),
                ),
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(
                        value='TestOffer01Title',
                        meta=create_update_meta(current_ts.seconds),
                    ),
                ),
            )),
        meta=create_meta(current_ts.seconds),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=23456,
            offer_id='TestOffer03'
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                actual=DTC.ProcessedSpecification(
                    title=DTC.StringValue(
                        value='TestOffer03Title',
                        meta=create_update_meta(current_ts.seconds)
                    ),
                ),
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(
                        value='TestOffer01Title',
                        meta=create_update_meta(current_ts.seconds),
                    ),
                ),
            )
        ),
        status=DTC.OfferStatus(
            consistency=DTC.ConsistencyStatus(
                mboc_consistency=True,
            ),
        ),
        meta=create_meta(current_ts.seconds),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=23456,
            offer_id='TestOffer04'
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                actual=DTC.ProcessedSpecification(
                    title=DTC.StringValue(
                        value='TestOffer04Title',
                        meta=create_update_meta(current_ts.seconds)
                    ),
                ),
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(
                        value='TestOffer01Title',
                        meta=create_update_meta(current_ts.seconds),
                    ),
                ),
            ),
        ),
        status=DTC.OfferStatus(
            consistency=DTC.ConsistencyStatus(
                mboc_consistency=True,
            ),
        ),
        meta=create_meta(current_ts.seconds),
    ))] + [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=23456,
            offer_id=offer_id
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                actual=DTC.ProcessedSpecification(
                    title=DTC.StringValue(
                        value='OfferTitle{}'.format(i),
                        meta=create_update_meta(current_ts.seconds)
                    ),
                ),
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(
                        value='OfferTitle{}'.format(i),
                        meta=create_update_meta(current_ts.seconds),
                    ),
                ),
            ),
        ),
        status=DTC.OfferStatus(
            consistency=DTC.ConsistencyStatus(
                mboc_consistency=True,
            ),
        ),
        meta=create_meta(current_ts.seconds),
    )) for i, offer_id in enumerate(OFFER_IDS_FOR_TEST_CASE_SENSITIVITY)
]


@pytest.fixture(scope='module')
def service_offers():
    return [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=12345,
            offer_id='TestOffer01',
            shop_id=123451,
            warehouse_id=0,
        ),
        status=DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(current_ts.seconds, source=DTC.PUSH_PARTNER_API),
                )
            ],
            united_catalog=DTC.Flag(flag=True)
        ),
        meta=create_meta(10),
    ))]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=12345,
            offer_id='TestOffer01',
            shop_id=123451,
            warehouse_id=0,
        ),
        status=DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(current_ts.seconds, source=DTC.MARKET_IDX),
                )
            ]
        ),
        meta=create_meta(10),
    ))]


@pytest.fixture(scope='module')
def saas():
    with SaasEnv(cluster_config='cluster_1be_internal.cfg', config_patch={'Server.Components': ['INDEX,DDK,MAKEUP']}) as saas:
        yield saas


def _set_initial_offers(client, saas):
    batch_request = OffersBatchRequest()

    for element in DATA:
        business_id = element['business_id']
        offer_id = element['offer_id']
        entry = batch_request.entries.add()
        entry.method = RequestMethod.POST
        entry.business_id = business_id
        entry.offer_id = offer_id
        entry.offer.CopyFrom(message_from_data(element['offer'], DTC.Offer()))

    response = client.do_request(
        method='post',
        path='/v1/offers/batch',
        data=batch_request.SerializeToString(),
    )
    assert_that(response, HasStatus(200))

    get_offer_response = client.get("/v1/partners/{}/offers/basic?offer_id={}".format(23456, 'TestOffer04'))
    assert_that(get_offer_response, HasStatus(200))

    batch = SyncGetOffer.GetOffersResponse()
    batch.ParseFromString(get_offer_response.data)
    offer = batch.offers[0]

    update = DTC.Offer()
    update.identifiers.CopyFrom(offer.identifiers)
    update.identifiers.shop_id = 123451
    update.content.market.CopyFrom(update.content.market)
    update.content.market.meta.timestamp.FromJsonString('2022-02-15T15:55:55Z')
    update.content.market.real_uc_version.counter = offer.status.version.uc_data_version.counter
    batch_request.Clear()
    entry = batch_request.entries.add()
    entry.business_id = update.identifiers.business_id
    entry.offer_id = update.identifiers.offer_id
    entry.shop_id = update.identifiers.shop_id
    entry.offer.CopyFrom(update)
    entry.method = RequestMethod.POST

    response = client.do_request(
        method='post',
        path='/v1/offers/batch',
        data=batch_request.SerializeToString(),
    )
    assert_that(response, HasStatus(200))

    time.sleep(5)  # индексация документа может подтупливать


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    subscription_service_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    saas
):
    cfg = DispatcherConfig()
    cfg.create_initializer(
        yt_server=yt_server,
        yt_token_path=yt_token.path
    )

    lb_reader = cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path,
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    filter = cfg.create_subscription_filter('SAAS_SUBSCRIBER', extra_params={
        'Mode': 'ORIGINAL',
        'UseActualServiceFields': True,
        'OneServicePerUnitedOffer': False,
        'FillOnlyAffectedOffers': False,
        'IgnoreBlueOffersWithoutContent': True,
        'EnableIntegralStatusTrigger': True,
        'Color': 'UNKNOWN_COLOR;WHITE;BLUE',
    })
    converter = cfg.create_united_saas_docs_converter(
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    sender = cfg.create_united_saas_sender(saas)

    cfg.create_link(lb_reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, filter)
    cfg.create_link(filter, converter)
    cfg.create_link(converter, sender)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
        dispatcher_config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        subscription_service_topic,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        saas
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            local_saas=saas,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table
    ) as stroller_env:
        _set_initial_offers(stroller_env, saas)
        yield stroller_env


def _get_group(stroller, business_id, group_id, group_is_ready=True, force=False, legacy=True, mboc_filter=False):
    force_mode = 'force=true' if force else ''
    legacy_mode = 'legacy=true' if legacy else ''
    filter = 'mode=mboc' if mboc_filter else ''
    response = stroller.get('/v1/partners/{}/groups/{}?{}&{}&{}'.format(business_id, group_id, force_mode, legacy_mode, filter))

    if group_is_ready or force:
        assert_that(response, HasStatus(200))
        assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    else:
        assert_that(response, HasStatus(404))

    return response


def _prepare_expected_offers(business_id, group_id, mboc_mode=False):
    def _needed_offer(element):
        return element['business_id'] == business_id and element['group_id'] == group_id
    expected_elements = [element for element in DATA if _needed_offer(element)]
    expected_resp = {
        'offers': [element['expected_offer'] for element in expected_elements],
    }

    # remove fields not from mboc subscription
    if mboc_mode:
        for offer in expected_resp['offers']:
            basic = offer['basic']
            actual = basic['content']['partner']['actual']
            if 'price_from' in actual:
                del actual['price_from']
            del basic['meta']

    return expected_resp


def test_get_group_default_mode(dispatcher, stroller):
    """Тест проверяет кейс получение оффера в дефолтном режиме. Случай, когда в группе 1 оффер"""
    business_id = 12345
    group_id = 11111
    expected_resp = _prepare_expected_offers(business_id=business_id, group_id=group_id)

    response = _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id
    )

    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, expected_resp))


def test_get_group_for_mboc_mode_with_force(dispatcher, stroller):
    """Тест проверяет кейс форс режима для запроса в MBOC-режиме. Случай, когда в группе 1 оффер, неконсистентный оффер"""
    business_id = 12345
    group_id = 11111
    expected_resp = _prepare_expected_offers(business_id=business_id, group_id=group_id, mboc_mode=True)

    response = _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id,
        force=True,
        mboc_filter=True
    )

    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, expected_resp))


def test_get_group_for_default_mode_multiple_offers(dispatcher, stroller):
    """Тест проверяет кейс получение оффера в дефолтном режиме. Случай, когда в группе офферов несколько"""
    business_id = 23456
    group_id = 22222
    expected_resp = _prepare_expected_offers(business_id=business_id, group_id=group_id)

    response = _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id
    )

    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, expected_resp))


def test_get_group_for_default_mode_for_empty_group(dispatcher, stroller):
    """Тест проверяет кейс получение оффера в дефолтном режиме. Случай, когда в группе вообще нет"""
    business_id = 23456
    group_id = 23456

    response = _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id,
        force=True
    )

    assert_that(six.ensure_str(response.data), equal_to(''))


def test_get_not_consistent_group(dispatcher, stroller):
    """Тест проверяет кейс запроса в MBOC-режиме для полностью неконсистентной группы"""
    business_id = 12345
    group_id = 11111

    _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id,
        group_is_ready=False,
        mboc_filter=True
    )


def test_get_group_for_mboc_mode_multiple_offers_some_not_consistent(dispatcher, stroller):
    """Тест проверяет кейс запроса в MBOC-режиме для частично неконсистентной группы"""
    business_id = 23456
    group_id = 22222

    _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id,
        group_is_ready=False,
        mboc_filter=True
    )


def test_get_group_for_mboc_mode_consistent_offers(dispatcher, stroller):
    """Тест проверяет кейс запроса в MBOC-режиме для консистентной группы"""
    business_id = 23456
    group_id = 33333
    expected_resp = _prepare_expected_offers(business_id=business_id, group_id=group_id, mboc_mode=True)

    response = _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id,
        mboc_filter=True
    )

    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, expected_resp))


def test_get_group_two_requests_internal_error(dispatcher, stroller):
    """Тест проверяет MARKETINDEXER-37030. Два запроса к несуществующей группе стабильно приводит к 500"""
    business_id = 23456
    group_id = 0

    # run twice
    response = _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id,
        mboc_filter=True
    )

    response = _get_group(
        stroller,
        business_id=business_id,
        group_id=group_id,
        mboc_filter=True
    )

    assert_that(six.ensure_str(response.data), equal_to(''))
