# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to, is_in, has_length

from concurrent.futures import TimeoutError

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.proto.offer.TechCommands_pb2 import COMPLETE_FEED_FINISHED
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import UpdateMeta
from market.idx.datacamp.proto.offer import DataCampOffer_pb2

from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp


SHOP_ID = 123
CATEGORIES_BATCH_SIZE = 10
OFFERS_BATCH_SIZE = 10
TIMESTAMP = create_pb_timestamp(100500)
CATEGORIES_DICT = FakeMds.generate_categories(categories_count=40)
META = UpdateMeta(source=DataCampOffer_pb2.PUSH_PARTNER_FEED, timestamp=TIMESTAMP, applier=DataCampOffer_pb2.QPARSER)
YML_META = UpdateMeta(source=DataCampOffer_pb2.PUSH_PARTNER_FEED, applier=DataCampOffer_pb2.QPARSER)
BUSINESS_ID = 12345


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


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, input_topic, output_topic, categories_topic, datacamp_output_topic):
    cfg = {
        'general': {
            'categories_batch_size': CATEGORIES_BATCH_SIZE,
        },
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic,
            'categories_topic': categories_topic.topic,
            'categories_in_dedicated_topic': True,
            'categories_topic_batch_size': CATEGORIES_BATCH_SIZE,
            'batch_size': OFFERS_BATCH_SIZE,
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


@pytest.fixture(
    # В xml фидах сначала идут батчи с категориями. Если последний батч оказался слишком маленьким,
    # то он будет посылаться после офферных батчей.
    # В csv фидах батчи идут вперемешку, но категорийный "хвост" тоже гарантированно будет в самом конце.
    params=[
        # yml фид, количество категорий в котором совпадает с размером батча
        {
            'feed_id': 100,
            'is_csv_feed': False,
            'feed_format': 'xml',
            'categories_count': CATEGORIES_BATCH_SIZE,
            'categories_batches_count': 1,
            'offers_count': 10,
        },
        # yml фид, количество категорий в котором меньше размера батча
        {
            'feed_id': 200,
            'is_csv_feed': False,
            'feed_format': 'xml',
            'categories_count': CATEGORIES_BATCH_SIZE // 2,
            'categories_batches_count': 1,
            'offers_count': 10,
        },
        # yml фид, количество категорий в котором кратно размеру батча
        {
            'feed_id': 300,
            'is_csv_feed': False,
            'feed_format': 'xml',
            'categories_count': 2 * CATEGORIES_BATCH_SIZE,
            'categories_batches_count': 2,
            'offers_count': 10,
        },
        # yml фид, количество категорий в котором не кратно размеру батча
        {
            'feed_id': 400,
            'is_csv_feed': False,
            'feed_format': 'xml',
            'categories_count': 2 * CATEGORIES_BATCH_SIZE + 1,
            'offers_batches_count': 1,
            'categories_batches_count': 3,
            'offers_count': 10,
        },
    ],
    ids=[
        'yml_feed_num_categories_equal_to_batch',
        'yml_feed_num_categories_less_than_batch',
        'yml_feed_num_categories_2_batch',
        'yml_feed_num_categories_21_batch',
    ]
)
def feed_info_yml(request):
    return request.param


@pytest.fixture(
    # В xml фидах сначала идут батчи с категориями. Если последний батч оказался слишком маленьким,
    # то он будет посылаться после офферных батчей.
    # В csv фидах батчи идут вперемешку, но категорийный "хвост" тоже гарантированно будет в самом конце.
    params=[

        # В csv нет отдельной выгрузки категорийного дерева, категории берутся из офферов.
        # TODO cейчас в fake_mds у офферов задаются всего три категории, надо научиться задавать
        # любое количество категорий, чтобы тоже проверять кратность размеру батча
        {
            'feed_id': 500,
            'is_csv_feed': True,
            'feed_format': 'csv',
            'categories_count': 3,
            'offers_batches_count': 1,
            'offers_count': 10,
            'categories_batches_count': 1,
        },
    ],
    ids=[
        'csv_feed_num_categories',
    ]
)
def feed_info_csv(request):
    return request.param


@pytest.yield_fixture()
def qp_runner(config, log_broker_stuff, datacamp_output_topic, categories_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_format='csv',
        qparser_config={
            'logbroker': {
                'complete_feed_finish_command_batch_size': 1000,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
                'categories_topic': categories_topic.topic,
                'categories_in_dedicated_topic': True,
                'categories_topic_batch_size': CATEGORIES_BATCH_SIZE,
                'categories_topic_writers_count': 1,
            }
        },
    )


@pytest.yield_fixture()
def qp_runner_yml(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'feature': {
                'enable_deduplicate_categories': True,
            }
        }
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture()
def push_parser_yml(monkeypatch, config, qp_runner_yml):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_yml.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_parsing_task_for_categories_yml(feed_info_yml, push_parser_yml,  mds, input_topic, output_topic, categories_topic, datacamp_output_topic):
    categories_count = feed_info_yml['categories_count']
    offers_count = feed_info_yml['offers_count']
    feed_id = feed_info_yml['feed_id']
    is_csv = feed_info_yml['is_csv_feed']
    categories_batches_count = feed_info_yml['categories_batches_count']
    mds.generate_feed(feed_id, offer_count=offers_count, categories_count=categories_count, is_csv=is_csv)

    feed_parsing_task = make_input_task(
        mds,
        feed_id,
        BUSINESS_ID,
        SHOP_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        timestamp=TIMESTAMP
    )

    input_topic.write(feed_parsing_task.SerializeToString())

    push_parser_yml.run(total_sessions=1)

    tech = datacamp_output_topic.read(1)
    tech_msg = DatacampMessage()
    tech_msg.ParseFromString(tech[0])
    assert_that(tech_msg.tech_command[0].command_type, equal_to(COMPLETE_FEED_FINISHED))
    offers = output_topic.read(offers_count)
    assert_that(offers, has_length(offers_count))

    categories = categories_topic.read(categories_batches_count)
    categories_msg = DatacampMessage()
    # подсчитываем количество обработанных категорий
    cat_count = categories_count
    for i in range(categories_batches_count):
        categories_msg.ParseFromString(categories[i])
        assert_that(categories_msg.partner_categories[0].categories, has_length(min(cat_count, CATEGORIES_BATCH_SIZE)))
        cat_count -= CATEGORIES_BATCH_SIZE

        for category in categories_msg.partner_categories[0].categories:
            gen_category = CATEGORIES_DICT[category.id]
            # TODO сделать отдельный матчер для категорий
            assert_that(str(category.id), equal_to(gen_category['id']))
            assert_that(category.name, equal_to(gen_category['value']))
            assert_that(str(category.parent_id), equal_to(gen_category['parentId']))
            assert_that(str(category.business_id), equal_to(str(BUSINESS_ID)))
            assert not category.auto_generated_id
            assert_that(category.meta.source, equal_to(YML_META.source))
            assert_that(category.meta.applier, equal_to(YML_META.applier))


def test_parsing_task_for_categories_csv(feed_info_csv, push_parser, mds, input_topic, output_topic, categories_topic, datacamp_output_topic):
    categories_count = feed_info_csv['categories_count']
    offers_count = feed_info_csv['offers_count']
    feed_id = feed_info_csv['feed_id']
    is_csv = feed_info_csv['is_csv_feed']
    categories_batches_count = feed_info_csv['categories_batches_count']
    mds.generate_feed(feed_id, offer_count=offers_count, categories_count=categories_count, is_csv=is_csv)

    feed_parsing_task = make_input_task(
        mds,
        feed_id,
        BUSINESS_ID,
        SHOP_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        timestamp=TIMESTAMP
    )

    input_topic.write(feed_parsing_task.SerializeToString())

    push_parser.run(total_sessions=1)

    tech = datacamp_output_topic.read(1)
    tech_msg = DatacampMessage()
    tech_msg.ParseFromString(tech[0])
    assert_that(tech_msg.tech_command[0].command_type, equal_to(COMPLETE_FEED_FINISHED))
    offers = output_topic.read(offers_count)
    offers_msg = DatacampMessage()
    offers_counter = 0
    for i in range(offers_count):
        offers_msg.ParseFromString(offers[i])
        offers_counter += 1

    assert_that(offers_counter, equal_to(offers_count))

    categories = categories_topic.read(categories_batches_count)
    categories_msg = DatacampMessage()
    # завяжемся на то, что fake_mds генерит для офферов категории 2 - 4
    generated_categories_names = {'category 2', 'category 3', 'category 4'}
    # подсчитываем количество обработанных категорий
    categories_msg.ParseFromString(categories[0])
    assert_that(categories_msg.partner_categories[0].categories, has_length(categories_count))

    for category in categories_msg.partner_categories[0].categories:
        assert_that(category.meta, equal_to(META))
        assert_that(category.auto_generated_id, True)
        assert_that(category.name, is_in(generated_categories_names))


def test_parsing_task_for_categories_with_duplicates(push_parser_yml,  mds, input_topic, output_topic, categories_topic, datacamp_output_topic):
    categories_count = 9
    offers_count = 5
    feed_id = 10
    is_csv = False
    categories_with_duplicates = dict()

    # "1", "2" - дубликаты, не будем сохнанять "2"
    # "4", "5", "8" - дубликаты, не будем сохнанять "5", "8"
    # у вершины "7" теперь будет родитель "4"
    categories_with_duplicates[0] = FakeMds.gen_category(id=0, name="root")
    categories_with_duplicates[1] = FakeMds.gen_category(id=1, parent_id=0, name="same1")
    categories_with_duplicates[2] = FakeMds.gen_category(id=2, parent_id=0, name="same1")
    categories_with_duplicates[3] = FakeMds.gen_category(id=3, parent_id=0, name="not same")
    categories_with_duplicates[4] = FakeMds.gen_category(id=4, parent_id=1, name="same2")
    categories_with_duplicates[5] = FakeMds.gen_category(id=5, parent_id=2, name="same2")
    categories_with_duplicates[6] = FakeMds.gen_category(id=6, parent_id=3, name="same2")
    categories_with_duplicates[7] = FakeMds.gen_category(id=7, parent_id=5, name="same2 child")
    categories_with_duplicates[8] = FakeMds.gen_category(id=8, parent_id=2, name="same2")

    mds.generate_feed(feed_id, offer_count=offers_count, categories_count=categories_count, is_csv=is_csv, categories=categories_with_duplicates)

    feed_parsing_task = make_input_task(
        mds,
        feed_id,
        BUSINESS_ID,
        SHOP_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        timestamp=TIMESTAMP
    )

    input_topic.write(feed_parsing_task.SerializeToString())

    push_parser_yml.run(total_sessions=1)

    tech = datacamp_output_topic.read(1)
    tech_msg = DatacampMessage()
    tech_msg.ParseFromString(tech[0])
    assert_that(tech_msg.tech_command[0].command_type, equal_to(COMPLETE_FEED_FINISHED))
    offers = output_topic.read(offers_count)
    assert_that(offers, has_length(offers_count))

    categories = categories_topic.read(1)
    categories_msg = DatacampMessage()
    categories_msg.ParseFromString(categories[0])

    expected_categories = [0, 1, 3, 4, 6, 7]

    def expected_remaining_id(id):
        if id == "2":
            return "1"
        if id == "5" or id == "8":
            return "4"
        return id

    assert_that(categories_msg.partner_categories[0].categories, has_length(categories_count - 3))  # 3 карегориии-дубля не будут сохранены

    for category in categories_msg.partner_categories[0].categories:
        assert_that(category.id, is_in(expected_categories))

        id = category.id
        gen_category = categories_with_duplicates[id]
        assert_that(str(category.id), equal_to(gen_category['id']))
        assert_that(category.name, equal_to(gen_category['value']))
        if 'parentId' in gen_category:
            assert_that(str(category.parent_id), equal_to(expected_remaining_id(gen_category['parentId'])))
        else:
            assert_that(category.id, equal_to(0))


def test_parsing_task_category_id_equals_parent_id(push_parser_yml,  mds, input_topic, output_topic, categories_topic):
    categories_count = 5
    offers_count = 5
    feed_id = 15
    is_csv = False
    categories_with_duplicates = dict()

    categories_with_duplicates[0] = FakeMds.gen_category(id=0, name="cat0")
    categories_with_duplicates[1] = FakeMds.gen_category(id=1, parent_id=0, name="cat1")
    categories_with_duplicates[2] = FakeMds.gen_category(id=2, parent_id=0, name="cat2")
    categories_with_duplicates[3] = FakeMds.gen_category(id=3, parent_id=3, name="cat3")
    categories_with_duplicates[4] = FakeMds.gen_category(id=4, parent_id=3, name="cat4")

    mds.generate_feed(feed_id, offer_count=offers_count, categories_count=categories_count, is_csv=is_csv, categories=categories_with_duplicates)

    feed_parsing_task = make_input_task(
        mds,
        feed_id,
        BUSINESS_ID,
        SHOP_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        timestamp=TIMESTAMP
    )

    input_topic.write(feed_parsing_task.SerializeToString())

    push_parser_yml.run(total_sessions=1)

    with pytest.raises(TimeoutError):
        categories_topic.read(count=1)

    with pytest.raises(TimeoutError):
        output_topic.read(count=1)
