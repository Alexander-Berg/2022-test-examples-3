from hamcrest import assert_that
from hamcrest.core.base_matcher import BaseMatcher
import json
import pytest
import time

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.yatf.test_envs.its_server import ItsAppMock
from market.pylibrary.putil.protector import retry


@pytest.fixture(scope='module')
def its_server_mock(its_get_responses):
    with ItsAppMock(its_get_responses) as server:
        yield server


@pytest.fixture(scope='module')
def its_server_mock_last_time(its_get_responses_last_time):
    with ItsAppMock(its_get_responses_last_time) as server:
        yield server


@pytest.fixture(scope='module')
def its_get_responses():
    full_response = {
        'market/datacamp/piper/production-datacamp-piper-white-man/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"piper.white.man.somefield": 1}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/piper/production-datacamp-piper-white-sas/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"piper.white.sas.somefield": 2}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/piper/production-datacamp-piper-white-vla/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"piper.white.vla.somefield": 3}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/scanner/production-datacamp-scanner-man/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"scanner.man.somefield": 1}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/scanner/production-datacamp-scanner-sas/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"scanner.sas.somefield": 2}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/scanner/production-datacamp-scanner-vla/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"scanner.vla.somefield": 3}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/stroller-white/production-datacamp-stroller-white-man/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"stroller_white.white.man.somefield": 1}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/stroller-white/production-datacamp-stroller-white-sas/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"stroller_white.white.sas.somefield": 2}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/stroller-white/production-datacamp-stroller-white-vla/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"stroller_white.white.vla.somefield": 3}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
    }
    return {
        '/v1/values/market/datacamp': json.dumps(full_response),
        '/v1/values/market/datacamp/': json.dumps(full_response),
    }


