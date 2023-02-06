# coding: utf-8

import pytest
from hamcrest import assert_that, empty
from datetime import datetime

from google.protobuf.timestamp_pb2 import Timestamp

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.fake_stroller import StrollerServer
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.proto.offer.TechCommands_pb2 import COMPLETE_FEED_FINISHED
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from hamcrest import anything


SHOP_ID = 111
BUSINESS_ID = 3824
COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE = 10

NOW = datetime.utcnow()
TIMESTAMP = Timestamp()
TIMESTAMP.FromDatetime(NOW)


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


@pytest.fixture(
    params=[
        True,
        False
    ],
    ids=[
        'blue',
        'white'
    ]
)
def is_blue(request):
    return request.param


@pytest.fixture(
    params=[
        # yml complete-фид количество офферов в котором совпадает с размером батча команды конца
        {
            'feed_id': 1100,
            'warehouse_id': 145,
            'offer_count': COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE,
        },
        # yml complete-фид количество офферов у которого меньше размера батча команды конца
        {
            'feed_id': 4400,
            'warehouse_id': 147,
            'offer_count': COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE // 2,
        },
        # yml complete-фид количество офферов в котором больше размера батча команды конца
        {
            'feed_id': 5500,
            'warehouse_id': 148,
            'offer_count': COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE * 3 + 1,
        },
    ],
    ids=[
        'complete_feed_num_offers_equal_to_batch',
        'complete_feed_num_offers_less_than_batch',
        'complete_feed_num_offers_greater_than_batch',
    ]
)
def feed_info(request):
    return request.param


@pytest.yield_fixture()
def stroller():
    stroller = StrollerServer()
    # У магазина уже есть дерево "корень -> category 100 -> category 3"
    # 'category 3' встретится и в csv фиде, ее описание должно будет взяться из этой yml иерархии,
    # и она не должна посылаться в Хранилище, т.к уже там есть
    stroller.add_categories(BUSINESS_ID, [{'id': '500', 'name': 'category 3', 'parentId': '100'},
                                          {'id': '100', 'name': 'category 100'}])
    stroller.start_server()
    return stroller


@pytest.yield_fixture()
def qp_runner(config, log_broker_stuff, datacamp_output_topic, is_blue, stroller):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'logbroker': {
                'complete_feed_finish_command_batch_size': COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
            },
            'stroller': {
                'host': stroller.host,
                'port': stroller.port,
            }
        },
        color='blue' if is_blue else 'white',
        feed_format='csv',
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def create_expected_tech_commands(feed_id, supplemental_id, timestamp, offers):
    batches = []
    for i in range(0, len(offers), COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE):
        batches.append(offers[i:i + COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE])

    if len(batches) == 0:
        # if there is no offers in feed it means that partner wants to disable all his offers
        # that would be one command to finish complete feed without untouchable offers
        batches.append([])

    expected_tech_commands = []
    for i, batch in enumerate(batches):
        tech_command = {
            'command_type': COMPLETE_FEED_FINISHED,
            'command_params': {
                'shop_id': SHOP_ID,
                'business_id': BUSINESS_ID,
                'supplemental_id': supplemental_id,
                'feed_id': feed_id,
                'complete_feed_command_params': {
                    'untouchable_offers': [
                        offer['offer_id'] for offer in batch if offer['feed_id'] == feed_id
                    ],
                    'default_offer_values': {
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                        'timestamp': {
                                            'seconds': timestamp.seconds,
                                        },
                                    },
                                }
                            ]
                        }
                    }
                }
            }
        }

        if i != 0:
            tech_command['command_params']['complete_feed_command_params'].update({'start_offer_id': batch[0]['offer_id']})

        if i != len(batches) - 1:
            tech_command['command_params']['complete_feed_command_params'].update({'last_offer_id': batches[i + 1][0]['offer_id']})

        expected_tech_commands.append(tech_command)

    return expected_tech_commands


