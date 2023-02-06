# coding=utf-8

import pytest
import yatest
import requests_mock
import json
from market.idx.pylibrary.juggler import JugglerClient


@pytest.fixture(scope='module')
def mock_requests():
    real_juggler = yatest.common.get_param('real_juggler')
    if real_juggler and real_juggler.lower() in ['true', '1', 't', 'y', 'yes']:
        yield False
        return
    with requests_mock.Mocker() as m:
        m.register_uri(requests_mock.ANY, requests_mock.ANY, text='{}')
        yield True


def test_get_checks_config(mock_requests):
    client = JugglerClient()
    result = client.get_checks_config(
        filters=[
            {"tags": ["market_indexer_datacamp", "market_production"]},
            {"tags": ["market_indexer_datacamp", "market_prestable"]},
        ],
        limit=2,
        offset=0,
        project='market.datacamp',
    )
    print(json.dumps(result, indent=2))


def test_get_checks_count(mock_requests):
    client = JugglerClient()
    result = client.get_checks_count(
        filters=[
            {"tags": ["market_indexer_datacamp", "market_production"]},
            {"tags": ["market_indexer_datacamp", "market_prestable"]},
        ],
        project='market.datacamp',
    )
    print(json.dumps(result, indent=2))


def test_get_checks_state(mock_requests):
    client = JugglerClient()
    result = client.get_checks_state(
        filters=[
            {"tags": ["market_indexer_datacamp", "market_production"]},
            {"tags": ["market_indexer_datacamp", "market_prestable"]},
        ],
        include_mutes=True,
        limit=2,
        project='market.datacamp',
        sort={"field": "DEFAULT", "order": "DESC"},
        statuses=['CRIT', 'WARN'],
    )
    print(json.dumps(result, indent=2))


def test_get_check_history(mock_requests):
    client = JugglerClient()
    result = client.get_check_history(
        host='mi-datacamp-white',
        page=0,
        page_size=20,
        service='ping',
        since=1649883600,
        statuses=['CRIT', 'WARN'],
        until=1649969999,
    )
    print(json.dumps(result, indent=2))


def test_get_check_snapshot(mock_requests):
    client = JugglerClient()
    result = client.get_check_snapshot(state_id='LTE3NDE0ODQxNzM5NTM5NzYwMDY6MTY0OTk0MDE5MA==')
    print(json.dumps(result, indent=2))


def test_get_notifications(mock_requests):
    client = JugglerClient()
    result = client.get_notifications(
        filters=[{"login": "razmser"}], page=0, page_size=100, since=1651006800, until=1651093200
    )
    print(json.dumps(result, indent=2))


def test_get_raw_events(mock_requests):
    client = JugglerClient()
    result = client.get_raw_events(
        filters=[
            {"tags": ["market_indexer_datacamp", "market_production"]},
            {"tags": ["market_indexer_datacamp", "market_prestable"]},
        ],
        limit=2,
        offset=0,
        sort={"field": "DEFAULT", "order": "DESC"},
        statuses=['CRIT', 'WARN'],
    )
    print(json.dumps(result, indent=2))


def test_get_escalations_log(mock_requests):
    client = JugglerClient()
    result = client.get_escalations_log(
        filters=[
            {"tags": ["market_disaster", "market_datacamp_disaster"]},
            {"tags": ["market_disaster", "market_datacamp_disaster_no_night_calls"]},
        ],
        only_running=False,
        page=0,
        page_size=100,
        project='market.indexer',
    )
    print(json.dumps(result, indent=2))
