# coding: utf-8

"""
Тест проверяет жизненный путь офера от нахождения оффера в хранилище до генлогов.
Состоит из 6 этапов:
 - Записываем офера в топик контроллера (piper) наподобие ручного создания оффера в каталоге в ПИ
 - Контроллер читает офера и кладет их в таблицу хранилища
 - Запускается таска рутин united-datacamp-dumper - из таблиц хранилища получаем выгрузки (в этом тесте только white_out)
 - Запускается full-maker, которому на вход подсовывается таблица хранилища, и генерит таблицу offers_raw
 - Запускается main-idx, который по offers_raw генерит шардированные таблицы
 - Запускается оферный индексатор, который отсеивает офера и строит различные индексы и генлоги
 - Проверяем, что офера прошли все круги ада и дошли до генлогов
"""

import pytest
import yatest
import os

from datetime import datetime
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    has_items,
    all_of,
    not_,
    has_properties,
    has_item,
    any_of,
)
from yt.wrapper import ypath_join

from market.pylibrary import shopsdat
from market.idx.pylibrary.datacamp.utils import wait_until

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.proto.common.eproduct_type_pb2 import EProductType
from market.proto.content.pictures_pb2 import Picture

from market.idx.generation.yatf.resources.prepare.blue_promo_table import BluePromoDetailsTable
from market.idx.generation.yatf.resources.prepare.feeds_yt_table import FeedsTable
from market.idx.generation.yatf.resources.prepare.sessions_yt_table import SessionsTable
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.matchers.main_idx.env_matchers import HasRowInShards
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.test_envs.full_maker import Or3FullMakerTestEnv

from market.pylibrary.proto_utils import message_from_data

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import (
    HasGenlogPicturesRecord,
    HasGenlogRecord,
    HasGenlogRecordRecursive,
)
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

import market.proto.indexer.FeedLog_pb2 as FeedLog

import market.proto.ir.UltraController_pb2 as UC
from market.proto.indexer.FeedLog_pb2 import PUSH

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

GENERATION = datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1
HALF_MODE = False
NOW_TIME_UTC = (datetime.utcnow())
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
CURRENT_TIME = NOW_TIME_UTC.strftime(time_pattern)


SHOP_ID = 111  # дефолтный shop_id в генерируемом shops-dat для теста в фидпарсере
BUSINESS_ID = 12345  # дефолтный business_id в генерируемом shops-dat для теста в фидпарсере
SHOP_NAME = 'test_shop'  # дефолтный shop_name в генерируемом shops-dat для теста в фидпарсере
FEED_ID = 1234
DEFAULT_FEED_ID = 4321

OFFER_WITH_PIC='offerWithPic'
PIC_URL='https://yandex.net/image.jpg'
PIC_ID = '0GDMbKY19ULEtDjcmNPVFQ'

IN_PICROBOT_SUCCESS = [
    {
        'id': PIC_ID,
        'is_good_size_pic': True,
        'pic': Picture(
            width=800,
            height=600,
            crc='thisiscrc'
        ).SerializeToString()
    }
]


