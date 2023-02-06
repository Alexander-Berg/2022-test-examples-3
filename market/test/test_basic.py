#!/usr/bin/python
# -*- coding: utf-8 -*-

import pytest

from six.moves import zip_longest
from yql_utils import yql_binary_path
from market.idx.pylibrary.yql.yql_query import YqlExecutor, YqlQueryException


@pytest.fixture(params=[
    {
        'query': '''
            INSERT INTO local.`//tmp/foo`
            SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3;
        ''',
        'expected_result': None,
    },
    {
        'query': '''
            INSERT INTO local.[//tmp/foo]
            SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3;
        ''',
        'expected_error': 'Failed to parse SQL',
    },

    {
        'query': 'SELECT (COUNT(*) * COUNT(*)) AS c FROM local.`tmp/foo`;',
        'expected_result': [[(9,)]],
    },
    {
        'query': 'SELECT YQL::Concat(\"FOO\", \"bar\");',
        'expected_result': [[('FOObar',)]],
    },
    {
        'query': 'Some Incorrect Query',
        'expected_error': 'Error: Parse Sql'
    },
],
    ids=[
        'CreateTable',
        'CreateTableOldSyntax',
        'ReadTable',
        'LocalCalculate',
        'WrongSyntax',
    ],
    scope='module'
)
def query(request):
    return request.param


@pytest.fixture(scope='module')
def mrjob():
    return yql_binary_path('yql/tools/mrjob/mrjob')


@pytest.fixture(scope='module')
def udf_resolver():
    return yql_binary_path('yql/tools/udf_resolver/udf_resolver')


@pytest.fixture(scope='module')
def yql_executor(yt_server, mrjob, udf_resolver):
    return YqlExecutor(yt_server.get_server(), mrjob=mrjob, udf_resolver=udf_resolver, syntax_version=1)


def test_execute(yql_executor, query):
    try:
        actual_result = yql_executor.yql_waiting_execute(None, query['query'], use_embedded=True)
        expected_result = query['expected_result']
        if expected_result is None:
            assert actual_result.raw_data is None
        else:
            for actual_table, expected_table in zip_longest(actual_result, expected_result):
                actual_table.fetch_full_data()
                for executed_row, expected_row in zip_longest(actual_table.rows, expected_table):
                    assert executed_row == expected_row

    except YqlQueryException as e:
        assert query['expected_error'] in str(e)
