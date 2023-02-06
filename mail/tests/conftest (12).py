from mail.pg.mopsdb.tests.common import *  # noqa

import pytest


@pytest.fixture(scope="function", autouse=True)
def config_setup(context):
    context.config = dict(
        conninfo=dict(
            host='localhost',
            port=context.mopsdb.port(),
            dbname='mopsdb',
        )
    )
