# coding: utf-8

import mock
from ora2pg.clone_user.source_user_fetcher import (
    PGSourceFetcher)
from helpers import mk_source_user


def sf_patch(target):
    return mock.patch(
        'ora2pg.clone_user.source_user_fetcher.' + target,
        autospec=True,
    )


def test_pg_source_fetcher():
    with \
        sf_patch('pg_rorr_transaction') as pg_transaction_mock, \
        sf_patch('get_pg_user') as get_pg_user_mock \
    :
        fake_user_data = mock.Mock()
        get_pg_user_mock.side_effect = [fake_user_data]
        source_user = mk_source_user('pg')
        fetcher = PGSourceFetcher(
            user=source_user,
            shard_id=1,
            pg_dsn='PG_DSN',
            min_received_date=None,
            max_received_date=None,
        )
        with fetcher.fetch() as user_data:
            assert user_data is fake_user_data

        pg_transaction_mock.assert_called_once_with(
            'PG_DSN'
        )
        get_pg_user_mock.assert_called_once_with(
            uid=source_user.uid,
            conn=mock.ANY,
            min_received_date=None,
            max_received_date=None,
        )