OFFERS_FROM_PI = [
    # оффер, созданный через интерфейс и привязаный к дефолтному фиду
    {
        'id': offer_id,
        'datacamp_offer': {
            'identifiers': {
                'offer_id': offer_id,
                'shop_id': SHOP_ID,
                'business_id': BUSINESS_ID,
                'extra': {
                    'ware_md5': 'LTb0b0kAB9sXXYgQJd6_SQ',
                    "recent_feed_id": DEFAULT_FEED_ID,
                },
            },
            'meta': {
                'ts_created': CURRENT_TIME,
                'scope': DTC.SELECTIVE,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 4440000000
                    },
                },
            },
            'pictures': {
                'partner': {
                    'original': {
                        'source': [
                            {'url': 'http://www.testofferfrompi.ru/pic/PIC1.jpg'}
                        ]
                    }
                }
            },
            'content': {
                'market': {
                    'enriched_offer': {
                        'enrich_type': UC.EnrichedOffer.ET_APPROVED,
                        'market_sku_id': 123456,
                        'market_sku_published_on_market': True,
                        'model_id': 125,
                        'matched_id': 125,
                        'vendor_id': 456,
                        'market_model_name': 'Test model',
                        'category_id': 90490,
                        'market_category_name': 'Test category',
                        'market_sku_name': 'SKU name',
                    }
                },
                'partner': {
                    'actual': {
                        'description': {
                            'value': 'My Test Desc',
                        },
                        'title': {
                            'value': offer_id,
                        },
                        'type': {
                            'value': EProductType.SIMPLE,
                        },
                        'vendor_code': {
                            'value': 'vendor code from offer',
                        },
                        'url': {
                            'value': "www.testofferfrompi.ru/?ID=1"
                        },
                        'category': {
                            'id': 5752776,
                            'path_category_ids': '5752718\\5752776',
                            'path_category_names': 'Компрессоры\\Компрессоры металлические',
                        },
                    },
                },
            },
            'delivery': {
                'specific': {
                    'delivery_currency': 'RUR',
                    'delivery_options': [
                        {
                            'DaysMin': 1,
                            'OrderBeforeHour': 13,
                            'Cost': 150.0,
                            'DaysMax': 1
                        },
                    ]
                },
                'calculator': {
                    'delivery_calc_generation': 0,
                    'fulfillment_delivery_calc': [
                        {
                            'Generation': 0,
                            'Id': 0
                        },
                    ],
                },
                'delivery_info': {
                    'available': True,
                    'use_yml_delivery': True,
                    'real_deliverycalc_generation': 0,
                    'has_delivery': True,
                    'pickup': True,
                    'store': True,
                },
            },
            'status': {
                'disabled': [
                    {
                        'flag': False,
                        'meta': {
                            'source': DTC.MARKET_IDX,
                        },
                    },
                ],
                'publish': True,
                'publish_by_partner': True,
            },
            'partner_info': {
                'use_market_dimensions': False,
                'supplier_id': 0,
                'autobroker_enabled': False,
                'cpa': 4,
                'is_blue_offer': False,
                'shop_name': SHOP_NAME,
            },
        },
    } for offer_id in ['TestOfferFromPI', 'TestOfferFromPIWareMd5Dup']
]

EXPECTED_GENLOG_PRICES = {
    'TestOfferFromPI': {
        'price': 4440000000,
        'rate': '1',
        'plus': 0,
        'id': 'RUR',
        'ref_id': 'RUR'
    },
    'TestOfferFromPIWareMd5Dup': {
        'price': 4440000000,
        'rate': '1',
        'plus': 0,
        'id': 'RUR',
        'ref_id': 'RUR'
    },
}


@pytest.fixture(scope='module')
def offers_from_pi():
    return OFFERS_FROM_PI


# ====================== datacamp piper ===================


