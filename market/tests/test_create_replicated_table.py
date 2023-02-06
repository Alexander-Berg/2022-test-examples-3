# coding: utf-8

from hamcrest import assert_that, has_entries
import pytest

from market.pylibrary.dyt.dyt import is_table_mounted
from market.idx.yatf.resources.yt_replicated_table_resource import YtReplicatedTable


REPLICATED_TABLE = '//home/test/replicated_table'
SYNC_REPLICA_TABLE = '//home/test/sync_replica'
ASYNC_REPLICA_TABLE = '//home/test/async_replica'

ATTRIBUTES = {
    'dynamic': True,
    'schema': [
        dict(name='key', type='uint32', sort_order='ascending'),
        dict(name='value', type='string'),
    ]
}


@pytest.fixture(scope='module')
def yt_client(yt_server):
    return yt_server.get_yt_client()


@pytest.fixture(scope='module')
def table_creator(yt_server):
    table = YtReplicatedTable(yt_server,
                              path=REPLICATED_TABLE,
                              sync_path=SYNC_REPLICA_TABLE,
                              async_path=ASYNC_REPLICA_TABLE,
                              attributes=ATTRIBUTES)
    table.create()


def test_creation_replication_table(table_creator, yt_client):
    """Проверяем, что реплицированная таблица и все реплики созданы"""
    assert_that(yt_client.exists(REPLICATED_TABLE), "replicated table does not exist")
    assert_that(yt_client.exists(SYNC_REPLICA_TABLE), "sync replica does not exist")
    assert_that(yt_client.exists(ASYNC_REPLICA_TABLE), "async replica does not exist")


def test_mounting_replicated_table(table_creator, yt_client):
    """Проверяем, что реплицированная таблица и все ее реплики замаунчены"""
    assert_that(is_table_mounted(yt_client, REPLICATED_TABLE), "replicated table is not mounted")
    assert_that(is_table_mounted(yt_client, SYNC_REPLICA_TABLE), "sync replica is not mounted")
    assert_that(is_table_mounted(yt_client, ASYNC_REPLICA_TABLE), "async replica is not mounted")


def test_replicas(table_creator, yt_client):
    """Проверяем корректность объектов реплик"""
    replicas = yt_client.get('{}/@replicas'.format(REPLICATED_TABLE))
    assert_that(len(replicas), 2, "incorrect attribute 'replicas' for replicated table")

    sync_replica_id = None
    async_replica_id = None
    for id, value in replicas.items():
        if value['mode'] == 'sync':
            sync_replica_id = id
        elif value['mode'] == 'async':
            async_replica_id = id
        else:
            raise RuntimeError('What the hell am I? Mode = {}'.format(value['mode']))

    assert_that(replicas[sync_replica_id], has_entries({
        'replica_path': SYNC_REPLICA_TABLE,
        'mode': 'sync',
        'state': 'enabled',
    }), "replicas do not contain sync replica")

    assert_that(replicas[async_replica_id], has_entries({
        'replica_path': ASYNC_REPLICA_TABLE,
        'mode': 'async',
        'state': 'enabled',
    }), "replicas do not contain async replica")

    sync_replica_object_attrs = yt_client.get('#{}/@'.format(sync_replica_id))
    assert_that(sync_replica_object_attrs, has_entries({
        'replica_path': SYNC_REPLICA_TABLE,
        'id': sync_replica_id,
        'state': 'enabled',
        'mode': 'sync',
    }), "sync replica object is incorrect")

    async_replica_object_attrs = yt_client.get('#{}/@'.format(async_replica_id))
    assert_that(async_replica_object_attrs, has_entries({
        'replica_path': ASYNC_REPLICA_TABLE,
        'id': async_replica_id,
        'state': 'enabled',
        'mode': 'async',
    }), "async replica object is incorrect")
