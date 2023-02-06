# coding: utf-8

import pytest

from hamcrest import assert_that, equal_to

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock, QParserFailedTestLauncher
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    FPR_SUCCESS,
    FPR_FAIL,
    FPR_RETRY,
    FATAL,
    OK,
    FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE
)
from market.idx.datacamp.proto.api.GeneralizedMessage_pb2 import GeneralizedMessage


BUSINESS_ID = 10
SHOP_ID = 111


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def technical_topic(log_broker_stuff):
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
def mbi_report_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server, input_topic, output_topic, technical_topic, mbi_report_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
        },
        'logbroker_technical': {
            'topic': technical_topic.topic,
        },
        'logbroker_mbi_reports': {
            'topic': mbi_report_topic.topic,
        },
    }

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture(scope='module')
def mds(tmpdir_factory, config):
    return FakeMds(tmpdir_factory.mktemp('mds'), config)


@pytest.yield_fixture(scope='module')
def qp_runner(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'explanation_log': {
                "enable": True,
                "filename": "feed_errors.pbuf.sn",
                "log_level": "message"
            },
            'partner_stats': {
                'enable': True,
                'filename': "partner_stats.pbuf.sn"
            },
        },
        color='white',
        feed_info={
            'cpa': 'REAL'
        }
    )


@pytest.yield_fixture(scope='module')
def qp_runner_csv(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'explanation_log': {
                "enable": True,
                "filename": "feed_errors.pbuf.sn",
                "log_level": "message"
            },
            'partner_stats': {
                'enable': True,
                'filename': "partner_stats.pbuf.sn"
            },
        },
        color='white',
        feed_info={
            'cpa': 'REAL'
        },
        feed_format='csv'
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.base_worker.BaseParsingTaskWorker.launch_parsing_process", qp_runner.launch_parsing_process)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture()
def push_parser_csv(monkeypatch, config, qp_runner_csv):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.base_worker.BaseParsingTaskWorker.launch_parsing_process", qp_runner_csv.launch_parsing_process)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture()
def push_parser_failed(monkeypatch, config):
    with monkeypatch.context() as m:
        failed_launcher = QParserFailedTestLauncher()
        m.setattr("market.idx.datacamp.parser.lib.base_worker.BaseParsingTaskWorker.launch_parsing_process", failed_launcher.launch_parsing_process)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_mbi_successful_report_to_mbi_about_parsing(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что ушел отчет в mbi об успешной обработке задания на парсинг"""
    mds.generate_feed(4000)
    task_success = make_input_task(mds, 4000, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_success.SerializeToString())
    push_parser.run(total_sessions=1)
    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'status': OK,
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': equal_to(task_success)
        }
    }))


def test_mbi_report_topic_without_timestamp(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что в MBI отправляется отчет об ошибочно сформированных заданиях без таймстемпов"""
    mds.generate_feed(3100)
    input_task_3100 = make_input_task(mds, 3100, BUSINESS_ID, SHOP_ID, timestamp=None)

    input_topic.write(input_task_3100.SerializeToString())
    push_parser.run(total_sessions=1)
    # проверяем, что ушел отчет в mbi о задании без ts
    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_FAIL,
            'feed_parsing_error_text': 'processing task has no timestamp and will be skipped',
            'feed_parsing_task': equal_to(input_task_3100)
        }
    }))


def test_mbi_report_topic_without_type(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что в MBI отправляется отчет об ошибочно сформированных заданиях без типа"""
    mds.generate_feed(3110)
    input_task_3110 = make_input_task(mds, 3110, BUSINESS_ID, SHOP_ID, task_type=None)

    input_topic.write(input_task_3110.SerializeToString())
    push_parser.run(total_sessions=1)
    # проверяем, что ушел отчет в mbi о задании без типа
    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_FAIL,
            'feed_parsing_error_text': 'processing task has no type and will be skipped',
            'feed_parsing_task': equal_to(input_task_3110),
        }
    }))


def test_mbi_report_topic_without_base_identifiers(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что в MBI отправляется отчет об ошибочно сформированных заданиях без базовых идентификаторов"""
    mds.generate_feed(3120)
    input_task_3120 = make_input_task(mds, 3120, None, SHOP_ID)

    input_topic.write(input_task_3120.SerializeToString())
    push_parser.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_FAIL,
            'feed_parsing_error_text': 'processing task has no business_id and will be skipped',
            'feed_parsing_task': equal_to(input_task_3120)
        }
    }))


