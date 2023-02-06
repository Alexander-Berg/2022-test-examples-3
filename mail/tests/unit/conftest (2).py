import logging

import pytest

from sendr_qlog import LoggerContext

from mail.ciao.ciao.core.context import CORE_CONTEXT
from mail.ciao.ciao.tests.common_conftest import *  # noqa
from mail.ciao.ciao.tests.entities import *  # noqa
from mail.ciao.ciao.tests.interactions import *  # noqa
from mail.ciao.ciao.utils.logging import LOGGER


@pytest.fixture
def test_logger():
    return LoggerContext(logging.getLogger(), {})


@pytest.fixture
def request_id(rands):
    return rands()


@pytest.fixture(autouse=True)
def setup_logger(test_logger):
    LOGGER.set(test_logger)


@pytest.fixture(autouse=True)
def setup_context(test_logger, request_id):
    CORE_CONTEXT.logger = test_logger
    CORE_CONTEXT.request_id = request_id
