# coding: utf-8

import os
from hamcrest import assert_that, not_
from six.moves.configparser import ConfigParser
import pytest
import json

from market.idx.datacamp.parser.lib.parser_engine.process_log import ExplanationLogWriter
from market.idx.datacamp.parser.lib.parser_engine.process_log_messages import RobotProcessLogMessages

import market.idx.datacamp.proto.errors.Explanation_pb2 as EXP
from market.idx.yatf.matchers.env_matchers import ErrorMessageHasJsonDetails
from market.idx.yatf.matchers.protobuf_matchers import IsProtobuf
from market.idx.yatf.resources.pbsn import ExplanationLogOutput

from market.pylibrary.snappy_protostream import SnappyProtoWriter

EXPLANATION_LOG_MAGIC = 'EXPM'


@pytest.fixture
def config():
    config = ConfigParser()
    return config


def write_error_message_in_log_and_get_result(config, feed_errors_filepath, message):
    explanation_log = ExplanationLogWriter(config, feed_errors_filepath)
    explanation_log.safe_write(message)

    actual_log = ExplanationLogOutput(feed_errors_filepath)
    actual_log.load()
    return actual_log.proto_results[0]


def prepare_original_file_with_expectation_log(feed_errors_filepath, error_code):
    with open(feed_errors_filepath, 'wb') as explanation_log:
        with SnappyProtoWriter(explanation_log, EXPLANATION_LOG_MAGIC) as writer:
            result_message = EXP.ExplanationBatch()
            new_explanation_message = result_message.explanation.add()
            new_explanation_message.code = error_code
            writer.write(result_message)


def test_fatal_errors_written_in_explanation_log(tmpdir_factory, config):
    """Тест проверяет, что ошибка записывается в лог с фатальными ошибками"""
    details_dict = {
        'test_details': 'test'
    }
    details = json.dumps(details_dict)

    expected_message = RobotProcessLogMessages.FC50C_MALFORMED_URL(details=details)

    feed_errors_filepath = os.path.join(str(tmpdir_factory.mktemp('session_dir')), 'fatal_errors_file.pbuf.sn')
    result = write_error_message_in_log_and_get_result(config, feed_errors_filepath, expected_message)

    assert_that(result, IsProtobuf({
        'explanation': [
            {
                'code': '50C',
                'details': ErrorMessageHasJsonDetails({
                    'test_details': 'test'
                }),
            }
        ]
    }))


def test_extend_of_fatal_errors_in_explanation_log(tmpdir_factory, config):
    """Тест проверяет, что если лог с фатальными ошибками уже был, то новая ошибка не затрет все старые,
     а только расширит их множество"""
    expected_message = RobotProcessLogMessages.FC50C_MALFORMED_URL()

    feed_errors_filepath = os.path.join(str(tmpdir_factory.mktemp('session_dir')), 'fatal_errors_file.pbuf.sn')
    prepare_original_file_with_expectation_log(feed_errors_filepath, '550')

    result = write_error_message_in_log_and_get_result(config, feed_errors_filepath, expected_message)

    assert_that(result, IsProtobuf({
        'explanation': [
            {
                'code': '550',
            },
            {
                'code': '50C',
            }
        ]
    }))


def test_wrong_level_of_error_in_explanation_log(tmpdir_factory, config):
    """Тест проверяет, что ошибка с неправильным уровнем не будет записана в лог с фатальными ошибками"""
    message_with_wrong_level = RobotProcessLogMessages.FW350_OFFER_PARTIALLY_IGNORED()

    feed_errors_filepath = os.path.join(str(tmpdir_factory.mktemp('session_dir')), 'fatal_errors_file.pbuf.sn')
    prepare_original_file_with_expectation_log(feed_errors_filepath, '550')

    result = write_error_message_in_log_and_get_result(config, feed_errors_filepath, message_with_wrong_level)
    assert_that(result, IsProtobuf({
        'explanation': [
            {
                'code': '550',
            },
        ]
    }))
    assert_that(result, not_(IsProtobuf({
        'explanation': [
            {
                'code': '350',
            },
        ]
    })))
