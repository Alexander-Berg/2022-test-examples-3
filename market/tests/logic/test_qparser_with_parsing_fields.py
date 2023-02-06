# coding: utf-8

import pytest
import os

from datetime import datetime
from google.protobuf.json_format import MessageToDict
from google.protobuf.timestamp_pb2 import Timestamp
from hamcrest import assert_that, is_, equal_to, not_

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.proto.SessionMetadata_pb2 import Feedparser


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100
REAL_FEED_ID = 736
TIMESTAMP = create_pb_timestamp(100500)
YML_PLATFORM = 'market test YML from_dicts'
YML_VERSION = 'test version 2.1'


def yml_fields():
    # offer/basic/content/partner/original
    return [
        ('description', 'offer description'),
        ('barcode', '12345'),
        ('vendor', 'offer vendor'),
        ('name', 'offer name'),
        ('manufacturer_warranty', '1'),
        ('condition', {
            'type': 'likenew',
            'reason': 'some reason'
        })
    ]


def create_yml_offer_data():
    offer = {
        'id': 'offerId',
        'shop-sku': 'offerId'
    }
    for (name, value) in yml_fields():
        offer[name] = value
    return offer


def create_csv_offer_data():
    return {
        'id': 'offerId',
        'shop-sku': 'offerId',
        'description': 'offer description',
        'barcode': '12345',
        'vendor': 'offer vendor',
        'name': 'offer name',
        'manufacturer_warranty': '1',
        'condition-reason': 'some reason',
        'condition-type': 'likenew',
    }


def create_meta(date):
    meta = {
        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
        'applier': DataCampOffer_pb2.QPARSER
    }
    if date is not None:
        ts_expected = Timestamp()
        ts_expected.FromDatetime(date)
        meta['timestamp'] = ts_expected
    return meta


def create_offer_with_field(field_name, date):
    meta = create_meta(date)
    return [{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'offerId',
                    },
                    'content': {
                        'partner': {
                            'original': {
                                field_name: {
                                    'meta': meta
                                }
                            }
                        }
                    }
                },
            }]
        }]
    }]


def create_offer_with_cpa(date):
    meta = create_meta(date)
    return [{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'offerId',
                    },
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'status': {
                            'original_cpa': {
                                'meta': meta
                            },
                        },
                    },
                })
            }]
        }]
    }]


def check_has_field_cpa(data, date):
    assert_that(data, HasSerializedDatacampMessages(create_offer_with_cpa(date)))


def check_has_not_field_cpa(data, date):
    assert_that(data, not_(HasSerializedDatacampMessages(create_offer_with_cpa(date))))


def check_has_field(data, field_name, date=None):
    assert_that(data, HasSerializedDatacampMessages(create_offer_with_field(field_name, date)))


def check_has_not_field(data, field_name, date=None):
    assert_that(data, not_(HasSerializedDatacampMessages(create_offer_with_field(field_name, date))))


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
def qp_runner_yml(config, log_broker_stuff):
    yield QParserTestLauncherMock(log_broker_stuff=log_broker_stuff, config=config)


@pytest.yield_fixture(scope='module')
def qp_runner_csv(config, log_broker_stuff):
    yield QParserTestLauncherMock(log_broker_stuff=log_broker_stuff, config=config, feed_format='csv')


@pytest.fixture()
def push_parser_yml(monkeymodule, config, qp_runner_yml):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_yml.process_task)
        yield WorkersEnv(config=config, parsing_service=UpdateTaskServiceMock)


@pytest.fixture()
def push_parser_csv(monkeymodule, config, qp_runner_csv):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_csv.process_task)
        yield WorkersEnv(config=config, parsing_service=UpdateTaskServiceMock)


def test_yml_qparser_without_parsing_fields(push_parser_yml, input_topic, output_topic, mds, datacamp_output_topic, categories_topic, config, qp_runner_yml):
    '''Проверяем, что если не задавать parsingFields при передаче yml фида,
    то парсер обрабатывает все переданные поля из оффера и из shop_dict'''
    date = datetime.utcnow().replace(microsecond=0)
    yml_date = date.strftime('%Y-%m-%d %H:%M:%SZ')

    mds.generate_feed(
        FEED_ID,
        force_offers=[create_yml_offer_data()],
        shop_dict={
            'date': yml_date,
            'platform': YML_PLATFORM,
            'version': YML_VERSION,
            'adult': '1',
            'cpa': '1',
        }
    )

    input_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE,
        timestamp=TIMESTAMP,
        real_feed_id=REAL_FEED_ID,
        parsing_fields=[]
    )
    input_topic.write(input_task.SerializeToString())
    push_parser_yml.run(total_sessions=1)
    data = output_topic.read(count=1)
    for (name, value) in yml_fields():
        check_has_field(data, name, date)
    check_has_field(data, 'adult', date)
    check_has_field_cpa(data, date)


