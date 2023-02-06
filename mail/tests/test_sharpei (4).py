from contextlib import contextmanager
from collections import namedtuple
from itertools import product

import mock
import pytest
from psycopg2.errorcodes import UNIQUE_VIOLATION
from ora2pg.sharpei import (
    IntegrityError,
    init_in_sharpei,
    QUERIES,
    UserAlreadyInited,
    group_by_shards,
    http_request,
)
from mail.pypg.pypg.fake_cursor import FakeCursor

UID = 100
DSN = 'fake sharddb dsn'
SHARD_ID = 666


parametrize = pytest.mark.parametrize


@contextmanager
def mocked_queries(queries2results):
    def side_effects(conn, query, **kwargs):  # pylint: disable=W0613
        return queries2results.get(query, None)

    with mock.patch('ora2pg.sharpei.qexec') as mocked:
        mocked.side_effect = side_effects
        yield mocked


@mock.patch('ora2pg.sharpei.qexec')
@mock.patch('ora2pg.sharpei.get_sharddb_pooled_conn')
def test_init_in_sharpei_calls_save_user_in_shard(mocked_pooled_conn, mocked_qexec):
    mocked_conn = mocked_pooled_conn().__enter__()
    init_in_sharpei(UID, DSN, True, SHARD_ID)
    mocked_qexec.assert_called_once_with(
        mocked_conn,
        QUERIES.save_user_in_shard,
        uid=UID,
        shard_id=SHARD_ID
    )


@mock.patch('ora2pg.sharpei.get_sharddb_pooled_conn')
def test_init_in_sharpei_empty_shard_id(mocked_pooled_conn):
    mocked_conn = mocked_pooled_conn().__enter__()
    with mocked_queries({
        QUERIES.generate_shard_id: FakeCursor(['shard_id'], [[SHARD_ID]]),
    }) as mocked_qexec:
        init_in_sharpei(UID, DSN, True, SHARD_ID)
        mocked_qexec.assert_called_with(
            mocked_conn,
            QUERIES.save_user_in_shard,
            uid=UID,
            shard_id=SHARD_ID
        )


class FakeIntegrityError(IntegrityError):
    def __init__(self, fake_pgcode):
        self.fake_pgcode = fake_pgcode

    @property
    def pgcode(self):
        return self.fake_pgcode


def raise_integrity_for_insert(pgcode=UNIQUE_VIOLATION):
    def side_effect(conn, query, **kwargs):  # pylint: disable=W0613
        if query == QUERIES.save_user_in_shard:
            raise FakeIntegrityError(pgcode)
    return side_effect


@mock.patch('ora2pg.sharpei.qexec')
@mock.patch('ora2pg.sharpei.get_sharddb_pooled_conn')
@mock.patch('ora2pg.sharpei.update_shard_id')
def test_init_in_sharpei_integrity_error(
        mocked_update_shard_id,
        mocked_pooled_conn,
        mocked_qexec,
):
    mocked_qexec.side_effect = raise_integrity_for_insert()
    init_in_sharpei(UID, DSN, True, SHARD_ID)
    mocked_update_shard_id.assert_called_once_with(
        DSN,
        UID,
        SHARD_ID,
    )


@mock.patch('ora2pg.sharpei.qexec')
@mock.patch('ora2pg.sharpei.get_sharddb_pooled_conn')
@mock.patch('ora2pg.sharpei.update_shard_id')
def test_init_in_sharpei_integrity_error_not_allowed(
        mocked_update_shard_id,
        mocked_pooled_conn,
        mocked_qexec,
):
    mocked_qexec.side_effect = raise_integrity_for_insert()
    with pytest.raises(UserAlreadyInited):
        init_in_sharpei(UID, DSN, False, SHARD_ID)


@mock.patch('ora2pg.sharpei.qexec')
@mock.patch('ora2pg.sharpei.get_sharddb_pooled_conn')
@mock.patch('ora2pg.sharpei.update_shard_id')
def test_init_in_sharpei_integrity_error_not_unique_viol(
        mocked_update_shard_id,
        mocked_pooled_conn,
        mocked_qexec,
):
    mocked_qexec.side_effect = raise_integrity_for_insert(None)
    with pytest.raises(IntegrityError):
        init_in_sharpei(UID, DSN, False, SHARD_ID)


def get_pg_dsn_from_sharpei_side_effect(sharpei, uid, dsn_suffix):
    assert isinstance(uid, int)
    if uid >= 1000:
        return 'big-dsn'
    return 'small-dsn'


def test_group_by_shards_for_uids_list():
    with mock.patch(
        'ora2pg.sharpei.get_pg_dsn_from_sharpei',
        autospec=True
    ) as get_dsn_mock:
        get_dsn_mock.side_effect = get_pg_dsn_from_sharpei_side_effect
        assert group_by_shards(
            'test://sharpei',
            'dsn_suffix',
            [1, 2, 10000, 20000]
        ) == {'small-dsn': [1, 2], 'big-dsn': [10000, 20000]}


def test_group_by_shards_for_iterable_of_objects():
    Obj = namedtuple('Obj', ['uid', 'payload'])
    with mock.patch(
        'ora2pg.sharpei.get_pg_dsn_from_sharpei',
        autospec=True
    ) as get_dsn_mock:
        get_dsn_mock.side_effect = get_pg_dsn_from_sharpei_side_effect
        assert group_by_shards(
            'test://sharpei',
            'dsn_suffix',
            [Obj(1, 'xxx'), Obj(10000, 'yyy')],
            lambda o: o.uid
        ) == {'small-dsn': [Obj(1, 'xxx')], 'big-dsn': [Obj(10000, 'yyy')]}


@parametrize(
    ('url', 'do_retries'),
    product(['sharpei_url'], [False, None, True])
)
@mock.patch('ora2pg.tools.http.request')
def test_no_retry_on_4xx(mocked_request, url, do_retries):
    if do_retries:
        http_request(url, do_retries)
    else:
        http_request(url)
        do_retries = False
    mocked_request.assert_called_once_with(url=url, do_retries=do_retries, skip_retry_codes=[400, 404])
