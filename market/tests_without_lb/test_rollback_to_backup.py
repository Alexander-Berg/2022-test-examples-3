# coding: utf-8

import pytest
from hamcrest import assert_that, has_entries, equal_to

import yt.wrapper as yt
from yt.yson import YsonUint64

from market.idx.pylibrary.datacamp.backup.backup import set_backup_attributes
from market.idx.pylibrary.datacamp.schema import service_offers_attributes, basic_offers_attributes, COLUMN_TO_PROTO_OFFERS_UNITED_TABLE
from market.idx.datacamp.yatf.utils import create_meta
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.pylibrary.dyt.dyt import is_table_mounted
from market.pylibrary.dyt.replication import create_replicated_table
from market.pylibrary.yt_replicator import TableConverter

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.resources.yt_replicated_table_resource import YtReplicatedTable
from market.idx.yatf.resources.yt_table_resource import YtTableResource


BACKUP_GENERATION = "20770101_0000"

SERVICE_DATACAMP_BACKUP = [
    {
        'business_id': 1,
        'shop_id': 1,
        'shop_sku': 'T1000',
        'warehouse_id': 0,
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, shop_id=1, offer_id='T1000'),
            meta=create_meta(10)
        ).SerializeToString()
    },
]

ACTUAL_SERVICE_DATACAMP_BACKUP = [
    {
        'business_id': 1,
        'shop_id': 1,
        'shop_sku': 'T1000',
        'warehouse_id': 100,
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, shop_id=1, offer_id='T1000', warehouse_id=100),
            meta=create_meta(10)
        ).SerializeToString()
    },
]

BASIC_DATACAMP_BACKUP = [
    {
        'business_id': 1,
        'shop_sku': 'T1000',
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, shop_id=1, offer_id='T1000'),
            meta=create_meta(10)
        ).SerializeToString()
    },
]


SERVICE_DATACAMP_CORRUPTED_TABLE = [
    {
        'business_id': 1,
        'shop_id': 1,
        'shop_sku': 'BAD_DATA',
        'warehouse_id': 0,
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, shop_id=1, offer_id='T1000'),
            meta=create_meta(10)
        ).SerializeToString()
    },
]

ACTUAL_SERVICE_DATACAMP_CORRUPTED_TABLE = [
    {
        'business_id': 1,
        'shop_id': 1,
        'shop_sku': 'BAD_DATA',
        'warehouse_id': 100,
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, shop_id=1, offer_id='T1000'),
            meta=create_meta(10)
        ).SerializeToString()
    },
]

BASIC_DATACAMP_CORRUPTED_TABLE = [
    {
        'business_id': 1,
        'shop_sku': 'BAD_DATA',
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, shop_id=1, offer_id='T1000'),
            meta=create_meta(10)
        ).SerializeToString()
    },
]


def backup_table(yt_server, table, backup_path):
    table.create()

    client = yt_server.get_yt_client()
    table_converter = TableConverter(client)
    table_converter.convert_to_static(table.table_path, backup_path)

    set_backup_attributes(client, backup_path, COLUMN_TO_PROTO_OFFERS_UNITED_TABLE)
    client.set_attribute(backup_path, '_yt_dump_restore_pivot_keys', [
        [],
        [YsonUint64(1), '1']
    ])

    return YtTableResource(yt_server, backup_path, load=True)


def meta_tablepath(table):
    return table + '_meta'


@pytest.fixture(scope="session")
def monkeysession():
    from _pytest.monkeypatch import MonkeyPatch
    mpatch = MonkeyPatch()
    yield mpatch
    mpatch.undo()


@pytest.fixture(scope='module')
def yt_client(yt_server):
    return yt_server.get_yt_client()


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'white',
            }
        })
    return config


@pytest.fixture(scope='module')
def backup_service_offers_table(yt_server, config):
    backup_path = yt.ypath_join(config.service_backup_dir, BACKUP_GENERATION)
    temp_dynamic_backup_path = backup_path + "_dyt"

    table = DataCampServiceOffersTable(
        yt_server,
        temp_dynamic_backup_path,
        data=SERVICE_DATACAMP_BACKUP
    )
    return backup_table(yt_server, table, backup_path)


@pytest.fixture(scope='module')
def backup_actual_service_offers_table(yt_server, config):
    backup_path = yt.ypath_join(config.actual_service_backup_dir, BACKUP_GENERATION)
    temp_dynamic_backup_path = backup_path + "_dyt"

    table = DataCampServiceOffersTable(
        yt_server,
        temp_dynamic_backup_path,
        data=ACTUAL_SERVICE_DATACAMP_BACKUP
    )
    return backup_table(yt_server, table, backup_path)


@pytest.fixture(scope='module')
def backup_basic_offers_table(yt_server, config):
    backup_path = yt.ypath_join(config.basic_backup_dir, BACKUP_GENERATION)
    temp_dynamic_backup_path = backup_path + "_dyt"

    table = DataCampBasicOffersTable(
        yt_server,
        temp_dynamic_backup_path,
        data=BASIC_DATACAMP_BACKUP
    )
    return backup_table(yt_server, table, backup_path)


@pytest.fixture(scope='module')
def basic_offers_table(config, yt_server):
    return YtReplicatedTable(
        yt_server,
        path=meta_tablepath(config.yt_basic_offers_tablepath),
        sync_path=config.yt_basic_offers_tablepath,
        data=BASIC_DATACAMP_CORRUPTED_TABLE,
        attributes=basic_offers_attributes()
    )


@pytest.fixture(scope='module')
def service_offers_table(config, yt_server):
    return YtReplicatedTable(
        yt_server,
        path=meta_tablepath(config.yt_service_offers_tablepath),
        sync_path=config.yt_service_offers_tablepath,
        data=SERVICE_DATACAMP_CORRUPTED_TABLE,
        attributes=service_offers_attributes()
    )


