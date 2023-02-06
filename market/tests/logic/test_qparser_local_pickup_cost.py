# coding: utf-8

import pytest
import six
from datetime import datetime
from hamcrest import assert_that, has_items

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import UpdateMeta
from market.idx.datacamp.proto.offer.OfferBlog_pb2 import OfferBlog

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp

from market.proto.common.common_pb2 import EComponent
import market.proto.common.process_log_pb2 as PL


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100
TIMESTAMP = create_pb_timestamp(100500)
META = UpdateMeta(
    source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
    applier='QPARSER',
    timestamp=TIMESTAMP
)


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
def blog_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic,
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
def qp_runner(config, log_broker_stuff, blog_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'blog': {
                'enable': True,
                'topic': blog_topic.topic,
                'log_level': 'message',
            }
        },
        feed_format='csv',
    )


@pytest.fixture(scope='module')
def push_parser(monkeymodule, config, qp_runner):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def create_offer(**kwargs):
    result = dict(kwargs)
    result.update({
        'available': '1',
        'delivery': '1',
        'pickup': '1',
        'store': '1',
        'url': 'https://mytestshop.ru/{}'.format(result['id']),
        'vendor': '{}'.format(result['id']),
        'name': 'offer {}'.format(result['id']),
        'category': 'Смартфоны',
        'currencyId': 'RUR',
        'picture': 'https://mytestshop.ru/{}/pic.jpg'.format(result['id']),
        'sales_notes': 'Цвет|серый фантом;',
        'manufacturer_warranty': '1',
    })
    return result


def test_qparser_csv_local_pickup_cost(push_parser, input_topic, output_topic, blog_topic, mds, datacamp_output_topic, config, qp_runner):
    """Тест проверяет, что csv фид с пустыми значениями в local-pickup-cost обработается нормально и ошибок 45s не будет"""
    business_id = 10000
    shop_id = 12345
    feed_id = 54321
    # Офер с валидными данными самовывоза
    offer_with_valid_pickups = {
        'id': 'valid-pickups',
        'local-pickup-days': '1',
        'local-pickup-cost': '100',
    }
    # Офер с валидным промежутком дней для самовывоза
    offer_with_valid_pickups_day_interval = {
        'id': 'valid-pickups-day-interval',
        'local-pickup-days': '2-4',
        'local-pickup-cost': '100',
    }
    # Офер с невалидным количество дней самовывоза
    offer_with_invalid_pickups_wrong_days = {
        'id': 'wrong-pickups-days',
        'local-pickup-days': 'text',
        'local-pickup-cost': '100',
    }
    # Офер с валидной с отрицительной ценной самовывоза (сохраним нормально, ошибка позже в майнере)
    offer_with_invalid_pickups_wrong_price = {
        'id': 'wrong-pickup-price',
        'local-pickup-days': '1',
        'local-pickup-cost': '-123',
    }
    # Валидный офер с большим промежутком дней самовывоза
    offer_with_valid_pickups_large_day_interval = {
        'id': 'large-day-interval',
        'local-pickup-days': '1-7',
        'local-pickup-cost': '200',
    }
    # Валидный офер без самовывоза
    offer_without_any_pickups = {
        'id': 'without-any-pickups',
    }
    # Валидный оффер с очень большой ценой (сохраним нормально, ошибка позже в майнере)
    offer_with_huge_local_cost = {
        'id': 'with-huge-local-cost',
        'local-pickup-days': '1',
        'local-pickup-cost': '100100100',
    }

    offers = [
        create_offer(**offer_with_valid_pickups),
        create_offer(**offer_with_valid_pickups_day_interval),
        create_offer(**offer_with_invalid_pickups_wrong_days),
        create_offer(**offer_with_invalid_pickups_wrong_price),
        create_offer(**offer_with_valid_pickups_large_day_interval),
        create_offer(**offer_without_any_pickups),
        create_offer(**offer_with_huge_local_cost),
    ]

    mds.generate_feed(
        feed_id,
        force_offers=offers,
        is_csv=True,
        shop_dict={
            'name': six.ensure_text('Магазин  Audio-Video'),
            'company': 'Audio-Video',
            'url': 'http://www.aydio-video.ru',
            'cpa': '1',
            'date': str(datetime.now()),
        },
        currencies=[
            {
                'id': 'RUB',
                'rate': '1',
            },
        ],
    )

    parsing_feed_task = make_input_task(
        mds,
        feed_id,
        business_id,
        shop_id,
        timestamp=TIMESTAMP,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
    )

    input_topic.write(parsing_feed_task.SerializeToString())

    push_parser.run(total_sessions=1)

    data = blog_topic.read(count=1)

    EXPECTED = [
        IsSerializedProtobuf(OfferBlog, {
            'identifiers': {
                'feed_id': feed_id,
                'shop_id': shop_id,
                'offer_id': 'wrong-pickups-days',
            },
            'errors': {
                'error': [{
                    'code': '45s',
                    'level': PL.ERROR,
                    'text': 'Invalid pickup-option days',
                    'namespace': PL.OFFER,
                    'source': EComponent.QPARSER,
                }]
            },
        }),
    ]

    assert_that(data, has_items(*EXPECTED))
