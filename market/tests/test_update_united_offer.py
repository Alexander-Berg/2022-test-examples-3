# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, equal_to, is_not, greater_than, not_

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.business.Business_pb2 import BusinessStatus
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers, deserialize_united_row
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable,
    DataCampBusinessStatusTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.pylibrary.proto_utils import message_from_data

BLOCKED_BUSINESS = 1000
MIGRATING_OLD_BLUE_SHOP = 10
NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)
max_pictures_count = 20
max_params_count = 30

FUTURE_UTC = NOW_UTC + timedelta(minutes=45)


def generate_pictures(count):
    items = []
    for i in range(count):
        items.append({'url': 'https://image{}.jpg'.format(i)})
    return {
        'partner': {
            'original': {
                'source': items
            }
        }
    }


def generate_params(count):
    items = []
    for i in range(count):
        items.append({
            'name': 'param-{}'.format(i),
            'unit': 'unit-{}'.format(i),
            'value': 'value-{}'.format(i)
        })
    return {
        'partner': {
            'original': {
                'offer_params': {
                    'param': items
                }
            }
        }
    }


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
                    'price': {
                        'basic': {
                            'binary_price': {
                                'price': 10
                            },
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern)
                            }
                        }
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                }
                            }
                        }
                    }
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                            'warehouse_id': 1,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 20
                                },
                                'meta': {
                                    'timestamp': NOW_UTC.strftime(time_pattern)
                                }
                            }
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.MARKET_STOCK,
                                        'timestamp': NOW_UTC.strftime(time_pattern),
                                    },
                                },
                            ],
                        },
                        'partner_info': {
                            'is_dsbs': True,
                        },
                    },
                }
            }]
        }, {
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                    },
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                            'warehouse_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_API,
                                        'timestamp': NOW_UTC.strftime(time_pattern),
                                    },
                                },
                            ],
                        },
                        'delivery': {
                            'specific': {
                                'delivery_currency': 'd'
                            }
                        },
                        'partner_info': {
                            'is_dsbs': True,
                        },
                    }
                }
            }]
        }, {
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o2',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'price': {
                        'basic': {
                            'binary_price': {
                                'price': 30
                            },
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern)
                            }
                        }
                    },
                }
            }]
        }, {
            # Вердикт МДМ по базовой части должен при вести к скрытию синих сервисных офферов
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 2,
                        'offer_id': 'blue1',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'resolution': {
                        'by_source': [{
                            'meta': {
                                'source': DTC.MARKET_MDM,
                                'timestamp': datetime.utcfromtimestamp(100).strftime(time_pattern),
                            },
                            'verdict': [{
                                'results': [{
                                    'is_valid': False,
                                    'is_banned': True,
                                    'applications': [DTC.FULFILLMENT, DTC.CPA]
                                }]
                            }]
                        }]
                    },
                }
            }]
        }, {
            # Вердикт майнера по базовой части должен сохраняться в контроллерах и существовать без базовой части.
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 2,
                        'offer_id': 'basicOnly',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'resolution': {
                        'by_source': [{
                            'meta': {
                                'source': DTC.MARKET_IDX,
                                'timestamp': datetime.utcfromtimestamp(100).strftime(time_pattern),
                            },
                            'verdict': [{
                                'results': [{
                                    'is_valid': False,
                                    'is_banned': True,
                                }]
                            }]
                        }]
                    },
                },
            }]
        }, {
            'offer': [{
                # В хранилище united_catalog=false, в шопсдате - true, перезапишем
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'update-united-catalog-by-shopsdat',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'update-united-catalog-by-shopsdat',
                            'shop_id': 1,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                    },
                }
            }, {
                # Мигрирующий синий магазин, шопсдата еще не доехала, смотрим на таблицу статуса бизнеса
                'basic': {
                    'identifiers': {
                        'business_id': 10,
                        'offer_id': 'update-united-catalog-by-business-status',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                },
                'service': {
                    MIGRATING_OLD_BLUE_SHOP: {
                        'identifiers': {
                            'business_id': 10,
                            'offer_id': 'update-united-catalog-by-business-status',
                            'shop_id': MIGRATING_OLD_BLUE_SHOP,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                    },
                }
            }, {
                # Попытка изменить united_catalog c true на false не сработает
                'basic': {
                    'identifiers': {
                        'business_id': 2,
                        'offer_id': 'not-update-united-catalog',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                },
                'service': {
                    2: {
                        'identifiers': {
                            'business_id': 2,
                            'offer_id': 'not-update-united-catalog',
                            'shop_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'status': {
                            'united_catalog': {
                                'flag': False,
                                'meta': {
                                    'timestamp': NOW_UTC.strftime(time_pattern)
                                }
                            }
                        }
                    },
                }
            }]
        }, {
            # Оффер заблокированного бизнеса
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BLOCKED_BUSINESS,
                        'offer_id': 'blocked_business',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'price': {
                        'basic': {
                            'binary_price': {
                                'price': 30
                            },
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern)
                            }
                        }
                    },
                }
            }]
        }, {
            # Проверяем, что скрытия от MBI из-за overprice прорастают в сервисную часть
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 4,
                        'offer_id': 'offer4',
                    },
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 4,
                            'offer_id': 'offer4',
                            'shop_id': 4,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.MARKET_MBI_OVERPRICE,
                                        'timestamp': current_time,
                                    },
                                },
                            ],
                        },
                    }
                }
            }]
        }, {
            # Проверяем валидацию offer_id
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 5,
                        'offer_id': 'offer-with-too-looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong-offer-id',
                    },
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 5,
                        'offer_id': 'offer-with-ok-offer-id',
                    },
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 5,
                        'offer_id': 'offer-with*invalid*chars*in-offer-id',
                    },
                }
            }]
        }, {
            # Проверяем валидацию валюты
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 6,
                        'offer_id': 'offer-USD-white',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    }
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': 'offer-USD-white',
                            'shop_id': 1,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'USD',
                                    'price': 20
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 6,
                        'offer_id': 'offer-invalid-cur',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    }
                },
                'service': {
                    2: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': 'offer-invalid-cur',
                            'shop_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'XXX',
                                    'price': 20
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 6,
                        'offer_id': 'offer-invalid-cur-direct',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    }
                },
                'service': {
                    2: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': 'offer-invalid-cur-direct',
                            'shop_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.DIRECT,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'XXX',
                                    'price': 20
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 6,
                        'offer_id': 'offer-semi-invalid-cur',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    }
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': 'offer-semi-invalid-cur',
                            'shop_id': 1,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'XXX',
                                    'price': 20
                                }
                            }
                        }
                    },
                    2: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': 'offer-semi-invalid-cur',
                            'shop_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'RUR',
                                    'price': 20
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 6,
                        'offer_id': 'offer-BYR-cur',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    }
                },
                'service': {
                    3: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': 'offer-BYR-cur',
                            'shop_id': 3,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'BYR',
                                    'price': 20
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 6,
                        'offer_id': 'offer-RUR-blue',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    }
                },
                'service': {
                    4: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': 'offer-RUR-blue',
                            'shop_id': 4,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'RUR',
                                    'price': 20
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 6,
                        'offer_id': 'offer-USD-blue',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    }
                },
                'service': {
                    4: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': 'offer-USD-blue',
                            'shop_id': 4,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'USD',
                                    'price': 20
                                }
                            }
                        }
                    }
                }
            }]
        }, {
            # Проверяем превышение максимального кол-ва картинок
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 7,
                        'offer_id': 'offer-id-with-max-pictures-count',
                    },
                    'pictures': generate_pictures(10)
                },
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 7,
                        'offer_id': 'offer-id-with-exceeding-limit-pictures-count',
                    },
                    'pictures': generate_pictures(max_pictures_count + 10)
                },
            }]
        }, {
            # Проверяем удаление невалидных строк из content.partner.original и original_terms
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 8,
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
                                        'this string will be deleted because the next one is invalid',
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
            }]
        }, {
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 8,
                        'offer_id': 'offer.with.vendor.name',
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'vendor': {
                                    'value': 'vendor\nname',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern),
                                    },
                                },
                            }
                        }
                    }
                }
            }]
        }, {
            # Проверяем превышение максимального кол-ва параметров
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 9,
                        'offer_id': 'offer-id-with-params-count-within-limit',
                    },
                    'content': generate_params(10)
                },
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 9,
                        'offer_id': 'offer-id-with-exceeding-limit-params-count',
                    },
                    'content': generate_params(max_params_count + 10)
                },
            }]
        }, {
            # Проверяем исправление цвета оффера
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'offer-color-fix',
                    },
                    'content': generate_params(10)
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'offer-color-fix',
                            'shop_id': 1,
                            'warehouse_id': 2,
                        },
                        'meta': {
                            'rgb': DTC.WHITE,
                        },
                    },
                    2: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'offer-color-fix',
                            'shop_id': 6,
                            'warehouse_id': 3,
                        },
                        'meta': {
                            # неверно заданный цвет
                            'rgb': DTC.BLUE,
                        },
                    },
                }
            }]
        }]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module', params=['blue', 'white'])
