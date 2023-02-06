import pytest
import mock
import ujson as json
import psycopg2
from hamcrest import assert_that, equal_to
from mail.settings.scripts.settings_modification.settings_modification import (
    get_master_dsn,
    get_shards_name_from_args,
    run_select,
    get_settings,
    filter_dsn,
    run_query,
    Worker,
    CreateTaskResult,
)


@pytest.mark.parametrize(('sharpei_stat', 'expected'), (
    (
        """{
           "1" : {
              "name" : "shard1",
              "id" : "1",
              "databases" : [
                 {
                    "role" : "master",
                    "status" : "alive",
                    "address" : {
                       "dbname" : "maildb",
                       "host" : "shard1m.yandex.net",
                       "port" : "6432"
                    }
                 },
                 {
                    "role" : "replica",
                    "status" : "alive",
                    "address" : {
                       "dbname" : "maildb",
                       "host" : "shard1r.yandex.net",
                       "port" : "6432"
                    }
                 }
              ]
           },
        }""",
        [["shard1", 'host=shard1m.yandex.net dbname=maildb user=settings port=6432']]
    ),
    (
        """{
           "1" : {
              "name" : "shard1",
              "id" : "1",
              "databases" : [
                 {
                    "role" : "replica",
                    "status" : "alive",
                    "address" : {
                       "dbname" : "maildb",
                       "host" : "shard1r.yandex.net",
                       "port" : "6432"
                    }
                 }
              ]
           },
        }""",
        []
    ),
))
def test_get_database_dsn(sharpei_stat, expected):
    dsns = [[name, dsn] for (name, dsn) in get_master_dsn(json.loads(sharpei_stat), 'settings')]
    assert_that(dsns, equal_to(expected))


@pytest.mark.parametrize(('settings', 'expected'), (
    (
        '{"test":"test"}',
        '{"single_settings":{"test":"test"}}'
    ),
    (
        '{"test":"", "hello":"kitty"}',
        '{"single_settings":{"test":"","hello":"kitty"}}'
    ),
    (
        '{"+": "-"}',
        None
    ),
    (
        'test',
        None
    ),
))
def test_get_settings_(settings, expected):
    assert_that(get_settings(settings), equal_to(expected))


@pytest.mark.parametrize(('dsns', 'shards_arg', 'expected'), (
    (
        {'shard1': 'dsn1', 'shard2': 'dsn2', 'shard3': 'dsn3'},
        [],
        {'shard1': 'dsn1', 'shard2': 'dsn2', 'shard3': 'dsn3'}
    ),
    (
        {'shard1': 'dsn1', 'shard2': 'dsn2', 'shard3': 'dsn3'},
        ['shard1', 'shard2'],
        {'shard1': 'dsn1', 'shard2': 'dsn2'}
    ),
    (
        {'shard1': 'dsn1', 'shard2': 'dsn2', 'shard3': 'dsn3'},
        ['shard4'],
        {}
    ),
))
def test_filter_dsn(dsns, shards_arg, expected):
    assert_that(filter_dsn(dsns, shards_arg), equal_to(expected))


@pytest.mark.parametrize(('shards_arg', 'expected'), (
    (
        'shard1,shard2,shard3',
        ['shard1', 'shard2', 'shard3'],
    ),
    (
        'all',
        [],
    ),
    (
        'shard1 , shard2',
        ['shard1', 'shard2'],
    ),
))
def test_get_shards_name_from_args(shards_arg, expected):
    assert_that(get_shards_name_from_args(shards_arg), equal_to(expected))


@mock.patch('psycopg2.connect')
def test_get_exception_on_run_select_should_retry_query(mock_connect):
    pg_conn = mock_connect.return_value.__enter__.return_value
    pg_cursor = pg_conn.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = [psycopg2.DataError(), [dict(status=True)]]
    assert_that(run_select(pg_conn, 'test', CreateTaskResult),  equal_to([CreateTaskResult(True)]))
    assert_that(pg_cursor.__enter__.return_value.execute.call_count, equal_to(2))


