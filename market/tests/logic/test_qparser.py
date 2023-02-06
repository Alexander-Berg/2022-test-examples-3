# coding: utf-8

import pytest
import os

from datetime import datetime
from google.protobuf.json_format import MessageToDict
from google.protobuf.timestamp_pb2 import Timestamp
from hamcrest import assert_that, is_, equal_to

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import UpdateMeta
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp

from market.proto.SessionMetadata_pb2 import Feedparser


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100
REAL_FEED_ID = 736
TIMESTAMP = create_pb_timestamp(100500)


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
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def categories_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic, categories_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic,
            'categories_topic': categories_topic.topic,
            'categories_batch_size': 1,
            'categories_in_dedicated_topic': True,
        },
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


@pytest.yield_fixture(scope='module')
def qp_runner(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
    )


@pytest.fixture(scope='module')
def push_parser(monkeymodule, config, qp_runner):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser(push_parser, input_topic, output_topic, mds, datacamp_output_topic, categories_topic, config, qp_runner):
    date = datetime.utcnow().replace(microsecond=0)
    yml_date = date.strftime('%Y-%m-%d %H:%M:%SZ')
    yml_platform = 'market test YML from_dicts'
    yml_version = 'test version 2.1'
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        offer_count=5,
        is_advanced_blue=True,
        shop_dict={
            'date': yml_date,
            'platform': yml_platform,
            'version': yml_version,
        }
    )

    input_topic.write(make_input_task(mds, FEED_ID, BUSINESS_ID, SHOP_ID, timestamp=TIMESTAMP, real_feed_id=REAL_FEED_ID).SerializeToString())

    push_parser.run(total_sessions=1)

    data = output_topic.read(count=5)

    categories_data = categories_topic.read(1)

    ts_expected = Timestamp()
    ts_expected.FromDatetime(date)
    META = UpdateMeta(
        source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
        applier=DataCampOffer_pb2.QPARSER,
        timestamp=ts_expected)

    assert_that(categories_data, HasSerializedDatacampMessages([
        {
            'partner_categories': [{
                'categories': [
                    {
                        'business_id': BUSINESS_ID,
                        'id': 1,
                        'name': 'root category',
                        'meta': META,
                    },
                ]
            }]
        },
    ]))

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                    },
                    'meta': {
                        'scope': DataCampOffer_pb2.BASIC,
                        'ts_created': ts_expected,
                    }
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'real_feed_id': REAL_FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                        },
                        'stock_info': {
                            'partner_stocks_default': {
                                'count': offer_id + 1,
                                'meta': META,
                            }
                        }
                    }
                })
            }]
        }] for offer_id in range(5)
    }]))

    fp_quick_metadata_path = os.path.join(
        qp_runner.qp_env.output_dir,
        qp_runner.qp_env.feed_cfg.options['fp_metadata']['filename']
    )
    assert_that(os.path.exists(fp_quick_metadata_path), is_(True))

    with open(fp_quick_metadata_path, "rb") as proto:
        quick_feedparser = Feedparser()
        quick_feedparser.ParseFromString(proto.read())
        assert_that(
            MessageToDict(quick_feedparser, preserving_proto_field_name=True),
            equal_to({
                'platform': yml_platform,
                'version': yml_version,
                'deduplication_time': 0,
                'deduplication_request_time': 0,
                'offers_with_shop_sku': 5,
                'offers_with_shop_sku_and_offer_id': 5,
                'offers_with_shop_sku_equals_offer_id': 5,
                'parse_stats': {
                    'total_offers': 5,
                    'valid_offers': 5,
                    'error_offers': 0,
                },
                'deduplication_stats': {
                    'original_partner_content_updated': 0,
                    'actual_partner_content_updated': 0,
                    'original_terms_updated': 0,
                    'binding_updated': 0,
                    'partner_info_updated': 0,
                    'partner_stocks_updated': 0,
                    'partner_stocks_default_updated': 0,
                    'status_updated': 0,
                    'delivery_updated': 0,
                    'price_updated': 0,
                    'bids_updated': 0,
                    'pictures_updated': 0,
                    'resolution_updated': 0,
                    'meta_updated': 0,
                    'handler_failed': 0,
                    'yt_request_failed': 0,
                    'skipped_by_binding': 0,
                    'new_offers': 0,
                    'total_sent': 5
                },
                'yml_date': yml_date,
            })
        )
