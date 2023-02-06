# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, equal_to

import market.idx.datacamp.proto.category.PartnerCategory_pb2 as DT_CATEGORY
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.utils import LogBrokerEvenlyWriter
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp
from market.proto.common.common_pb2 import PriceExpression
from market.pylibrary.proto_utils import message_from_data

TIME_PATTERN = "%Y-%m-%dT%H:%M:%SZ"

OFFERS_COUNT = 100

OFFERS = [
    {
        'shop_id': shop_id,
        'offer_id': str(offer_id),
    }
    for shop_id, offer_id
    in zip(
        list(range(1, 1 + OFFERS_COUNT)),
        list(range(1001, 1001 + OFFERS_COUNT))
    )
]

# количество партиций в топике
PARTITIONS_COUNT = 3
TS_CREATED = create_pb_timestamp()
TS_CREATED_TIME_STAMP = datetime.utcfromtimestamp(TS_CREATED.seconds).strftime(TIME_PATTERN)


@pytest.fixture(scope='session')
def offers():
    return [DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            shop_id=offer['shop_id'],
            business_id=offer['shop_id'],
            offer_id=offer['offer_id'],
            warehouse_id=offer['shop_id'],
        ),
        meta=DTC.OfferMeta(
            ts_created=TS_CREATED,
            rgb=DTC.BLUE,
        ),
        status=DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=DTC.UpdateMeta(
                        source=DTC.MARKET_IDX
                    )
                )
            ]
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                master_data=DTC.MasterData(),
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(),
                    description=DTC.StringValue(),
                    type_prefix=DTC.StringValue(),
                    vendor=DTC.StringValue(),
                    model=DTC.StringValue(),
                    vendor_code=DTC.StringValue(),
                    barcode=DTC.StringListValue(),
                    offer_params=DTC.ProductYmlParams(),
                    group_id=DTC.Ui32Value(),
                    type=DTC.ProductType(),
                    downloadable=DTC.Flag(),
                    adult=DTC.Flag(),
                    age=DTC.Age(),
                    url=DTC.StringValue(),
                    condition=DTC.Condition(),
                    manufacturer_warranty=DTC.Flag(),
                    expiry=DTC.Expiration(),
                    country_of_origin=DTC.StringListValue(),
                    weight=DTC.PreciseWeight(),
                    dimensions=DTC.PreciseDimensions(),
                    supplier_info=DTC.SupplierInfo(),
                    price_from=DTC.Flag(),
                    isbn=DTC.StringListValue(),
                    cargo_types=DTC.I32ListValue()
                ),
                original_terms=DTC.OriginalTerms(
                    sales_notes=DTC.StringValue(),
                    quantity=DTC.Quantity(),
                    seller_warranty=DTC.Warranty(),
                ),
                actual=DTC.ProcessedSpecification(
                    title=DTC.StringValue(
                        value='test',  # should be one non empty value, otherwise the whole block is cleared
                    ),
                    description=DTC.StringValue(),
                    country_of_origin_id=DTC.I64ListValue(),
                    offer_params=DTC.ProductYmlParams(),
                    price_from=DTC.Flag(),
                    adult=DTC.Flag(),
                    age=DTC.Age(),
                    barcode=DTC.StringListValue(),
                    expiry=DTC.Expiration(),
                    manufacturer_warranty=DTC.Flag(),
                    url=DTC.StringValue(),
                    weight=DTC.PreciseWeight(),
                    dimensions=DTC.PreciseDimensions(),
                    downloadable=DTC.Flag(),
                    sales_notes=DTC.StringValue(),
                    type_prefix=DTC.StringValue(),
                    type=DTC.ProductType(),
                    quantity=DTC.Quantity(),
                    seller_warranty=DTC.Warranty(),
                    isbn=DTC.StringListValue(),
                    cargo_types=DTC.I32ListValue(),
                    category=DT_CATEGORY.PartnerCategory(),
                ),
            ),
            market=DTC.MarketContent(),
            binding=DTC.ContentBinding(
                partner=DTC.Mapping(),
                smb_partner=DTC.Mapping(),
                approved=DTC.Mapping(),
                uc_mapping=DTC.Mapping(),
            ),
        ),
        pictures=DTC.OfferPictures(
            partner=DTC.PartnerPictures(
                original=DTC.SourcePictures(),
                actual=dict(
                    pic_url=DTC.MarketPicture()
                )
            ),
            market=DTC.MarketPictures(),
        ),
        price=DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=PriceExpression(
                    id='RUR',
                    price=offer['shop_id'] * 10,
                )
            ),
            price_by_warehouse={
                10: DTC.PriceBundle(
                    binary_price=PriceExpression(
                        id='RUR',
                        price=offer['shop_id'] * 10,
                    )
                ),
            },
            purchase_price=DTC.PriceBundle(
                binary_price=PriceExpression(
                    id='RUR',
                    price=offer['shop_id'] * 10,
                )
            ),
            enable_auto_discounts=DTC.Flag(),  # для белого нет дефолтного значения
        ),
        delivery=DTC.OfferDelivery(
            specific=DTC.SpecificDeliveryOptions(),
            calculator=DTC.DeliveryCalculatorOptions(
                delivery_calc_generation=10,  # should be one non empty value, otherwise the whole block is cleared
            ),
            delivery_info=DTC.DeliveryInfo(),
        ),
        order_properties=DTC.OfferOrderProperties(),
        bids=DTC.OfferBids(
            bid=DTC.Ui32Value(),
            bid_actual=DTC.Ui32Value(),
            fee=DTC.Ui32Value(),
            flag_dont_pull_up_bids=DTC.Flag(),
            amore_data=DTC.AmoreDataValue(),
        ),
        partner_info=DTC.PartnerInfo(),
        stock_info=DTC.OfferStockInfo(
            market_stocks=DTC.OfferStocks(),
            partner_stocks=DTC.OfferStocks(),
            partner_stocks_default=DTC.OfferStocks()
        ),
    ) for offer in OFFERS]


