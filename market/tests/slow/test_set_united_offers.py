# coding: utf-8

import time
import pytest
from hamcrest import assert_that, equal_to, is_not, has_items, empty

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC, UnitedOffer_pb2
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row

from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.datacamp.proto.api import OffersBatch_pb2 as OffersBatch
from market.idx.datacamp.proto.business.Business_pb2 import BusinessStatus
from market.idx.datacamp.yatf.utils import dict2tskv, create_meta, create_update_meta
from market.pylibrary.proto_utils import message_from_data

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers, HasDatacampYtUnitedOffersRows
from market.idx.datacamp.controllers.stroller.yatf.utils import expected_offer, _build_offer, request

BLOCKED_BUSINESS = 1000
TIMESTAMP = '2021-08-01T15:55:55Z'


@pytest.fixture()
def business_status():
    return [
        {
            'business_id': BLOCKED_BUSINESS,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
            ).SerializeToString(),
        },
    ]


@pytest.fixture()
def partners():
    return [
        {
            'shop_id': 2,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 2,
                    'business_id': 1,
                    'is_site_market': 'true'
                }),
            ]),
        },
        {
            'shop_id': 3,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 3,
                    'business_id': 1,
                    'blue_status': 'REAL',
                    'united_catalog_status': 'SUCCESS',
                    'is_site_market': 'true'
                }),
            ]),
        },
        {
            'shop_id': 1111,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 1111,
                    'business_id': 2,
                    'united_catalog_status': 'SUCCESS',
                    'is_site_market': 'true'
                }),
            ]),
        },
        {
            'shop_id': 2222,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 2222,
                    'business_id': 2,
                    'united_catalog_status': 'SUCCESS',
                    'is_site_market': 'true'
                }),
            ]),
        },
        {
            'shop_id': 3333,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 3333,
                    'business_id': 2,
                    'united_catalog_status': 'SUCCESS',
                    'is_site_market': 'true'
                }),
            ]),
        },
    ]


@pytest.fixture()
def basic_offers(color_name):
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_to_remove',
            ),
            meta=create_meta(10, scope=DTC.BASIC),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_for_migrator_verdict',
            ),
            meta=create_meta(10, scope=DTC.BASIC),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_resolution_update',
            ),
            meta=create_meta(10, scope=DTC.BASIC),
            resolution=DTC.Resolution(
                by_source=[
                    DTC.Verdicts(
                        meta=create_update_meta(10, source=DTC.MARKET_MBI_MIGRATOR),
                        verdict=[
                            DTC.Verdict(
                                results=[
                                    DTC.ValidationResult(
                                        is_banned=False
                                    )
                                ]
                            )
                        ]
                    )
                ]
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_blue_migration',
            ),
            meta=create_meta(10, scope=DTC.BASIC),
        )),
    ]


@pytest.fixture()
def service_offers(color_name):
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_to_remove',
                shop_id=3,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_for_migrator_verdict',
                shop_id=3,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_blue_migration',
                shop_id=3,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
    ]


@pytest.fixture()
def actual_service_offers(color_name):
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_to_remove',
                shop_id=3,
                warehouse_id=145,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_for_migrator_verdict',
                shop_id=3,
                warehouse_id=145,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_blue_migration',
                shop_id=3,
                warehouse_id=145,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
    ]


@pytest.yield_fixture()
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        business_status_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            partners_table=partners_table,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table,
            business_status_table=business_status_table,
    ) as stroller_env:
        yield stroller_env


