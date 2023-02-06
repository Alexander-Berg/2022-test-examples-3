# -*- coding: utf-8 -*-

from ConfigParser import ConfigParser
import getpass
import os
import platform
import shutil
import unittest
import pytest
import yatest
from mock import patch

from market.pylibrary.mindexerlib import sql
from market.pylibrary.yatestwrap.yatestwrap import source_path
from market.idx.marketindexer.marketindexer import miconfig
from market.pylibrary.mindexerlib import util, configure_logging
import market.pylibrary.database as database
import market.pylibrary.zkclient as zkclient


rootdir = os.path.realpath('tmp/hard_tests')
workdir = os.path.join(rootdir, 'indexer_market')
blueworkdir = os.path.join(rootdir, 'indexer_market_blue')
logdir = os.path.join(rootdir, 'log')
rundir = os.path.join(rootdir, 'run')
lockdir = os.path.join(rootdir, 'lock')
switchdir = os.path.join(rootdir, 'switch')
ybindir = os.path.join(rootdir, 'yandex_bin_dir')
mboclustersdir = os.path.join(rootdir, 'mbo_clusters')
host = platform.node()
user = getpass.getuser()


def init_configs(port_mysql):
    params = {'user': user, 'host': host}
    params['uniqname'] = 'mi_%(host)s_%(user)s' % params

    ic_config = {
        'general': {
            'working_dir': workdir,
            'blue_working_dir': blueworkdir,
            'shared_dir': os.path.join(workdir, 'shared'),
            'download_dir': os.path.join(workdir, 'download'),
            'command_status_dir': rootdir,
            'log_dir': logdir,
            'run_dir': rundir,
            'lock_dir': lockdir,
            'switches_dir': switchdir,
            'ybin_dir': ybindir,
            'global_currency_rates_path': 'tests/data/currencies.xml',
            'mbo_clusters': mboclustersdir,
            },
        'bin': {
            'mifclt': 'echo',
            'feedlog_uploader': 'python hard_tests/create_unused_table.py feed_log_',
            'reductor': 'echo',
            'feedage': 'echo',
            'pbsncat': source_path('market/idx/marketindexer/hard_tests/data/dummy_pbsncat.py'),
            'qbid_create_meta': 'echo',
            'mindexer': './mindexer_clt.py',
            },
        'feeds': {
            'status_set': "'system', 'mock', 'publish'",
            'download_after_switch_off_days': '2',
            },
        'bid': {
            'credentials': os.path.join(rootdir, 'mbi_credentials')
            },
        'mbo-preview': {
            'host': 'mbo01ht.supermarket.yanedex.net',
            'port': 3131
            },
        }

    ds_config = {
        'marketindexer.worker': {
            'host': 'localhost',
            'port': port_mysql,
            'name': '%(uniqname)s_worker' % params,
            'user': 'root',
            'password': '',
            },
        'marketindexer.super': {
            'host': 'localhost',
            'port': port_mysql,
            'name': '%(uniqname)s_super' % params,
            'user': 'root',
            'password': '',
            },
        }

    connection = ''
    zk_config = {
        'zookeeper': {
            'connection': '%s' % connection,
            'master_dir': '/%(uniqname)s/mimaster' % params,
            'current_master_file': '/%(uniqname)s/mimaster/current' % params,
            'current_master_mitype_file': '/%(uniqname)s/mimaster/currentmitype' % params,
            'publish_lock_dir': '/%(uniqname)s/mimaster/publish_lock_dir' % params,
            'delta_publish_lock_dir': '/%(uniqname)s/mimaster/delta_publish_lock_dir' % params,
            'qindex_publish_lock_dir': '/%(uniqname)s/mimaster/qindex_publish_lock_dir' % params,
            'global_lock_dir': '/%(uniqname)s/milock' % params,
            'master_index_file': '/%(uniqname)s/mimaster/index' % params,
            'master_publishing_index_file': '/%(uniqname)s/mimaster/publishing_index' % params,
            'awaps_lock_dir': '/%(uniqname)s/mimaster/awaps_lock_dir' % params
            },
        }

    def write(filepath, data):
        cp = ConfigParser()
        for section in data.keys():
            cp.add_section(section)
            for option in data[section]:
                cp.set(section, option, data[section][option])
        cp.write(open(filepath, 'w'))

    ic_config_path = os.path.join(rootdir, 'common.ini')
    ds_config_path = os.path.join(rootdir, 'datasources.conf')
    zk_config_path = os.path.join(rootdir, 'zookeeper.conf')
    util.makedirs(rootdir)
    util.makedirs(logdir)
    util.makedirs(rundir)
    util.makedirs(lockdir)
    util.makedirs(switchdir)
    util.makedirs(ybindir)
    util.makedirs(mboclustersdir)
    touch(mboclustersdir + '/model-transitions.json')
    write(ic_config_path, ic_config)
    write(ds_config_path, ds_config)
    write(zk_config_path, zk_config)

    os.environ['IC_CONFIG_PATH'] = ic_config_path
    os.environ['IL_CONFIG_PATH'] = ''
    os.environ['DS_CONFIG_PATH'] = ds_config_path
    os.environ['ZK_CONFIG_PATH'] = zk_config_path
    miconfig.default()

    init_mbi_credentials()