def test_parsing_task_for_complete_feed(feed_info, push_parser, mds, input_topic, output_topic, datacamp_output_topic, is_blue):
    """
    Что проверяем - что задание на парсинг комплит фида будет корректно обработано.
    Все оффера будут отправлены в топик с сторону пайпера, а так же туда будет отправлена команда о завершении
    комплит фида.
    """
    offer_count = feed_info['offer_count']
    categories_count = 5
    feed_id = feed_info['feed_id']
    parsing_task_warehouse_id = feed_info['warehouse_id'] if is_blue else None  # от mbi для белого не получаем warehouse_id
    tech_commands_supplemental_id = feed_info['warehouse_id'] if is_blue else 0  # в команде завершения комплит фида для белого константа 0

    mds.generate_feed(feed_id, offer_count=offer_count, categories_count=categories_count, is_blue=is_blue, is_csv=True)

    feed_type = FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
    feed_parsing_task = make_input_task(
        mds,
        feed_id,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=parsing_task_warehouse_id,
        task_type=feed_type,
        timestamp=TIMESTAMP,
    )

    input_topic.write(feed_parsing_task.SerializeToString())

    push_parser.run(total_sessions=1)

    output = output_topic.read(count=offer_count)

    # для тестовых csv фидов задаются опции доставки
    pickup_options = {
        'options': [{
            'Cost': 50,
            'DaysMin': 1,
            'DaysMax': 1,
        }]
    }

    # для синих не заданы настройки pickup - заполняется только мета
    pickup_options_empty = {
        'options': empty(),
        'meta': {}
    }

    offers_categories_info = dict()
    for offer_id in range(offer_count):
        offers_categories_info[offer_id] = dict()
        category_info = offers_categories_info[offer_id]
        category_id = FakeMds.category_for_offer(categories_count, offer_id)
        category_info['name'] = 'category {}'.format(category_id)
        if category_id == 3:  # для нее строллер возвращает yml категорию
            category_info['id'] = 500
            category_info['parent_id'] = 100
            category_info['path_category_ids'] = '100\\500'
            category_info['path_category_names'] = 'category 100\\category 3'
        else:
            category_info['id'] = anything()  # генерятся из названия
            category_info['parent_id'] = None
            category_info['path_category_ids'] = anything()
            category_info['path_category_names'] = category_info['name']

    assert_that(output, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(feed_id, offer_id),
                            },
                            'content': {
                                'partner': {
                                    'original': {
                                        'category': {
                                            'id': offers_categories_info[offer_id]['id'],
                                            'name': offers_categories_info[offer_id]['name'],
                                            'parent_id': offers_categories_info[offer_id]['parent_id'],
                                            'path_category_ids': offers_categories_info[offer_id]['path_category_ids'],
                                            'path_category_names': offers_categories_info[offer_id]['path_category_names'],
                                        }
                                    }
                                }
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: {
                                'identifiers': {
                                    'feed_id': feed_id,
                                    'offer_id': '{}xXx{}'.format(feed_id, offer_id),
                                },
                                'delivery': {
                                    'partner': {
                                        'original': {
                                            'pickup_options': pickup_options_empty if is_blue else pickup_options
                                        }
                                    }
                                },
                                'status': {
                                    'united_catalog': {
                                        'flag': True,
                                    }
                                }
                            }
                        })
                    }
                ]
            }
        ]
    } for offer_id in range(offer_count)]))

    # test new topic
    expected = [{
        'feed_id': int(feed_id),
        'offer_id': '{}xXx{}'.format(feed_id, offer_id)
    } for offer_id in range(offer_count)]
    expected_commands = create_expected_tech_commands(
        feed_id=feed_id,
        supplemental_id=tech_commands_supplemental_id,
        timestamp=TIMESTAMP,
        offers=sorted(expected, key=lambda x: x['offer_id'])
    )

    # в FakeMds для csv фидов генерится три категории, и для тестов по-умолчанию в батче по 1-й категории
    expected_categories_messages_count = 3 - 1  # количество категорий в фиде, минус yml категория 'category 3' из выдачи строллера
    datacamp_output_topic_messages = datacamp_output_topic.read(count=len(expected_commands) + expected_categories_messages_count)

    # check categories https://st.yandex-team.ru/MARKETINDEXER-36993
    category_msg = DatacampMessage()
    categories_names = set()
    for i in range(0, expected_categories_messages_count):
        category_msg.ParseFromString(datacamp_output_topic_messages[i])
        category = category_msg.partner_categories[0].categories[0]
        # id сгенерилось из имени, его не будем проверять
        assert not category.HasField('parent_id')
        categories_names.add(category.name)
        assert category.business_id == BUSINESS_ID
        assert category.auto_generated_id
    assert categories_names == {'category 2', 'category 4'}  # 'category 3' не посылается, т.к она уже есть в yml категориях

    # check that command was created with correct structure
    assert_that(datacamp_output_topic_messages[expected_categories_messages_count:], HasSerializedDatacampMessages([{
        'tech_command': [
            expected_command
        ]
    } for expected_command in expected_commands]))