def test_set_united_offer(yt_server, stroller, config):
    warehouse_id = None

    basic_offer = expected_offer(
        business_id=1,
        offer_id='o1',
        shop_id=2,
        warehouse_id=warehouse_id,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.BASIC,
    )
    response = request(stroller, "/v1/partners/{business_id}/offers/basic?offer_id={offer_id}", basic_offer, TIMESTAMP)
    assert_that(response, HasStatus(200))

    service_offer = expected_offer(
        business_id=1,
        offer_id='o1',
        shop_id=2,
        warehouse_id=warehouse_id,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.SERVICE,
        status=DTC.HIDDEN
    )
    response = request(stroller, "/v1/partners/{business_id}/offers/services/{service_id}?offer_id={offer_id}", service_offer, TIMESTAMP)
    assert_that(response, HasStatus(200))

    basic_offers_table = DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)
    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(5))
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'extra': {
                    'recent_business_id': 1,
                }
            }
        }, DTC.Offer())
    ]))

    meta = service_offer['meta']
    meta['scope'] = DTC.SERVICE
    meta['ts_created'] = TIMESTAMP
    service_offers_table = DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)
    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(4))
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 2,
                'extra': {
                    'recent_business_id': 1,
                }
            },
            'meta': meta,
        }, DTC.Offer())
    ]))


def test_set_united_offer_some_service_offer_already_exists(yt_server, stroller, config):
    """Тест проверяет, что при изменении разных сервисных частей возвращается именно та, которую меняли"""
    def _set_price(shop_id):
        business_id = 2
        offer_id = '22222'
        warehouse_id = None
        service_offer = expected_offer(
            business_id=business_id,
            offer_id=offer_id,
            shop_id=shop_id,
            warehouse_id=warehouse_id,
            source=DTC.PUSH_PARTNER_OFFICE,
            ts=TIMESTAMP,
            price=700,
            scope=DTC.SERVICE,
            status=DTC.HIDDEN
        )
        expected_resp = {
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
                'shop_id': shop_id,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 700,
                    },
                }
            }
        }
        response = request(stroller, "/v1/partners/{business_id}/offers/services/{service_id}?offer_id={offer_id}", service_offer, TIMESTAMP)

        assert_that(response, HasStatus(200))
        assert_that(response.data, IsSerializedProtobuf(DTC.Offer, expected_resp))
        assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))

    _set_price(shop_id=1111)
    _set_price(shop_id=3333)
    _set_price(shop_id=2222)


