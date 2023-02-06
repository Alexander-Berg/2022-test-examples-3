import sys
from contextlib import contextmanager
from datetime import datetime, timezone
from inspect import iscoroutine

import pytest

from mail.beagle.beagle.core.actions.base import BaseAction
from mail.beagle.beagle.tests.common_conftest import *  # noqa
from mail.beagle.beagle.tests.db import *  # noqa
from mail.beagle.beagle.tests.entities import *  # noqa
from mail.beagle.beagle.tests.interactions import *  # noqa
from mail.beagle.beagle.tests.utils import dummy_coro, dummy_coro_generator


@pytest.fixture
def db_engine(mocked_db_engine):
    return mocked_db_engine


@pytest.fixture
def test_logger():
    import logging
    from sendr_qlog import LoggerContext
    return LoggerContext(logging.getLogger('test_logger'), {})


@pytest.fixture
def request_id(rands):
    return rands()


@pytest.fixture(autouse=True)
def action_context_setup(test_logger, app, db_engine, request_id):
    # app dependency is required to ensure exit order
    BaseAction.context.logger = test_logger
    BaseAction.context.request_id = request_id
    BaseAction.context.db_engine = db_engine
    assert BaseAction.context.storage is None


@pytest.fixture
def now():
    return datetime(2019, 11, 4, 17, 23, 12, tzinfo=timezone.utc)


@pytest.fixture
def response_status():
    return 200


@pytest.fixture
def response_json():
    return {}


@pytest.fixture
def response_json_generator(response_json):
    return dummy_coro_generator(response_json)


@pytest.fixture
def response_mock(mocker, response_status, response_json_generator):
    mock = mocker.Mock()
    mock.status = response_status
    mock.json = mocker.Mock(side_effect=response_json_generator)
    return mock


@pytest.fixture
def client_mocker(mocker, request_id, test_logger, response_mock):
    def _inner(client_cls):
        client = client_cls(test_logger, request_id)
        client.request_mock = mocker.patch.object(
            client,
            '_make_request',
            mocker.Mock(side_effect=dummy_coro_generator(response_mock))
        )
        return client

    return _inner


# Clients

def create_class_mocker(mocker, cls_path: str):
    """
    Create class mocker which will make mock for any method by its name.

    Returns context manager where:
        __enter__ returns mock of method
        __exit__ tears down mock return value (e. g. closes non-awaited coroutine)

    Direct usage example:
        >>> with mocker('method_name') as mock:
        >>>     mock.assert_called_once_with(...)

    Usage in pytest fixture definition:
        >>> @pytest.fixture
        >>> def fixture():
        >>>     with mocker('mock') as mock:
        >>>         yield mock
    """

    @contextmanager
    def method_mocker(method_name, result=None, exc=None, sync_result=None, multiple_calls=False):

        def create_return_value(*args, **kwargs):
            """Create return value for mocked method."""
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

        mock = mocker.patch(f'{cls_path}.{method_name}')
        if multiple_calls:
            mock.side_effect = create_return_value
        else:
            mock.return_value = create_return_value()

        yield mock

        if iscoroutine(mock.return_value):
            mock.return_value.close()

    return method_mocker


@pytest.fixture
def sender_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.beagle.beagle.interactions.sender.SenderClient')


@pytest.fixture
def blackbox_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.beagle.beagle.interactions.blackbox.BlackBoxClient')


@pytest.fixture
def base_tvm(mocker):
    mock = mocker.patch('sendr_tvm.qloud_async_tvm.TicketCheckResult')
    return mock


@pytest.fixture
def tvm(base_tvm, user, randn):
    base_tvm.src = randn()
    # base_tvm.default_uid = user.uid if user else None
    base_tvm.default_uid = randn()
    return base_tvm