@mock.patch('psycopg2.connect')
def test_get_exception_on_run_query_should_retry_query(mock_connect):
    pg_conn = mock_connect.return_value.__enter__.return_value
    pg_cursor = pg_conn.cursor.return_value
    pg_cursor.__enter__.return_value.execute.side_effect = [psycopg2.DataError(), []]
    pg_cursor.__enter__.return_value.rowcount = 1
    assert_that(run_query(pg_conn, 'test'),  equal_to(1))
    assert_that(pg_cursor.__enter__.return_value.execute.call_count, equal_to(2))


@mock.patch('psycopg2.connect')
def test_for_unsuccessful_create_task_should_return_false(mock_connect):
    pg_conn = mock_connect.return_value.__enter__.return_value
    pg_cursor = pg_conn.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = [[dict(status=False)]]
    assert_that(Worker('update_for_all', 'test').prologue(pg_conn), equal_to(False))
    assert_that(pg_cursor.__enter__.return_value.execute.call_count, equal_to(2))


@mock.patch('psycopg2.connect')
def test_for_unsuccessful_prologue_should_return_false_and_dsn(mock_connect):
    pg_conn = mock_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = [[dict(status=False)]]
    assert_that(Worker('update_for_all', 'test')(['name', 'dsn']), equal_to([False, 'name']))


@mock.patch('psycopg2.connect')
def test_for_successful_prologue_should_return_true_and_dsn(mock_connect):
    pg_conn = mock_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = [[dict(status=True)]]
    assert_that(Worker('update_for_all', 'test')(['name', 'dsn']), equal_to([True, 'name']))
    assert_that(pg_cursor.__enter__.return_value.execute.call_count, equal_to(2))


@mock.patch('psycopg2.connect')
def test_for_exception_should_return_false_and_dsn(mock_connect):
    pg_conn = mock_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = Exception('fail')
    assert_that(Worker('run_task', 'test')(['name', 'dsn']), equal_to([False, 'name']))


@mock.patch('psycopg2.connect')
def test_for_successful_modification_empty_data_base_should_return_true_and_dsn(mock_connect):
    pg_conn = mock_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = [[dict(count=0)]]
    assert_that(Worker('run_task', 'test')(['name', 'dsn']), equal_to([True, 'name']))
    assert_that(pg_cursor.__enter__.return_value.execute.call_count, equal_to(1))


@mock.patch('psycopg2.connect')
def test_for_successful_modification_should_return_true_and_dsn(mock_connect):
    pg_conn = mock_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = [
        [dict(count=100)], [dict(count=0)]
    ]
    assert_that(Worker('run_task', 'test')(['name', 'dsn']), equal_to([True, 'name']))
    assert_that(pg_cursor.__enter__.return_value.execute.call_count, equal_to(2))


def test_for_not_exists_operation_return_false_and_dsn():
    assert_that(Worker('tutu', 'test')(['name', 'dsn']), equal_to([False, 'name']))


@mock.patch('psycopg2.connect')
def test_for_successful_unlock_should_return_true_and_dsn(mock_connect):
    pg_conn = mock_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = [
        [dict(count=1)]
    ]
    assert_that(Worker('unlock', 'test')(['name', 'dsn']), equal_to([True, 'name']))
    assert_that(pg_cursor.__enter__.return_value.execute.call_count, equal_to(1))


@mock.patch('psycopg2.connect')
def test_for_successful_result_of_count_uids_for_update_should_return_true_and_dsn(mock_connect):
    pg_conn = mock_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchall.side_effect = [[dict(count=100)]]
    assert_that(Worker('count_uids_for_update', 'test')(['name', 'dsn']), equal_to([True, 'name']))
    assert_that(pg_cursor.__enter__.return_value.execute.call_count, equal_to(1))
