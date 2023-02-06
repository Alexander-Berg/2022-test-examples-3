import pytest

from {{cookiecutter.import_path}}.tests.common_conftest import *  # noqa
from {{cookiecutter.import_path}}.tests.db import *  # noqa


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine
