# coding: utf-8

import contextlib
import copy
import json
import os.path
import pytest
import requests
import yatest

from hamcrest import assert_that, equal_to, none, has_entries

from yatest.common.network import PortManager

from market.idx.quick.saashub.yatf.test_envs.its import Its
from market.idx.quick.saashub.yatf.test_envs.saas_hub import SaasHubTestEnv
from market.idx.quick.saashub.yatf.resources.saas_hub_config import SaasHubConfig
from market.idx.yatf.test_envs.saas_env import SaasEnv


ITS_OAUTH_TOKEN = 'ITS_NANNY_TOKEN'
EXP_ITS_HANDLE_NAME = 'market_report_experiment_flags'


@pytest.fixture(scope='module')
def port_manager():
    with PortManager() as port_manager:
        yield port_manager


@contextlib.contextmanager
def run_its_server(
    port_manager,
    oauth=ITS_OAUTH_TOKEN,
    handle_name=EXP_ITS_HANDLE_NAME,
    report_flags=None,
    script=None
):
    port = port_manager.get_port()
    flags = report_flags or {}
    with Its(
        port,
        oauth=ITS_OAUTH_TOKEN,
        handle_name=EXP_ITS_HANDLE_NAME,
        report_flags=flags,
        script=script
    ) as its:
        yield its


@pytest.fixture(scope='function')
def its_server(port_manager):
    with run_its_server(port_manager) as its:
        yield its


@pytest.fixture(scope='function')
def saas():
    rel_path = os.path.join('market', 'idx', 'yatf', 'resources', 'saas', 'stubs', 'market-idxapi')
    with SaasEnv(saas_service_configs=yatest.common.source_path(rel_path), prefixed=True) as saas:
        yield saas


@contextlib.contextmanager
def run_saas_hub(
    saas,
    use_its=False,
    its=None,
    its_host=None,
    its_port=None,
    its_token=ITS_OAUTH_TOKEN,
    its_attempts_count=None
):
    resources = {
        'saas_hub_cfg': SaasHubConfig()
            .with_exp_flags(
                saas,
                use_its,
                its_host=its_host,
                its_port=its_port or its.port,
                its_token=(its.config.oauth if its is not None else its_token),
                its_attempts_count=its_attempts_count)
    }
    with SaasHubTestEnv(**resources) as saas_hub:
        yield saas_hub


RTY_QPIPE_FLAG = 'test_value'
RTY_DYNAMICS_FLAG = {
    'default_value': 'default',
    'conditions': [
        {'condition': 'IS_SAS', 'value': 'sas'},
        {'condition': 'IS_VLA', 'value': 'vla'},
    ]
}


def get_exp_flags_url(saas_hub):
    return 'http://{host}:{port}/exp_flags'.format(host=saas_hub.host, port=saas_hub.http_port)


def set_flags(url, data=None):
    if data is None:
        request = '{}/set?login=yamake&comment=unit_test&rty_qpipe={}&rty_dynamics={}'.format(
            url, RTY_QPIPE_FLAG, json.dumps(RTY_DYNAMICS_FLAG))
        return requests.get(request)
    else:
        request = '{}/set?login=yamake&comment=unit_test'.format(url)
        return requests.post(request, data=data)


def check_flag_expression(flag, expression, response=None, response_json=None):
    data = response.json() if response else response_json
    assert_that(
        data.get(flag),
        has_entries(
            'default_value', equal_to(expression['default_value']),
            'conditions', equal_to(expression['conditions'])
        )
    )


def check_answer(response):
    assert_that(response.status_code, equal_to(200))
    data = response.json()
    assert_that(data.get('rty_qpipe'), has_entries('default_value', equal_to(RTY_QPIPE_FLAG)))
    check_flag_expression('rty_dynamics', RTY_DYNAMICS_FLAG, response_json=data)
    assert_that(data.get('rty_qbids'), none())


def check_signal(saas_hub, expected_value=0):
    signal = 'ReportExpFlags_exp_flags_differs_max'
    signals = saas_hub.tass(find_signals=[signal])
    assert_that(signals, has_entries(signal, equal_to(str(expected_value))))


@pytest.mark.parametrize('use_its', [
    False,
    True,
])
def test_base_logic(saas, its_server, use_its):
    with run_saas_hub(saas, its=its_server, use_its=use_its) as saas_hub:
        url = get_exp_flags_url(saas_hub)

        response = set_flags(url)
        assert_that(response.status_code, equal_to(200))
        check_signal(saas_hub)

        response = requests.get('{}/get'.format(url))
        check_answer(response)
        check_signal(saas_hub)


