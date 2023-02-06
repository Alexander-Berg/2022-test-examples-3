# coding: utf-8

from datetime import datetime
import pytest

from hamcrest import assert_that, equal_to

from market.idx.datacamp.controllers.piper.yatf.resources.config import PiperConfig
from market.idx.datacamp.controllers.piper.yatf.test_env_new import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.category.PartnerCategory_pb2 import PartnerCategory
from market.idx.pylibrary.datacamp.utils import wait_until

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampCategoriesRows
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.pylibrary.proto_utils import message_from_data


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)


def create_category(business_id, category_id):
    params = {
        'name': 'category {}'.format(category_id) if category_id is not None else 'category without id',
        'parent_id': 56789
    }
    if business_id is not None:
        params['business_id'] = business_id
    if category_id is not None:
        params['id'] = category_id
    input_category = params.copy()
    input_category['meta']= {'timestamp': current_time}
    output_category = params.copy()
    output_category['meta'] = {'timestamp': {'seconds': current_ts.seconds}}
    return {'input_category': input_category, 'output_category': output_category}


GOOD_CATEGORIES = [
    create_category(business_id=111, category_id=12345),
    create_category(business_id=111, category_id=0),  # для проверки, что 0 - валидный айдишник
]
BAD_CATEGORIES = [
    create_category(business_id=111, category_id=None),  # для проверки, что без category_id сообщение пропустится,
    # но при этом не потянет за собой и весь остальной батч
    create_category(business_id=None, category_id=1),  # аналогично для случая с отсутствием business_id
]
CATEGORIES = GOOD_CATEGORIES + BAD_CATEGORIES


@pytest.fixture(scope='module')
def categories():
    return [message_from_data(category['input_category'], PartnerCategory()) for category in CATEGORIES]


@pytest.fixture(scope='module')
def category_datacamp_messages():
    categories = []
    for category in CATEGORIES:
        categories.append(category['input_category'])
    return [message_from_data({'partner_categories': [{'categories': categories}]}, DatacampMessage())]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic):
    cfg = PiperConfig(log_broker_stuff, YtTokenResource().path, yt_server, 'white')

    cfg.update_properties({
        'yt_server': yt_server,
        'lb_host': log_broker_stuff.host,
        'lb_port': log_broker_stuff.port,
    })

    cfg.create_initializer()
    datacamp_unpacker = cfg.create_processor('DATACAMP_MESSAGE_UNPACKER')
    offer_format_validator = cfg.create_offer_format_validator()
    united_offers_subscribers_gateway = cfg.create_input_gateway()
    united_updater = cfg.create_united_updater(united_offers_subscribers_gateway)
    categories_storage_updater = cfg.create_categories_storage_updater()
    category_converter = cfg.create_processor('DATACAMP_MESSAGE_TO_CATEGORY_CONVERTER')
    tech_commands_processor = cfg.create_tech_commands_processor()

    cfg.create_links([
        (cfg.create_lb_reader(datacamp_messages_topic.topic), datacamp_unpacker),
        (offer_format_validator, united_updater),
        (category_converter, categories_storage_updater),
        (datacamp_unpacker, offer_format_validator),
        (cfg.create_lb_reader(datacamp_messages_topic.topic), datacamp_unpacker),
        (datacamp_unpacker, tech_commands_processor),
        (cfg.create_lb_reader(datacamp_messages_topic.topic), datacamp_unpacker),
        (datacamp_unpacker, category_converter),
    ])

    return cfg


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic):
    resources = {
        'piper_config': config,
        'datacamp_message_topic': datacamp_messages_topic,
    }
    options = {
        'tables': ['partners', 'categories'],
    }
    with PiperTestEnv(yt_server, log_broker_stuff, options, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_categories(category_datacamp_messages, piper, datacamp_messages_topic):
    """Проверяем, что Piper корректно обрабатывает категории и что они попадают в таблицу
    """
    for message in category_datacamp_messages:
        datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: piper.categories_processed >= len(category_datacamp_messages), timeout=60)

    assert_that(len(piper.categories_table.data), equal_to(len(GOOD_CATEGORIES)), 'Too few categories in table')
    assert_that(piper.categories_table.data,
                HasDatacampCategoriesRows(
                    [{
                        'business_id': category['output_category']['business_id'],
                        'category_id': category['output_category']['id'],
                        'content': IsSerializedProtobuf(PartnerCategory, category['output_category']),
                    } for category in GOOD_CATEGORIES]),
                'Missing categories')
