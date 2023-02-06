# coding: utf-8

import pytest
import time
from hamcrest import assert_that, contains_inanyorder, equal_to, is_not
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.datacamp.proto.api import OffersBatch_pb2 as OffersBatch
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta, dict2tskv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampYtUnitedOffersRows
from market.pylibrary.proto_utils import message_from_data
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig


TIMESTAMP = '2019-02-15T15:55:55Z'
BUSINESS_ID = 1
OFFER_ID = 'o1'
ONLY_BASIC_OFFER_ID = 'only_basic_offer'
NO_CONTENT_OFFER_ID = 'no_content_offer'
SHOP_ID = 2


@pytest.fixture(scope='module')
def partners():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'business_id': BUSINESS_ID,
                    'united_catalog_status': 'SUCCESS',
                }),
            ]),
            'status': 'publish'
        },
    ]


@pytest.fixture(scope='module')
def basic_offers():
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=OFFER_ID,
            ),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(price=10),
                    meta=create_update_meta(10)
                )),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        vendor=DTC.StringValue(
                            value="Old vendor",
                            meta=create_update_meta(10)
                        ),
                    )
                )),
            meta=create_meta(10),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=ONLY_BASIC_OFFER_ID,
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        name=DTC.StringValue(
                            value="Name",
                            meta=create_update_meta(10)
                        ),
                    )
                )
            ),
            meta=create_meta(10),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=NO_CONTENT_OFFER_ID,
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        name=DTC.StringValue(
                            value="Name",
                            meta=create_update_meta(10)
                        ),
                    )
                )
            ),
            meta=create_meta(10),
        )),
    ]


@pytest.fixture(scope='module')
def service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=OFFER_ID,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish_by_partner=DTC.AVAILABLE,
                united_catalog=DTC.Flag(
                    flag=True,
                    meta=create_update_meta(10)
                )
            )
        ))]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=OFFER_ID,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish=DTC.AVAILABLE,
            )
        ))]


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
        partners_table,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
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
    ) as stroller_env:
        yield stroller_env


def test_on_basic_offer_update(dispatcher, stroller, saas):

    def _create_cargo_types(cargo_types, update_ts=None):
        result = DTC.I32ListValue(
            meta=create_update_meta(update_ts) if update_ts else None
        )
        for cargo_type in cargo_types:
            result.value.append(cargo_type)
        return result

    update = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=OFFER_ID,
            shop_id=SHOP_ID,
            warehouse_id=0
        ),
        meta=create_meta(10, scope=DTC.BASIC),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                original=DTC.OriginalSpecification(
                    vendor=DTC.StringValue(
                        value="New vendor",
                        meta=create_update_meta(20)
                    )
                )
            ),
            master_data=DTC.MarketMasterData(
                cargo_type=_create_cargo_types(cargo_types=[100, 200], update_ts=20)
            )
        )
    )

    response = stroller.post('/v1/partners/{}/offers/basic?offer_id={}'.format(BUSINESS_ID, OFFER_ID), data=update.SerializeToString())
    assert_that(response, HasStatus(200))

    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(BUSINESS_ID, OFFER_ID),
        {'offer_id', 'vendor', 'cargo_type'},
        kps=BUSINESS_ID,
        sgkps=BUSINESS_ID
    )
    assert_that(saas_doc['offer_id'], equal_to(OFFER_ID))
    assert_that(saas_doc['vendor'], equal_to('New vendor'))
    assert_that(saas_doc['cargo_type'], contains_inanyorder("100", "200"))


def test_on_actual_service_offer_update(dispatcher, stroller, saas):
    """При обновлении actual-сервисного оффера не должны потерять поля из сервисного оффера. И наоборот"""
    def _send_update_and_check_saas(update):
        response = stroller.post('/v1/partners/{}/offers/services/{}?offer_id={}'.format(BUSINESS_ID, SHOP_ID, OFFER_ID), data=update.SerializeToString())
        assert_that(response, HasStatus(200))

        # раньше вместо поля  'supply_plan' использовалось поле 'publish_by_partner',
        # но второе поле перестало быть Saas-триггером, поэтому было заменено
        saas_doc = saas.kv_client.wait_and_get(
            's/{}/{}'.format(BUSINESS_ID, OFFER_ID),
            {'offer_id', 'supply_plan'},
            kps=BUSINESS_ID,
            sgkps=BUSINESS_ID
        )
        assert_that(saas_doc['offer_id'], equal_to(OFFER_ID))

    # сначала отправим обновление по сервисному офферу чтобы в Саасе появился документ
    update = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=OFFER_ID,
            shop_id=SHOP_ID,
            warehouse_id=0
        ),
        meta=create_meta(10, scope=DTC.SERVICE),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                original_terms=DTC.OriginalTerms(
                    supply_plan=DTC.SupplyPlan(
                        value=DTC.SupplyPlan.WILL_SUPPLY
                    )
                )
            )
        ),
    )
    _send_update_and_check_saas(update)

    # затем отправляем изменения по актуальной сервисной части - проверяем, что они никак
    # не повлияли на документ в саасе
    update = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=OFFER_ID,
            shop_id=SHOP_ID,
            warehouse_id=0
        ),
        meta=create_meta(10, scope=DTC.SERVICE),
        status=DTC.OfferStatus(
            publish=DTC.AVAILABLE,
        )
    )
    _send_update_and_check_saas(update)


