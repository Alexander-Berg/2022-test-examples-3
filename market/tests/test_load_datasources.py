import os

import pytest
import mock
from market.pylibrary.yatestwrap.yatestwrap import source_path

import market.pylibrary.database as database

DATA_DIR = source_path('market/pylibrary/database/tests/data')


@pytest.fixture()
def mysql_ds():
    return {'super':
                {'passwd': 'some_pass',
                 'db': 'marketindexer2super',
                 'hosts': ['pershistdb01h.market.yandex.net', 'pershistdb01v.market.yandex.net',
                           'pershistdb01w.market.yandex.net'],
                 'user': 'marketindexer',
                 'port': 3308,
                 'drivername': 'mysql+pymysql'}
            }


def test_load_mysql_datasources(mysql_ds):
    ds = database.load_datasources_from_config(os.path.join(DATA_DIR, 'ds-mysql.conf'))
    assert mysql_ds == ds

    expect_t = 'mysql+pymysql://marketindexer:some_pass@{host}:3308/marketindexer2super?connect_timeout=20'
    args = mysql_ds.get('super').copy()

    for host in args.get('hosts', []):
        args['host'] = host
        args['query'] = {'connect_timeout': '20'}
        assert expect_t.format(host=host) == str(database._get_url(**args))


def test_load_old_datasources(mysql_ds):
    ds = database.load_datasources_from_config(os.path.join(DATA_DIR, 'ds-mysql-old.conf'))
    assert mysql_ds == ds


@pytest.fixture()
def mysql_ds_no_passwd():
    return {'super':
                {'passwd': '',    # empty by default
                 'db': 'marketindexer2super',
                 'hosts': ['pershistdb01h.market.yandex.net', 'pershistdb01v.market.yandex.net',
                           'pershistdb01w.market.yandex.net'],
                 'user': 'marketindexer',
                 'port': 3308,
                 'drivername': 'mysql+pymysql'}
            }


def test_load_datasources_no_passwd(mysql_ds_no_passwd):
    ds = database.load_datasources_from_config(os.path.join(DATA_DIR, 'ds-mysql-no-passwd.conf'))
    assert mysql_ds_no_passwd == ds


@pytest.fixture()
def mysql_ds_passwd_from_file():
    return {'super':
                {'passwd': 'password_from_file',  # stored in data/super_passwd file, which must be set in SUPER_PASSWORD_PATH env variable
                 'db': 'marketindexer2super',
                 'hosts': ['pershistdb01h.market.yandex.net', 'pershistdb01v.market.yandex.net',
                           'pershistdb01w.market.yandex.net'],
                 'user': 'marketindexer',
                 'port': 3308,
                 'drivername': 'mysql+pymysql'}
            }


def test_load_datasources_passwd_from_file(mysql_ds_passwd_from_file):
    with mock.patch('market.pylibrary.database._get_passwd_path', auto_spec=True, return_value=os.path.join(DATA_DIR, 'super_passwd')):
        ds = database.load_datasources_from_config(os.path.join(DATA_DIR, 'ds-mysql-passwd-from-file.conf'))
        assert mysql_ds_passwd_from_file == ds


@pytest.fixture()
def postgresql_ds():
    return {'super':
                {'passwd': 'some_pass',
                 'db': 'market_mindexer_super_test',
                 'hosts': ['iva-0t5bhg4yixwi3b8u.db.yandex.net', 'sas-2z2azve5yaambwp4.db.yandex.net',
                           'vla-4td6jhzq7tlfc52m.db.yandex.net'],
                 'user': 'market_mindexer_super_test',
                 'port': 6432,
                 'drivername': 'postgresql'}
            }


def test_load_postgresql_datasources(postgresql_ds):
    ds = database.load_datasources_from_config(os.path.join(DATA_DIR, 'ds-postgresql.conf'))
    assert postgresql_ds == ds

    expect_url = 'postgresql://market_mindexer_super_test:some_pass@iva-0t5bhg4yixwi3b8u.db.yandex.net,' \
                 'sas-2z2azve5yaambwp4.db.yandex.net,vla-4td6jhzq7tlfc52m.db.yandex.net:6432/market_mindexer_super_test?target_session_attrs=read-write'
    args = ds.get('super').copy()
    assert expect_url == str(database._get_url(**args))


@pytest.fixture()
def sqlite_ds():
    return {'super':  {'db': '/tmp/super.db', 'drivername': 'sqlite'}}


def test_load_sqlite_datasource(sqlite_ds):
    ds = database.load_datasources_from_config(os.path.join(DATA_DIR, 'ds-sqlite.conf'))
    assert sqlite_ds == ds

    expect_url = 'sqlite:////tmp/super.db'
    args = ds.get('super').copy()
    assert expect_url == str(database._get_url(**args))