def test_batch_request(yt_server, stroller, config):

    basic_offer_1 = expected_offer(
        business_id=1,
        offer_id='o1',
        shop_id=2,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.BASIC,
    )
    service_offer_2_145 = expected_offer(
        business_id=1,
        offer_id='o2',
        shop_id=2,
        warehouse_id=145,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.SERVICE,
        status=DTC.HIDDEN
    )
    service_offer_2_147 = expected_offer(
        business_id=1,
        offer_id='o2',
        shop_id=2,
        warehouse_id=147,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.SERVICE,
        status=DTC.HIDDEN
    )
    basic_offer_3 = expected_offer(
        business_id=1,
        offer_id='o3',
        shop_id=2,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.BASIC,
    )
    service_offer_3 = expected_offer(
        business_id=1,
        offer_id='o3',
        shop_id=2,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.SERVICE,
        status=DTC.HIDDEN
    )

    body = OffersBatch.UnitedOffersBatchRequest(
        entries=[
            OffersBatch.UnitedOffersBatchRequest.Entry(
                method=OffersBatch.RequestMethod.POST,
                offer=UnitedOffer_pb2.UnitedOffer(
                    basic=_build_offer(basic_offer_1, TIMESTAMP),
                )
            ),
            OffersBatch.UnitedOffersBatchRequest.Entry(
                method=OffersBatch.RequestMethod.POST,
                offer=UnitedOffer_pb2.UnitedOffer(
                    actual={
                        # Будет создана только актуальная часть
                        2: UnitedOffer_pb2.ActualOffers(
                            warehouse={
                                145: _build_offer(service_offer_2_145, TIMESTAMP),
                                147: _build_offer(service_offer_2_147, TIMESTAMP),
                            }
                        )
                    }
                )
            ),
            OffersBatch.UnitedOffersBatchRequest.Entry(
                method=OffersBatch.RequestMethod.POST,
                offer=UnitedOffer_pb2.UnitedOffer(
                    basic=_build_offer(basic_offer_3, TIMESTAMP),
                    service={
                        2: _build_offer(service_offer_3, TIMESTAMP),
                    }
                )
            )
        ]
    )

    expected_resp = {
        'entries': [
            {
                'united_offer': {
                    'basic': {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                        }
                    }
                }
            },
            {
                'united_offer': {
                    # Сервисная часть не была создана
                    'service': empty(),
                    'actual': IsProtobufMap({
                        2: {
                            'warehouse': IsProtobufMap({
                                145: {
                                    'identifiers': {
                                        'business_id': 1,
                                        'offer_id': 'o2',
                                        'shop_id': 2,
                                        'warehouse_id': 145,
                                    }
                                },
                                147: {
                                    'identifiers': {
                                        'business_id': 1,
                                        'offer_id': 'o2',
                                        'shop_id': 2,
                                        'warehouse_id': 147,
                                    }
                                },
                            })
                        }
                    })
                }
            },
            {
                'united_offer': {
                    'basic': {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o3',
                        }
                    },
                    'service': IsProtobufMap({
                        2: {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'o3',
                                'shop_id': 2
                            }
                        }
                    })
                }
            }
        ]
    }

    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, expected_resp))

    basic_offers_table = DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)
    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(7))
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
            },
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o3',
            },
        }, DTC.Offer())
    ]))

    service_offers_table = DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)
    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(4))  # Для o2 создалась только актуальная часть
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o3',
                'shop_id': 2,
            },
        }, DTC.Offer())
    ]))

    # проверяем, что корректно работает добавление новой услуги
    # для бизнеса, который уже представлен в других услугах
    new_shop_id = 3
    service_offer_4 = expected_offer(
        business_id=1,
        offer_id='o3',
        shop_id=new_shop_id,
        warehouse_id=145,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.SERVICE,
        status=DTC.HIDDEN
    )

    body = OffersBatch.UnitedOffersBatchRequest(
        entries=[
            OffersBatch.UnitedOffersBatchRequest.Entry(
                method=OffersBatch.RequestMethod.POST,
                offer=UnitedOffer_pb2.UnitedOffer(
                    service={
                        # Установка сервсиной части приведет к созданию актуальной
                        new_shop_id: _build_offer(service_offer_4, TIMESTAMP),
                    }
                )
            ),
        ]
    )

    expected_resp = {
        'entries': [
            {
                'united_offer': {
                    'service': IsProtobufMap({
                        3: {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'o3',
                                'shop_id': 3,
                                # Для обратной совместимости подмерживается одна из актуальных частей
                                'warehouse_id': 145,
                            },
                            'meta': {
                                # цвет устанавливается из shopsdat
                                'rgb': DTC.BLUE,
                            },
                            'status': {
                                # united_catalog True проставляется из шопсдат
                                'united_catalog': {
                                    'flag': True,
                                },
                            },
                        }
                    }),
                    'actual': IsProtobufMap({
                        3: {
                            'warehouse': IsProtobufMap({
                                145: {
                                    'identifiers': {
                                        'business_id': 1,
                                        'offer_id': 'o3',
                                        'shop_id': 3,
                                        'warehouse_id': 145,
                                    },
                                }
                            })
                        }
                    })
                }
            }
        ]
    }
    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, expected_resp))


def test_blocked_business_update(yt_server, stroller, config, basic_offers_table):
    """ Проверяем, что обновление/создание офферов заблокированного бизнеса не происходит """
    blocked_business = expected_offer(
        business_id=BLOCKED_BUSINESS,
        offer_id='blocked_business',
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.BASIC,
    )

    body = OffersBatch.UnitedOffersBatchRequest(
        entries=[
            OffersBatch.UnitedOffersBatchRequest.Entry(
                method=OffersBatch.RequestMethod.POST,
                offer=UnitedOffer_pb2.UnitedOffer(
                    basic=_build_offer(blocked_business, TIMESTAMP),
                )
            ),
        ]
    )

    expected_resp = {
        'entries': is_not(has_items({
            'united_offer': {
            }
        },)),
    }

    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, expected_resp))

    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(4))
    assert_that(basic_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BLOCKED_BUSINESS,
                'offer_id': 'blocked_business',
            }
        }, DTC.Offer()),
    ])))


