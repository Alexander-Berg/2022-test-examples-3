from __future__ import absolute_import

import datetime

import mock
import pytest

import crypta.lib.python.yql.client as yql
from crypta.lib.python.yql.client.test import common


class MockTable(object):

    rows = [(1,)]


class MockHandle(object):

    is_success = True
    is_ok = True

    operation_id = 'aaaaa'
    query = 'test'

    json = {'username': '%USERNAME%'}

    def get_results(self, **kwargs):
        yield MockTable()

    def run(self, **kwargs):
        pass

    def explain(self, **kwargs):
        return MockPlanHandle()

    @property
    def share_url(self):
        return 'mock_share_url'


class MockPlanHandle(MockHandle):

    def get_results(self):
        return []

    @property
    def plan(self):
        return 'mock_plan'

    @property
    def ast(self):
        return 'mock_ast'


class MockYql(object):

    def __init__(self, **kwargs):
        pass

    def query(self, query, **kwargs):
        return MockHandle()


@pytest.fixture
def mock_yql():
    with mock.patch('crypta.lib.python.yql.client.create_delegate_yql_client', side_effect=MockYql) as patch:
        yield patch


@pytest.fixture
def mock_yt():
    with mock.patch('crypta.lib.python.yql.client.yt.YtClient', side_effect=common.MockYtClient) as patch:
        yield patch


@pytest.fixture
def mock_all(mock_yt, mock_yql):
    yield


def test_pragma_transaction():
    assert yql.pragma_transaction(None) == '-- no transaction'
    assert yql.pragma_transaction('777') == 'PRAGMA yt.ExternalTx="777";'


def test_pragma_pool():
    assert yql.pragma_pool(None) == '-- default pool'
    assert yql.pragma_pool('pool') == 'PRAGMA yt.StaticPool="pool";'


def test_pragma_weight():
    assert yql.pragma_weight(None) == '-- default weight'
    assert yql.pragma_weight(2) == 'PRAGMA yt.DefaultOperationWeight="2";'


def test_pragma_operation_spec():
    assert yql.pragma_operation_spec(None) == '-- default operation spec'
    assert yql.pragma_operation_spec({'some_key': 'some_value', 'empty_valued_key': ''}) == "PRAGMA yt.OperationSpec='{\"some_key\"=\"some_value\";\"empty_valued_key\"=\"\";}';"
    assert yql.pragma_operation_spec({'key_with_\'': 'value_with_\''}) == "PRAGMA yt.OperationSpec='{\"key_with_\\'\"=\"value_with_\\'\";}';"


def test_pragma_tmp_folder():
    assert yql.pragma_tmp_folder(None) == '-- default tmp folder'
    assert yql.pragma_tmp_folder('path/to/tmp') == 'PRAGMA yt.TmpFolder="//path/to/tmp";'
    assert yql.pragma_tmp_folder('//path/to/tmp') == 'PRAGMA yt.TmpFolder="//path/to/tmp";'


def test_yt_shortname():
    assert yql.yt_shortname('hahn.yt.yandex.net') == 'hahn'
    assert yql.yt_shortname('hahn') == 'hahn'


def test_query_with_pragmas():
    return yql.query_with_pragmas(
        'SELECT 1;',
        pool='crypta',
        transaction='777',
        tmp_folder="//path/to/tmp",
        weight=1,
        operation_spec={"key_with_'": "value_with_'", "empty": ""},
        compression_codec='brotli_3',
        erasure_codec='lrc_12_2_2',
        udfs=[yql.Udf("lib.so", "http://url.com")],
        yql_libs=[
            "$square = ($x) -> { RETURN $x * $x; };",
        ],
        binary_cache_tmp_folder="//tmp/binary",
        binary_cache_ttl=datetime.timedelta(days=1),
    )


def test_query_with_max_job_count():
    return yql.query_with_pragmas(
        'SELECT 1;',
        pool='crypta',
        transaction='777',
        tmp_folder=None,
        weight=1,
        operation_spec={"key_with_'": "value_with_'", "empty": ""},
        max_job_count=100500,
    )


def test_explain(mock_all):
    query = 'SELECT 1;'
    client = yql.CustomYqlClient('hahn.yt.yandex.net', 'AQDF')
    plan, ast = client.explain(query)
    assert plan == 'mock_plan'
    assert ast == 'mock_ast'


def test_custom_yql_client(mock_all):
    client = yql.CustomYqlClient('hahn.yt.yandex.net', 'AQDF')
    assert client.execute('SELECT 1;')


def test_create_yql_client(mock_all):
    client = yql.create_yql_client(yt_proxy='hahn.yt.yandex.net', token='AQDF')
    assert isinstance(client, yql.CustomYqlClient)
    for i, table in enumerate(client.execute('SELECT 1; SELECT 2')):
        assert table.rows == [(i+1,)]


def test_create_delegate():
    delegate = yql.create_delegate_yql_client()
    assert delegate


def test_path():
    client = yql.create_yql_client(yt_proxy='hahn.yt.yandex.net', token='AQDF')
    path = '//tmp/test'
    assert client.path(path) == 'tmp/test'