def test_parsing_task_with_fatal_error(push_parser_failed, technical_topic, input_topic, mds, mbi_report_topic):
    """Проверяет, что задание, которое по какой-то причине не отработало будет отправлено на перепарсинг"""
    mds.generate_feed(2000)  # задание обудет обработано с какой-то фатальной ошибкой
    task_will_fail = make_input_task(mds, 2000, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser_failed.run(total_sessions=1)
    # задание, для которого не прошел парсинг должно быть отправлено на повторную обработку
    actual_task = technical_topic.read(count=1)

    assert_that(actual_task[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task': equal_to(task_will_fail)
    }))

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_RETRY,
            'feed_parsing_error_text': 'Test how we deal with very bad situations',
            'feed_parsing_task': equal_to(task_will_fail),
        }
    }))


def test_parsing_file_with_5xx_errors_from_qp(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками"""
    feed_id = 3000
    mds.generate_feed(feed_id, bad=True)  # задание обудет обработано с какой-то фатальной ошибкой
    task_will_fail = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'status': FATAL,
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'feed_parsing_error_messages': [
                {
                    'code': '550',
                }
            ]
        }
    }))


def test_531_CATEGORY_NAME_IS_EMPTY(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками"""
    feed_id = 3000
    categories = mds.generate_categories(5)
    categories[4]['value'] = ''
    mds.generate_feed(feed_id, categories=categories)
    task_will_fail = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'feed_parsing_error_messages': [
                {
                    'code': '531',
                }
            ]
        }
    }))


def test_532_CATEGORY_ID_IS_NAN(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками"""
    feed_id = 3000
    categories = mds.generate_categories(5)
    categories[4]['id'] = 'lalala'
    mds.generate_feed(feed_id, categories=categories)
    task_will_fail = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'feed_parsing_error_messages': [
                {
                    'code': '532',
                }
            ]
        }
    }))


def test_530_CYCLE_DETECTED_IN_CATEGORY_TREE(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками"""
    feed_id = 3000
    categories = mds.generate_categories(5)
    categories[2]['parentId'] = '4'
    mds.generate_feed(feed_id, categories=categories)
    task_will_fail = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    # При цикле в категориях qparser возвращает feed_parsing_result = FPR_FAIL (feedparser возвращал FPR_SUCCESS)
    #  Ожидаемо ли это?
    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_FAIL,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'feed_parsing_error_messages': [
                {
                    'code': '530',
                }
            ]
        }
    }))


def test_qparser_feed_errors(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками"""
    feed_id = 30000
    mds.generate_feed(feed_id, offer_count=1, unknown_field=True)
    task_will_fail = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'feed_parsing_error_messages': [
                {
                    'code': '421',
                }
            ]
        }
    }))


def test_qparser_parsing_stats_csv(push_parser_csv, input_topic, mds, mbi_report_topic):
    """Проверяем, что задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками для csv-фида"""
    feed_id = 30000
    mds.generate_feed(feed_id, offer_count=1, bad=True, is_csv=True)
    task_will_fail = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser_csv.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    partner_parsing_stats = {
        'total_offers': 1,
        'error_offers': 1,
        'warning_offers': 0,
        'unloaded_offers': 0,
        'loaded_offers': 1,
        'ignored_offers': 0,
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'feed_id': feed_id,
    }

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'partner_parsing_stats': partner_parsing_stats,
            'parsing_stats': [partner_parsing_stats],
        }
    }))


def test_qparser_parsing_stats_yml(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками для yml-фида"""
    feed_id = 30000
    mds.generate_feed(feed_id, offer_count=1, bad=True)
    task_will_fail = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    partner_parsing_stats = {
        'total_offers': 1,
        'error_offers': 1,
        'warning_offers': 0,
        'unloaded_offers': 0,
        'loaded_offers': 1,
        'ignored_offers': 0,
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'feed_id': feed_id,
    }

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'partner_parsing_stats': partner_parsing_stats,
            'parsing_stats': [partner_parsing_stats],
        }
    }))