@pytest.mark.parametrize('use_its', [
    False,
    True,
])
def test_nothing_to_set(saas, its_server, use_its):
    with run_saas_hub(saas, its=its_server, use_its=use_its) as saas_hub:
        url = get_exp_flags_url(saas_hub)

        response = set_flags(url)
        assert_that(response.status_code, equal_to(200))
        check_signal(saas_hub)

        response = requests.get('{}/get'.format(url))
        check_answer(response)
        check_signal(saas_hub)

        response = set_flags(url, data=response.text)
        assert_that(response.status_code, equal_to(400))
        assert_that(response.json().get('ReportExpFlags'), has_entries('message', equal_to('nothing to set')))
        check_signal(saas_hub)

        response = requests.get('{}/get'.format(url))
        check_answer(response)
        check_signal(saas_hub)


def test_obsolate(saas, port_manager):
    report_flags = {
        'rty_dynamics': {
            'timestamp': 20000000000,
            'default_value': '0',
        },
        'rty_qpipe': {
            'timestamp': 20000000000,
            'default_value': '0',
        }
    }

    with run_its_server(port_manager, report_flags=report_flags) as its:
        with run_saas_hub(saas, its=its, use_its=True) as saas_hub:
            url = get_exp_flags_url(saas_hub)

            data = copy.deepcopy(report_flags)
            data['rty_dynamics']['timestamp'] = 1603224000
            data['rty_dynamics']['default_value'] = ''
            data['rty_qpipe']['default_value'] = ''

            response = set_flags(url, data=json.dumps(data))
            assert_that(response.status_code, equal_to(400))
            assert_that(
                response.json().get('ReportExpFlags'),
                has_entries('message', equal_to('obsolate flags: rty_dynamics')))

            response = requests.get('{}/get'.format(url))
            assert_that(response.status_code, equal_to(200))
            assert_that(response.json(), equal_to(report_flags))
            check_signal(saas_hub, expected_value=1)


def test_delete_flag(saas, port_manager):
    report_flags = {
        'rty_dynamics': {
            'timestamp': 1603224000,
            'default_value': '0',
        },
        'removed_from_report_flag': {
            'default_value': '0',
            'conditions': [
                {'condition': 'IS_SAS', 'value': '1'},
            ]
        }
    }
    with run_its_server(port_manager, report_flags=report_flags) as its:
        with run_saas_hub(saas, its=its, use_its=True) as saas_hub:
            url = get_exp_flags_url(saas_hub)

            response = set_flags(url)
            assert_that(response.status_code, equal_to(200))
            check_signal(saas_hub, expected_value=1)

            # flag 'removed_from_report_flag' must be removed
            response = requests.get('{}/get'.format(url))
            check_answer(response)
            assert_that(response.json().get('removed_from_report_flag'), none())
            check_signal(saas_hub, expected_value=0)

            # remove rty_dynamics
            data = json.dumps({'rty_dynamics': {}})
            response = set_flags(url, data)
            assert_that(response.status_code, equal_to(200))
            check_signal(saas_hub, expected_value=0)

            response = requests.get('{}/get'.format(url))
            assert_that(response.status_code, equal_to(200))
            data = response.json()
            assert_that(data.get('rty_qpipe'), has_entries('default_value', equal_to(RTY_QPIPE_FLAG)))
            assert_that(data.get('rty_dynamics'), none())
            check_signal(saas_hub, expected_value=0)


@pytest.mark.parametrize('script, check_get', [
    ('wrong_etag', True),
    ('wrong_oauth', True),
    ('timeout', False),
])
def test_its_errors(saas, port_manager, script, check_get):
    report_flags = {
        'rty_dynamics': {
            'timestamp': 1603224000,
            'default_value': '0',
        }
    }
    scripts = {
        'wrong_etag': {'POST': {'replace_etag': {'etag': 'xxx'}}},
        'wrong_oauth': {'POST': {'replace_oauth': {'oauth': ''}}},
        'timeout': {'POST': {'sleep': {'time': 5}}},
    }

    with run_its_server(port_manager, report_flags=report_flags, script=scripts[script]) as its:
        with run_saas_hub(saas, its=its, use_its=True) as saas_hub:
            url = get_exp_flags_url(saas_hub)
            response = set_flags(url)
            assert_that(response.status_code, equal_to(500))
            if check_get:
                response = requests.get('{}/get'.format(url))
                assert_that(response.status_code, equal_to(200))
                assert_that(response.json(), equal_to(report_flags))
                check_signal(saas_hub, expected_value=1)


