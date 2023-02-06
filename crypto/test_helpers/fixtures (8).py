import os

import pytest

from crypta.lib.python.postgres.test_helpers.postgres import Postgres


@pytest.fixture(scope="session")
def postgres():
    yield Postgres(
        host=os.environ["POSTGRES_RECIPE_HOST"],
        port=os.environ["POSTGRES_RECIPE_PORT"],
        user=os.environ["POSTGRES_RECIPE_USER"],
        dbname=os.environ["POSTGRES_RECIPE_DBNAME"],
    )