def color(request):
    return request.param


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            {
                'shop_id': 1,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 1,
                        'business_id': 1,
                        'cpc': 'NO',
                        'united_catalog_status': 'SUCCESS',
                        'is_site_market': 'true',
                    }),
                ]),
            },
            {
                'shop_id': 3,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 3,
                        'business_id': 3,
                        'united_catalog_status': 'NO',
                        'is_site_market': 'true',
                    }),
                ]),
            },
            {
                'shop_id': 6,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 6,
                        'business_id': 1,
                        'united_catalog_status': 'NO',
                        'is_site_market': 'true',
                    }),
                ]),
            },
            {
                'shop_id': MIGRATING_OLD_BLUE_SHOP,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': MIGRATING_OLD_BLUE_SHOP,
                        'business_id': 10,
                        'united_catalog_status': 'NO',
                        'is_site_market': 'true',
                    }),
                ]),
            }
        ],
    )


@pytest.fixture(scope='module')
def business_status_table(yt_server, config):
    return DataCampBusinessStatusTable(yt_server, config.yt_business_status_tablepath, data=[
        {
            'business_id': BLOCKED_BUSINESS,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
            ).SerializeToString(),
        },
        {
            'business_id': 10,
            'status': BusinessStatus(
                shops={MIGRATING_OLD_BLUE_SHOP: 10}
            ).SerializeToString(),
        },
    ])


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, color):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'general': {
            'color': color,
        },
        'features': {
            'enable_offer_format_validator': True,
            'enable_offer_id_validation': True,
            'enable_currency_validation': True,
            'enable_validate_pictures_limit': True,
            'offer_pictures_limit': max_pictures_count,
            'enable_original_partner_content_validation': True,
            'enable_validate_params_limit': True,
            'offer_params_limit': max_params_count,
            'enable_vendor_code_validation': True,
            'enable_color_validation': True,
        }
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='o1', shop_id=1),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=2, offer_id='blue1', shop_id=2),
            meta=create_meta(10, DTC.BLUE),
            status=DTC.OfferStatus(
                united_catalog=DTC.Flag(
                    flag=True,
                )
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='update-united-catalog-by-shopsdat', shop_id=1),
            meta=create_meta(10, DTC.WHITE),
            status=DTC.OfferStatus(
                united_catalog=DTC.Flag(
                    flag=False,
                )
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=10, offer_id='update-united-catalog-by-business-status', shop_id=MIGRATING_OLD_BLUE_SHOP),
            meta=create_meta(10, DTC.BLUE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=2, offer_id='not-update-united-catalog', shop_id=2),
            meta=create_meta(10, DTC.WHITE),
            status=DTC.OfferStatus(
                united_catalog=DTC.Flag(
                    flag=True,
                )
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=4, offer_id='offer4', shop_id=4),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='offer-color-fix', shop_id=1),
            meta=create_meta(10, color=DTC.WHITE, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='offer-color-fix', shop_id=6),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        ))])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=4, offer_id='o1', shop_id=5, warehouse_id=1),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        ))
    ])


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, partners_table, basic_offers_table,
          service_offers_table, actual_service_offers_table, business_status_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'partners_table': partners_table,
        'business_status_table': business_status_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, datacamp_messages_topic):
    united_offers = 0
    for message in DATACAMP_MESSAGES:
        for offers in message['united_offers']:
            united_offers += len(offers)
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= united_offers)


