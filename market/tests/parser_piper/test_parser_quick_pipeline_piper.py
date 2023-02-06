# coding: utf-8

from datetime import datetime
from hamcrest import assert_that, equal_to
import pytest
import six

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.yatf.utils import create_meta, create_price
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic

OFFER_ID = 'offer_id'
BUSINESS_ID = 10
SHOP_ID = 101
FEED_ID = 1001


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def quick_pipeline_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
        }
    }

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture()
def mds(tmpdir_factory, config):
    return FakeMds(tmpdir_factory.mktemp('mds'), config)


@pytest.yield_fixture()
def qp_runner_yml(config, log_broker_stuff, datacamp_output_topic, quick_pipeline_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_info={
            'market_color': 'white',
        },
        qparser_config={
            'logbroker': {
                'complete_feed_finish_command_batch_size': 1000,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
                'qoffers_quick_pipeline_messages_topic': quick_pipeline_topic.topic,
                'qoffers_quick_pipeline_messages_writers_count': 1
            },
            'feature': {
                "enable_quick_pipeline": True
            }
        },
    )


@pytest.fixture()
def push_parser_yml(monkeypatch, config, qp_runner_yml):
    with monkeypatch.context() as m:
        m.setattr(
            "market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task",
            qp_runner_yml.process_task
        )

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture(scope='module')
def rty_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'rty_topic')
    return topic


@pytest.fixture(scope='module')
def config_piper(yt_server, log_broker_stuff, quick_pipeline_topic, rty_topic):
    cfg = {
        'logbroker': {
            'quick_pipeline_topic': quick_pipeline_topic.topic,  # qoffers quick input topic
            'rty_topic': rty_topic.topic,             # output topic
        },
        'general': {
            'color': 'blue',
        }
    }
    return PiperConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config_piper):
    return DataCampBasicOffersTable(yt_server, config_piper.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID, feed_id=FEED_ID),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.SERVICE, ts_first_added=100500),
            price=create_price(100, 1)
        ))])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config_piper):
    return DataCampServiceOffersTable(yt_server, config_piper.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID, feed_id=FEED_ID),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.SERVICE, ts_first_added=100500),
            price=create_price(100, 200)
        ))])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config_piper):
    return DataCampServiceOffersTable(yt_server, config_piper.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID, feed_id=FEED_ID),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.SERVICE, ts_first_added=100500),
            price=create_price(100, 200)
        ))])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config_piper,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    quick_pipeline_topic,
    rty_topic
):
    resources = {
        'config': config_piper,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'quick_pipeline_topic': quick_pipeline_topic,
        'rty_topic': rty_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_qparser_quick_pipeline(piper, push_parser_yml, input_topic, rty_topic, output_topic, mds):
    # пишем во входной топик парсера
    # парсер пишет в быстрый топик для rty
    # читаем сообщение из rty топика piper'а
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        is_csv=False,
        offer_count=1,
        offers_dict={
            SHOP_ID: {
                'price': '110',
                'currencyId': 'USD',
                'weight': '1',
                'dimensions': '12211/1212/123123'
            }
        },
        is_advanced_blue=True,
        shop_dict={
            'name': six.ensure_text('Магазин  Audio-Video'),
            'company': 'Audio-Video',
            'date': str(datetime.now()),
        },
    )
    input_topic.write(
        make_input_task(
            mds,
            FEED_ID,
            BUSINESS_ID,
            SHOP_ID,
            task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
        ).SerializeToString()
    )
    push_parser_yml.run(total_sessions=1)
    data = rty_topic.read(count=1)
    assert_that(rty_topic, HasNoUnreadData())

    assert_that(len(data), equal_to(1))
