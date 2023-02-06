from collections import namedtuple
from ora2pg.storage import MulcaGate
from mail.python.tvm_requests import Tvm

import pytest

parametrize = pytest.mark.parametrize
User = namedtuple('User', ('uid', 'suid', 'login'))

Args = namedtuple('Args', (
    'sharpei', 'maildb_dsn_suffix',
    'sharddb', 'huskydb',
    'blackbox', 'mailhost',
    'mulcagate', 'tvm', 'bb_tvm_id'
))
App = namedtuple('App', ('args',))


APP = App(Args(
    sharpei='test://sharpei',
    maildb_dsn_suffix='suffix',
    sharddb='sharddb dsn',
    huskydb='huskydb dsn',
    blackbox='test://blackbox',
    mailhost='test://mailhost',
    mulcagate=MulcaGate(
        host='test://mulcagate',
        port=4242,
        mg_ca_path='//path/to/ca',
    ),
    tvm=Tvm(
        tvm_daemon_url='tvm_daemon_url',
        client_id='client_id',
        local_token=None,
    ),
    bb_tvm_id=None
))

TRANSFER_ID = 42
USER = User(100, 100500, 'test_user')
SRC_USER = User(200, 200500, 'test_src_user')

TO_DB_SHARD_ID = 2
TO_DB = 'postgre:%s' % TO_DB_SHARD_ID
TO_DB_NAME = 'postgre:xdb2'
FROM_DB_SHARD_ID = 1
FROM_DB = 'postgre:%s' % FROM_DB_SHARD_ID
FROM_DB_NAME = 'postgre:xdb1'
DISALLOW_INITED = 'disallow'
UPDATE_MAIL_MIGRATION = 'update_mm'
FILL_CHANGE_LOG = 'fill_cl'
EMPTY_TASK_ARGS = {"unused arg": "uninteresting value"}

TASK_ARGS = {
    'to_db': TO_DB,
    'from_db': FROM_DB,
    'fill_change_log': FILL_CHANGE_LOG,
}

FIX_TASK_ARGS = {"fix": ""}
CALL_SQL_TASK_ARGS = {"sql": "", "args": {}}


def make_handler_args(uid=USER.uid, task_args=EMPTY_TASK_ARGS):
    return APP, TRANSFER_ID, uid, task_args
