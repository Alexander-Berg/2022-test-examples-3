# coding: utf-8

import pytest
import six
from hamcrest import assert_that

from concurrent.futures import TimeoutError

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic

BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100

max_shop_sku_len = 80


@pytest.fixture(scope='module', params=['blue', 'white'])
def color(request):
    return request.param


@pytest.fixture(scope='module', params=[
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE
])
def feed_type(request):
    return request.param


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def output_topic(log_broker_stuff, color):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic, color):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic,
            'complete_feed_finish_command_batch_size': 1000,
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


@pytest.yield_fixture()
def qp_runner_yml(config, log_broker_stuff, color):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        color=color,
    )


@pytest.fixture()
def push_parser_yml(monkeypatch, config, qp_runner_yml):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_yml.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def _expected_offer(id, price):
    return {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': id,
                    },
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': id,
                        },
                        'price': {
                            'basic': {
                                'binary_price': {'id': 'RUR', 'price': price*10000000}
                            },
                        },
                    },
                })
            }]
        }
    ]}


def test_qparser_correct_work_with_ids(push_parser_yml, input_topic, output_topic, mds, feed_type, color):
    """
    Partner can use shop-sku or offer-id as offer identifier
    If there are both of them, should take shop-sku
    """
    is_blue = True if color == 'blue' else False
    warehouse_id = 145 if is_blue else 0
    mds.generate_feed(
        FEED_ID,
        is_blue=is_blue,
        force_offers=[
            # valid offer with good shop-sku and id (shop-sku has priority)
            {
                'id': 'offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1offer1',
                'price': '111',
                'shop-sku': 'goodshopsku',
            },
            # invalid offer with bad shop-sku and good id (shop-sku has priority)
            {
                'id': 'offer2',
                'price': '112',
                'shop-sku': 'bad shop sku'
            },
            # valid offer with id only
            {
                'id': 'offer3',
                'price': '113',
                'shop-sku': None,
            },
            # invalid offer with bad id
            {
                'id': 'offer 4',
                'price': '114',
                'shop-sku': None,
            },
            # valid offer with good shop-sku only
            {
                'id': None,
                'shop-sku': 'offer5',
                'price': '115',
            },
            # invalid offer with none id and none shop-sku
            {
                'id': None,
                'price': '116',
                'shop-sku': None,
            },
            # invalid offer with none id and bad shop-sku
            {
                'id': None,
                'price': '117',
                'shop-sku': six.ensure_text('Невалидный shop-sku117'),
            },
            # valid offer with none id and good shop-sku
            {
                'id': None,
                'price': '118',
                'shop-sku': six.ensure_text('Валидный-shop-sku118'),
            },
            # invalid offer with bad id and none shop-sku
            {
                'id': six.ensure_text('Невалидный id119'),
                'price': '119',
                'shop-sku': None,
            },
            # invalid offer with bad id and bad shop-sku
            {
                'id': six.ensure_text('Невалидный id120'),
                'price': '120',
                'shop-sku': six.ensure_text('Невалидный shop sku120'),
            },
            # valid offer with bad id and good shop-sku
            {
                'id': six.ensure_text('Невалидный id121'),
                'price': '121',
                'shop-sku': six.ensure_text('Валидный-shop-sku121'),
            },
            # valid offer with good id and none shop-sku
            {
                'id': six.ensure_text('Валидный-id122'),
                'price': '122',
                'shop-sku': None,
            },
            # invalid offer with good id and bad shop-sku
            {
                'id': six.ensure_text('Валидный-id123'),
                'price': '123',
                'shop-sku': six.ensure_text('Невалидный shop-sku123'),
            },
            # valid offer with good id and good shop-sku
            {
                'id': six.ensure_text('Валидный-id124'),
                'price': '124',
                'shop-sku': six.ensure_text('Валидный-shop-sku124'),
            },
            # valid offer with good id and shop-sku over 80 symbols (shop-sku has priority)
            {
                'id': six.ensure_text('Валидный-id125'),
                'price': '125',
                'shop-sku': six.ensure_text('Ю') * max_shop_sku_len,
            },
            # valid offer with id over 80 symbols and shop-sku (shop-sku has priority)
            {
                'id': six.ensure_text('Ю') * (max_shop_sku_len + 1),
                'price': '126',
                'shop-sku': six.ensure_text('Валидный-shop-sku126'),
            },
        ],
        is_advanced_blue=False,
        cpa=False,
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=warehouse_id,
        task_type=feed_type,
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser_yml.run(total_sessions=1)

    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='goodshopsku', price=111)]))
    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='offer3', price=113)]))
    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='offer5', price=115)]))
    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='Валидный-shop-sku118', price=118)]))
    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='Валидный-shop-sku121', price=121)]))
    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='Валидный-id122', price=122)]))
    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='Валидный-shop-sku124', price=124)]))
    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='Ю' * max_shop_sku_len, price=125)]))
    assert_that(output_topic.read(count=1), HasSerializedDatacampMessages([_expected_offer(id='Валидный-shop-sku126', price=126)]))

    with pytest.raises(TimeoutError):
        output_topic.read(count=1)
