# coding: utf-8

import pytest
import json
from datetime import datetime, timedelta
from hamcrest import assert_that, has_entries, has_items, matches_regexp, not_

import modadvert.bigmod.protos.interface.verdict_pb2 as BM
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta, dict2tskv
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable, DataCampPartnersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)

DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'T1000',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': current_time
                    },
                    'pictures': {
                        'partner': {
                            'original': {
                                'source': [
                                    {'url': 'http://a.b/c'}
                                ],
                                'meta': {
                                    'timestamp': current_time
                                }
                            },
                            'actual': {
                                'http://a.b/c': {
                                    'meta': {
                                        'timestamp': current_time
                                    },
                                    'original': {
                                        'url': '//avatars.mds.yandex.net/get-mrkt_idx_direct_test/12345/market_pictureid/orig',
                                    },
                                    'namespace': 'mrkt_idx_direct_test',
                                    'id': 'pictureid',
                                    'group_id': 12345,
                                    'mds_host': 'avatars.mds.yandex.net',
                                    'status': DTC.MarketPicture.AVAILABLE
                                }
                            },
                            # не отправим картинки не из директового namespace-а
                            'multi_actual': {
                                'http://a.b/c': {
                                    'by_namespace': {
                                        'marketpic': {
                                            'meta': {
                                                'timestamp': current_time
                                            },
                                            'original': {
                                                'url': '//avatars.mds.yandex.net/get-mrkt_idx_direct_test/12345/market_pictureid/orig',
                                            },
                                            'namespace': 'marketpic',
                                            'id': 'pictureid',
                                            'group_id': 12345,
                                            'mds_host': 'avatars.mds.yandex.net',
                                            'status': DTC.MarketPicture.AVAILABLE
                                        }
                                    }
                                }
                            }
                        }
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': current_time
                                    }
                                },
                                'description': {
                                    'value': 'description',
                                    'meta': {
                                        'timestamp': current_time
                                    }
                                },
                                'adult': {
                                    'flag': True,
                                    'meta': {
                                        'timestamp': current_time
                                    }
                                },
                            }
                        }
                    }
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'T1000',
                            'shop_id': 1,
                            'extra' : {
                                'shop_sku': 'T1000',
                            }
                        },
                        'content': {
                            'partner': {
                                'actual': {
                                    'url': {
                                        'value': 'someurl',
                                        'meta': {
                                            'timestamp': current_time
                                        }
                                    }
                                }
                            }
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.DIRECT_SEARCH_SNIPPET_GALLERY
                        },
                    }
                }
            }, {  # has no url
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'T2000',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': current_time
                    },
                    'pictures': {
                        'partner': {
                            'actual': {
                                'http://a.b/d': {
                                    'meta': {
                                        'timestamp': current_time
                                    },
                                    'original': {
                                        'url': '//avatars.mds.yandex.net/get-mrkt_idx_direct_test/12345/market_pictureid2/orig',
                                    },
                                    'namespace': 'mrkt_idx_direct_test',
                                    'id': 'pictureid2'
                                }
                            }
                        }
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': current_time
                                    }
                                },
                                'description': {
                                    'value': 'description',
                                    'meta': {
                                        'timestamp': current_time
                                    }
                                },
                                'adult': {
                                    'flag': False,
                                    'meta': {
                                        'timestamp': current_time
                                    }
                                },
                            }
                        }
                    }
                },
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'T3000',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': current_time
                    },
                    'pictures': {
                        'partner': {
                            'original': {
                                'source': [
                                    {'url': 'http://a.b/c'}
                                ],
                                'meta': {
                                    'timestamp': current_time
                                }
                            },
                            'multi_actual': {
                                'http://a.b/c': {
                                    'by_namespace': {
                                        'mrkt_idx_direct_test': {
                                            'meta': {
                                                'timestamp': current_time
                                            },
                                            'original': {
                                                'url': '//avatars.mds.yandex.net/get-mrkt_idx_direct_test/12345/market_pictureid/orig',
                                            },
                                            'namespace': 'mrkt_idx_direct_test',
                                            'id': 'pictureid',
                                            'group_id': 12345,
                                            'mds_host': 'avatars.mds.yandex.net',
                                            'status': DTC.MarketPicture.AVAILABLE
                                        }
                                    }
                                }
                            }
                        }
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': current_time
                                    }
                                },
                            }
                        }
                    }
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'T3000',
                            'shop_id': 1,
                            'extra' : {
                                'shop_sku': 'T3000',
                            }
                        },
                        'content': {
                            'partner': {
                                'actual': {
                                    'url': {
                                        'value': 'someurl',
                                        'meta': {
                                            'timestamp': current_time
                                        }
                                    }
                                }
                            }
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.DIRECT_SEARCH_SNIPPET_GALLERY
                        },
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
def direct_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def direct_moderation_response_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


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
                        'client_id': 10,
                        'direct_feed_id' : 200,
                        'direct_search_snippet_gallery': True,
                    }),
                ]),
            },
        ],
    )


