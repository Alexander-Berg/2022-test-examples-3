# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, is_not

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.pylibrary.proto_utils import message_from_data


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)


DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                },
                'service': {
                    # белый оффер
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                    },
                    # синий оффер без supply_plan - проставим значение по дефолту
                    2: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 2,
                            'warehouse_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                    },
                    # синий оффер с supply_plan - сохраним как есть
                    3: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 3,
                            'warehouse_id': 3,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'content': {
                            'partner': {
                                'original_terms': {
                                    'supply_plan': {
                                        'value': DTC.SupplyPlan.WONT_SUPPLY,
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    # существующий синий оффер без supply_plan, обновим на will_supply
                    4: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 4,
                            'warehouse_id': 0,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                    },
                    # синий оффер с пустым supply_plan, проставим will_supply
                    5: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 5,
                            'warehouse_id': 3,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'content': {
                            'partner': {
                                'original_terms': {
                                    'supply_plan': {
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }]
        }]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'general': {
            'color': 'white',
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=4,
                warehouse_id=0
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=5,
                warehouse_id=0
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE
            )
        ))
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, basic_offers_table,
          service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, datacamp_messages_topic):
    united_offers_processed = piper.united_offers_processed

    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + 1)


def test_dont_set_default_supply_plan_value_to_white_offer(inserter, config, basic_offers_table,
                                                           service_offers_table, actual_service_offers_table):
    """Тест проверяет, что белой сервисной части не проставляется дефолтное значение в план по поставкам"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 1,
            },
        }, DTC.Offer())
    ]))

    assert_that(service_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 1,
            },
            'content': {
                'partner': {
                    'original_terms': {
                        'supply_plan': {
                            'value': DTC.SupplyPlan.WILL_SUPPLY,
                        }
                    }
                }
            }
        }, DTC.Offer())
    ])))


def test_set_default_supply_plan_value_to_blue_offer(inserter, config, basic_offers_table,
                                                     service_offers_table, actual_service_offers_table):
    """Тест проверяет, что синей сервисной части проставляется дефолтное значение в план по поставкам"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 2,
            },
            'content': {
                'partner': {
                    'original_terms': {
                        'supply_plan': {
                            'value': DTC.SupplyPlan.WILL_SUPPLY,
                        }
                    }
                }
            }
        }, DTC.Offer())]))


def test_dont_change_existed_supply_plan_value_on_blue_offer(inserter, config, basic_offers_table,
                                                             service_offers_table, actual_service_offers_table):
    """Тест проверяет, что в синей сервисной части не меняется партнерское значение
     в плане по поставкам на дефолтное"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 3,
            },
            'content': {
                'partner': {
                    'original_terms': {
                        'supply_plan': {
                            'value': DTC.SupplyPlan.WONT_SUPPLY,
                        }
                    }
                }
            }
        }, DTC.Offer())]))


def test_update_default_supply_plan_value_to_untouched_blue_offer(inserter, config, basic_offers_table,
                                                                  service_offers_table, actual_service_offers_table):
    """Тест проверяет, что синей сервисной части проставляется дефолтное значение в план по поставкам, даже если сервисная часть не обновлялась"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 4,
            },
            'content': {
                'partner': {
                    'original_terms': {
                        'supply_plan': {
                            'value': DTC.SupplyPlan.WILL_SUPPLY,
                        }
                    }
                }
            }
        }, DTC.Offer())]))


def test_set_default_supply_plan_when_it_is_cleared(inserter, config, basic_offers_table,
                                                    service_offers_table, actual_service_offers_table):
    """Тест проверяет, что синей сервисной части проставляется дефолтное значение в план по поставкам,
    если в топики приходит запись supply_plan.value = null, но заполненной метой
    """
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 5,
            },
            'content': {
                'partner': {
                    'original_terms': {
                        'supply_plan': {
                            'value': DTC.SupplyPlan.WILL_SUPPLY,
                        }
                    }
                }
            }
        }, DTC.Offer())]))
