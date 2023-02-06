from sss_lib import app
from sss_lib import SOY_API_RELOAD

import pytest
import responses
import yatest
import json

QUERIES_PATH = yatest.common.source_path('search/metrics/sss/tests/data/queries_configs.json')
SOY_RESPONSE_PATH = yatest.common.source_path('search/metrics/sss/tests/data/soy_response.json')
PARSED_PATH = yatest.common.source_path('search/metrics/sss/tests/data/parsed.json')


@pytest.fixture
def client():
    with app.test_client() as client:
        yield client


@pytest.fixture
def queries():
    with open(QUERIES_PATH) as queries_file:
        yield queries_file.read()


@pytest.fixture
def soy_response():
    with open(SOY_RESPONSE_PATH) as soy_response_file:
        soy_response = json.load(soy_response_file)
        responses.add(responses.POST, SOY_API_RELOAD, json=soy_response)
        yield soy_response


@pytest.fixture()
def parsed():
    with open(PARSED_PATH) as parsed_file:
        yield json.load(parsed_file)


@responses.activate
def test_empty_body(client):
    response = client.post('/prepare')
    check_response(response, 500, {"message": "'NoneType' object is not subscriptable"}, 0)


@responses.activate
def test_prepare_and_download(client, queries, soy_response):
    response = client.post('/prepare-and-download',
                           headers={'Content-Type': 'application/json'},
                           data=queries)
    check_response(response, 200, {'soy_response': soy_response}, 1)


@responses.activate
def test_prepare_and_download_and_parse(client, queries, soy_response, parsed):
    response = client.post('/prepare-and-download-and-parse',
                           headers={'Content-Type': 'application/json'},
                           data=queries)
    check_response(response, 200, {'soy_response': soy_response, 'parsed': parsed}, 1)


def check_response(response, expected_status_code, expected_payload, expected_call_count):
    assert expected_status_code == response.status_code
    assert expected_payload == response.json
    assert expected_call_count == len(responses.calls)
