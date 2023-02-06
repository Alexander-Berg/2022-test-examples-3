# coding: utf-8

import pytest
from hamcrest import assert_that, has_items

from market.idx.datacamp.parser.yatf.env import make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.env import WorkersEnv as ParserWorkersEnv
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
    ShopsDatParameters,
)
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta, create_price
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
)
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 123
OFFERS_COUNT = 1
WAREHOUSE_ID = 20

PICTURE_ORIGINAL_URL = "www.1.ru/pic/PIC1.jpg"
NS_YABS_PERFORMANCE = 'yabs_performance'


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


@pytest.fixture(scope='module')
def bannerland_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'bannerland_topic')
    return topic


@pytest.fixture(scope='module')
def piper_config(yt_server, log_broker_stuff, parser_piper_topic, bannerland_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': parser_piper_topic.topic,
            'bannerland_topic': bannerland_topic.topic,
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
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(0),
                        source=[
                            DTC.SourcePicture(url=PICTURE_ORIGINAL_URL)
                        ],
                    ),
                    actual={
                        PICTURE_ORIGINAL_URL: DTC.MarketPicture(
                            namespace=NS_YABS_PERFORMANCE,
                            status=DTC.MarketPicture.Status.AVAILABLE
                        )
                    },
                    multi_actual={
                        PICTURE_ORIGINAL_URL: DTC.NamespacePictures(
                            by_namespace={
                                NS_YABS_PERFORMANCE: DTC.MarketPicture(
                                    namespace=NS_YABS_PERFORMANCE,
                                    status=DTC.MarketPicture.Status.AVAILABLE
                                )
                            }
                        )
                    }
                )
            ),
        ))
    ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, piper_config):
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
                color=DTC.DIRECT_STANDBY,
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
                color=DTC.DIRECT_STANDBY,
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
    bannerland_topic
):
    resources = {
        'config': piper_config,
        'offers_topic': parser_piper_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'bannerland_topic': bannerland_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        wait_until(lambda: piper_env.united_offers_processed >= OFFERS_COUNT)
        yield piper_env


@pytest.fixture(scope='module')
def parser_config(tmpdir_factory, log_broker_stuff, yt_server, parser_piper_topic, parser_input_topic, datacamp_output_topic):
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
def qp_runner(parser_config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=parser_config,
        feed_info={
            'market_color': 'direct',
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
def parser_workflow(push_parser, parser_input_topic, mds):
    mds.generate_feed(
        FEED_ID,
        offer_count=OFFERS_COUNT,
        currencies=[{
            'id': 'RUB',
            'rate': '1',
        }],
        offers_dict={
            0: {
                'name': 'new_title'
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
        shops_dat_parameters=ShopsDatParameters(direct_standby=True)
    )

    parser_input_topic.write(task.SerializeToString())
    push_parser.run(total_sessions=1)


def test_parser_piper(parser_workflow, piper, bannerland_topic):
    # Проверяем внутренний стейт
    assert_that(piper.basic_offers_table.data,
                HasOffers([
                    message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, 0)
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'name': {
                                        'value': 'new_title'
                                    }
                                }
                            }
                        }
                    }, DTC.Offer())
                ]),
                'Missing offers')

    assert_that(piper.service_offers_table.data,
                HasOffers([
                    message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, 0),
                            'shop_id': SHOP_ID,
                            'extra': {
                                'original_offer_id': '{}xXx{}'.format(FEED_ID, 0)
                            }
                        },
                        'meta': {
                            'rgb': DTC.DIRECT_STANDBY
                        }
                    }, DTC.Offer())
                ]),
                'Missing offers')

    assert_that(piper.actual_service_offers_table.data,
                HasOffers([
                    message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, 0),
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                        },
                        'meta': {
                            'rgb': DTC.DIRECT_STANDBY
                        }
                    }, DTC.Offer())
                ]),
                'Missing offers')

    # Проверяем топик bannerland
    data = bannerland_topic.read(count=1, wait_timeout=5)

    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(ExportMessage, {
                'offer': {
                    'business_id': BUSINESS_ID,
                    'offer_id': '{}xXx{}'.format(FEED_ID, 0),
                    'original_offer_id': '{}xXx{}'.format(FEED_ID, 0),
                    'shop_id': SHOP_ID,
                    'original_content': {
                        'name': 'new_title'
                    }
                }
            })
        )
    )

    assert_that(bannerland_topic, HasNoUnreadData())