def test_update_united_offer(inserter, config, basic_offers_table,
                             service_offers_table, actual_service_offers_table):
    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(21))
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 10
                    },
                }
            },
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o2',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 30
                    },
                }
            },
        }, DTC.Offer())]))
    uc_data_version = 0
    offer_version = 0
    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if (offer.identifiers.offer_id == 'o1' or offer.identifiers.offer_id == 'o2') and offer.identifiers.business_id == 1:
            uc_data_version = offer.status.version.uc_data_version.counter
            offer_version = offer.status.version.offer_version.counter
            assert_that(uc_data_version, greater_than(1000))
            assert_that(offer_version, greater_than(1000))

    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(13))
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 1,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 20
                    },
                }
            },
            'meta': {
                # цвет проставляется из шопсдат
                'rgb': DTC.WHITE,
            },
            'status': {
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.PUSH_PARTNER_API,
                        },
                    },
                ],
                # united_catalog True проставляется из шопсдат
                'united_catalog': {
                    'flag': True,
                }
            },
            'partner_info': {
                'is_dsbs': True,
            }
        }, DTC.Offer())]))
    for row in service_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == 'o1' and offer.identifiers.shop_id == 1 and offer.identifiers.business_id == 1:
            assert_that(offer.status.version.offer_version.counter, greater_than(1000))

    actual_service_offers_table.load()
    assert_that(len(actual_service_offers_table.data), equal_to(10))
    assert_that(actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'warehouse_id': 1,
                'shop_id': 1,
            },
            'meta': {
                # цвет проставляется из шопсдат
                'rgb': DTC.WHITE,
            },
            'status': {
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_STOCK,
                        },
                    },
                ],
            },
            'partner_info': {
                'is_dsbs': True,
            }
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'warehouse_id': 2,
                'shop_id': 1,
            },
            'meta': {
                # цвет проставляется из шопсдат
                'rgb': DTC.WHITE,
            },
            'delivery': {
                'specific': {
                    'delivery_currency': 'd'
                }
            },
            'partner_info': {
                'is_dsbs': True,
            }
        }, DTC.Offer())]))
    for row in service_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == 'o1' and offer.identifiers.shop_id == 1 and offer.identifiers.warehouse_id == 2 and offer.identifiers.business_id == 1:
            assert_that(offer.status.version.offer_version.counter, greater_than(1000))


