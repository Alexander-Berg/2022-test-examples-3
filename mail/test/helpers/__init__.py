# coding: utf-8

from ora2pg.blackbox import BBInfo
from ora2pg.storage import MulcaGate
from ora2pg.clone_user import CloneConfig
import mock


def mk_source_user(uid=1, suid=100, login='test-source-user', default_email='test-source-user@yandex-text.ru'):
    return BBInfo(
        uid=uid,
        suid=suid,
        login=login,
        default_email=default_email,
    )


def mk_dest_user(uid=2, suid=200, login='test-dest-user', default_email='test-dest-user@yandex-text.ru'):
    return BBInfo(
        uid=uid,
        suid=suid,
        login=login,
        default_email=default_email,
    )


TEST_CONF = {
    'sharpei': 'test://sharpei',
    'maildb_dsn_suffix': '',
    'dest_shard_id': 42,
    'mailhost': 'test//mailhost',
    'blackbox': 'test://blackbox',
    'sharddb': 'test-db-name=sharddb',
    'mulcagate': MulcaGate(host='test://mulcagate', port=12345, mg_ca_path='//path/to/ca'),
    'huskydb': 'test-db-name=huksydb',
}

test_config = CloneConfig(**TEST_CONF)


def auto_patch(module_path):
    def real_patcher(target):
        return mock.patch(
            module_path + '.' + target,
            autospec=True)
    return real_patcher
