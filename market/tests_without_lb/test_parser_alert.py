# coding=utf-8

from hamcrest import assert_that
from hamcrest.core.base_matcher import BaseMatcher
import json
import pytest
import time

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.yatf.test_envs.its_server import ItsAppMock


@pytest.fixture(scope='module')
def its_server_mock(its_get_responses):
    with ItsAppMock(its_get_responses) as server:
        yield server


@pytest.fixture(scope='module')
def its_server_last_time_mock(its_get_responses_last_time):
    with ItsAppMock(its_get_responses_last_time) as server:
        yield server


@pytest.fixture(scope='module')
def its_get_responses():
    full_response = {
        'market/datacamp/parser-blue/production-datacamp-parser-blue-man/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/parser-blue/production-datacamp-parser-blue-sas/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/parser-white/production-datacamp-parser-white-vla/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/parser-white/production-datacamp-parser-white-man/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/parser-white/production-datacamp-parser-white-sas/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
    }
    return {
        '/v1/values/market/datacamp': json.dumps(full_response),
        '/v1/values/market/datacamp/': json.dumps(full_response),
        '/v1/history/market/datacamp/parser-blue/production-datacamp-parser-blue-man/market_datacamp_auto_settings/?limit=1': json.dumps({}),
        '/v1/history/market/datacamp/parser-blue/production-datacamp-parser-blue-sas/market_datacamp_auto_settings/?limit=1': json.dumps({}),
        '/v1/history/market/datacamp/parser-white/production-datacamp-parser-white-man/market_datacamp_auto_settings/?limit=1': json.dumps({}),
        '/v1/history/market/datacamp/parser-white/production-datacamp-parser-white-sas/market_datacamp_auto_settings/?limit=1': json.dumps({}),
        '/v1/history/market/datacamp/parser-white/production-datacamp-parser-white-vla/market_datacamp_auto_settings/?limit=1': json.dumps({}),
    }


@pytest.fixture(scope='module')
def its_get_responses_last_time():
    full_response = {
        'market/datacamp/parser-blue/production-datacamp-parser-blue-man/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/parser-blue/production-datacamp-parser-blue-sas/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/parser-white/production-datacamp-parser-white-vla/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/parser-white/production-datacamp-parser-white-man/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
        'market/datacamp/parser-white/production-datacamp-parser-white-sas/market_datacamp_auto_settings': {
            'value': '{"restart_service": false, "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": false}}}]}',
            'version': '60e456bb3981d3f9fcf07cf4',
        },
    }
    formatted_time = time.strftime('%Y-%m-%dT%H:%M:%S.000000', time.gmtime())
    history_value = json.dumps({
        "result": [{"ruchka_id": "market_datacamp_auto_settings", "is_deleted": False, "user_value": "", "time": formatted_time}]
    })
    return {
        '/v1/values/market/datacamp': json.dumps(full_response),
        '/v1/values/market/datacamp/': json.dumps(full_response),
        '/v1/history/market/datacamp/parser-blue/production-datacamp-parser-blue-man/market_datacamp_auto_settings/?limit=1': history_value,
        '/v1/history/market/datacamp/parser-blue/production-datacamp-parser-blue-sas/market_datacamp_auto_settings/?limit=1': history_value,
        '/v1/history/market/datacamp/parser-white/production-datacamp-parser-white-man/market_datacamp_auto_settings/?limit=1': history_value,
        '/v1/history/market/datacamp/parser-white/production-datacamp-parser-white-sas/market_datacamp_auto_settings/?limit=1': history_value,
        '/v1/history/market/datacamp/parser-white/production-datacamp-parser-white-vla/market_datacamp_auto_settings/?limit=1': json.dumps({})
    }


@pytest.fixture(scope='module')
def post_results():
    return {
        'WARN': {
            'blue': {
                'man': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-blue/production-datacamp-parser-blue-man/market_datacamp_auto_settings/',
                },
                'sas': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-blue/production-datacamp-parser-blue-sas/market_datacamp_auto_settings/',
                },
            },
            'white': {
                'man': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-white/production-datacamp-parser-white-man/market_datacamp_auto_settings/',
                },
                'sas': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-white/production-datacamp-parser-white-sas/market_datacamp_auto_settings/',
                },
                'vla': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-white/production-datacamp-parser-white-sas/market_datacamp_auto_settings/',
                },
            },
        },
        'CRIT': {
            'blue': {
                'man': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-blue/production-datacamp-parser-blue-man/market_datacamp_auto_settings/',
                },
                'sas': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-blue/production-datacamp-parser-blue-sas/market_datacamp_auto_settings/',
                },
            },
            'white': {
                'man': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-white/production-datacamp-parser-white-man/market_datacamp_auto_settings/',
                },
                'sas': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-white/production-datacamp-parser-white-sas/market_datacamp_auto_settings/',
                },
                'vla': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-white/production-datacamp-parser-white-sas/market_datacamp_auto_settings/',
                },
            },
        },
    }