def test_update_united_offer_uc_versions_counter(config, piper, datacamp_messages_topic, basic_offers_table, color):
    """Тест проверяет, что корректно увеличивается версия группы данных, от которой зависит ответ UC"""
    # 0) добавим оффер в таблицу
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o3',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'description': {
                                    'value': 'description',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                },
                            },
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                }
                            }
                        }
                    }
                },
            }]
        }],
    }

    united_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_processed + 1)

    # проверяем, что версии проинициализировались
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o3',
            },
            'content': {
                'partner': {
                    'original': {
                        'description': {
                            'value': 'description',
                        }
                    },
                    'actual': {
                        'title': {
                            'value': 'title',
                        }
                    }
                }
            }
        }, DTC.Offer())]))

    uc_data_version = 0
    offer_version = 0
    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == 'o3' and offer.identifiers.business_id == 1:
            uc_data_version = offer.status.version.uc_data_version.counter
            offer_version = offer.status.version.offer_version.counter
            assert_that(uc_data_version, greater_than(1000))
            assert_that(offer_version, greater_than(1000))
            break

    # 1) отправляем обновление с изменением по полям группы
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o3',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'description': {
                                    'value': 'description V.2',
                                    'meta': {
                                        'timestamp': FUTURE_UTC.strftime(time_pattern)
                                    }
                                }
                            },
                        }
                    },
                },
            }]
        }],
    }

    united_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_processed + 1)

    # проверяем, что увеличилась и версия оффера, и версия uc-группы в базовой части - изменения к ней относились
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o3',
            },
            'content': {
                'partner': {
                    'original': {
                        'description': {
                            'value': 'description V.2',
                        }
                    },
                }
            }
        }, DTC.Offer())]))

    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == 'o3' and offer.identifiers.business_id == 1:
            assert_that(offer.status.version.uc_data_version.counter, greater_than(uc_data_version))
            assert_that(offer.status.version.offer_version.counter, greater_than(offer_version))
            uc_data_version = offer.status.version.uc_data_version.counter
            offer_version = offer.status.version.offer_version.counter
            break

    # 2) отправляем обновление без изменений по полям группы
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o3',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'content': {
                        'binding': {
                            'partner': {
                                'market_category_id': 12345,
                                'meta': {
                                    'timestamp': NOW_UTC.strftime(time_pattern)
                                }
                            }
                        }
                    }
                },
            }]
        }],
    }

    united_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_processed + 1)

    # проверяем, что увеличилась и версия оффера, но не увеличилась версия uc-группы
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o3',
            },
            'content': {
                'binding': {
                    'partner': {
                        'market_category_id': 12345,
                    }
                }
            }
        }, DTC.Offer())]))
    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == 'o3' and offer.identifiers.business_id == 1:
            assert_that(offer.status.version.uc_data_version.counter, equal_to(uc_data_version))
            assert_that(offer.status.version.offer_version.counter, greater_than(offer_version))
            uc_data_version = offer.status.version.uc_data_version.counter
            offer_version = offer.status.version.offer_version.counter
            break

    # 4) отправляем обновление только сервисной части

    # запомним синий базовый оффер до отправки изменений
    blue_offer = None
    if color == 'blue':
        for row in basic_offers_table.data:
            offer = message_from_data(deserialize_united_row(row), DTC.Offer())
            if offer.identifiers.offer_id == 'o3' and offer.identifiers.business_id == 1:
                blue_offer = offer
        blue_offer.status.version.ClearField('offer_version')

    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o3',
                    },
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o3',
                            'shop_id': 3,
                        },
                        'meta': {
                            'rgb': DTC.WHITE if color == 'white' else DTC.BLUE,
                            'scope': DTC.SERVICE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 300
                                },
                                'meta': {
                                    'timestamp': FUTURE_UTC.strftime(time_pattern)
                                }
                            }
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'url': {
                                        'value': 'original_url',
                                        'meta': {
                                            'timestamp': FUTURE_UTC.strftime(time_pattern)
                                        }
                                    }
                                },
                                'actual': {
                                    'url': {
                                        'value': 'actual_url',
                                        'meta': {
                                            'timestamp': FUTURE_UTC.strftime(time_pattern)
                                        }
                                    }
                                }
                            }
                        },
                        'status': {
                            'fields_placement_version': {
                                'value': 1
                            }
                        },
                    },
                },
            }]
        }],
    }

    united_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_processed + 1)

    # Вот раньше трава была зеленее и мы копировали цену и url из сервисного оффера в базовый.
    # Cейчас мы копируем только актуальный url. И тот удаляется из базовой части, если meta.scope == SELECTIVE
    # версии базового оффера должны остаться неизменными
    basic_offers_table.load()

    if color == 'white':
        assert_that(basic_offers_table.data, HasOffers([
            message_from_data({
                'identifiers': {
                    'business_id': 1,
                    'offer_id': 'o3',
                },
                'content': {
                    'binding': {
                        'partner': {
                            'market_category_id': 12345,
                        }
                    },
                    'partner': {
                        'original': {
                            'description': {
                                'value': 'description V.2',
                            }
                        },
                        'actual': {
                            'title': {
                                'value': 'title',
                            },
                        }
                    }
                }
            }, DTC.Offer())]))
        for row in basic_offers_table.data:
            offer = message_from_data(deserialize_united_row(row), DTC.Offer())
            if offer.identifiers.offer_id == 'o3' and offer.identifiers.business_id == 1:
                assert_that(offer.content.partner.original.url, None)
                assert_that(offer.status.version.uc_data_version.counter, equal_to(uc_data_version))
                assert_that(offer.status.version.offer_version.counter, equal_to(offer_version))
                break

    # для синего: базовый оффер остается такой же, как был
    else:
        assert_that(basic_offers_table.data, HasOffers([blue_offer]))