def test_qparser_parsing_stats_csv_bad_offer_id(push_parser_csv, input_topic, mds, mbi_report_topic):
    """Проверяем, что задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками в shop-sku(offer_id)для csv-фида"""
    feed_id = 30000
    mds.generate_feed(feed_id, is_csv=True, bad_offer_id=True)
    task_will_fail = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID)

    input_topic.write(task_will_fail.SerializeToString())
    push_parser_csv.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    partner_parsing_stats = {
        'total_offers': 5,
        'error_offers': 4,
        'warning_offers': 0,
        'unloaded_offers': 0,
        'loaded_offers': 5,
        'ignored_offers': 0
    }

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'partner_parsing_stats': partner_parsing_stats,
            'parsing_stats': [partner_parsing_stats],
        }
    }))


def test_qparser_multi_parsing_stats_csv(push_parser_csv, input_topic, mds, mbi_report_topic):
    """Проверяем, что мульти-задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками для csv-фида"""
    feed_id = 30000
    mds.generate_feed(feed_id, offer_count=1, bad=True, is_csv=True)
    task = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID, task_type=FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE)

    input_topic.write(task.SerializeToString())
    push_parser_csv.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'parsing_stats': [{
                'total_offers': 1,
                'error_offers': 1,
                'warning_offers': 0,
                'unloaded_offers': 0,
                'loaded_offers': 1,
                'ignored_offers': 0
            }]*2,
        }
    }))


def test_qparser_multi_parsing_stats_yml(push_parser, input_topic, mds, mbi_report_topic):
    """Проверяем, что мульти-задание, которое имело фатальные ошибки во время обработки, будет обработано
     и в mbi уйдет корректный отчет с ошибками для yml-фида"""
    feed_id = 30000
    mds.generate_feed(feed_id, offer_count=1, bad=True, is_csv=False)
    task = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID, task_type=FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE)

    input_topic.write(task.SerializeToString())
    push_parser.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'parsing_stats': [{
                'total_offers': 1,
                'error_offers': 1,
                'warning_offers': 0,
                'unloaded_offers': 0,
                'loaded_offers': 1,
                'ignored_offers': 0,
            }]*2,
        }
    }))


def test_qparser_multi_parsing_stats_csv_basic_ignored(push_parser_csv, input_topic, mds, mbi_report_topic):
    """Проверяем, что мульти-задание с проигнорированной базовой частью будет обработано и в mbi уйдет корректный отчет для csv-фида"""
    feed_id = 30000
    mds.generate_feed(feed_id, offer_count=1, bad=False, is_csv=True, data_type='book')
    task = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID, is_dbs=True, task_type=FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE)

    input_topic.write(task.SerializeToString())
    push_parser_csv.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'parsing_stats': [{
                'total_offers': 0,
                'error_offers': 0,
                'warning_offers': 1,
                'unloaded_offers': 0,
                'loaded_offers': 0,
                'ignored_offers': 1,
                'business_id': BUSINESS_ID,
                'feed_id': feed_id,
            }, {
                'total_offers': 1,
                'error_offers': 0,
                'warning_offers': 1,
                'unloaded_offers': 0,
                'loaded_offers': 1,
                'ignored_offers': 0,
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            }],
        }
    }))


def test_qparser_multi_parsing_stats_csv_service_ignored(push_parser_csv, input_topic, mds, mbi_report_topic):
    """Проверяем, что мульти-задание с проигнорированной сервисной частью будет обработано и в mbi уйдет корректный отчет для csv-фида"""
    feed_id = 30000
    mds.generate_feed(feed_id, offer_count=1, bad=False, is_csv=True, cpa=False)
    task = make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID, is_dbs=True, task_type=FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE)

    input_topic.write(task.SerializeToString())
    push_parser_csv.run(total_sessions=1)

    mbi_topic = mbi_report_topic.read(count=1)

    assert_that(mbi_topic[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task_report': {
            'feed_parsing_result': FPR_SUCCESS,
            'feed_parsing_task': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            },
            'parsing_stats': [{
                'total_offers': 1,
                'error_offers': 0,
                'warning_offers': 0,
                'unloaded_offers': 0,
                'loaded_offers': 1,
                'ignored_offers': 0,
                'business_id': BUSINESS_ID,
                'feed_id': feed_id,
            }, {
                'total_offers': 0,
                'error_offers': 0,
                'warning_offers': 0,
                'unloaded_offers': 0,
                'loaded_offers': 0,
                'ignored_offers': 1,
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'feed_id': feed_id,
            }],
        }
    }))
