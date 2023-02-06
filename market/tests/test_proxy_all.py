# coding: utf-8
import base64

import pytest

from lib.app import create_flask_app
from lib.settings import Settings
from lib.blueprints.proxy_all import parse_request_values


@pytest.fixture(scope='module')
def app():
    settings = Settings(statefile='./state.json')
    return create_flask_app(settings)


@pytest.mark.skip(reason="insert CH password to run this test")
def test_proxy_show_databases(app):
    with app.test_client() as client:
        password = ''  # insert password to run the test
        resp = client.post('/query',
                           data="show databases",
                           headers={"Authorization": "Basic %s" % base64.b64encode("chaas__cubes:" + password)})
        print(resp)
        assert resp.status_code == 200
        assert resp.data == 'cubes\nsystem\n'


@pytest.mark.parametrize(
    'test_case',
    [
        {
            'request': {
                'query_string': 'database=db&default_format=format',
                'headers': {
                    'Authorization': 'Basic %s' % (
                        base64.b64encode('clickhouse__user:pass'.encode('utf-8'))
                    ).decode()
                },
                'body': 'SELECT 1 AS fieldname;',
            },
            'expected_query': 'SELECT 1 AS fieldname;',
            'expected_database_type': 'clickhouse',
            'expected_username': 'user',
            'expected_password': 'pass',
            'expected_database_name': 'db',
            'expected_cluster': None,
            'expected_query_options': {
                'database': 'db',
                'default_format': 'format'
            }
        },
        {
            'request': {
                'query_string': 'database=db&default_format=format',
                'headers': {
                    'Authorization': 'Basic %s' % (
                        base64.b64encode('chyt__clique__cluster:token'.encode('utf-8'))
                    ).decode()
                },
                'body': 'SELECT 1 AS fieldname;',
            },
            'expected_query': 'SELECT 1 AS fieldname;',
            'expected_database_type': 'chyt',
            'expected_username': 'clique',
            'expected_password': 'token',
            'expected_database_name': 'clique',
            'expected_cluster': 'cluster',
            'expected_query_options': {
                'database': 'db',
                'default_format': 'format'
            }
        }
    ]
)
def test_request_parsing(test_case):
    (
        query,
        database,
        username,
        password,
        database_name,
        cluster,
        query_options
    ) = parse_request_values(
        query_string=test_case['request']['query_string'],
        headers=test_case['request']['headers'],
        body=test_case['request']['body']
    )

    assert query == test_case['expected_query']
    assert database.database_type == test_case['expected_database_type']
    assert username == test_case['expected_username']
    assert password == test_case['expected_password']
    assert database_name == test_case['expected_database_name']
    assert cluster == test_case['expected_cluster']
    assert query_options == test_case['expected_query_options']