@pytest.yield_fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.fixture(scope='module')
def offers_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def piper_config(yt_server, log_broker_stuff, offers_topic):
    cfg = {
        'logbroker': {
            'offers_topic': offers_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, piper_config, offers_topic):
    resources = {
        'config': piper_config,
        'offers_topic': offers_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope="module")
def datacamp_offers_tables(piper, offers_topic, offers_from_pi):
    # Топик offers_topic - формат DTC.Offer
    for offer in offers_from_pi:
        offers_topic.write(message_from_data(offer['datacamp_offer'], DTC.Offer()).SerializeToString())

    # ждем, пока контроллер подхватит распаршенные офера и положит в табличку
    wait_until(lambda: piper.united_offers_processed >= len(offers_from_pi), timeout=60)
    return piper.basic_offers_table, piper.service_offers_table, piper.actual_service_offers_table


@pytest.fixture(scope="module")
def united_datacamp_offers_table(yt_server, datacamp_offers_tables, piper):
    yt_server.get_yt_client().create('map_node', ypath_join(piper.config.yt_home, piper.config.yt_white_out, 'stats'), recursive=True, ignore_existing=True)
    table = ypath_join(piper.config.yt_home, piper.config.yt_white_out, 'united_offers')
    tasks_bin = yatest.common.binary_path(os.path.join('market', 'idx', 'datacamp', 'routines', 'tasks', 'tasks_runner'))
    cmd = [
        tasks_bin,
        'united-datacamp-dumper',
        '--config-path', piper.config.config_path,
        '--proxy', yt_server.yt_proxy,
        '--colors', 'WHITE',
        '--yt-token-path', 'token',
        '--calc-stats', 'True',
        '--table-name', 'united_offers'
    ]

    yatest.common.execute(cmd)
    return table

# ====================== or3 full-maker ===================


def make_mbi(feed_ids, default_feed_id=None):
    """
    Обязательные параметры для фида в shops.dat
    """
    def create_feed_part(
        feed_id,
        shop_id,
        business_id,
        is_push_partner=True,
        is_default=False,
        is_site_market=True
    ):
        feed_result = ''
        params = {
            'shop_id': str(shop_id),
            'business_id': str(business_id),
            'datafeed_id': str(feed_id),
            'tariff': 'CLICKS',
            'regions': '213;',
            'is_enabled': 'true',
            'is_mock': 'true',
            'is_push_partner': 'true' if is_push_partner else 'false',
            'is_site_market': 'true' if is_site_market else 'false',
        }
        if is_default:
            params['is_default'] = 'true'
        for k, v in params.iteritems():
            feed_result += k + '\t' + v + '\n'
        return feed_result
    result = ''
    for feed_id in feed_ids:
        result += create_feed_part(feed_id, SHOP_ID, BUSINESS_ID) + '\n'
    if default_feed_id:
        result += create_feed_part(default_feed_id, SHOP_ID, BUSINESS_ID, is_default=True) + '\n'
    # для проверки push_feeds_groupped_by_business.txt
    result += create_feed_part(feed_id=100500, shop_id=(SHOP_ID + 1), business_id=BUSINESS_ID) + '\n'  # другой магазин с тем же business_id
    result += create_feed_part(feed_id=100502, shop_id=(SHOP_ID + 3), business_id=(BUSINESS_ID + 1)) + '\n'  # магазин с другим business_id
    return result


@pytest.fixture(scope="module")
def or3_config(yt_server, piper_config, united_datacamp_offers_table):
    home_dir = yt_server.get_yt_client().config['prefix']
    config = {
        'yt': {
            'home_dir': home_dir,
            'yt_collected_promo_details_output_dir': 'collected_promo_details',
        },
        'datacamp': {
            'partners_path': piper_config.yt_partners_tablepath,
        },
        'feeds': {
            'status_set': "'mock', 'publish'",
        },
        'misc': {
            'ware_md5_deduplicator_enabled': 'true',
        },
    }

    config['datacamp']['indexation_enabled'] = 'true'
    config['datacamp']['united_offers_tablepath'] = united_datacamp_offers_table

    return config


@pytest.fixture(scope="module")
def datacamp_partners_table(yt_server, piper):
    update_rows = [{
        'shop_id': SHOP_ID,
        'status': 'publish',
        'mbi': make_mbi([FEED_ID], DEFAULT_FEED_ID),
    }]

    yt_client = yt_server.get_yt_client()
    yt_client.insert_rows(piper.partners_table.get_path(), update_rows, update=True)
    return piper.partners_table


@pytest.yield_fixture(scope="module")
def full_maker(or3_config, yt_server, united_datacamp_offers_table, datacamp_partners_table):
    resources = {
        'config': Or3Config(**or3_config),
        'sessions': SessionsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'headquarters', 'sessions'),
            data=[]
        ),
        'feeds': FeedsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'headquarters', 'feeds'),
            data=[]
        ),
        'datacamp_partners_table': datacamp_partners_table,
        'collected_promo_details_table': BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'collected_promo_details', 'recent'),
            data=[],
        ),
    }
    with Or3FullMakerTestEnv(yt_server, GENERATION, MI3_TYPE, **resources) as fm:
        fm.verify()
        fm.execute()
        yield fm


# ====================== or3 main-idx ===================


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, full_maker, tovar_tree):
    yt_home_path = full_maker.resources['config'].options['yt']['home_dir']
    resources = {
        'config': full_maker.resources['config'],
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
        'offer2pic': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic'),
            data=[]
        ),
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }
    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE,  **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


# ====================== offers-processor ===================


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(main_idx, yt_server):
    shards = [table.get_path() for table in main_idx.tables['offers_shards']]

    with OffersProcessorTestEnv(yt_server, input_table_paths=shards) as env:
        env.execute()
        env.verify()
        yield env


# ====================== tests ===================


