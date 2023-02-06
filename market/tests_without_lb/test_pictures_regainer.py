# coding: utf-8

from hamcrest import assert_that, equal_to, has_items, has_entries, not_none, not_
from datetime import datetime
import pytest

import yt.wrapper as yt

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import PicturesRegainerEnv
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import TPicrobotState
from market.idx.datacamp.picrobot.py_lib.state import encode_state
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.yt_tables.picrobot_state import PicrobotStateTable
from market.pylibrary.proto_utils import message_from_data


TIME_PATTERN = '%Y-%m-%dT%H:%M:%SZ'

MARKET_NAMESPACE = 'marketpic'
DIRECT_BANNERLAND_NAMESPACE = 'bannerland_ns'
VERTICAL_NAMESPACE = 'goods_pic'
ALL_NAMESPACES = [MARKET_NAMESPACE, DIRECT_BANNERLAND_NAMESPACE, VERTICAL_NAMESPACE]

NOT_ENCODED_PICTURE_URL = 'rt.tr/сладкая_vatrushka.jpg'
ENCODED_PICTURE_URL = 'https://rt.tr/%D1%81%D0%BB%D0%B0%D0%B4%D0%BA%D0%B0%D1%8F_vatrushka.jpg'


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        # Только базовый оффер, кладем в маркетный namespace, при этом вторая картинка уже есть в стейте её не обновляем
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'only_basic_offer',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'},
                            {'url': 'rt.tr/kazylyk.jpg'}
                        ]
                    },
                    'actual': {
                        'rt.tr/kazylyk.jpg': {
                            'status': DTC.AVAILABLE,
                            'id': 'kazylyk',
                            'group_id': 1,
                        }
                    },
                    'multi_actual': {
                        'rt.tr/kazylyk.jpg': {
                            'by_namespace': {
                                MARKET_NAMESPACE: {
                                    'status': DTC.AVAILABLE,
                                    'id': 'kazylyk',
                                    'group_id': 1,
                                }
                            }
                        }
                    }
                }
            }
        },
        # Картинки нет в стейте пикробота и её надо скачать
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'force_send_to_picrobot',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'meta': {
                            'timestamp': datetime.utcfromtimestamp(100).strftime(TIME_PATTERN)
                        },
                        'source': [
                            {'url': 'rt.tr/chakchak.jpg'},
                        ]
                    },
                }
            }
        },
        # директ
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'direct_offer',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'},
                        ]
                    },
                }
            }
        },
        # директ баннерленд
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'direct_bannerland_offer',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'},
                        ]
                    },
                }
            }
        },
        # директ ТГО сбрасывается из-за флага IsDirectSearchSnippetGallery
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'direct_shopsdat_offer',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'},
                        ]
                    },
                }
            }
        },
        # вертикали без отдельного цвета
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'verticals_offer',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'},
                        ]
                    },
                }
            }
        },
        # вертикали с отдельным цветом
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'vertical_goods_ads',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'},
                        ]
                    },
                }
            }
        },
        # чистовертикальный оффер с картинками в marketpic, в goods_pic перекачивать не надо
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'verticals_offer_migrate_from_marketpic',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'https://rt.rt/someurl.jpg'}
                        ]
                    },
                    'actual': {
                        'https://rt.rt/someurl.jpg': {
                            'namespace': MARKET_NAMESPACE,
                            'status': DTC.AVAILABLE,
                            'id': 'someurl',
                            'group_id': 1,
                        }
                    },
                    'multi_actual': {
                        'https://rt.rt/someurl.jpg': {
                            'by_namespace': {
                                MARKET_NAMESPACE: {
                                    'status': DTC.AVAILABLE,
                                    'id': 'someurl',
                                    'group_id': 1,
                                }
                            }
                        }
                    }
                }
            }
        },
        # Оффер в стейте доступен только в одном namespace, надо отправить задание в picrobot
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'ask_for_new_ns',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'https://rt.tr/uchpochmak.jpg'}
                        ]
                    },
                    'actual': {
                        'https://rt.tr/uchpochmak.jpg': {
                            'namespace': MARKET_NAMESPACE,
                            'status': DTC.AVAILABLE,
                            'id': 'uchpochmak',
                            'group_id': 1,
                        }
                    },
                    'multi_actual': {
                        'https://rt.tr/uchpochmak.jpg': {
                            'by_namespace': {
                                MARKET_NAMESPACE: {
                                    'status': DTC.AVAILABLE,
                                    'id': 'uchpochmak',
                                    'group_id': 1,
                                }
                            }
                        }
                    }
                }
            }
        },
        # Оффер, у которого картинки скачались с ошибкой, должны подтянуть статус
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'picture_with_error',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'https://rt.tr/balish.jpg'},
                            {'url': 'https://rt.tr/cheburek.jpg'},
                            {'url': 'https://rt.tr/gubadia.jpg'},
                        ]
                    },
                }
            }
        },
        # директ + маркет, в оффер только директовый namespace и надо приклеить маркетный
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'bannerland_and_market',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'},
                        ]
                    },
                    'actual': {
                        'rt.tr/beshbarmak.jpg': {
                            'namespace': VERTICAL_NAMESPACE,
                            'status': DTC.AVAILABLE,
                            'id': 'beshbarmak',
                            'group_id': 1,
                        }
                    },
                    'multi_actual': {
                        'rt.tr/beshbarmak.jpg': {
                            'by_namespace': {
                                VERTICAL_NAMESPACE: {
                                    'status': DTC.AVAILABLE,
                                    'id': 'beshbarmak',
                                    'group_id': 1,
                                }
                            }
                        }
                    }
                }
            }
        },
        # Оффер с failed картинкой. Если в picrobot есть ошибка скачивания, то не обновляем, т.к. стейт актуальный
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'offer_with_failed_picture',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/cheburek.jpg'}
                        ]
                    },
                    'actual': {
                        'rt.tr/cheburek.jpg': {
                            'status': DTC.MarketPicture.Status.FAILED,
                        }
                    },
                    'multi_actual': {
                        'rt.tr/cheburek.jpg': {
                            'by_namespace': {
                                MARKET_NAMESPACE: {
                                    'status': DTC.MarketPicture.Status.FAILED,
                                }
                            }
                        }
                    }
                }
            }
        },
        # Оффер с кириллическими символами в урле картинки
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'offer_with_cyrillic_chars',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': NOT_ENCODED_PICTURE_URL},
                        ]
                    },
                }
            }
        },
        # Оффер с невалидным url
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'offer_with_invalid_url',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': '/upload/iblock/c45/e6c3jd6uibhvkohcft21kv71urb0xgzp.jpeg'},
                        ]
                    },
                }
            }
        },
        # оффер с удаленной аватаркой картинкой
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'offer_with_deleted_meta',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'https://rt.rt/deleted.jpg'}
                        ]
                    },
                    'actual': {
                        'https://rt.rt/deleted.jpg': {
                            'namespace': MARKET_NAMESPACE,
                            'status': DTC.AVAILABLE,
                            'id': 'someurl',
                            'group_id': 1,
                        }
                    },
                    'multi_actual': {
                        'https://rt.rt/deleted.jpg': {
                            'by_namespace': {
                                MARKET_NAMESPACE: {
                                    'status': DTC.AVAILABLE,
                                    'id': 'someurl',
                                    'group_id': 1,
                                }
                            }
                        }
                    }
                }
            }
        },
        # оффер со сбитой аватаркой (у картинки в пикроботе другие параметры аватарки)
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'offer_with_corrupted_ava',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'}
                        ]
                    },
                    'actual': {
                        'rt.tr/beshbarmak.jpg': {
                            'namespace': MARKET_NAMESPACE,
                            'status': DTC.AVAILABLE,
                            'id': 'beshbarmak',
                            'group_id': 2,
                        }
                    },
                    'multi_actual': {
                        'rt.tr/beshbarmak.jpg': {
                            'by_namespace': {
                                MARKET_NAMESPACE: {
                                    'status': DTC.AVAILABLE,
                                    'id': 'beshbarmak',
                                    'group_id': 2,
                                }
                            }
                        }
                    }
                }
            }
        },
        # оффер с хорошей аватаркой (у картинки в пикроботе такиеже параметры аватарки)
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'offer_with_valid_ava',
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'rt.tr/beshbarmak.jpg'}
                        ]
                    },
                    'actual': {
                        'rt.tr/beshbarmak.jpg': {
                            'namespace': MARKET_NAMESPACE,
                            'status': DTC.AVAILABLE,
                            'id': 'beshbarmak',
                            'group_id': 1,
                        }
                    },
                    'multi_actual': {
                        'rt.tr/beshbarmak.jpg': {
                            'by_namespace': {
                                MARKET_NAMESPACE: {
                                    'status': DTC.AVAILABLE,
                                    'id': 'beshbarmak',
                                    'group_id': 1,
                                }
                            }
                        }
                    }
                }
            }
        },
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [{
        'identifiers': {
            'business_id': 111,
            'offer_id': 'direct_offer',
            'shop_id': 222
        },
        'meta': {
            'rgb': DTC.DIRECT
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'direct_bannerland_offer',
            'shop_id': 333
        },
        'meta': {
            'rgb': DTC.DIRECT_SITE_PREVIEW
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'direct_shopsdat_offer',
            'shop_id': 444
        },
        'meta': {
            'rgb': DTC.DIRECT
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'direct_shopsdat_offer',
            'shop_id': 444
        },
        'meta': {
            'rgb': DTC.DIRECT
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'verticals_offer',
            'shop_id': 555
        },
        'meta': {
            'vertical_approved_flag': {
                'flag': True
            }
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'vertical_goods_ads',
            'shop_id': 555
        },
        'meta': {
            'vertical_approved_flag': {
                'flag': True
            },
            'rgb': DTC.VERTICAL_GOODS_ADS
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'verticals_offer_migrate_from_marketpic',
            'shop_id': 555
        },
        'meta': {
            'vertical_approved_flag': {
                'flag': True
            },
            'rgb': DTC.VERTICAL_GOODS_ADS
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'ask_for_new_ns',
            'shop_id': 666
        },
        'meta': {
            'rgb': DTC.DIRECT
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'ask_for_new_ns',
            'shop_id': 111
        },
        'meta': {
            'rgb': DTC.WHITE
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'bannerland_and_market',
            'shop_id': 111
        },
        'meta': {
            'rgb': DTC.WHITE
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'bannerland_and_market',
            'shop_id': 222
        },
        'meta': {
            'rgb': DTC.DIRECT_SITE_PREVIEW
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'offer_with_failed_picture',
            'shop_id': 111
        },
        'meta': {
            'rgb': DTC.WHITE
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'offer_with_cyrillic_chars',
            'shop_id': 888
        },
        'meta': {
            'rgb': DTC.WHITE
        }
    }, {
        'identifiers': {
            'business_id': 111,
            'offer_id': 'synthetic_one',
            'shop_id': 888
        },
        'meta': {
            'rgb': DTC.WHITE,
            'synthetic': True,
        }
    },
    ]