@pytest.fixture(scope='module')
def post_results_last_time():
    return {
        'CRIT': {
            'white': {
                'vla': {
                    'data': {
                        "restart_service": False,
                        "files": [{"path": "file/path.json", "format": "json", "values": {"feature": {"enable_quick_pipeline": True}}}],
                    },
                    'url': '/v1/values/market/datacamp/parser-white/production-datacamp-parser-white-vla/market_datacamp_auto_settings/',
                },
                'man': {},
            },
            'blue': {
                'sas': {}
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
    def __init__(self, results, status, color):
        self.results = results
        self.status = status
        self.color = color

    def _matches(self, item):
        count = 0
        if self.status not in self.results:
            return len(item) == count
        results = self.results[self.status]

        if self.color not in results:
            return len(item) == count
        dcs = results[self.color]

        for dc in dcs:
            post_res = dcs[dc]
            if 'data' not in post_res or 'url' not in post_res:
                continue
            count += 1
            assert_that(item, IsPostDataEqualsToJson(post_res['data'], post_res['url']))
        return len(item) == count

    def describe_to(self, description):
        description.append_text('# error on requests with {}:{}\n'.format(self.color, self.status))
        if self.status not in self.results or self.color not in self.results[self.status]:
            description.append_text('Nothing')
        else:
            description.append_text(self.results[self.status][self.color])


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
        'parser_alerts': {
            'enable': True,
            'need_restart': False,
            'timeout': 0,
            'parser_blue_dcs': ['sas', 'man'],
            'parser_white_dcs': ['sas', 'man', 'vla'],
            'file_path': 'file/path.json',
            'one_task_at_time': False,
            'ruchka_name': 'market_datacamp_auto_settings',
            'global_timeout': 3600,
            'blue_host_name': 'datacamp_qparser_blue_testing',
            'blue_service_name': 'qoffers_lb_time_lag',
            'blue_services': ['parser-blue'],
            'united_host_name': 'datacamp_qparser_white_testing',
            'united_service_name': 'qoffers_lb_time_lag',
            'united_services': ['parser-white'],
        }
    }
    return RoutinesConfigMock(config=cfg, yt_server=yt_server)


@pytest.fixture(scope='module')
def config_last_time(its_server_last_time_mock, yt_server):
    cfg = {
        'alerts_handler': {
            'its_path': its_server_last_time_mock.server.url,
            'its_token_path': '',
            'juggler_token_path': '',
            'datacamp_path': 'market/datacamp',
            'cluster_type': 'production',
        },
        'parser_alerts': {
            'enable': True,
            'need_restart': False,
            'timeout': 0,
            'parser_blue_dcs': ['sas', 'man'],
            'parser_white_dcs': ['sas', 'man', 'vla'],
            'file_path': 'file/path.json',
            'one_task_at_time': False,
            'ruchka_name': 'market_datacamp_auto_settings',
            'global_timeout': 3600,
            'blue_host_name': 'datacamp_qparser_blue_testing',
            'blue_service_name': 'qoffers_lb_time_lag',
            'blue_services': ['parser-blue'],
            'united_host_name': 'datacamp_qparser_white_testing',
            'united_service_name': 'qoffers_lb_time_lag',
            'united_services': ['parser-white'],
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


@pytest.mark.parametrize('status', ['OK', 'WARN', 'CRIT'])
@pytest.mark.parametrize('color', ['blue', 'white'])
def test_parser_alerts_handling(its_server_mock, routines_http, post_results, color, status):
    """
    Проверяем, что parser_alerts_handler правильно обрабатывает проверки от juggler'а и пытается менять значения ITS
    """
    its_server_mock.reset_history()
    response = routines_http.post(
        '/juggler_alert_handler',
        data=json.dumps(
            {"checks": [{"host_name": "datacamp_qparser_{}_testing".format(color), "service_name": "qoffers_lb_time_lag", "status": status}]}
        ),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )
    assert response.status == '202 ACCEPTED'
    time.sleep(1)
    assert_that(its_server_mock.input_post, IsRequestsMatch(post_results, status, color))


@pytest.mark.parametrize('status', ['OK', 'CRIT'])
@pytest.mark.parametrize('color', ['blue', 'white'])
def test_last_time(its_server_last_time_mock, routines_http_last_time, post_results_last_time, color, status):
    """
    Проверяем, что parser_alerts_handler перед обновлением ITS проверяет время последнего обновления и выжиает global_timeout
    """
    its_server_last_time_mock.reset_history()
    response = routines_http_last_time.post(
        '/juggler_alert_handler',
        data=json.dumps(
            {"checks": [{"host_name": "datacamp_qparser_{}_testing".format(color), "service_name": "qoffers_lb_time_lag", "status": status}]}
        ),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )
    assert response.status == '202 ACCEPTED'
    time.sleep(1)
    assert_that(its_server_last_time_mock.input_post, IsRequestsMatch(post_results_last_time, status, color))
