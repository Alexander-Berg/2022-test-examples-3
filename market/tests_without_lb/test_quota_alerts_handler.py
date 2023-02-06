# coding=utf-8

import json
import pytest

from hamcrest import assert_that, has_entries, has_item, has_key, not_, all_of, has_entry, empty

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.text_matchers import IsSerializedJson
from market.idx.yatf.test_envs.its_server import ItsAppMock


@pytest.fixture(scope='module')
def its_server_mock(its_get_responses):
    with ItsAppMock(its_get_responses) as server:
        yield server


@pytest.fixture()
def history_cleaner(its_server_mock):
    its_server_mock.reset_history()


@pytest.fixture(scope='module')
def its_get_responses():
    default_cfg = json.dumps({
        'restart_service': True,
        'env_conf': [{'file': 'env/its.cfg', 'values': {
            'IGNORE_NEW_OFFERS_BY_QUOTA_FOR_EDA': 'true'
        }}]
    })
    full_response = {
        'market/datacamp/piper-white/testing-datacamp-piper-white-man/market_datacamp_auto_settings': {
            'value': default_cfg,
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/piper-white/production-datacamp-piper-white-man/market_datacamp_auto_settings': {
            'value': default_cfg,
            'version': '60e456bb3981d3f9fcf07cf4',
        },
    }
    return {
        '/v1/values/market/datacamp/': json.dumps(full_response)
    }


@pytest.fixture(scope='module')
def config(its_server_mock, yt_server):
    cfg = {
        'alerts_handler': {
            'its_path': its_server_mock.server.url,
            'its_token_path': '',
            'juggler_token_path': '',
            'datacamp_path': 'market/datacamp/',
        },
        'quota_alerts': {
            'enable': True,
            'ruchkas_regex': 'testing-datacamp-((\\bpiper-white\\b)|(\\bqpiper\\b)|(\\bscanner\\b))-((\\bsas\\b)|(\\bvla\\b)|(\\bman\\b))',
            'host_name': 'quota-alert-host',
            'service_name': 'quota-alert-service'
        }
    }
    return RoutinesConfigMock(config=cfg, yt_server=yt_server)


@pytest.yield_fixture(scope='module')
def routines_http(config, yt_server):
    resources = {'config': config}
    with HttpRoutinesTestEnv(yt_server=yt_server, **resources) as routines_http_env:
        yield routines_http_env


def assert_post_requests(requests, service_path, env_vars):
    assert_that(requests, has_entries({
        service_path: IsSerializedJson(has_entries({
            'value': IsSerializedJson(
                has_entries({
                    'env_conf': has_item(has_entries({
                        'values': all_of(*[not_(has_key(var)) if val is None else has_entry(var, val) for var, val in env_vars.items()])
                    }))
                })
            )
        }))
    }))


def test_enable_degradation_mode(its_server_mock, routines_http, history_cleaner):
    response = routines_http.post(
        '/juggler_alert_handler',
        data=json.dumps({
            'checks': [{
                'host_name': 'quota-alert-host',
                'service_name': 'quota-alert-service',
                'status': 'CRIT',
                'children': [{
                    'host_name': 'datacamp-state-quotas-testing',
                    'service_name': 'market-offers-count-quota',
                    'status': 'CRIT'
                }]
            }]
        }),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )

    assert_that(response, HasStatus(202))
    assert_post_requests(its_server_mock.input_post, '/v1/values/market/datacamp/piper-white/testing-datacamp-piper-white-man/market_datacamp_auto_settings/', {
        'IGNORE_NEW_OFFERS_BY_QUOTA_FOR_MARKET': 'true',
        'IGNORE_NEW_OFFERS_BY_QUOTA_FOR_EDA': 'true'
    })


def test_disable_degradation_mode(its_server_mock, routines_http, history_cleaner):
    response = routines_http.post(
        '/juggler_alert_handler',
        data=json.dumps({
            'checks': [{
                'host_name': 'quota-alert-host',
                'service_name': 'quota-alert-service',
                'status': 'OK',
                'children': [{
                    'host_name': 'datacamp-state-quotas-testing',
                    'service_name': 'eda-offers-count-quota',
                    'status': 'OK'
                }]
            }]
        }),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )

    assert_that(response, HasStatus(202))
    assert_post_requests(its_server_mock.input_post, '/v1/values/market/datacamp/piper-white/testing-datacamp-piper-white-man/market_datacamp_auto_settings/', {
        'IGNORE_NEW_OFFERS_BY_QUOTA_FOR_EDA': None
    })


def test_do_not_trigger_without_changes(its_server_mock, routines_http, history_cleaner):
    response = routines_http.post(
        '/juggler_alert_handler',
        data=json.dumps({
            'checks': [{
                'host_name': 'quota-alert-host',
                'service_name': 'quota-alert-service',
                'status': 'OK',
                'children': [{
                    'host_name': 'datacamp-state-quotas-testing',
                    'service_name': 'market-offers-count-quota',
                    'status': 'OK'
                }, {
                    'host_name': 'datacamp-state-quotas-testing',
                    'service_name': 'eda-offers-count-quota',
                    'status': 'CRIT'
                }]
            }]
        }),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )

    assert_that(response, HasStatus(202))
    assert_that(its_server_mock.input_post, empty())