@pytest.fixture(scope='module')
def partners_table_data():
    return [{
        'shop_id': 444,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': 444,
                'business_id': 111,
                'direct_search_snippet_gallery': False,
                'direct_standby': True
            }),
        ]),
    }, {
        'shop_id': 777,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': 777,
                'business_id': 111,
                'direct_search_snippet_gallery': False,
                'direct_standby': True
            }),
        ]),
    }]


PICROBOT_STATE_DATA = [
    encode_state('https://rt.tr/beshbarmak.jpg', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'beshbarmak'
                }
            } for ns in ALL_NAMESPACES
        ]
    }, TPicrobotState()), 'zstd_6'),
    encode_state('https://rt.tr/kazylyk.jpg', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'kazylyk'
                }
            } for ns in ALL_NAMESPACES
        ]
    }, TPicrobotState()), 'zstd_6'),
    encode_state('https://rt.tr/hamburger.jpg', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'hamburger'
                }
            } for ns in ALL_NAMESPACES
        ]
    }, TPicrobotState()), 'zstd_6'),
    encode_state('https://rt.tr/uchpochmak.jpg', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'uchpochmak'
                }
            } for ns in (MARKET_NAMESPACE,)
        ]
    }, TPicrobotState()), 'zstd_6'),
    encode_state('https://rt.rt/someurl.jpg', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'someurl'
                }
            } for ns in (MARKET_NAMESPACE,)
        ]
    }, TPicrobotState()), 'zstd_6'),
    # Картинка не скачана, но нет ошибки скачивания в стейте => не накатываем, возможно она прокачается
    encode_state('https://rt.tr/balish.jpg', message_from_data({
        'MdsInfo': []
    }, TPicrobotState()), 'zstd_6'),
    # Картинка не скачана, есть ошибка скачивания => должны накатить статус FAILED
    encode_state('https://rt.tr/cheburek.jpg', message_from_data({
        'MdsInfo': [],
        'DownloadMeta': {
            'HttpCode': 404,
            'Namespace': MARKET_NAMESPACE,
        },
        'RequestInfo': {
            'Namespaces': [
                MARKET_NAMESPACE
            ]
        }
    }, TPicrobotState()), 'zstd_6'),
    # Картинки в целевом неймспейсе нет, есть ошибка копирования в целевой неймспейс => должны накатить статус FAILED
    encode_state('https://rt.tr/gubadia.jpg', message_from_data({
        'MdsInfo': [],
        'DownloadMeta': {
            'HttpCode': 200,
            'Namespace': DIRECT_BANNERLAND_NAMESPACE,
        },
        'CopierMeta': [{
            'MdsHttpCode': 400,
            'Namespace': MARKET_NAMESPACE,
        }]
    }, TPicrobotState()), 'zstd_6'),
    encode_state(ENCODED_PICTURE_URL, message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': MARKET_NAMESPACE,
                    'GroupId': 1,
                    'ImageName': 'vatrushka'
                }
            }
        ]
    }, TPicrobotState()), 'zstd_6'),
    # Удаленная из аватарницы картинка
    encode_state('https://rt.rt/deleted.jpg', message_from_data({
        'MdsInfo': [],
        'DeleteMeta': [{
            'Deleted': True
        }],
        'RequestInfo': {
            'Namespaces': [
                MARKET_NAMESPACE
            ]
        }
    }, TPicrobotState()), 'zstd_6'),
]


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'yt_home': '//home/datacamp/united',
                'picrobot_state': '//picrobot_state',
            },
            'pictures_regainer': {
                'enable': True,
                'yt_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'output_dir': 'out_dir',
                'picrobot_mds_namespace': MARKET_NAMESPACE,
                'picrobot_direct_bannerland_mds_namespace': DIRECT_BANNERLAND_NAMESPACE,
                'picrobot_vertical_mds_namespace': VERTICAL_NAMESPACE,
                'force_send_interval': 3600,
                'recover_avatars': True,
            }
        })
    return config