def test_set_content_binding(yt_server, stroller, config):
    warehouse_id = None

    basic_offer = expected_offer(
        business_id=1,
        offer_id='o1',
        shop_id=2,
        warehouse_id=warehouse_id,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=TIMESTAMP,
        price=100,
        scope=DTC.BASIC,
    )
    basic_offer['content'] = {
        'binding': {
            'partner': {
                'meta': {
                    'source': DTC.PUSH_PARTNER_OFFICE,
                },
                'market_category_id': 15449848,
            },
            'smb_partner': {
                'meta': {
                    'source': DTC.PUSH_PARTNER_OFFICE,
                },
                'market_category_id': 15449848,
            }
        }
    }

    response = request(stroller, "/v1/partners/{business_id}/offers/basic?offer_id={offer_id}", basic_offer, TIMESTAMP)
    assert_that(response, HasStatus(200))

    basic_offers_table = DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
            },
            'content': {
                'binding': {
                    'partner': {
                        'market_category_id': 15449848,
                    }
                }
            },
        }, DTC.Offer())
    ]))


def test_remove_united_offer(stroller):
    """ Выставление признака удаления для частей оффера """
    warehouse_id = 145
    business_id = 1
    shop_id = 3
    offer_id = 'offer_to_remove'

    assert_that(stroller.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_sku': offer_id,
        'state_flags': None,
    }]))
    assert_that(stroller.service_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'state_flags': None,
    }]))
    assert_that(stroller.actual_service_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'warehouse_id': warehouse_id,
        'state_flags': None,
    }]))

    body = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'offer': {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                    'status': {
                        'removed': {
                            'flag': True,
                            'meta': {
                                'timestamp': TIMESTAMP,
                            }
                        }
                    },
                },
                'service': {
                    shop_id: {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                        'status': {
                            'removed': {
                                'flag': True,
                                'meta': {
                                    'timestamp': TIMESTAMP,
                                }
                            }
                        },
                    }
                },
                'actual': {
                    shop_id: {
                        'warehouse': {
                            warehouse_id: {
                                'identifiers': {
                                    'business_id': business_id,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                    'warehouse_id': warehouse_id,
                                },
                                'status': {
                                    'removed': {
                                        'flag': True,
                                        'meta': {
                                            'timestamp': TIMESTAMP,
                                        }
                                    }
                                },
                            }
                        },
                    }
                },
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())
    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, {
        'entries': [{
            'united_offer': {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                    'status': {
                        'removed': {
                            'flag': True,
                        }
                    },
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                        'status': {
                            'removed': {
                                'flag': True,
                            }
                        },
                    }
                }),
                'actual': IsProtobufMap({
                    shop_id: {
                        'warehouse': IsProtobufMap({
                            warehouse_id: {
                                'identifiers': {
                                    'business_id': business_id,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                    'warehouse_id': warehouse_id,
                                },
                                'status': {
                                    'removed': {
                                        'flag': True,
                                    }
                                },
                            }
                        })
                    }
                }),
            }
        }]
    }))
    assert_that(stroller.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_sku': offer_id,
        'state_flags': 1,
    }]))
    assert_that(stroller.service_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'state_flags': 1,
    }]))
    assert_that(stroller.actual_service_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'warehouse_id': warehouse_id,
        'state_flags': 1,
    }]))


def test_migrator_verdict(stroller):
    """ Выставление вердикта на базовую часть оффера """
    business_id = 1
    offer_id = 'offer_for_migrator_verdict'

    basic = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
        },
        'resolution': {
            'by_source': [{
                'meta': {
                    'source': DTC.MARKET_MBI_MIGRATOR,
                },
                'verdict': [{
                    'results': [{
                        'is_banned': True,
                    }]
                }]
            }]
        },
    }

    body = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'offer': {
                'basic': basic,
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())

    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, {
        'entries': [{
            'united_offer': {
                'basic': basic
            }
        }]
    }))
    assert_that(stroller.basic_offers_table.data, HasOffers([
        message_from_data(basic, DTC.Offer())
    ]))