@pytest.fixture(scope='module', params=[True, False])
def relaxed(request):
    return request.param


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, direct_topic, direct_moderation_response_topic, relaxed):
    cfg = {
        'general': {
            'color': 'white',
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'direct_out_topic': direct_topic.topic,
            'direct_in_topic': direct_moderation_response_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg, force_relaxed=relaxed)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T1000',
            ),
            meta=create_meta(10),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    category_id=10,
                    meta=create_update_meta(10)
                )
            ))),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=2,
                offer_id='T3000',
            ),
            meta=create_meta(10))),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=3,
                offer_id='T3000',
            ),
            meta=create_meta(10))),
    ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T1000',
                shop_id=1,
            ),
            meta=create_meta(10, color=DTC.DIRECT_SEARCH_SNIPPET_GALLERY),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=2,
                offer_id='T3000',
                shop_id=1,
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    direct_search_snippet_moderation_subscription_version=DTC.VersionCounter(
                        counter=2
                    )
                ),
            ),
            meta=create_meta(10, color=DTC.DIRECT_SEARCH_SNIPPET_GALLERY),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=3,
                offer_id='T3000',
                shop_id=1,
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    direct_search_snippet_moderation_subscription_version=DTC.VersionCounter(
                        counter=2
                    )
                ),
            ),
            meta=create_meta(10, color=DTC.DIRECT_SEARCH_SNIPPET_GALLERY),
        ))
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T1000',
                shop_id=1,
                warehouse_id=0
            ),
            meta=create_meta(10, color=DTC.DIRECT_SEARCH_SNIPPET_GALLERY),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=2,
                offer_id='T3000',
                shop_id=1,
                warehouse_id=0
            ),
            meta=create_meta(10, color=DTC.DIRECT_SEARCH_SNIPPET_GALLERY)
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=3,
                offer_id='T3000',
                shop_id=1,
                warehouse_id=0
            ),
            meta=create_meta(10, color=DTC.DIRECT_SEARCH_SNIPPET_GALLERY)
        )),
    ])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    direct_topic,
    direct_moderation_response_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    partners_table,
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'direct_out_topic': direct_topic,
        'direct_in_topic': direct_moderation_response_topic,
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_apply_moderation_verdict(piper, direct_moderation_response_topic, relaxed):
    if relaxed:  # TODO support verdicts in relaxed mode
        return
    processed = piper.united_offers_processed
    direct_moderation_response_topic.write(json.dumps({
        'meta': {
            'business_id': 2,
            'shop_id': 1,
            'version': '2',
            'client_id': 11,
            'offer_yabs_id': '123445',
            'original_b2b_offer_id': 'T3000'
        },
        'result': {
            'verdict': 0,
            'reasons': [1, 2, 3, 4, 5],
            'flags': [6, 501, 502],
            'minus_regions': [8, 9, 10],
            'timestamp': 90000
        }
    }) + '\n' + json.dumps({
        'meta': {
            'business_id': 3,
            'shop_id': 1,
            'version': '2',
            'client_id': 11,
            'offer_yabs_id': '123445',
            'original_b2b_offer_id': 'T3000'
        },
        'result': {
            'verdict': 0,
            'reasons': [1, 2, 3, 4, 5],
            'flags': [6, 501, 502],
            'minus_regions': [8, 9, 10],
            'timestamp': 90000
        }
    }))
    wait_until(lambda: piper.united_offers_processed >= processed + 2)

    assert_that(piper.service_offers_table.data, HasOffers([
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=business_id,
                shop_id=1,
                offer_id='T3000'
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    direct_search_snippet_moderation_subscription_version=DTC.VersionCounter(
                        counter=2
                    )
                ),
            ),
            resolution=DTC.Resolution(
                direct=DTC.Verdicts(
                    verdict_version=DTC.VersionCounter(
                        counter=2
                    ),
                    bigmod_verdict=BM.TDataCampVerdict(
                        Verdict=0,
                        Reasons=[1, 2, 3, 4, 5],
                        Flags=[6, 501, 502],
                        MinusRegions=[8, 9, 10],
                        Timestamp=90000
                    )
                )
            )
        ) for business_id in range(2, 4)
    ]))


