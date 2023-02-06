# coding: utf8

from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from travel.rasp.library.python.sqs.wrapper import SQSClientWrapper


@pytest.mark.parametrize('enable_writing, call_times', [(True, 1), (False, 0)])
def test_disable_send_message(enable_writing, call_times):

    with mock.patch('botocore.client.BaseClient._make_api_call') as m_boto_api_call:
        client = SQSClientWrapper(
            aws_access_key_id='id',
            aws_secret_access_key='key',
            aws_session_token='token',
            endpoint_url='http://localhost:2134',
            enable_writing=enable_writing
        )

        client.send_message(QueueUrl="http://localhost:2134/rasp/test", MessageBody="msg")
        assert m_boto_api_call.call_count == call_times
