# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from hamcrest import assert_that, has_entries, anything

from travel.rasp.library.python.common23.data_api.error_booster.raven import (
    RavenErrorBoosterClient, RavenErrorBoosterRemoteConfig, RavenErrorBoosterTransport
)


def test_raven_client():
    fake_sender = mock.Mock()

    client = RavenErrorBoosterClient(
        service_name='my_proj',
        transport=RavenErrorBoosterTransport,
        environment='env',
        logbroker_sender=fake_sender
    )

    assert client.service_name == 'my_proj'
    assert client.project_name == 'rasp'
    assert client.get_full_service_name() == 'rasp_my_proj_env'

    assert isinstance(client.remote, RavenErrorBoosterRemoteConfig)
    assert client.remote.project == 'rasp'
    assert client.remote.store_endpoint == 'error booster project: rasp'

    transport = client.remote.get_transport()
    assert isinstance(transport, RavenErrorBoosterTransport)
    assert transport.logbroker_sender is fake_sender
    assert fake_sender.call_count == 0

    data = {
        'event_id': '123',
        'level': 40,
        'server_name': 'server',
        'message': 'some error',
        'environment': 'env',
        'request': {'url': '/some_url'}
    }
    converted_data = client.error_booster_convert(data)
    assert_that(converted_data, has_entries({
        'level': 'error',
        'additional': {'eventid': '123', 'request': {'url': '/some_url'}, 'vars': []},
        'env': 'env',
        'host': 'server',
        'language': 'python',
        'message': 'some error',
        'project': 'rasp',
        'service': 'my_proj',
        'timestamp': anything(),
        'version': '',
        'url': '/some_url'

    }))

    with mock.patch('travel.rasp.library.python.common23.data_api.error_booster.raven.time', return_value=123):
        client.send(auth_header=None, **data)
    converted_data['timestamp'] = 123000
    fake_sender.assert_called_once_with(converted_data)