def test_offer_from_pi_inserted(datacamp_offers_tables, offers_from_pi):
    """
    Шаг полтора:
    проверяем, что оффера созданные через ПИ будут в выходной таблице Хранилища
    """
    basic, service, actual_service = datacamp_offers_tables

    for offer in offers_from_pi:
        assert_that(basic.data,
                    has_items(has_entries(
                        {
                            'business_id': BUSINESS_ID,
                            'shop_sku': offer['id'],
                        })),
                    'Missing offers in datacamp offers table')

        assert_that(service.data,
                    has_items(has_entries(
                        {
                            'business_id': BUSINESS_ID,
                            'shop_sku': offer['id'],
                        })),
                    'Missing offers in datacamp offers table')

        assert_that(actual_service.data,
                    has_items(has_entries(
                        {
                            'business_id': BUSINESS_ID,
                            'shop_sku': offer['id'],
                        })),
                    'Missing offers in datacamp offers table')


def test_offer_raw_data(full_maker, offers_from_pi):
    """
    Шаг второй:
    Проверяем, что офера из хранилища есть в offers_raw
    """
    assert_that(full_maker.offers_raw.data,
                has_items(
                    *[has_entries({
                        'offer_id': offer['id'],
                    }) for offer in offers_from_pi]),
                'Missing offers in offers_raw table')


def test_generated_shopsdat(full_maker):
    """
    Проверяем, что в сгенеренном shopsdat есть пуш партнеры
    """
    generated_shopsdat = shopsdat.loadfeeds(full_maker.shopsdat_generated_path, status_flags=shopsdat.STATUS_ANY)

    def check_feed_in_generated_shopd_dat(feed_id):
        assert_that(generated_shopsdat,
                    has_item(has_properties(
                        {'id': feed_id}
                    )))

    check_feed_in_generated_shopd_dat(FEED_ID)
    check_feed_in_generated_shopd_dat(DEFAULT_FEED_ID)


def test_generated_shops_from_push_feeds(full_maker):
    """
    Проверка, что в сгенерированном push_feeds_groupped_by_business.txt есть списки
    business_id->feed_id1 feed_id2 ...
    для всех пуш партнеров
    """
    import csv
    with open(os.path.join(full_maker.output_generation_dir, "push_feeds_groupped_by_business.txt")) as f:
        reader = csv.reader(f, delimiter=' ')
        businesses_dict = dict()
        for row in reader:
            businesses_dict[row[0]] = row[1:]
        assert len(businesses_dict) == 2
        assert sorted(businesses_dict[str(BUSINESS_ID)]) == sorted([str(FEED_ID), str(DEFAULT_FEED_ID), '100500'])
        assert businesses_dict[str(BUSINESS_ID + 1)] == ['100502']


