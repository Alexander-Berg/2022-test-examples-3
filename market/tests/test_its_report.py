from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import threading
import pytest
import requests

from yatest.common.network import PortManager
from market.idx.pylibrary.report_control.its_report import ItsHelper, get_flag_conditions


@pytest.yield_fixture
def its_server():
    stop = False
    its_flags = {}

    class MyHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path.endswith('/market_report_emergency_flags/'):
                self.send_json({'user_value': json.dumps(its_flags)})
            else:
                self.send_error(400)

        def do_POST(self):
            nonlocal its_flags
            nonlocal stop

            if self.path.endswith('/market_report_emergency_flags/'):
                content_len = int(self.headers.get('content-length', 0))
                payload = self.rfile.read(content_len)
                payload = json.loads(payload)

                its_flags = json.loads(payload['value'])

                self.send_response(200)
                self.end_headers()

            elif self.path.endswith('/stop'):
                stop = True

                self.send_response(200)
                self.end_headers()

            elif self.path.endswith('/raw_flags'):
                content_len = int(self.headers.get('content-length', 0))
                payload = self.rfile.read(content_len)
                payload = json.loads(payload)

                its_flags = payload

                self.send_response(200)
                self.end_headers()

            else:
                self.send_error(400)

        def send_json(self, data):
            data_str = json.dumps(data)
            data_str = bytes(data_str, encoding='utf-8')
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Content-Length', len(data_str))
            self.send_header('ETag', '1234')
            self.end_headers()
            self.wfile.write(data_str)

    class MyServer():
        def __init__(self):
            self._server = None
            self._thread = None

        def start(self):
            self.port = PortManager().get_port()
            self._server = HTTPServer(('localhost', self.port), MyHandler)
            self._thread = threading.Thread(target=self._serve)
            self._thread.setDaemon(True)
            self._thread.start()

        def _serve(self):
            while not stop:
                self._server.handle_request()

        def stop(self):
            requests.post('http://localhost:{}/stop'.format(self.port)).raise_for_status()
            self._thread.join()
            del self._server
            del self._thread

        def set_raw_flags(self, raw_flags):
            requests.post('http://localhost:{}/raw_flags'.format(self.port), data=raw_flags)

    its_server = MyServer()
    its_server.start()
    yield its_server
    its_server.stop()


@pytest.fixture
def its_client(its_server):
    return ItsHelper('AUTH-TOKEN', host='localhost:{}'.format(its_server.port), schema='http')


def test_get_emergency_flags(its_client):
    """ Test default value of flags """
    etag, flags = its_client.report_emergency_flags

    assert etag == '1234'
    assert flags == {}


def test_set_emergency_flag_for_subrole_group(its_client):
    """ Test setting a flag """
    its_client.set_report_emergency_flag(['api'], ['sas', 'vla'], 'enable_report_safe_mode')

    _, flags = its_client.report_emergency_flags

    condition = 'is_in(SUBROLE, {"api"}) and is_in(GEO, {"sas","vla"})'
    assert flags == {
        'enable_report_safe_mode': {
            'conditions': [{
                'value': '1',
                'condition': condition
            }],
            'default_value': '0'
        }
    }


def test_set_emergency_flag_in_dry_mode(its_client):
    """ Test that setting a flag in dry-run mode has no effect """
    its_client.set_report_emergency_flag(['api'], ['sas', 'vla'], 'enable_report_safe_mode', dry_mode=True)

    _, flags = its_client.report_emergency_flags
    assert flags == {}


def test_reset_emergency_flag(its_client):
    """ Test resetting a flag """
    its_client.set_report_emergency_flag(['api'], ['sas', 'vla'], 'enable_report_safe_mode')

    its_client.reset_report_emergency_flags('enable_report_safe_mode')

    _, flags = its_client.report_emergency_flags
    assert flags == {
        'enable_report_safe_mode': {
            'conditions': [{
                'condition': '',
                'value': '0'
            }],
            'default_value': '0'
        }
    }


def test_reset_emergency_flag_in_dry_mode(its_client):
    """ Test that resetting a flag in dry-run mode has no effect """
    its_client.set_report_emergency_flag(['api'], ['sas', 'vla'], 'enable_report_safe_mode')

    its_client.reset_report_emergency_flags('enable_report_safe_mode', dry_mode=True)

    _, flags = its_client.report_emergency_flags
    condition = 'is_in(SUBROLE, {"api"}) and is_in(GEO, {"sas","vla"})'
    assert flags == {
        'enable_report_safe_mode': {
            'conditions': [{
                'value': '1',
                'condition': condition
            }],
            'default_value': '0'
        }
    }