@pytest.fixture(scope='module')
def picrobot_state_table(yt_server, config):
    return PicrobotStateTable(yt_server, config.picrobot_state_tablepath, data=PICROBOT_STATE_DATA)


@pytest.yield_fixture(scope='module')
def workflow(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        partners_table,
        picrobot_state_table
):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'partners_table': partners_table,
        'picrobot_state_table': picrobot_state_table,
        'config': config,
    }
    with PicturesRegainerEnv(yt_server, **resources) as routines_env:
        yield routines_env


@pytest.fixture(scope='module')
def offers_table(yt_server, config, workflow):
    yt_client = yt_server.get_yt_client()
    return list(yt_client.read_table(yt.ypath_join(config.pictures_regainer_output_dir, 'recent')))


def expected_offer_matcher(offer_id, namespace):
    ava_url = '//avatars.mds.yandex.net/get-{namespace}/1/{ns_specific_prefix}beshbarmak/orig'.format(
        namespace=namespace,
        ns_specific_prefix='market_' if namespace in ('marketpic', 'marketpictesting') else ''
    )

    return IsSerializedProtobuf(DTC.Offer, {
        'identifiers': {
            'business_id': 111,
            'offer_id': offer_id
        },
        'pictures': {
            'partner': {
                'actual': IsProtobufMap({
                    'rt.tr/beshbarmak.jpg': {
                        'namespace': namespace,
                        'id': 'beshbarmak',
                        'group_id': 1,
                        'original': {
                            'url': ava_url
                        },
                        'meta': {
                            'timestamp': {
                                'seconds': not_none(),
                            },
                            'source': DTC.MARKET_IDX
                        },
                        'status': DTC.AVAILABLE,
                    },
                }),
                'multi_actual': IsProtobufMap({
                    'rt.tr/beshbarmak.jpg': {
                        'by_namespace': IsProtobufMap({
                            namespace: {
                                'namespace': namespace,
                                'id': 'beshbarmak',
                                'group_id': 1,
                                'original': {
                                    'url': ava_url
                                },
                                'meta': {
                                    'timestamp': {
                                        'seconds': not_none(),
                                    },
                                    'source': DTC.MARKET_IDX
                                },
                                'status': DTC.AVAILABLE,
                            }
                        })
                    },
                })
            }
        }
    })


