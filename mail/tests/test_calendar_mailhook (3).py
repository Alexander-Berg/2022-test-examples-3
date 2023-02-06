import logging

import pytest

from test_input import mail_attach_log
from logbroker_client_common.handler import CommonHandler
import os

logging.basicConfig(level=logging.DEBUG)

expected_call_args = ('4001257443',
                      '162411061562057793',
                      '1000017.0.75162664518811577144170693552',
                      '1.4',
                      'application/ics')


@pytest.fixture
def handler():
    os.environ['SECRET_101'] = "c2VjcmV0"
    cls_conf = {
        'flush_lines': 100,
        'format': 'TSKV',
        'slow_log': 100.0,
        'stream': {
            'calendar_mailhook': {
                'filename': '.+/attach.tskv',
                'processor': 'calendar_mailhook_processor.MailAttachLogProcessor',
                'args': {
                    'prod': {
                        'blackbox': {
                            "url": "http://pass-test.yandex.ru/blackbox",
                            "self_ip": "127.0.0.1"
                        },
                        'calendar': "http://calendar/mailhook",
                        'sharpei': "http://sharpei-testing.mail.yandex.net/conninfo",
                        'storage': "http://storage.test.yandex.net:10010",
                        'ml': {
                            'url': "http://ml:10000",
                            'tvm': {
                                'client': "101",
                                'secret_env': "SECRET_101"
                            }
                        },
                        'pg': {
                            'user': 'test-pg-user',
                            'pwd_env': 'HOME'
                        }
                    },
                    'corp': {
                        'blackbox': {
                            "url": "http://pass-test.yandex.ru/blackbox",
                            "self_ip": "127.0.0.1"
                        },
                        'calendar': "http://calendar/mailhook",
                        'sharpei': "http://sharpei-testing.mail.yandex.net/conninfo",
                        'storage': "http://storage.test.yandex.net:10010",
                        'ml': {
                            'url': "http://ml:10000",
                            'tvm': {
                                'client': "101",
                                'secret_env': "SECRET_101"
                            }
                        },
                        'pg': {
                            'user': 'test-pg-user',
                            'pwd_env': 'HOME'
                        }
                    }
                }
            }
        }
    }
    return CommonHandler(**cls_conf)


@pytest.fixture
def process_calendar_attachment_mock(mocker):
    return mocker.patch(
        'logbroker_processors.calendar_mailhook_processor.CalendarMailhookProcessor.process_calendar_attach')


@pytest.fixture(autouse=True)
def set_env_name(monkeypatch):
    monkeypatch.setenv('QLOUD_ENVIRONMENT', 'testing')


def test_handle_calendar_attach(handler, process_calendar_attachment_mock):
    handler.process(mail_attach_log.header, mail_attach_log.data)

    assert process_calendar_attachment_mock.call_count == 2
    assert process_calendar_attachment_mock.call_args[0] == expected_call_args
