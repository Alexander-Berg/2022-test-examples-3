# coding: utf-8

from yatest.common import network
from hamcrest import is_
from library.python.testing.pyremock.lib.pyremock import mocked_http_server, MatchRequest, MockResponse
from .run import run_vm

INPUT_STREAM = {
    'ContentGroupID': '12345',
    'ContentVersionID': '321',
    'FaasCustomParameters': None,
    'InputStreamID': '123',
    'Status': 'faas-sent',
    'UUID': 'uuid',
    'VodProviderOptions': '{}',
    'VodProviderThumbIdx': 5,
}


def test_signatures(tmpdir):
    webhook = {
        'TaskId': '91a3cf9d-9e6af6b8-1fc1f79-45d5aed1',
        'DurationMs': 69360,
        'PreviewStatus': 2,
        'PreviewStatusStr': 'EPSDone',
        'SignaturesStatus': 2,
        'SignaturesStatusStr': 'ESSDone',
        'ContentStatus': 2,
        'ContentStatusStr': 'ECSDone',
        'MetarobotResultsUrl': 'some-url',
        'User': 'rt'
    }

    with network.PortManager() as pm:
        port = pm.get_port(9999)

        webhook['SignaturesUrl'] = 'http://localhost:%d/sigs' % port
        with mocked_http_server(port) as mock:
            request = MatchRequest(path=is_('/sigs'))
            response = MockResponse(status=200, body='{"sig1": "val1", "sig2": "val2"}')
            mock.expect(request, response)

            return run_vm(tmpdir, INPUT_STREAM, webhook)


def test_s2t(tmpdir):
    webhook = {
        'TaskId': '91a3cf9d-9e6af6b8-1fc1f79-45d5aed1',
        'SpeechToTextStatus': 2,
        'SpeechToTextStatusStr': 'ES2TSDone',
    }

    with network.PortManager() as pm:
        port = pm.get_port(9999)

        webhook['SpeechToTextUrl'] = 'http://localhost:%d/s2t' % port
        with mocked_http_server(port) as mock:
            request = MatchRequest(path=is_('/s2t'))
            response = MockResponse(status=200, body='{"Speech2TextCloudDssmV1": "val1", "Speech2TextCloudV1": "val2"}')
            mock.expect(request, response)

            return run_vm(tmpdir, INPUT_STREAM, webhook)
