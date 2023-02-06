import os
import pytest

from sqlalchemy import select

import market.pylibrary.database as database


@pytest.fixture()
def sqlite_ds():
    return {'super':  {'db': './super.db', 'drivername': 'sqlite'}}


def test_create_db(sqlite_ds):
    database.create_database(sqlite_ds.get('super'))
    assert os.path.exists(sqlite_ds.get('super').get('db'))


def test_simple_select(sqlite_ds):
    conn = database.connect(**sqlite_ds.get('super'))
    res = conn.execute(select([1]))
    assert res.scalar() == 1


def test_drop_db(sqlite_ds):
    database.drop_database(sqlite_ds.get('super'))
    assert os.path.exists(sqlite_ds.get('super').get('db')) is False
