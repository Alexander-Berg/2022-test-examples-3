import pytest

from {{cookiecutter.import_path}}.tests.common_conftest import *  # noqa
from {{cookiecutter.import_path}}.tests.db import *  # noqa


@pytest.fixture
def db_engine(mocked_db_engine):
    return mocked_db_engine