@pytest.mark.parametrize('use_its', [
    False,
    True,
])
def test_its_error_with_kv(saas, use_its):
    with run_saas_hub(saas, use_its=use_its, its_host='xxx', its_port=10) as saas_hub:
        url = get_exp_flags_url(saas_hub)
        status_code = 500 if use_its else 200
        response = set_flags(url)
        assert_that(response.status_code, equal_to(status_code))
        response = requests.get('{}/get'.format(url))
        assert_that(response.status_code, equal_to(status_code))


@pytest.mark.parametrize('use_its', [
    False,
    True,
])
def test_synchronise_flags(saas, port_manager, use_its):
    report_flags = {
        'ext_debug': {
            'default_value': '0',
            'conditions': [
                {'condition': 'IS_SAS', 'value': '1'},
            ]
        },
        'rty_dynamics': {
            'default_value': '0',
            'conditions': [
                {'condition': 'IS_SAS', 'value': 'sas'},
            ]
        }
    }
    script = {
        'POST': {
            'error_on_request': {
                'request_counts': [3],
                'code': 500,
                'message': 'drop update'
            }
        },
    }
    with run_its_server(port_manager, script=(None if use_its else script)) as its:
        with run_saas_hub(saas, its=its, use_its=use_its, its_attempts_count=1) as saas_hub:
            url = get_exp_flags_url(saas_hub)

            # Saving the same flags
            response = set_flags(url)
            assert_that(response.status_code, equal_to(200))
            check_signal(saas_hub, expected_value=0)

            # Breaking the synchronization
            if not use_its:
                # Its will drop update
                data = {'ext_debug': report_flags['ext_debug']}
                response = set_flags(url, data=json.dumps(data))
                assert_that(response.status_code, equal_to(200))
                check_signal(saas_hub, expected_value=0)
            else:
                # Update its flags directly
                its.config.report_flags['ext_debug'] = report_flags['ext_debug']

            # Check that synchronization is broken
            response = requests.get('{}/get'.format(url))
            check_answer(response)
            check_flag_expression('ext_debug', report_flags['ext_debug'], response=response)
            check_signal(saas_hub, expected_value=1)

            # Update and synchronise flags
            data = {'rty_dynamics': report_flags['rty_dynamics']}
            response = set_flags(url, data=json.dumps(data))
            assert_that(response.status_code, equal_to(200))
            check_signal(saas_hub, expected_value=1)

            # Checking that the flags were synchronized
            response = requests.get('{}/get'.format(url))
            assert_that(response.status_code, equal_to(200))
            data = response.json()
            assert_that(data.get('rty_qpipe'), has_entries('default_value', equal_to(RTY_QPIPE_FLAG)))
            check_flag_expression('rty_dynamics', report_flags['rty_dynamics'], response_json=data)
            check_flag_expression('ext_debug', report_flags['ext_debug'], response_json=data)
            check_signal(saas_hub, expected_value=0)


def test_retry(saas, port_manager):
    script = {
        'GET': {
            'error_on_request': {
                'request_counts': [0, 1, 2, 8],
                'code': 500,
                'message': 'drop request'
            }

        },
        'POST': {
            'error_on_request': {
                'request_counts': [4, 5, 6],
                'code': 500,
                'message': 'drop update'
            }
        },
    }
    with run_its_server(port_manager, script=script) as its:
        with run_saas_hub(saas, its=its, use_its=True, its_attempts_count=5) as saas_hub:
            url = get_exp_flags_url(saas_hub)

            response = set_flags(url)
            assert_that(response.status_code, equal_to(200))
            check_signal(saas_hub)
            assert_that(its.config.request_count, equal_to(8))

            response = requests.get('{}/get'.format(url))
            check_answer(response)
            check_signal(saas_hub)
            assert_that(its.config.request_count, equal_to(10))


def test_escape_flags(saas, port_manager):
    report_flags = {
        '_dev_test_flag': {
            'timestamp': 1603224000,
            'default_value': '0',
            'conditions': [
                {
                    'condition': "CLUSTER > 14 and SUBROLE == 'fresh-base'",
                    'value': '{"type": "ROUND_ROBIN_VERSION_AWARE", "parameters": {"allow_preferred_hints": true}}'
                },
            ]
        },
    }
    with run_its_server(port_manager, report_flags=report_flags) as its:
        with run_saas_hub(saas, its=its, use_its=True) as saas_hub:
            url = get_exp_flags_url(saas_hub)

            # just get html page
            requests.get('{}/index.html'.format(url)).text