def test_apply_basic_mdm_verdicts(inserter, basic_offers_table, service_offers_table):
    """ Скрытия на основе вердиктов из базовой части не применяются к сервсиным частям, сохраняется только сам
        вердикт
    """
    service_offers_table.load()
    assert_that(service_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': 'blue1',
                'shop_id': 2,
            },
            'status': {
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_MDM,
                        },
                    },
                ],
            }
        }, DTC.Offer())])))

    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': 'blue1',
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': DTC.MARKET_MDM,
                        'timestamp': datetime.utcfromtimestamp(100).strftime(time_pattern),
                    },
                    'verdict': [{
                        'results': [{
                            'is_valid': False,
                            'is_banned': True,
                            'applications': [DTC.FULFILLMENT, DTC.CPA]
                        }]
                    }]
                }]
            },
        }, DTC.Offer())]))


def test_apply_basic_idx_verdicts(inserter, basic_offers_table):
    """ Вердикт майнера из базовой части не отфильтровывается в контроллерах и существует без скрытия """
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': 'basicOnly',
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': DTC.MARKET_IDX,
                        'timestamp': datetime.utcfromtimestamp(100).strftime(time_pattern),
                    },
                    'verdict': [{
                        'results': [{
                            'is_valid': False,
                            'is_banned': True,
                        }]
                    }]
                }]
            },
        }, DTC.Offer())]))


