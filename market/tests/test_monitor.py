from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import pytest
from multiprocessing import Process

from market.idx.devtools.common_proxy_monitor.lib.monitor import MonitorConfig, get_env_suffix, monitor


service_status = None
event = None


@pytest.fixture
def sink():
    def sink_func(monitor_event):
        global event
        event = monitor_event
    return sink_func


@contextmanager
def stub_service_status(status):
    global service_status
    service_status = status
    yield
    service_status = None


@pytest.fixture
def config():
    cfg = MonitorConfig('', 8000)
    cfg.measurements = 1
    return cfg


@pytest.yield_fixture
def service_status_ok():
    response = json.loads('''
        {
            "controller_status": "Active",
            "controller_uptime": 293,
            "config": {
                "Proxy": [{
                    "Processors": [{
                        "A": [{
                            "MaxQueue": 10
                        }],
                        "B": [{
                            "MaxQueue": 10
                        }]
                    }]
                }]
            },
            "processors": {
                "A": {
                    "queue_size": 5
                },
                "B": {
                    "queue_size": 0
                }
            },
            "links": {
                "A->B": {
                }
            }
        }
        ''')
    with stub_service_status(response):
        yield


@pytest.yield_fixture
def service_status_warn():
    response = json.loads('''
        {
            "controller_status": "Active",
            "controller_uptime": 293,
            "config": {
                "Proxy": [{
                    "Processors": [{
                        "A": [{
                            "MaxQueue": 10
                        }],
                        "B": [{
                            "MaxQueue": 10
                        }]
                    }]
                }]
            },
            "processors": {
                "A": {
                    "queue_size": 9
                },
                "B": {
                    "queue_size": 0
                }
            },
            "links": {
                "A->B": {
                }
            }
        }
        ''')
    with stub_service_status(response):
        yield


@pytest.yield_fixture
def service_status_crit():
    response = json.loads('''
        {
            "controller_status": "Active",
            "controller_uptime": 293,
            "config": {
                "Proxy": [{
                    "Processors": [{
                        "A": [{
                            "MaxQueue": 10
                        }],
                        "B": [{
                            "MaxQueue": 10
                        }]
                    }]
                }]
            },
            "processors": {
                "A": {
                    "queue_size": 10
                },
                "B": {
                    "queue_size": 0
                }
            },
            "links": {
                "A->B": {
                }
            }
        }
        ''')
    with stub_service_status(response):
        yield


@pytest.yield_fixture
def service_status_malformed():
    response = json.loads('''
        {
            "controller_status": "Active",
            "controller_uptime": 293,
            "processors": {
                "A": {
                    "queue_size": 10
                },
                "B": {
                    "queue_size": 0
                }
            },
            "links": {
                "A->B": {
                }
            }
        }
        ''')
    with stub_service_status(response):
        yield


@pytest.yield_fixture
def service_status_incomplete():
    response = json.loads('''
        {
            "config": {
                "Proxy": [
                    {
                        "Processors": [
                            {
                                "A": [
                                    {
                                        "InflightCount": "100",
                                        "MaxCount": "100",
                                        "MaxInProcess": "100",
                                        "MaxMemoryUsage": "104857600",
                                        "MaxQueue": "10000",
                                        "MaxSize": "10485760",
                                        "MaxUncommittedCount": "0",
                                        "MaxUncommittedSize": "0",
                                        "Threads": "10",
                                        "TimeoutSec": "10",
                                        "Type": "A"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            "controller_status": "Active",
            "controller_uptime": 293,
            "processors": {
                "A": {
                    "queue_size": 10
                },
                "B": {
                    "queue_size": 0
                }
            },
            "links": {
                "A->B": {
                }
            }
        }
        ''')
    with stub_service_status(response):
        yield


@pytest.yield_fixture
def service_status_inactive():
    response = json.loads('''
        {
            "controller_status": "Stopped",
            "controller_uptime": 0,
            "config": {
                "Proxy": [{
                    "Processors": [{
                        "A": [{
                            "MaxQueue": 10
                        }],
                        "B": [{
                            "MaxQueue": 10
                        }]
                    }]
                }]
            },
            "processors": {
                "A": {
                    "queue_size": 5
                },
                "B": {
                    "queue_size": 0
                }
            },
            "links": {
                "A->B": {
                }
            }
        }
        ''')
    with stub_service_status(response):
        yield


@pytest.yield_fixture
def server(config):
    class CommonProxyRequestHandler(BaseHTTPRequestHandler):
        def send_json(self, data):
            data_str = json.dumps(data)
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Content-Length', len(data_str))
            self.end_headers()
            self.wfile.write(bytes(data_str, encoding='utf-8'))

        def do_GET(self):
            if self.path.startswith('/info_server'):
                assert service_status
                self.send_json(service_status)
            else:
                self.send_error(400, 'Bad request')

    server_address = (config.host, config.port)
    httpd = HTTPServer(server_address, CommonProxyRequestHandler)

    server_process = Process(target=httpd.serve_forever)
    server_process.start()
    yield
    server_process.terminate()
    server_process.join()
    server_process.close()


def test_get_env_suffix_default(config):
    assert get_env_suffix(config) == ''


def test_get_env_suffix_explicit(config):
    config.env = 'myenv'
    assert get_env_suffix(config) == '_myenv'


def test_get_env_suffix_testing(config):
    config.service = 'testing_market_saashub_stratocaster_sas'
    assert get_env_suffix(config) == '_testing'


def test_get_env_suffix_production(config):
    config.service = 'production_market_lbdumper_uc_sas'
    assert get_env_suffix(config) == '_production'


def test_get_env_suffix_prestable(config):
    config.service = 'prestable_market_saashub_plainshift_gibson_vla'
    assert get_env_suffix(config) == '_prestable'


def test_get_env_suffix_missing(config):
    config.service = 'mt_mbo--mbo02_f3b7f80b_sas'
    assert get_env_suffix(config) == ''


def test_monitor_ok(config, sink, service_status_ok, server):
    monitor(config, sink, loop=False)

    assert event.status == 'OK', event


def test_monitor_warn(config, sink, service_status_warn, server):
    monitor(config, sink, loop=False)

    assert event.status == 'WARN'


def test_monitor_crit(config, sink, service_status_crit, server):
    monitor(config, sink, loop=False)

    assert event.status == 'CRIT'


def test_monitor_unreachable(config, sink):
    monitor(config, sink, loop=False)

    assert event.status == 'WARN'


def test_monitor_malformed(config, sink, service_status_malformed, server):
    monitor(config, sink, loop=False)

    assert event.status == 'CRIT'


def test_monitor_inactive(config, sink, service_status_inactive, server):
    monitor(config, sink, loop=False)

    assert event.status == 'WARN'


def test_monitor_incomplete(config, sink, service_status_incomplete, server):
    monitor(config, sink, loop=False)

    assert event.status == 'WARN'