@pytest.fixture(scope='session')
def bad_offers():
    return [
        # DTC.Offer(),  # No identifiers at all # пустой оффер не пропускает логброкер, а код заливки постоянно пытается записать заново, из-за этого куча ошибок в логах
        DTC.Offer(  # No offer id
            identifiers=DTC.OfferIdentifiers(
                shop_id=101,
            ),
            meta=DTC.OfferMeta(
                ts_created=create_pb_timestamp(),
            ),
        ),
        DTC.Offer(  # No shop id
            identifiers=DTC.OfferIdentifiers(
                offer_id='101',
            ),
            meta=DTC.OfferMeta(
                ts_created=create_pb_timestamp(),
            ),
        ),
        DTC.Offer(  # No meta
            identifiers=DTC.OfferIdentifiers(
                shop_id=101,
                offer_id='101',
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE,
            ),
        )
    ]


@pytest.fixture(scope='session')
def lbk_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, partitions_count=PARTITIONS_COUNT)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, lbk_topic):
    cfg = {
        'general': {
            'worker_count': 3,
            'batch_size': 3,
            'color': 'blue',
        },
        'logbroker': {
            'max_read_count': 3,
            'offers_topic': lbk_topic.topic,
        },
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


@pytest.yield_fixture(scope='module')
def piper_2(yt_server, log_broker_stuff, config, lbk_topic):
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(offers, bad_offers, piper, piper_2, lbk_topic):
    writer = LogBrokerEvenlyWriter(lbk_topic)
    for offer in offers + bad_offers:
        writer.write(offer.SerializeToString())

    wait_until(lambda: piper.united_offers_processed + piper_2.united_offers_processed >= len(offers), timeout=60)


def test_multiple_piper(inserter, offers, piper, piper_2):
    """
    Проверяет основную, инфрастуктурную логику Piper'а:
      - чтения из логброкера и запись в таблицу оферов
      - чтения и запись происходит из двух инстансов,
            каждый из которых работает со своими партициями
    """

    # Проверяем, что все входные оферы, есть в выходной табличке
    assert_that(
        len(piper.basic_offers_table.data),
        equal_to(len(offers)),
        'Too few offers in table'
    )
    assert_that(piper.basic_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'business_id': offer.identifiers.shop_id,
                            'offer_id': offer.identifiers.offer_id,
                        }
                    }, DTC.Offer()) for offer in offers]),
                'Missing offers')