def test_update_united_catalog(inserter, service_offers_table):
    """ Проверяем обновление поля united_catalog в таблице """
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'update-united-catalog-by-shopsdat',
                'shop_id': 1,
            },
            'status': {
                'united_catalog': {
                    'flag': True
                },
            },
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 10,
                'offer_id': 'update-united-catalog-by-business-status',
                'shop_id': MIGRATING_OLD_BLUE_SHOP,
            },
            'status': {
                'united_catalog': {
                    'flag': True
                },
            },
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': 'not-update-united-catalog',
                'shop_id': 2,
            },
            'status': {
                'united_catalog': {
                    'flag': True
                },
            },
        }, DTC.Offer())]))


def test_update_restriction_for_blocked_business(inserter, basic_offers_table):
    """ Проверяем, что оффер заблокированного бизнеса не записывается в таблицу """
    basic_offers_table.load()
    assert_that(basic_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BLOCKED_BUSINESS,
                'offer_id': 'blocked_business',
            }
        }, DTC.Offer())])))


def test_mbi_overprice(inserter, service_offers_table):
    """ Скрытие MBI OVERPRICE применилось """
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 4,
                'offer_id': 'offer4',
                'shop_id': 4,
            },
            'status': {
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_MBI_OVERPRICE,
                        },
                    },
                ],
            }
        }, DTC.Offer())]))


def test_offer_id_validation(inserter, basic_offers_table):
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 5,
                'offer_id': 'offer-with-ok-offer-id',
            },
        }, DTC.Offer())
    ]))

    assert_that(basic_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 5,
                'offer_id': 'offer-with-too-looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong-offer-id',
            },
        }, DTC.Offer()), message_from_data({
            'identifiers': {
                'business_id': 5,
                'offer_id': 'offer-with*invalid*chars*in-offer-id',
            },
        }, DTC.Offer())
    ])))