def test_append_emergency_flag(its_client):
    """ Test that updating a flag does not remove existing data """
    # arrange
    its_client.set_report_emergency_flag(['api'], ['sas', 'vla'], 'enable_report_safe_mode')

    # act
    its_client.set_report_emergency_flag(['market'], ['sas'], 'enable_report_safe_mode', append=True)

    # assert
    _, flags = its_client.report_emergency_flags
    api_condition = 'is_in(SUBROLE, {"api"}) and is_in(GEO, {"sas","vla"})'
    market_condition = 'is_in(SUBROLE, {"market"}) and is_in(GEO, {"sas"})'
    assert flags == {
        'enable_report_safe_mode': {
            'conditions': [{
                'value': '1',
                'condition': api_condition
            }, {
                'value': '1',
                'condition': market_condition
            }],
            'default_value': '0'
        }
    }


def test_append_emergency_flag_in_dry_mode(its_client):
    """ Test that updating a flag in dry-run mode has no effect """
    # arrange
    its_client.set_report_emergency_flag(['api'], ['sas', 'vla'], 'enable_report_safe_mode')

    # act
    its_client.set_report_emergency_flag(['market'], ['sas'], 'enable_report_safe_mode', append=True, dry_mode=True)

    # assert
    _, flags = its_client.report_emergency_flags
    api_condition = 'is_in(SUBROLE, {"api"}) and is_in(GEO, {"sas","vla"})'
    assert flags == {
        'enable_report_safe_mode': {
            'conditions': [{
                'value': '1',
                'condition': api_condition
            }],
            'default_value': '0'
        }
    }


def test_append_to_missing_emergency_flag(its_client):
    """ Test updating a flag that doesn't exist """
    # act
    its_client.set_report_emergency_flag(['market'], ['sas'], 'enable_report_safe_mode', append=True)

    # assert
    _, flags = its_client.report_emergency_flags
    market_condition = 'is_in(SUBROLE, {"market"}) and is_in(GEO, {"sas"})'
    assert flags == {
        'enable_report_safe_mode': {
            'conditions': [{
                'value': '1',
                'condition': market_condition
            }],
            'default_value': '0'
        }
    }


def test_append_to_conditionless_emergency_flag(its_client, its_server):
    """ Test updating a condtionless flag """
    # arrange
    flags = {
        'enable_report_safe_mode': {
            'default_value': '0'
        }
    }
    its_server.set_raw_flags(json.dumps(flags))

    # act
    its_client.set_report_emergency_flag(['market'], ['sas'], 'enable_report_safe_mode', append=True)

    # assert
    _, flags = its_client.report_emergency_flags
    market_condition = 'is_in(SUBROLE, {"market"}) and is_in(GEO, {"sas"})'
    assert flags == {
        'enable_report_safe_mode': {
            'conditions': [{
                'value': '1',
                'condition': market_condition
            }],
            'default_value': '0'
        }
    }


def test_append_to_conflicting_emergency_flag(its_client, its_server):
    """ Test that it's not possible to update a flag with conflicting default_value """
    # arrange
    flags = {
        'enable_report_safe_mode': {
            'conditions': [{
                'value': '1',
                'condition': 'is_in(SUBROLE, {"api"})'
            }],
            'default_value': '1'
        }
    }
    its_server.set_raw_flags(json.dumps(flags))

    # act, assert
    with pytest.raises(RuntimeError):
        its_client.set_report_emergency_flag(['market'], ['sas'], 'enable_report_safe_mode', append=True)


def test_flag_default_value():
    value = {
        'conditions': [{
            'value': '0',
            'condition': ''
        }],
        'default_value': '1'
    }
    result = get_flag_conditions(value)
    assert result == ['DEFAULT=1']


def test_flag_conditions_exists():
    value = {
        'conditions': [{
            'value': '1',
            'condition': 'IS_TEST'
        }, {
            'value': '1',
            'condition': 'IS_PREP'
        }],
        'default_value': '0'
    }
    result = get_flag_conditions(value)
    assert result == ['IS_TEST', 'IS_PREP']


def test_flag_conditions_not_exists():
    value = {
        'conditions': [{
            'value': '0',
            'condition': ''
        }],
        'default_value': '0'
    }
    result = get_flag_conditions(value)
    assert result == []