def test_rows_count(offers_table):
    assert_that(len(offers_table), equal_to(13))


def test_namespaces_mapping(offers_table):
    # Проверяем, что есть обновление картинки не из стейта
    assert_that(offers_table, has_items(
        has_entries({'offer': expected_offer_matcher('only_basic_offer', MARKET_NAMESPACE)}),
        has_entries({'offer': expected_offer_matcher('direct_offer', DIRECT_BANNERLAND_NAMESPACE)}),
        has_entries({'offer': expected_offer_matcher('direct_bannerland_offer', DIRECT_BANNERLAND_NAMESPACE)}),
        has_entries({'offer': expected_offer_matcher('direct_shopsdat_offer', DIRECT_BANNERLAND_NAMESPACE)}),
        has_entries({'offer': expected_offer_matcher('bannerland_and_market', MARKET_NAMESPACE)}),
        has_entries({'offer': expected_offer_matcher('vertical_goods_ads', VERTICAL_NAMESPACE)}),
        has_entries({'offer': expected_offer_matcher('offer_with_corrupted_ava', MARKET_NAMESPACE)}),
    ))


def test_untouchable_pictures(offers_table):
    assert_that(offers_table, not_(has_items(
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'ask_for_new_ns'
                },
                'pictures': {
                    'partner': {
                        'actual': IsProtobufMap({
                            'https://rt.tr/uchpochmak.jpg': {}
                        })
                    }
                }
            })
        })
    )), u'Актуальная картинка для MARKET_NAMESPACE уже есть в стейте оффера, обновлять не надо')

    assert_that(offers_table, not_(has_items(
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'only_basic_offer'
                },
                'pictures': {
                    'partner': {
                        'actual': IsProtobufMap({
                            'rt.tr/kazylyk.jpg': {},
                        })
                    }
                }
            })
        }),
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'picture_with_error'
                },
                'pictures': {
                    'partner': {
                        'actual': IsProtobufMap({
                            'https://rt.tr/balish.jpg': {},
                        })
                    }
                }
            })
        }),
    )), u'Актуальная картинка уже есть в стейте оффера, обновлять не надо')

    assert_that(offers_table, not_(has_items(
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'verticals_offer_migrate_from_marketpic'
                },
                'meta': {
                    'picrobot_force_send': not_none()
                }
            })
        })
    )), u'Актуальная картинка для MARKET_NAMESPACE уже есть в стейте оффера, в VERTICAL_NAMESPACE не надо скачивать')

    assert_that(offers_table, has_items(
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'offer_with_cyrillic_chars'
                },
                'meta': {
                    'picrobot_force_send': None,
                },
                'pictures': {
                    'partner': {
                        'actual': IsProtobufMap({
                            NOT_ENCODED_PICTURE_URL: {
                                'status': DTC.AVAILABLE
                            }
                        }),
                        'multi_actual': IsProtobufMap({
                            NOT_ENCODED_PICTURE_URL: {
                                'by_namespace': IsProtobufMap({
                                    'marketpic': {
                                        'status': DTC.AVAILABLE
                                    }
                                })
                            }
                        })
                    }
                }
            })
        })
    ), u'Из пикробота должна подтянуться актуальная картинка для оффера с кириллицей в урле')


