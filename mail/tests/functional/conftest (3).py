import pytest

from mail.ipa.ipa.tests.common_conftest import *  # noqa
from mail.ipa.ipa.tests.data.report_data import *  # noqa
from mail.ipa.ipa.tests.db import *  # noqa
from mail.ipa.ipa.tests.entities import *  # noqa
from mail.ipa.ipa.tests.interactions import *  # noqa


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine
