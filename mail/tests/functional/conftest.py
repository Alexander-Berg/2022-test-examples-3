import sys
from asyncio import iscoroutine
from contextlib import contextmanager

import pytest

from mail.beagle.beagle.tests.common_conftest import *  # noqa
from mail.beagle.beagle.tests.db import *  # noqa
from mail.beagle.beagle.tests.entities import *  # noqa
from mail.beagle.beagle.tests.interactions import *  # noqa
from mail.beagle.beagle.tests.utils import dummy_coro


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine


@pytest.fixture
def mail_list_id(mail_list):
    return mail_list.mail_list_id


@pytest.fixture
def client_mocker(mocker):
    def _client_mocker(cls_path):
        @contextmanager
        def _inner(method, result=None, exc=None, sync_result=None, multiple_calls=False):
            def return_value(*args, **kwargs):
                """Returns mock return value"""
                if sync_result:
                    return sync_result
                else:
                    if sys.version_info >= (3, 8):
                        if exc:
                            raise exc
                        else:
                            return result
                    else:
                        return dummy_coro(result=result, exc=exc)

            def return_value_generator():
                while True:
                    yield return_value()

            mock = mocker.patch(f'{cls_path}.{method}')
            if multiple_calls:
                # every time mock is called a new return value is generated
                side_effect = return_value()
                if iscoroutine(side_effect):
                    side_effect.close()
                    side_effect = return_value_generator()
                mock.side_effect = side_effect
            else:
                mock.return_value = return_value()

            yield mock

            if iscoroutine(mock.return_value):
                mock.return_value.close()

        return _inner

    return _client_mocker


@pytest.fixture
def blackbox_mocker(client_mocker):
    return client_mocker('mail.beagle.beagle.interactions.blackbox.BlackBoxClient')


@pytest.fixture
def sender_mocker(client_mocker):
    return client_mocker('mail.beagle.beagle.interactions.sender.SenderClient')