def test_csv_qparser_without_parsing_fields(push_parser_csv, input_topic, output_topic, mds, datacamp_output_topic, categories_topic, config, qp_runner_csv):
    '''Проверяем, что если не задавать parsingFields при передаче csv фида,
    то парсер обрабатывает все переданные поля из оффера и из shop_dict'''
    mds.generate_feed(FEED_ID, force_offers=[create_csv_offer_data()], is_csv=True)

    input_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE,
        timestamp=TIMESTAMP,
        real_feed_id=REAL_FEED_ID,
        parsing_fields=[]
    )
    input_topic.write(input_task.SerializeToString())
    push_parser_csv.run(total_sessions=1)
    data = output_topic.read(count=1)
    for (name, value) in yml_fields():
        check_has_field(data, name)


def test_yml_qparser_with_parsing_fields(push_parser_yml, input_topic, output_topic, mds, datacamp_output_topic, categories_topic, config, qp_runner_yml):
    '''Проверяем, что при передаче yml фида:
    - при указании тегов в parsing_fields парсятся только они и тег shop_sku
    shop_sku нужно обрабатывать независимо от того, указан ли он в parsingFields, т.к он перетирает id если его задать
    Если этого не сделать, то получим одинаковые оффера с разными идентификаторами в случае ошибки партнёра
    - Если передать значение через shop_info и добавить его в parsing_fields, то оно будет обработано
    - Если передать значение через shop_info и НЕ добавить в parsing_fields, то оно НЕ будет обработано'''
    date = datetime.utcnow().replace(microsecond=0)
    yml_date = date.strftime('%Y-%m-%d %H:%M:%SZ')

    mds.generate_feed(
        FEED_ID,
        force_offers=[create_yml_offer_data()],
        shop_dict={
            'date': yml_date,
            'platform': YML_PLATFORM,
            'version': YML_VERSION,
            'adult': '1',
            'cpa': '1',
        }
    )

    parsing_fields = {'description', 'manufacturer_warranty', 'adult'}
    input_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE,
        timestamp=TIMESTAMP,
        real_feed_id=REAL_FEED_ID,
        parsing_fields=list(parsing_fields)
    )
    input_topic.write(input_task.SerializeToString())

    push_parser_yml.run(total_sessions=1)
    data = output_topic.read(count=1)

    for (name, value) in yml_fields():
        if name in parsing_fields:
            check_has_field(data, name, date)
        else:
            check_has_not_field(data, name, date)
    check_has_field(data, 'adult', date)
    check_has_not_field_cpa(data, date)

    fp_quick_metadata_path = os.path.join(
        qp_runner_yml.qp_env.output_dir,
        qp_runner_yml.qp_env.feed_cfg.options['fp_metadata']['filename']
    )
    assert_that(os.path.exists(fp_quick_metadata_path), is_(True))

    with open(fp_quick_metadata_path, "rb") as proto:
        quick_feedparser = Feedparser()
        quick_feedparser.ParseFromString(proto.read())
        d = MessageToDict(quick_feedparser, preserving_proto_field_name=True)
        assert_that(d['offers_with_shop_sku'], equal_to(1))
        assert_that(d['offers_with_shop_sku_and_offer_id'], equal_to(1))
        assert_that(d['offers_with_shop_sku_equals_offer_id'], equal_to(1))


def test_csv_qparser_with_parsing_fields(push_parser_csv, input_topic, output_topic, mds, datacamp_output_topic, categories_topic, config, qp_runner_csv):
    '''Проверяем, что при передаче csv фида и указании тегов в parsing_fields парсятся только они и тег shop_sku'''
    mds.generate_feed(FEED_ID, force_offers=[create_csv_offer_data()], is_csv=True)

    parsing_fields = {'description', 'manufacturer_warranty', 'condition'}
    input_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE,
        timestamp=TIMESTAMP,
        real_feed_id=REAL_FEED_ID,
        parsing_fields=list(parsing_fields)
    )
    input_topic.write(input_task.SerializeToString())

    push_parser_csv.run(total_sessions=1)
    data = output_topic.read(count=1)

    check_has_field(data, 'description')
    check_has_field(data, 'manufacturer_warranty')
    # condition не задаётся в create_csv_offer_data, но он есть из за задания полей 'condition-reason' и 'condition-type'
    # в yml это дочерние теги, а в csv это колонки
    check_has_field(data, 'condition')
    check_has_not_field(data, 'barcode')
    check_has_not_field(data, 'vendor')
    check_has_not_field(data, 'name')

    fp_quick_metadata_path = os.path.join(
        qp_runner_csv.qp_env.output_dir,
        qp_runner_csv.qp_env.feed_cfg.options['fp_metadata']['filename']
    )
    assert_that(os.path.exists(fp_quick_metadata_path), is_(True))

    with open(fp_quick_metadata_path, "rb") as proto:
        quick_feedparser = Feedparser()
        quick_feedparser.ParseFromString(proto.read())
        d = MessageToDict(quick_feedparser, preserving_proto_field_name=True)
        assert_that(d['offers_with_shop_sku'], equal_to(1))
        assert_that(d['offers_with_shop_sku_and_offer_id'], equal_to(1))
        assert_that(d['offers_with_shop_sku_equals_offer_id'], equal_to(1))