def test_update_meta_ts_basic_offers_table(inserter, offers, piper):
    """Проверяет, что при создании оффера в хранилище у всех существующих полей
    будет проставлен update meta ts, если его не было, в нужных местах на верхнем
    уровне будет проставлен самый большой ts"""

    assert_that(piper.basic_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'business_id': offer.identifiers.shop_id,
                            'offer_id': offer.identifiers.offer_id,
                        },
                        'pictures': {
                            'market': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP,
                                },
                            },
                            'partner': {
                                'original': {
                                    'meta': {
                                        'timestamp': TS_CREATED_TIME_STAMP,
                                    },
                                },
                                'actual': {
                                    'pic_url': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP,
                                        },
                                    },
                                }
                            }
                        }
                    }, DTC.Offer()) for offer in offers]))


def test_update_meta_ts_service_offers_table(inserter, offers, piper):
    assert_that(piper.service_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'business_id': offer.identifiers.shop_id,
                            'offer_id': offer.identifiers.offer_id,
                        },
                        'bids': {
                            'bid': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                },
                            },
                            'fee': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                }
                            },
                            'flag_dont_pull_up_bids': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                }
                            },
                            'amore_data': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                }
                            },
                        },
                        'content': {
                            'binding': {
                                'smb_partner': {
                                    'meta': {
                                        'timestamp': TS_CREATED_TIME_STAMP
                                    },
                                },
                            },
                            'partner': {
                                'actual': {
                                    'title': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'description': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'country_of_origin_id': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'offer_params': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'price_from': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'adult': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'age': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'barcode': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'expiry': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'manufacturer_warranty': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'url': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'weight': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'dimensions': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'downloadable': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'sales_notes': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'type_prefix': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'type': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'quantity': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'seller_warranty': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'isbn': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'cargo_types': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'category': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    }
                                },
                                'original': {
                                    'url': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        }
                                    },
                                    'supplier_info': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        }
                                    },
                                    'name': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'description': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'type_prefix': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'vendor': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'model': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'vendor_code': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'barcode': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'offer_params': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'group_id': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'downloadable': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'type': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'adult': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'age': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'condition': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'manufacturer_warranty': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'expiry': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'country_of_origin': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'weight': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'dimensions': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'price_from': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'isbn': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                    'cargo_types': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                },
                                'original_terms': {
                                    'sales_notes': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        }
                                    },
                                    'quantity': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        }
                                    },
                                    'seller_warranty': {
                                        'meta': {
                                            'timestamp': TS_CREATED_TIME_STAMP
                                        },
                                    },
                                },
                            }
                        },
                        'stock_info': {
                            'partner_stocks_default': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                },
                            },
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price':  offer.identifiers.shop_id * 10,
                                    'id': 'RUR'
                                },
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                }
                            }
                        },
                    }, DTC.Offer()) for offer in offers]))


def test_update_meta_ts_actual_service_offers_table(inserter, offers, piper):
    assert_that(piper.actual_service_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'business_id': offer.identifiers.shop_id,
                            'offer_id': offer.identifiers.offer_id,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'meta': {
                                        'timestamp': TS_CREATED_TIME_STAMP
                                    },
                                }
                            ]
                        },
                        'delivery': {
                            'specific': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                },
                            },
                            'calculator': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                },
                            },
                            'delivery_info': {
                                'meta': {
                                    'timestamp': TS_CREATED_TIME_STAMP
                                },
                            },
                        }}, DTC.Offer()) for offer in offers]))
