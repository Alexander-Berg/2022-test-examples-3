import pytest

from mail.ohio.ohio.tests.common_conftest import *  # noqa
from mail.ohio.ohio.tests.db import *  # noqa


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine
