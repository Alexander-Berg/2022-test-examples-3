# coding: utf-8
#
import os
import json
import yatest.common
from yatest.common import network
from hamcrest import is_
from library.python.testing.pyremock.lib.pyremock import mocked_http_server, MatchRequest, MockResponse, HttpMethod


def run_vm(work_dir, input_stream, webhook, callback_body=None):
    vm_bin = yatest.common.binary_path('extsearch/video/robot/rt_transcoder/video-manager/video-manager')

    webhook_file = os.path.join(str(work_dir), 'webhook')
    with open(webhook_file, 'w') as f:
        json.dump(webhook, f)

    with network.PortManager() as pm:
        port = pm.get_port(9999)
        input_stream['VodProviderCallback'] = 'http://localhost:%d/callback' % port

        input_stream_file = os.path.join(str(work_dir), 'input_stream')
        with open(input_stream_file, 'w') as f:
            json.dump(input_stream, f)

        with mocked_http_server(port) as mock:
            if callback_body:
                request = MatchRequest(method=is_(HttpMethod.POST), path=is_('/callback'), body=is_(callback_body))
                response = MockResponse(status=200, body='{}')
                mock.expect(request, response)

            res = yatest.common.canonical_execute(vm_bin, ['dry-run', '--webhook', webhook_file, '--input-stream', input_stream_file])
            mock.assert_expectations()
            return res