@pytest.fixture(scope='module')
def actual_service_offers_table(config, yt_server):
    return YtReplicatedTable(
        yt_server,
        path=meta_tablepath(config.yt_actual_service_offers_tablepath),
        sync_path=config.yt_actual_service_offers_tablepath,
        data=ACTUAL_SERVICE_DATACAMP_CORRUPTED_TABLE,
        attributes=service_offers_attributes()
    )


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config, service_offers_table, actual_service_offers_table, basic_offers_table,
                  backup_service_offers_table, backup_actual_service_offers_table, backup_basic_offers_table):
    resources = {
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'basic_offers_table': basic_offers_table,
        'config': config,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


@pytest.yield_fixture(scope='module')
def mock_all(monkeysession):
    with monkeysession.context() as m:
        class TransferManagerMock(object):
            def __init__(self, *args, **kwargs):
                pass

        def empty_foo(config, src, log, backup_dir, dst_name, clean_old):
            pass

        def mock_create_replicated_table(yt_client, replicated_table, replicas, attributes, enable_replicated_table_tracker=True, pivot_keys=[]):
            # меняем путь реплицированной таблицы
            create_replicated_table(yt_client, meta_tablepath(replicated_table), replicas, attributes, enable_replicated_table_tracker, pivot_keys)

        m.setattr('market.idx.pylibrary.datacamp.backup.restore_backup.TransferManager', TransferManagerMock)

        m.setattr('market.idx.pylibrary.datacamp.backup.restore_backup.get_service_offers_meta_tablepath',
                  lambda config: meta_tablepath(config.yt_service_offers_tablepath))
        m.setattr('market.idx.pylibrary.datacamp.backup.restore_backup.get_actual_service_offers_meta_tablepath',
                  lambda config: meta_tablepath(config.yt_actual_service_offers_tablepath))
        m.setattr('market.idx.pylibrary.datacamp.backup.restore_backup.get_basic_offers_meta_tablepath',
                  lambda config: meta_tablepath(config.yt_basic_offers_tablepath))
        m.setattr('market.idx.pylibrary.datacamp.backup.restore_backup.do_backup', empty_foo)
        m.setattr('market.pylibrary.dyt.replication.create_replicated_table', mock_create_replicated_table)

        yield


def do_rollback(client):
    return client.put('/rollback_to_backup?generation={}&reason={}&sync=true'.format(BACKUP_GENERATION, 'just_because_i_can'))


@pytest.fixture(scope='module')
def rollback(routines_http, mock_all):
    response = do_rollback(routines_http)
    assert_that(response, HasStatus(200), "Rollback failed")


def test_restored_table(rollback, yt_client, config):
    """Проверяем, что реплицированная таблица и все реплики созданы"""
    for table in [config.yt_service_offers_tablepath,
                  config.yt_actual_service_offers_tablepath,
                  config.yt_basic_offers_tablepath]:
        assert_that(yt_client.exists(meta_tablepath(table)), "replicated table {} does not exist".format(meta_tablepath(table)))
        assert_that(yt_client.exists(table), "sync replica does not exist")

        assert_that(is_table_mounted(yt_client, meta_tablepath(table)), "replicated table is not mounted")
        assert_that(is_table_mounted(yt_client, table), "sync replica is not mounted")
    pivot_keys = yt_client.get_attribute(meta_tablepath(table), 'pivot_keys')
    assert_that(len(pivot_keys), equal_to(2))
    assert_that(pivot_keys[0], equal_to([]))
    assert_that(pivot_keys[1], equal_to([1, '1']))


def test_restored_replicas(rollback, yt_client, config):
    """Проверяем корректность объектов реплик"""
    for table in [config.yt_service_offers_tablepath,
                  config.yt_actual_service_offers_tablepath,
                  config.yt_basic_offers_tablepath]:
        replicas = yt_client.get('{}/@replicas'.format(meta_tablepath(table)))
        assert_that(len(replicas), 2, "incorrect attribute 'replicas' for replicated table")

        sync_replica_id = None
        for id, value in replicas.items():
            if value['mode'] == 'sync':
                sync_replica_id = id
            else:
                raise RuntimeError('What the hell am I? Mode = {}'.format(value['mode']))

        assert_that(replicas[sync_replica_id], has_entries({
            'replica_path': table,
            'mode': 'sync',
            'state': 'enabled',
        }), "replicas do not contain sync replica")

        sync_replica_object_attrs = yt_client.get('#{}/@'.format(sync_replica_id))
        assert_that(sync_replica_object_attrs, has_entries({
            'replica_path': table,
            'id': sync_replica_id,
            'state': 'enabled',
            'mode': 'sync',
        }), "sync replica object is incorrect")


@pytest.mark.skip('MARKETINDEXER-35802')
def test_get_offers(rollback, yt_client, config):
    """Тест проверяет, что бекап работает корректно"""

    shop_id = 1
    business_id = 1
    offer_id = 'T1000'
    warehouse_id = 100

    service_rows = list(yt_client.lookup_rows(config.yt_service_offers_tablepath, [{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'warehouse_id': 0,
    }]))

    assert_that(len(service_rows), 1)

    actual_service_rows = list(yt_client.lookup_rows(config.yt_actual_service_offers_tablepath, [{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'warehouse_id': warehouse_id,
    }]))

    assert_that(len(actual_service_rows), 1)

    basic_rows = list(yt_client.lookup_rows(config.yt_basic_offers_tablepath, [{
        'business_id': business_id,
        'shop_sku': offer_id,
    }]))

    assert_that(len(basic_rows), 1)