def init_db(port_mysql):
    args = {'host': 'localhost', 'port': port_mysql, 'user': 'root', 'drivername': 'mysql+pymysql'}
    # cursor.execute("select user,host from mysql.user where user='%s' and host='localhost'" % user)
    # if cursor.fetchone() is None:
    #     try:
    #         cursor.execute("drop user '%s'@'localhost'" % user)
    #     except:
    #         pass
    #     cursor.execute("flush privileges")
    #     cursor.execute("create user '%s'@'localhost' identified by 'development'" % user)

    databases = [ds['db'] for name, ds in miconfig.default().datasources.items()]
    for dbname in databases:
        print('(re)creation of db ' + dbname)
        args['db'] = dbname
        database.drop_database(args)
        database.create_database(args)
        # cursor.execute("grant all privileges on `%s`.* to '%s'@'localhost'" % (dbname, user))


def create_zk():
    zk = zkclient.Client(hosts=('localhost:{}'.format(os.getenv('RECIPE_ZOOKEEPER_PORT'))))
    return zk


def init_zk():
    conf = miconfig.default()
    home_user_zk_dir = os.path.dirname(conf.get('zookeeper', 'master_dir',
                                                '/for_tests/mimaster'))
    zk = create_zk()
    zk.rmtree(home_user_zk_dir)
    zk.create_dir(home_user_zk_dir)


def init_mbi_credentials():
    with open(miconfig.default().mbi_credentials, 'w') as _f:
        _f.write('mbidding.username=mbiuser\n')
        _f.write('mbidding.password=mbipsswd\n')


def setup(port_mysql):
    configure_logging()
    init_configs(port_mysql)
    init_db(port_mysql)
    init_zk()


def cleanup():
    shutil.rmtree(rootdir, ignore_errors=True)


# testing common utilities and constants
COLLECTION_JSON_CONTENT = """
{
  "offers": [
    {
      "xmlconfig": "config-0.xml",
      "offersconfig": "offers-0.conf",
      "nparts": 16,
      "host": "dharma.market.yandex.net",
      "parts": [
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15
      ],
      "dists": [
        "search-part-0",
        "search-part-1",
        "search-part-2",
        "search-part-3",
        "search-part-4",
        "search-part-5",
        "search-part-6",
        "search-part-7",
        "search-part-8",
        "search-part-9",
        "search-part-10",
        "search-part-11",
        "search-part-12",
        "search-part-13",
        "search-part-14",
        "search-part-15"
      ],
      "id": 0
    }
  ]
}
"""


def create_table_from_description(ds, db_type, description):
    db_connection = database.connect(**ds[db_type])
    db_connection.drop_table(description, sql.metadata)
    with db_connection.begin():
        db_connection.create_table(description, sql.metadata)


def create_unused_table(connection, table_name):
    with connection.begin():
        connection.execute_sql('create table if not exists `%s`(not_used tinyint not null primary key)' % table_name)


def create_workdir_test_environment(indexer_workdir):
    if os.path.exists(indexer_workdir):
        shutil.rmtree(indexer_workdir, ignore_errors=True, onerror=None)
    util.makedirs(indexer_workdir)


def touch(filepath, *args):
    filepath = os.path.join(filepath, *args)
    dirname = os.path.dirname(filepath)
    util.makedirs(dirname)
    open(filepath, 'w').close()


def write(filepath, value):
    util.makedirs(os.path.dirname(filepath))
    with open(filepath, 'w') as _f:
        _f.write(value)


def rm(filepath):
    if os.path.exists(filepath):
        os.unlink(filepath)


class Null(object):
    def __init__(self, *args, **kwargs):
        pass

    def __nonzero__(self):
        return True

    def __enter__(self, *args):
        return self

    def __exit__(self, *args):
        return False

    def __call__(self, *args, **kwargs):
        return self

    def __getattr__(self, name):
        return self

    def __setattr__(self, name, value):
        return self

    def __delattr__(self, name):
        return self


class HbaseTestCase(unittest.TestCase):
    ZK_HOSTS = ('localhost:{}'.format(os.getenv('RECIPE_ZOOKEEPER_PORT')))

    @pytest.yield_fixture(scope='class', autouse=True)
    def do_patch_miconfig(self):
        config = miconfig.default()
        config.zookeeper_hosts = self.ZK_HOSTS
        config.bids_dir = os.path.join(yatest.common.work_path(), 'tmp', 'hard_tests', 'indexer_market')
        config.indexer_workdir = yatest.common.work_path()
        with patch('market.idx.marketindexer.miconfig.default', auto_spec=True, return_value=config):
            yield

    @pytest.yield_fixture(scope='class', autouse=True)
    def do_patch_full_miconfig(self):
        config = miconfig.default()
        config.zookeeper_hosts = self.ZK_HOSTS
        with patch('market.idx.marketindexer.miconfig.default', auto_spec=True, return_value=config):
            yield

    @pytest.yield_fixture(scope='class', autouse=True)
    def do_patch_mitype(self):
        with patch('market.pylibrary.yenv.marketindexer_type', auto_spec=True, return_value='superstrat'):
            yield


class MysqlTestCase(unittest.TestCase):
    pass