def test_write_to_direct_moderation(piper, datacamp_messages_topic, direct_topic):
    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 2)

    data = [json.loads(message) for message in direct_topic.read(count=2)]
    assert_that(data, has_items(has_entries({
        'meta': has_entries({
            'business_id': 1,
            'shop_id': 1,
            'version': matches_regexp('[1-9]\\d{18}'),
            'original_b2b_offer_id': 'T1000',
            'client_id': 10,
            'offer_yabs_id': '17900908483242088853'
        }),
        'data': has_entries({
            'title': 'title',
            'description': 'description',
            'url': 'someurl',
            'adult': True,
            'service': 'EDCS_DIRECT',
            'images': has_items(
                has_entries({
                    'namespace': 'mrkt_idx_direct_test',
                    'group_id': 12345,
                    'image_name': 'pictureid',
                    'host': 'avatars.mds.yandex.net'
                })
            )
        }),
        'type': "offer"
    })))

    assert_that(data, has_items(has_entries({
        'meta': has_entries({
            'business_id': 1,
            'shop_id': 1,
            'original_b2b_offer_id': 'T3000',
        }),
        'data': has_entries({
            'images': has_items(
                has_entries({
                    'namespace': 'mrkt_idx_direct_test',
                    'group_id': 12345,
                    'image_name': 'pictureid',
                    'host': 'avatars.mds.yandex.net'
                })
            )
        }),
    })))

    assert_that(data, not_(has_items(has_entries({
        'data': has_entries({
            'images': has_items(
                has_entries({
                    'namespace': 'marketpic',
                })
            )
        }),
    }))))


FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
future_time = FUTURE_UTC.strftime(time_pattern)

DATACAMP_MESSAGES_UPDATE = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'T1000',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': future_time
                    },
                    'pictures': {
                        'partner': {
                            'original': {
                                'source': [
                                    {'url': 'http://a.b/c'}
                                ],
                                'meta': {
                                    'timestamp': future_time
                                }
                            },
                            'actual': {
                                'http://a.b/c': {
                                    'meta': {
                                        'timestamp': future_time
                                    },
                                    'original': {
                                        'url': '//avatars.mds.yandex.net/get-mrkt_idx_direct_test/12345/market_pictureid/orig',
                                    },
                                    'namespace': 'mrkt_idx_direct_test',
                                    'id': 'pictureid',
                                    'group_id': 12345,
                                    'mds_host': 'avatars.mds.yandex.net'
                                }
                            }
                        }
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': future_time
                                    }
                                },
                                'description': {
                                    'value': 'description',
                                    'meta': {
                                        'timestamp': future_time
                                    }
                                },
                                'adult': {
                                    'flag': True,
                                    'meta': {
                                        'timestamp': future_time
                                    }
                                },
                            }
                        }
                    }
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'T1000',
                            'shop_id': 1,
                            'extra' : {
                                'shop_sku': 'T1000',
                            }
                        },
                        'content': {
                            'partner': {
                                'actual': {
                                    'url': {
                                        'value': 'someurl',
                                        'meta': {
                                            'timestamp': future_time
                                        }
                                    }
                                }
                            }
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.DIRECT_SEARCH_SNIPPET_GALLERY,
                            'ts_created': future_time
                        },
                    }
                }
            }]
        }]
    }
]


def test_direct_offer_enriched_from_partners_table(piper, datacamp_messages_topic, direct_topic):
    for message in DATACAMP_MESSAGES_UPDATE:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 1)

    assert_that(
        piper.service_offers_table.data,
        HasOffers([message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'T1000',
                'shop_id': 1,
                'extra' : {
                    'client_id': 10,
                    'direct_feed_id' : 200,
                }
            }
        }, DatacampOffer())]))
