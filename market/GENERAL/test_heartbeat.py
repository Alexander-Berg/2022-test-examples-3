from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from datetime import datetime, timedelta
import os
import pytest
import requests
import threading

import heartbeat
import test_common
from yatest.common.network import PortManager


@pytest.fixture
def report_port():
    report_port = PortManager().get_port()
    base_port = report_port - 1
    test_common.generate_monitoring_config(port=base_port)
    yield report_port


@pytest.yield_fixture
def report_service(report_port):
    class context(object):
        stop = False

    class MyHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            response = '0;OK'
            if self.path.find('/yandsearch') != -1:
                self.send_response(200)
                self.send_header('Content-Type', 'text/plain')
                self.send_header('Content-Length', len(response))
                self.end_headers()
                self.wfile.write(response)
            else:
                self.send_error(400)
                self.end_headers()

        def do_POST(self):
            if self.path.endswith('/stop'):
                context.stop = True
                self.send_response(200)
            else:
                self.send_error(400)
            self.end_headers()

        def log_message(self, *args, **kwargs):
            pass

    class MyServer():
        def __init__(self):
            self._server = None
            self._thread = None
            self.port = report_port

        def start(self):
            self._server = HTTPServer(('localhost', self.port), MyHandler)
            self._thread = threading.Thread(target=self._serve)
            self._thread.setDaemon(True)
            self._thread.start()

        def _serve(self):
            while not context.stop:
                self._server.handle_request()

        def stop(self):
            if hasattr(self, '_server'):
                requests.post('http://localhost:{}/stop'.format(self.port)).raise_for_status()
                self._thread.join()
                del self._server
                del self._thread

    report = MyServer()
    report.start()
    yield report
    report.stop()


def test_report_is_alive(report_service):
    with test_common.OutputCapture() as capture:
        heartbeat.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-heartbeat;0;Ok\n'
        assert capture.get_stderr() == ''


def test_report_is_down(report_service):
    report_service.stop()

    with test_common.OutputCapture() as capture:
        heartbeat.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-null;0;Ok\n'
        assert capture.get_stderr() == ''


def test_report_is_down_snippet(report_service):
    report_service.stop()

    test_common.generate_monitoring_config(is_snippet=True)
    with test_common.OutputCapture() as capture:
        heartbeat.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-heartbeat;0;Ok\n'
        assert capture.get_stderr() == ''


def test_report_has_just_crashed(monitoring_dir, report_service):
    crash_time = datetime.now()

    with open(os.path.join(monitoring_dir, heartbeat.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write(crash_time.strftime('%Y-%m-%dT%H:%M:%S'))

    with test_common.OutputCapture() as capture:
        heartbeat.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-heartbeat;2;Report has crashed\n'
        assert capture.get_stderr() == ''


def test_report_crashed_before(monitoring_dir, report_service):
    crash_time = datetime.now() - timedelta(minutes=15)

    with open(os.path.join(monitoring_dir, heartbeat.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write(crash_time.strftime('%Y-%m-%dT%H:%M:%S'))

    with test_common.OutputCapture() as capture:
        heartbeat.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-heartbeat;0;Ok\n'
        assert capture.get_stderr() == ''


@pytest.mark.usefixtures('indigo_cluster')
def test_ignore_indigo_clusters(monitoring_dir, report_service):
    crash_time = datetime.now()

    with open(os.path.join(monitoring_dir, heartbeat.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write(crash_time.strftime('%Y-%m-%dT%H:%M:%S'))

    with test_common.OutputCapture() as capture:
        heartbeat.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-heartbeat;0;Ok\n'
        assert capture.get_stderr() == ''


def test_empty_last_crash_info(monitoring_dir, report_service):
    with open(os.path.join(monitoring_dir, heartbeat.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write('')

    with test_common.OutputCapture() as capture:
        heartbeat.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-heartbeat;0;Ok\n'
        assert capture.get_stderr() == ''


def test_last_crash_info_wrong_format(monitoring_dir, report_service):
    crash_time = datetime.now()

    with open(os.path.join(monitoring_dir, heartbeat.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write(crash_time.strftime('%Y-%m-%d %H:%M:%S'))

    with test_common.OutputCapture() as capture:
        heartbeat.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-null;0;Ok\n'
        assert capture.get_stderr() == ''