def test_force_send_pictures(offers_table):
    assert_that(offers_table, has_items(
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'force_send_to_picrobot'
                },
                'meta': {
                    'picrobot_force_send': not_none()
                }
            })
        }),
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'ask_for_new_ns'
                },
                'meta': {
                    'picrobot_force_send': not_none()
                }
            })
        }),
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'picture_with_error'
                },
                'meta': {
                    'picrobot_force_send': not_none()
                }
            })
        }),
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'offer_with_deleted_meta'
                },
                'meta': {
                    'picrobot_force_send': not_none()
                }
            })
        })
    ))


def test_picture_failed_status_update(offers_table):
    # Проверяем, что в случае каких-либо проблем с картинкой проставляется статус FAILED
    assert_that(offers_table, has_items(
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'picture_with_error'
                },
                'pictures': {
                    'partner': {
                        'actual': IsProtobufMap({
                            'https://rt.tr/cheburek.jpg': {
                                'meta': {
                                    'timestamp': {
                                        'seconds': not_none(),
                                    },
                                    'source': DTC.MARKET_IDX
                                },
                                'status': DTC.MarketPicture.Status.FAILED,
                            },
                            'https://rt.tr/gubadia.jpg': {
                                'meta': {
                                    'timestamp': {
                                        'seconds': not_none(),
                                    },
                                    'source': DTC.MARKET_IDX
                                },
                                'status': DTC.MarketPicture.Status.FAILED,
                            },
                        }),
                        'multi_actual': IsProtobufMap({
                            'https://rt.tr/cheburek.jpg': {
                                'by_namespace': IsProtobufMap({
                                    MARKET_NAMESPACE: {
                                        'meta': {
                                            'timestamp': {
                                                'seconds': not_none(),
                                            },
                                            'source': DTC.MARKET_IDX
                                        },
                                        'status': DTC.MarketPicture.Status.FAILED,
                                    }
                                })
                            },
                            'https://rt.tr/gubadia.jpg': {
                                'by_namespace': IsProtobufMap({
                                    MARKET_NAMESPACE: {
                                        'meta': {
                                            'timestamp': {
                                                'seconds': not_none(),
                                            },
                                            'source': DTC.MARKET_IDX
                                        },
                                        'status': DTC.MarketPicture.Status.FAILED,
                                    }
                                })
                            },
                        })
                    }
                }
            })
        })
    ))


def test_ignore_synthetic(offers_table):
    assert_that(offers_table, not_(has_items(
        has_entries({
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 111,
                    'offer_id': 'synthetic_one'
                },
            })
        }),
    )))