def test_currency_validation(inserter, service_offers_table):
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-USD-white',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'id': 'USD',
                        'price': 20
                    }
                }
            }
        }, DTC.Offer())
    ]))

    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-invalid-cur-direct',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'id': 'XXX',
                        'price': 20
                    }
                }
            }
        }, DTC.Offer())
    ]))

    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-semi-invalid-cur',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'id': 'RUR',
                        'price': 20
                    }
                }
            }
        }, DTC.Offer())
    ]))

    assert_that(service_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-semi-invalid-cur',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'id': 'XXX',
                        'price': 20
                    }
                }
            }
        }, DTC.Offer())
    ])))

    assert_that(service_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-invalid-cur',
            },
        }, DTC.Offer()), message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-BYR-cur',
            },
        }, DTC.Offer())
    ])))

    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-RUR-blue',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'id': 'RUR',
                        'price': 20
                    }
                }
            }
        }, DTC.Offer()), message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-USD-blue',
            }
        }, DTC.Offer())
    ]))

    assert_that(service_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': 'offer-USD-blue',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'id': 'USD',
                        'price': 20
                    }
                }
            }
        }, DTC.Offer())
    ])))


def test_validate_pictures_limit(inserter, basic_offers_table):
    basic_offers_table.load()
    # Если картинок <= limit, то всё ок
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 7,
                'offer_id': 'offer-id-with-max-pictures-count',
            },
            'pictures': generate_pictures(10)
        }, DTC.Offer())
    ]))

    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 7,
                'offer_id': 'offer-id-with-exceeding-limit-pictures-count',
            },
            'pictures': generate_pictures(max_pictures_count)
        }, DTC.Offer())
    ]))

    # Если картинок > limit, то режем их количество до лимита
    assert_that(basic_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 7,
                'offer_id': 'offer-id-with-exceeding-limit-pictures-count',
            },
            'pictures': generate_pictures(max_pictures_count + 10)
        }, DTC.Offer())
    ])))


def test_validate_params_limit(inserter, basic_offers_table):
    basic_offers_table.load()
    # Если параметров <= limit, то всё ок
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 9,
                'offer_id': 'offer-id-with-params-count-within-limit',
            },
            'content': generate_params(10)
        }, DTC.Offer())
    ]))

    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 9,
                'offer_id': 'offer-id-with-exceeding-limit-params-count',
            },
            'content': generate_params(max_params_count)
        }, DTC.Offer())
    ]))

    # Если параметров > limit, то режем их количество до лимита
    assert_that(basic_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 9,
                'offer_id': 'offer-id-with-exceeding-limit-params-count',
            },
            'content': generate_params(max_params_count + 10)
        }, DTC.Offer())
    ])))


def test_original_partner_content_validation(inserter, basic_offers_table):
    basic_offers_table.load()
    assert_that(basic_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 8,
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
                        'barcode': {
                            'value': [
                                'this string will be deleted because the next one is invalid',
                                '\0',
                            ],
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern),
                            },
                        },
                    },
                },
            },
        }, DTC.Offer())
    ])))


def test_vendor_code_validation(inserter, basic_offers_table):
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 8,
                'offer_id': 'offer.with.vendor.name',
            },
            'content': {
                'partner': {
                    'original': {
                        'vendor': {
                            'value': 'vendor name',
                        },
                    },
                },
            },
        }, DTC.Offer())
    ]))


def test_color_validation(inserter, service_offers_table):
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer-color-fix',
                'shop_id': 1,
            },
            'meta': {
                'rgb': DTC.WHITE
            }
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer-color-fix',
                'shop_id': 6,
            },
            'meta': {
                'rgb': DTC.WHITE
            },
        }, DTC.Offer()),
    ]))


def test_filter_incorrect_original_barcode(config, piper, datacamp_messages_topic, basic_offers_table, color):
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'offer-with-incorrect-barcode',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'description': {
                                    'value': 'description',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                },
                                'barcode': {
                                    'value': [
                                        '123456789',
                                        '123',
                                        '3' * 32,
                                        '12345678901'
                                    ],
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern),
                                    },
                                },
                            },
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                }
                            }
                        }
                    }
                },
            }]
        }],
    }

    united_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_processed + 1)

    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer-with-incorrect-barcode',
            },
            'content': {
                'partner': {
                    'original': {
                        'description': {
                            'value': 'description',
                        },
                        'barcode': {
                            'value': [
                                '123456789',
                                '12345678901',
                            ],
                        },
                    },
                    'actual': {
                        'title': {
                            'value': 'title',
                        }
                    }
                }
            }
        }, DTC.Offer())]))
