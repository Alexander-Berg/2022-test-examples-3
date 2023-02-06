# -*- coding: utf-8 -*-
from __future__ import absolute_import

import mock
import pytest

import checks.ping


def _mocked_requests_get(*args, **kwargs):
    class MockedResponse:
        def __init__(self, text, status_code, reason=None):
            self.text = text
            self.content = text
            self.status_code = status_code
            self.reason = reason

        def raise_for_status(self):
            if self.status_code != 200:
                raise checks.ping.requests.exceptions.HTTPError('raise on {}'.format(self.status_code))

    if kwargs['url'] in ['http://coolservice:80/ping', 'http://coolservice:80/monitoring']:
        return MockedResponse('0;OK', 200)
    if kwargs['url'] == 'http://notcoolservice:80/ping':
        return MockedResponse('oops', 503)
    if kwargs['url'] == 'http://utf8service:80/ping':
        return MockedResponse(u'1;Упс', 200)

    return MockedResponse(None, 404, 'Not found')


class TestPing(object):
    @pytest.mark.parametrize(
        argnames='params,result',
        argvalues=[
            (('0;OK', checks.ping.Status.OK), (checks.ping.Status.OK, 'OK')),
            (('1;WARN', checks.ping.Status.OK), (checks.ping.Status.WARN, 'WARN')),
            (('2;CRIT', checks.ping.Status.OK), (checks.ping.Status.CRIT, 'CRIT')),
            ((u'1;Ворнинг', checks.ping.Status.OK), (checks.ping.Status.WARN, u'Ворнинг')),
            (('pong', checks.ping.Status.OK), (checks.ping.Status.OK, 'pong')),
            (('pong', checks.ping.Status.CRIT), (checks.ping.Status.CRIT, 'pong')),
            (('pong', checks.ping.Status.CRIT, 'desc prefix '), (checks.ping.Status.CRIT, 'desc prefix pong')),
            (('o;OK', checks.ping.Status.OK), (checks.ping.Status.OK, 'o;OK')),
        ]
    )
    def test_parse_result(self, params, result):
        assert checks.ping._parse_response_text(*params) == result

    @pytest.mark.parametrize(
        argnames='config,event',
        argvalues=[
            (checks.ping.Config(service='ping', host='http://coolservice', port=80, path='/ping'),
             checks.ping.Event(status=checks.ping.Status.OK, description='OK', service='ping')),
            (checks.ping.Config(service='ping', host='http://notcoolservice', port=80, path='/ping'),
             checks.ping.Event(
                 status=checks.ping.Status.CRIT,
                 description='args: host=http://notcoolservice port=80 port_offset=0 path=/ping; desc: oops',
                 service='ping')),
            (checks.ping.Config(service='ping', host='http://utf8service', port=80, path='/ping'),
             checks.ping.Event(
                 status=checks.ping.Status.WARN,
                 description=u'Упс',
                 service='ping')),
            (checks.ping.Config(service='ping', host='http://notexists', port=80, path='/ping'),
             checks.ping.Event(
                 status=checks.ping.Status.CRIT,
                 description='args: host=http://notexists port=80 port_offset=0 path=/ping; desc: Not found',
                 service='ping'))
        ]
    )
    def test_do_ping(self, config, event):
        with mock.patch('checks.ping.requests.get', side_effect=_mocked_requests_get):
            assert checks.ping._do_ping(config=config).to_dict() == event.to_dict()

    @mock.patch('checks.ping.requests.get', side_effect=_mocked_requests_get)
    def test_ping(self, manifest):
        assert manifest.execute('ping', ('http://coolservice', 80))

    @mock.patch('checks.ping.requests.get', side_effect=_mocked_requests_get)
    def test_monitoring(self, manifest):
        assert manifest.execute('monitoring', ('http://coolservice', 80))