def test_on_new_service_offer(dispatcher, stroller, saas):
    new_shop_id = 20

    update = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=OFFER_ID,
            shop_id=new_shop_id,
        ),
        meta=create_meta(20),
        status=DTC.OfferStatus(
            publish=DTC.AVAILABLE,
            united_catalog=DTC.Flag(
                flag=True,
                meta=create_update_meta(10)
            )
        )
    )

    response = stroller.post('/v1/partners/{}/offers/services/{}?offer_id={}'.format(BUSINESS_ID, new_shop_id, OFFER_ID), data=update.SerializeToString())
    assert_that(response, HasStatus(200))

    time.sleep(5)  # TODO improve with 'wait until'
    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(BUSINESS_ID, OFFER_ID),
        {'offer_id', 'vendor', 'shop_id'},
        kps=BUSINESS_ID,
        sgkps=BUSINESS_ID
    )
    assert_that(saas_doc['offer_id'], equal_to(OFFER_ID))
    assert_that(saas_doc['shop_id'], contains_inanyorder(str(SHOP_ID), str(new_shop_id)))


def create_partner_content():
    return {
        'original': {
            'vendor': {
                'value': 'The best vendor',
            },
        },
    }


def test_remove_united_offer(dispatcher, stroller, saas):
    """ Проверяем удаление оффера из saas """
    business_id = BUSINESS_ID
    offer_id = 'offer_to_remove'
    shop_id = SHOP_ID

    # Оффера в таблице нет
    assert_that(stroller.basic_offers_table.data, is_not(HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_sku': offer_id,
    }])))

    # Добавляем
    # Из-за снятия Saas-триггера на 'publish_by_partner', был добавлен 'supply_plan'
    body = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'offer': {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                    'meta': {
                        'ts_created': TIMESTAMP,
                    },
                    'content': {
                        'partner': create_partner_content(),
                    },
                },
                'service': {
                    shop_id: {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                            'shop_id': shop_id
                        },
                        'meta': {
                            'ts_created': TIMESTAMP,
                        },
                        'status': {
                            'united_catalog': {
                                'flag': True,
                                'meta': {
                                    'timestamp': TIMESTAMP
                                }
                            }
                        },
                        'content': {
                            'partner': {
                                'original_terms': {
                                    'supply_plan': {
                                        'value': DTC.SupplyPlan.WILL_SUPPLY,
                                        'meta': {
                                            'timestamp': TIMESTAMP
                                        }
                                    }
                                }
                            }
                        },
                    }
                }
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())
    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(stroller.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_sku': offer_id,
        'state_flags': 0,
    }]))

    # Ждем, пока появится в saas
    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(business_id, offer_id),
        {'offer_id', 'vendor', 'supply_plan'},
        kps=business_id,
        sgkps=business_id
    )
    assert_that(saas_doc['offer_id'], equal_to(offer_id))

    # Удаляем
    body = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'offer': {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                    'content': {
                        'partner': create_partner_content(),
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
                            'shop_id': shop_id
                        },
                        'status': {
                            'removed': {
                                'flag': True,
                                'meta': {
                                    'timestamp': TIMESTAMP,
                                }
                            }
                        },
                        'content': {
                            'partner': {
                                'original_terms': {
                                    'supply_plan': {
                                        'value': DTC.SupplyPlan.WILL_SUPPLY,
                                        'meta': {
                                            'timestamp': TIMESTAMP
                                        }
                                    }
                                }
                            }
                        },
                    }
                }
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())
    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(stroller.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_sku': offer_id,
        'state_flags': 1,
    }]))

    # Ждем, пока удалится из saas
    def is_removed():
        return not saas.kv_client.get(
            's/{}/{}'.format(business_id, offer_id),
            {'offer_id', 'supply_plan'},
            kps=business_id,
            sgkps=business_id,
            timeout=2
        )
    wait_until(is_removed, timeout=10)


def test_only_basic_offer_update(dispatcher, stroller, saas):
    meta = create_meta(10, scope=DTC.BASIC)
    meta.saas_force_send.meta.timestamp.FromSeconds(10)
    meta.saas_force_send.ts.FromSeconds(10)

    update = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=ONLY_BASIC_OFFER_ID,
        ),
        meta=meta,
    )

    response = stroller.post(
        '/v1/partners/{}/offers/basic?offer_id={}'.format(BUSINESS_ID, ONLY_BASIC_OFFER_ID),
        data=update.SerializeToString()
    )
    assert_that(response, HasStatus(200))

    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(BUSINESS_ID, ONLY_BASIC_OFFER_ID),
        {'offer_id'},
        kps=BUSINESS_ID,
        sgkps=BUSINESS_ID
    )
    assert_that(saas_doc['offer_id'], equal_to(ONLY_BASIC_OFFER_ID))


def update_offer_no_content(dispatcher, stroller, saas):
    meta = create_meta(10, scope=DTC.BASIC)
    meta.saas_force_send.meta.timestamp.FromSeconds(10)
    meta.saas_force_send.ts.FromSeconds(10)

    update = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=NO_CONTENT_OFFER_ID,
        ),
        meta=meta,
    )

    response = stroller.post(
        '/v1/partners/{}/offers/basic?offer_id={}'.format(BUSINESS_ID, NO_CONTENT_OFFER_ID),
        data=update.SerializeToString()
    )
    assert_that(response, HasStatus(200))

    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(BUSINESS_ID, NO_CONTENT_OFFER_ID),
        {'offer_id'},
        kps=BUSINESS_ID,
        sgkps=BUSINESS_ID
    )
    assert_that(saas_doc['offer_id'], equal_to(None))
