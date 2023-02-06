# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to

from market.idx.datacamp.parser.yatf.env import make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.idx.pylibrary.datacamp.utils import parse_market_color
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.env import WorkersEnv as ParserWorkersEnv
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
    ShopsDatParameters,
)
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.yatf.utils import create_meta, create_price
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
)
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 10
SHOP_ID = 111  # дефолтный shop_id в генерируемом shops-dat для теста в фидпарсере
FEED_ID = 123
OFFERS_COUNT = 4
WAREHOUSE_ID = 20


@pytest.fixture(scope='module')
def parser_input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def parser_piper_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module', params=['blue', 'white', 'turbo', 'direct'])
def color(request):
    return request.param


@pytest.fixture(scope='module')
def piper_config(yt_server, log_broker_stuff, parser_piper_topic, color):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': parser_piper_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.fixture(scope='module')
def mds(tmpdir_factory, piper_config):
    return FakeMds(tmpdir_factory.mktemp('mds'), piper_config)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, piper_config):
    return DataCampBasicOffersTable(yt_server, piper_config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 0),
                shop_id=SHOP_ID,
                feed_id=FEED_ID
            ),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.BASIC),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 1),
                shop_id=SHOP_ID,
                feed_id=FEED_ID
            ),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.BASIC),
        )),
    ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, piper_config, color):
    return DataCampServiceOffersTable(yt_server, piper_config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 0),
                shop_id=SHOP_ID,
                feed_id=FEED_ID
            ),
            meta=create_meta(
                10,
                color=parse_market_color(color) if color != 'direct' else DTC.DIRECT_STANDBY,
                scope=DTC.SERVICE
            ),
            price=create_price(100, 200)
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 1),
                shop_id=SHOP_ID,
                feed_id=FEED_ID
            ),
            meta=create_meta(
                10,
                color=parse_market_color(color) if color != 'direct' else DTC.DIRECT_STANDBY,
                scope=DTC.SERVICE
            ),
            price=create_price(100, 200)
        ))
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, piper_config):
    return DataCampServiceOffersTable(yt_server, piper_config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 0),
                shop_id=SHOP_ID,
                warehouse_id=WAREHOUSE_ID,
            ),
            meta=create_meta(
                10,
                color=parse_market_color(color) if color != 'direct' else DTC.DIRECT_STANDBY,
                scope=DTC.SERVICE
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 1),
                shop_id=SHOP_ID,
                warehouse_id=WAREHOUSE_ID,
            ),
            meta=create_meta(
                10,
                color=parse_market_color(color) if color != 'direct' else DTC.DIRECT_STANDBY,
                scope=DTC.SERVICE
            ),
        ))
    ])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    piper_config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    parser_piper_topic,
    color
):
    resources = {
        'config': piper_config,
        'offers_topic': parser_piper_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        wait_until(lambda: piper_env.united_offers_processed >= OFFERS_COUNT)
        yield piper_env


@pytest.fixture(scope='module')
def parser_config(tmpdir_factory, log_broker_stuff, yt_server, parser_piper_topic, parser_input_topic, datacamp_output_topic, color):
    cfg = {
        'logbroker_in': {
            'topic': parser_input_topic.topic,
        },
        'logbroker': {
            'topic': parser_piper_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
        }
    }

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.yield_fixture(scope='module')
def qp_runner(parser_config, log_broker_stuff, color):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=parser_config,
        feed_info={
            'market_color': color,
        },
    )


@pytest.fixture(scope='module')
def push_parser(monkeymodule, parser_config, qp_runner):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield ParserWorkersEnv(
            config=parser_config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture(scope='module')
def parser_workflow(push_parser, parser_input_topic, mds, color):
    mds.generate_feed(
        FEED_ID,
        offer_count=OFFERS_COUNT,
        currencies=[{
            'id': 'RUB',
            'rate': '1',
        }],
        offers_dict={
            0: {
                'video': 'https://test.yandex.video/',
            },
            1: {
                'video': 'https://test.yandex.video/',
                'price': '-1.0',
            },
            2: {
                'video': 'https://test.yandex.video/',
            },
            3: {
                'video': 'https://test.yandex.video/',
            }
        }
    )

    task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        WAREHOUSE_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        shops_dat_parameters=None if color != 'direct' else ShopsDatParameters(direct_standby=True)
    )

    parser_input_topic.write(task.SerializeToString())
    push_parser.run(total_sessions=1)


def test_parser_piper(parser_workflow, piper, color):
    """
    Добавляем задание на парсинг фида во входной топик парсера.
    Проверяем, что фид успешно распарсится и офера добавятся во входной топик контроллера,
    после чего контроллер должен успешно прочитать эти офера и добавить в соответствующую таблицу в ыте
    """
    assert_that(len(piper.basic_offers_table.data), equal_to(OFFERS_COUNT), 'Too few offers in table')
    assert_that(len(piper.service_offers_table.data), equal_to(OFFERS_COUNT), 'Too few offers in table')
    assert_that(len(piper.actual_service_offers_table.data), equal_to(OFFERS_COUNT), 'Too few offers in table')

    assert_that(piper.basic_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'shop_id': SHOP_ID,
                        },
                        'pictures': {
                            'videos': {
                                'source': {
                                    'meta': {
                                        'source': 'PUSH_PARTNER_FEED',
                                        'applier': 'QPARSER'
                                    },
                                    'videos': [{
                                        'url': 'https://test.yandex.video/'
                                    }]
                                }
                            }
                        }
                    }, DTC.Offer()) for offer_id in range(4)]),
                'Missing offers')

    assert_that(piper.service_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                            'shop_id': SHOP_ID,
                        },
                        'meta': {
                            'rgb': parse_market_color(color) if color != 'direct' else DTC.DIRECT_STANDBY
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': False,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_FEED,
                                    }
                                }
                            ],
                            'is_sampled_for_redirect': {
                                'flag': True,
                            }
                        },
                    }, DTC.Offer()) for offer_id in range(4)]),
                'Missing offers')

    assert_that(piper.actual_service_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                        },
                        'meta': {
                            'rgb': parse_market_color(color) if color != 'direct' else DTC.DIRECT_STANDBY
                        },
                        'status': {
                            'disabled': [
                                {
                                    # 0-й оффер ок
                                    # 1-й оффер задизейблен из-за цены
                                    # 2-й и 3-й - как только что созданные
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.MARKET_IDX,
                                    }
                                }
                            ]
                        } if offer_id > 0 else {}
                    }, DTC.Offer()) for offer_id in range(4)]),
                'Missing offers')
