import pytest

from mail.ohio.ohio.core.actions.base import BaseAction
from mail.ohio.ohio.tests.common_conftest import *  # noqa
from mail.ohio.ohio.tests.db import *  # noqa


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