def test_basic_resolution_update(stroller):
    """ Обновление вердикта базовой части оффера. Работает, даже если не указывать scope для оффера. """
    business_id = 1
    offer_id = 'offer_resolution_update'

    basic = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
        },
        'resolution': {
            'by_source': [{
                'meta': {
                    'source': DTC.MARKET_MBI_MIGRATOR,
                },
                'verdict': [{
                    'results': [{
                        'is_banned': True,
                    }]
                }]
            }]
        },
    }

    body = message_from_data(basic, DTC.Offer())
    body.resolution.by_source[0].meta.timestamp.seconds = int(time.time())

    response = stroller.post(
        '/v1/partners/{business_id}/offers/basic?offer_id={offer_id}'.format(
            business_id=business_id, offer_id=offer_id
        ),
        data=body.SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(DTC.Offer, basic))
    assert_that(stroller.basic_offers_table.data, HasOffers([
        message_from_data(basic, DTC.Offer())
    ]))


def test_blue_offer_migration(stroller):
    """ Проверка миграции синего оффера. Мигратор приносит синие офферы со статусом united_catalog=True и
        scope=SELECTIVE, указываея контентные поля в basic и service. Хранилище должно правильно разложить поля.
    """
    business_id = 1
    shop_id = 3
    offer_id = 'offer_blue_migration'
    content = {
        'binding': {
            'approved': {
                'market_category_id': 16044466,
                'market_sku_id': 100451512760,
                'meta': {
                    'timestamp': TIMESTAMP
                }
            },
            'partner': {
                'market_category_id': 16044466,
                'market_sku_id': 100451512760,
                'meta': {
                    'timestamp': TIMESTAMP
                }
            }
        },
        'partner': {
            'original_terms': {
                'supply_plan': {
                    'value': DTC.SupplyPlan.WILL_SUPPLY,
                    'meta': {
                        'timestamp': TIMESTAMP
                    }
                },
                'seller_warranty': {
                    'warranty_period': {
                        'years': 1,
                    },
                    'meta': {
                        'timestamp': TIMESTAMP
                    }
                },
            }
        }
    }

    basic = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
        },
        'content': content,
        'meta': {
            'scope': DTC.SELECTIVE,
        },
        'status': {
            'united_catalog': {
                'flag': True,
            },
        },
    }
    service = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
            'shop_id': shop_id,
        },
        'content': content,
        'meta': {
            'scope': DTC.SELECTIVE,
        },
        'status': {
            'united_catalog': {
                'flag': True,
            },
        },
    }

    body = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'offer': {
                'basic': basic,
                'service': {
                    shop_id: service
                }
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())

    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, {
        'entries': [{
            'united_offer': {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                    },
                }),
            }
        }]
    }))
    assert_that(stroller.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
            },
            'content': {
                'binding': {
                    'approved': {
                        'market_category_id': 16044466,
                        'market_sku_id': 100451512760,
                    },
                    'partner': {
                        'market_category_id': 16044466,
                        'market_sku_id': 100451512760,
                    }
                },
                'partner': {
                    'original_terms': {
                        'supply_plan': None,
                        'seller_warranty': {
                            'warranty_period': {
                                'years': 1,
                            },
                            'meta': {
                                'timestamp': TIMESTAMP
                            }
                        },
                    }
                }
            },
        }, DTC.Offer())
    ]))
    assert_that(stroller.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
                'shop_id': shop_id,
            },
            'content': {
                'binding': None,
                'partner': {
                    'original_terms': {
                        'supply_plan': {
                            'value': DTC.SupplyPlan.WILL_SUPPLY,
                        },
                        'seller_warranty': None,
                    }
                }
            },
        }, DTC.Offer())
    ]))