def test_main_feedlog(full_maker):
    """
    Проверяем, что в фидлогах есть пуш партнер
    """
    feedlog = full_maker.feedlog_main.load()
    print feedlog

    def check_feed_in_generated_feedlog(feed_id, is_default_feed, total_offers):
        assert_that(feedlog,
                    has_item(has_properties(
                        {
                            'shop_id': SHOP_ID,
                            'feed_id': feed_id,
                            'indexed_status': 'ok',
                            'feed_processing_type': PUSH,
                            'is_default_feed': is_default_feed,
                            'last_session': has_properties(
                                {
                                    'parsing': has_properties(
                                        {
                                            'statistics': has_properties(
                                                {
                                                    'total_offers': total_offers,
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )))
        assert_that(feedlog,
                    not_(has_item(has_properties(
                        {
                            'shop_id': SHOP_ID,
                            'feed_id': feed_id,
                            'indexation': {
                                'status': FeedLog.CRIT,
                            }
                        }
                    ))))
    check_feed_in_generated_feedlog(DEFAULT_FEED_ID, True, 0)


def test_offer_shard_data(main_idx, offers_from_pi):
    """
    Шаг третий:
    Проверяем, что офера из хранилища есть в шардированных табличках после main-idx
    """
    def check_offers_in_shards(feed_id, expected_offers):
        assert_that(main_idx, all_of(*[HasRowInShards(
            {
                'offer_id': offer['id'],
                'feed_id': feed_id,
            }) for offer in expected_offers])
        )
    check_offers_in_shards(DEFAULT_FEED_ID, offers_from_pi)


def test_ware_md5_duplicates_data(main_idx):
    """
    Проверяем, что дублирующиеся ware_md5 попали в таблицу (все, кроме одного не скрытого оффера)
    """
    data = main_idx.outputs['ware_md5_duplicates']
    assert_that(len(data), equal_to(1))
    assert_that(
        data,
        has_items(
            has_entries({
                'feed_id': DEFAULT_FEED_ID,
                'offer_id': any_of('TestOfferFromPI', 'TestOfferFromPIWareMd5Dup'),
                'ware_md5': 'LTb0b0kAB9sXXYgQJd6_SQ'
            })
        )
    )


def test_rejected_by_duplicated_ware_md5(main_idx):
    """
    Проверяем, что для офферов с одинаковыми ware_md5 устанавливается rejected_by_duplicated_ware_md5
    """
    assert_that(
        main_idx.outputs['offers'],
        has_items(
            has_entries({
                'offer_id': any_of('TestOfferFromPI', 'TestOfferFromPIWareMd5Dup'),
                'feed_id': DEFAULT_FEED_ID,
                'offer': has_entries({
                    'rejected_by_duplicated_ware_md5': True,
                }),
            })
        ),
    )


def test_genlog_data(offers_processor_workflow, offers_from_pi):
    """
    Шаг четвертый:
    Проверяем, что офера из хранилища не были выкинуты оферным индексатором и есть в генлогах
    """
    checked = []

    def create_expected_offer(offer, is_from_PI=False):
        binary_price = EXPECTED_GENLOG_PRICES[offer['id']]
        expected_offer = {
            'offer_id': offer['id'],
            'feed_id': (DEFAULT_FEED_ID if is_from_PI else FEED_ID),
            'shop_id': SHOP_ID,
            'shop_name': SHOP_NAME,
            'binary_price': binary_price,
            'vendor_code': 'vendor code from offer',
        }

        if not is_from_PI:
            for param in ('barcode', 'description', 'title', 'shop_sku'):
                if param in offer:
                    expected_offer[param] = offer[param]

            if 'downloadable' in offer:
                expected_offer['downloadable'] = offer['downloadable'] == 'true'

            if 'picture' in offer:
                expected_offer['picture_url'] = offer['picture'].replace('http://', '')

        checked.append(expected_offer)

    for offer in offers_from_pi:
        if offer['id'] == 'TestOfferFromPI':
            create_expected_offer(offer, is_from_PI=True)

    assert_that(len(offers_processor_workflow.genlog), equal_to(len(offers_from_pi) - 1))
    matchers = [
        HasGenlogRecordRecursive(offer) for offer in checked
    ]
    offers_processor_workflow.verify(matchers)


def test_delivery_options(offers_processor_workflow, offers_from_pi):
    """
    Проверяем, что для оферов из хранилища правильно прокидываются опции доставки
    """
    checked = []
    for offer in offers_from_pi:
        if 'delivery-options' in offer:
            checked.append({
                'offer_id': offer['id'],
                'offer_delivery_options': has_items(*[has_properties({
                    'Cost': float(option['cost']),
                }) for option in offer['delivery-options']]),
            })

    assert_that(offers_processor_workflow, all_of(*[HasGenlogRecord(offer) for offer in checked]))


def test_pickup_options(offers_processor_workflow, offers_from_pi):
    """
    Проверяем, что для оферов из хранилища правильно прокидываются опции самовывоза
    """
    checked = []
    for offer in offers_from_pi:
        if 'pickup-options' in offer:
            checked.append({
                'offer_id': offer['id'],
                'pickup_options': has_items(*[has_properties({
                    'Cost': float(option['cost']),
                }) for option in offer['pickup-options']]),
            })

    assert_that(offers_processor_workflow, all_of(*[HasGenlogRecord(offer) for offer in checked]))


def test_picture_matching(offers_processor_workflow):
    """
    Проверяем, что поле картинок прикрепилось
    """
    expected_record = {
        'offer_id': OFFER_WITH_PIC,
        'picture_crcs': ['thisiscrc']
    }
    expected_pictures = {
        'width': 800,
        'height': 600,
    }
    assert_that(
        offers_processor_workflow,
        HasGenlogPicturesRecord(genlog_fields=expected_record, picture_fields=expected_pictures)
    )


def test_offers_age(offers_processor_workflow, offers_from_pi):
    """
    Проверяем, что возрастная категория прокинулась в индекс
    """
    checked = []

    for offer in offers_from_pi:
        if 'age' in offer:
            checked.append({
                'offer_id': offer['id'],
                'age': offer['age']['value'],
                'age_unit': offer['age']['unit'],
            })

    assert_that(offers_processor_workflow, all_of(*[HasGenlogRecord(offer) for offer in checked]))