@pytest.fixture(scope='module')
def its_get_responses_last_time():
    full_response = {
        'market/datacamp/piper/production-datacamp-piper-white-man/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"piper.white.man.somefield": 1}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/piper/production-datacamp-piper-white-sas/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"piper.white.sas.somefield": 2}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/piper/production-datacamp-piper-white-vla/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"piper.white.vla.somefield": 3}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/scanner/production-datacamp-scanner-man/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"scanner.man.somefield": 1}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/scanner/production-datacamp-scanner-sas/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"scanner.sas.somefield": 2}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/scanner/production-datacamp-scanner-vla/market_datacamp_auto_settings': {
            'value': '{\n "restart_service": false,\n "robotPatches": {},\n "patches": {"scanner.vla.somefield": 3}\n}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
    }
    return {
        '/v1/values/market/datacamp': json.dumps(full_response),
        '/v1/values/market/datacamp/': json.dumps(full_response),
    }


@pytest.fixture(scope='session')
def post_results():
    return {
        'warn': {
            'scanner': {
                'vla': {
                    'blue': {
                        'data': {
                            "restart_service": True,
                            "robotPatches": {
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.meta/saas_force_send': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/binding/uc_mapping': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/disabled': 'ST_NONE',
                            },
                            "patches": {"scanner.vla.somefield": 3},
                        },
                        'url': '/v1/values/market/datacamp/scanner/production-datacamp-scanner-vla/market_datacamp_auto_settings/',
                    }
                },
                'man': {
                    'blue': {
                        'data': {
                            "restart_service": True,
                            "robotPatches": {
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.meta/saas_force_send': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/binding/uc_mapping': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/disabled': 'ST_NONE',
                            },
                            "patches": {"scanner.man.somefield": 1},
                        },
                        'url': '/v1/values/market/datacamp/scanner/production-datacamp-scanner-man/market_datacamp_auto_settings/',
                    }
                },
            },
            'piper': {
                'sas': {
                    'white': {
                        'data': {
                            "restart_service": True,
                            "robotPatches": {
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.meta/saas_force_send': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/binding/uc_mapping': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/disabled': 'ST_NONE',
                            },
                            "patches": {"piper.white.sas.somefield": 2},
                        },
                        'url': '/v1/values/market/datacamp/piper/production-datacamp-piper-white-sas/market_datacamp_auto_settings/',
                    }
                }
            },
        },
        'crit': {
            'scanner': {
                'vla': {
                    'blue': {
                        'data': {
                            "restart_service": True,
                            "robotPatches": {
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.meta/saas_force_send': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/binding/uc_mapping': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/disabled': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/result/card_status': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/result': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/allow_model_create_update': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/cpa_state': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/cpc_state': 'ST_NONE'
                            },
                            "patches": {"scanner.vla.somefield": 3},
                        },
                        'url': '/v1/values/market/datacamp/scanner/production-datacamp-scanner-vla/market_datacamp_auto_settings/',
                    }
                },
                'man': {
                    'blue': {
                        'data': {
                            "restart_service": True,
                            "robotPatches": {
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.meta/saas_force_send': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/binding/uc_mapping': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/disabled': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/result/card_status': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/result': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/allow_model_create_update': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/cpa_state': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/cpc_state': 'ST_NONE'
                            },
                            "patches": {"scanner.man.somefield": 1},
                        },
                        'url': '/v1/values/market/datacamp/scanner/production-datacamp-scanner-man/market_datacamp_auto_settings/',
                    }
                },
            },
            'piper': {
                'sas': {
                    'white': {
                        'data': {
                            "restart_service": True,
                            "robotPatches": {
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.meta/saas_force_send': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/binding/uc_mapping': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/disabled': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/result/card_status': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/result': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/allow_model_create_update': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/cpa_state': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/cpc_state': 'ST_NONE'
                            },
                            "patches": {"piper.white.sas.somefield": 2},
                        },
                        'url': '/v1/values/market/datacamp/piper/production-datacamp-piper-white-sas/market_datacamp_auto_settings/',
                    }
                }
            },
        },
    }


@pytest.fixture(scope='session')
def post_results_last_time():
    return {
        'crit': {
            'scanner': {
                'vla': {
                    'blue': {
                        'data': {
                            "restart_service": True,
                            "robotPatches": {
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.meta/saas_force_send': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/binding/uc_mapping': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/disabled': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/result/card_status': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.status/result': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/allow_model_create_update': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/cpa_state': 'ST_NONE',
                                'Proxy.Processors.Initializer.SubscriptionOverrides.SAAS_SUBSCRIBER.content/status/content_system_status/cpc_state': 'ST_NONE'
                            },
                            "patches": {"scanner.vla.somefield": 3},
                        },
                        'url': '/v1/values/market/datacamp/scanner/production-datacamp-scanner-vla/market_datacamp_auto_settings/',
                    }
                },
                'man': {
                    'blue': {}
                },
            },
            'piper': {
                'sas': {
                    'white': {}
                }
            },
        },
    }


class IsPostDataEqualsToJson(BaseMatcher):
    def __init__(self, matchJs, key):
        self.matchJs = matchJs
        self.key = key

    def _matches(self, item):
        if self.key not in item:
            return False
        item_key = json.loads(item[self.key])
        if 'value' not in item_key:
            return False
        valuejs = json.loads(item_key['value'])
        return valuejs == self.matchJs

    def describe_to(self, description):
        description.append_text('error on match ' + self.key + ' to ' + json.dumps(self.matchJs))


class IsRequestsMatch(BaseMatcher):
    def __init__(self, results, status):
        self.results = results
        self.status = status

    def _matches(self, item):
        count = 0
        if self.status in self.results:
            for service in self.results[self.status]:
                for dc in self.results[self.status][service]:
                    for color in self.results[self.status][service][dc]:
                        post_res = self.results[self.status][service][dc][color]
                        if 'data' not in post_res or 'url' not in post_res:
                            continue
                        count += 1
                        assert_that(item, IsPostDataEqualsToJson(post_res['data'], post_res['url']))
        return len(item) == count

    def describe_to(self, description):
        description.append_text('error on requests with status: ' + self.status)


@pytest.fixture(scope='module')
def config(its_server_mock, yt_server):
    cfg = {
        'alerts_handler': {
            'its_path': its_server_mock.server.url,
            'its_token_path': '',
            'juggler_token_path': '',
            'datacamp_path': 'market/datacamp',
            'cluster_type': 'production',
        },
        'saas_alerts': {
            'enable': True,
            'need_restart': True,
            'services': ['piper-white', 'scanner'],
            'timeout': 0,
            'host_name': 'mi-datacamp-saas',
            'service_name': 'saas-docs-age',
            'scanner_dcs': ['vla', 'man'],
            'piper_dcs': ['sas'],
            'scanner_processor_path': 'Proxy.Processors.UnitedSaasFilter.',
            'piper_processor_path': 'Proxy.Processors.UnitedSaasFilter.',
            'one_task_at_time': False,
            'ruchka_name': 'market_datacamp_auto_settings',
            'global_timeout': 0,
            'lock_path': ''
        }
    }
    return RoutinesConfigMock(config=cfg, yt_server=yt_server)


@pytest.fixture(scope='module')
def config_last_time(its_server_mock_last_time, yt_server):
    cfg = {
        'alerts_handler': {
            'its_path': its_server_mock_last_time.server.url,
            'its_token_path': '',
            'juggler_token_path': '',
            'datacamp_path': 'market/datacamp',
            'cluster_type': 'production',
        },
        'saas_alerts': {
            'enable': True,
            'need_restart': True,
            'services': ['piper-white', 'scanner'],
            'timeout': 0,
            'host_name': 'mi-datacamp-saas',
            'service_name': 'saas-docs-age',
            'scanner_dcs': ['vla', 'man'],
            'piper_dcs': ['sas'],
            'scanner_processor_path': 'Proxy.Processors.UnitedSaasFilter.',
            'piper_processor_path': 'Proxy.Processors.UnitedSaasFilter.',
            'one_task_at_time': False,
            'ruchka_name': 'market_datacamp_auto_settings',
            'global_timeout': 3600,
            'lock_path': '//tmp/last_update'
        }
    }
    return RoutinesConfigMock(config=cfg, yt_server=yt_server)


@pytest.yield_fixture(scope='module')
def routines_http(config, yt_server):
    resources = {'config': config}
    with HttpRoutinesTestEnv(yt_server=yt_server, **resources) as routines_http_env:
        yield routines_http_env


@pytest.yield_fixture(scope='module')
def routines_http_last_time(config_last_time, yt_server):
    resources = {'config': config_last_time}
    with HttpRoutinesTestEnv(yt_server=yt_server, **resources) as routines_http_env:
        yield routines_http_env


@retry(20, timeout=1)
def checker(condition, *args):
    return condition(*args)


def test_simple(its_server_mock, routines_http, post_results):
    def assert_condition(status, result):
        assert response.status == status
        assert_that(its_server_mock.input_post, IsRequestsMatch(post_results, result))
        return True
    # OK
    response = routines_http.post(
        '/juggler_alert_handler',
        data=json.dumps(
            {"checks": [{"host_name": "mi-datacamp-saas", "service_name": "saas-docs-age", "status": "OK"}]}
        ),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )
    assert checker(assert_condition, '202 ACCEPTED', 'ok')

    its_server_mock.reset_history()
    # WARN
    response = routines_http.post(
        '/juggler_alert_handler',
        data=json.dumps(
            {"checks": [{"host_name": "mi-datacamp-saas", "service_name": "saas-docs-age", "status": "WARN"}]}
        ),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )
    assert checker(assert_condition, '202 ACCEPTED', 'warn')

    its_server_mock.reset_history()

    # CRIT
    response = routines_http.post(
        '/juggler_alert_handler',
        data=json.dumps(
            {"checks": [{"host_name": "mi-datacamp-saas", "service_name": "saas-docs-age", "status": "CRIT"}]}
        ),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )

    assert checker(assert_condition, '202 ACCEPTED', 'crit')


def test_last_time(its_server_mock_last_time, routines_http_last_time, post_results_last_time, yt_server):
    # CRIT
    yt_client = yt_server.get_yt_client()
    time_now = time.time()
    yt_client.set('//tmp/last_update', json.dumps({"production_piper_sas": time_now, "production_scanner_man": time_now}))
    response = routines_http_last_time.post(
        '/juggler_alert_handler',
        data=json.dumps(
            {"checks": [{"host_name": "mi-datacamp-saas", "service_name": "saas-docs-age", "status": "CRIT"}]}
        ),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )

    def assert_condition(status, result):
        assert response.status == status
        assert_that(its_server_mock_last_time.input_post, IsRequestsMatch(post_results_last_time, result))
        last_update = json.loads(yt_client.get('//tmp/last_update'))
        last_update_scanner_vla = last_update['production_scanner_vla']
        assert last_update_scanner_vla >= time_now - 10 and last_update_scanner_vla <= time_now + 10
        return True

    assert checker(assert_condition, '202 ACCEPTED', 'crit')
