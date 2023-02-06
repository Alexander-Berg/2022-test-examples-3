import mock
import pytest
import requests

import market.sre.juggler.bundles.checks.ping.lib.main as ping_check


def _mocked_requests_get(*args, **kwargs):
    class MockedResponse:
        def __init__(self, text, status_code, reason=None):
            self.text = text
            self.content = text
            self.status_code = status_code
            self.reason = reason

        def raise_for_status(self):
            if self.status_code != 200:
                raise requests.exceptions.HTTPError('raise on {}'.format(self.status_code))

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
            (('0;OK', ping_check.Status.OK), (ping_check.Status.OK, 'OK')),
            (('1;WARN', ping_check.Status.OK), (ping_check.Status.WARN, 'WARN')),
            (('2;CRIT', ping_check.Status.OK), (ping_check.Status.CRIT, 'CRIT')),
            ((u'1;Ворнинг', ping_check.Status.OK), (ping_check.Status.WARN, u'Ворнинг')),
            (('pong', ping_check.Status.OK), (ping_check.Status.OK, 'pong')),
            (('pong', ping_check.Status.CRIT), (ping_check.Status.CRIT, 'pong')),
            (('pong', ping_check.Status.CRIT, 'desc prefix '), (ping_check.Status.CRIT, 'desc prefix pong')),
            (('o;OK', ping_check.Status.OK), (ping_check.Status.OK, 'o;OK')),
        ]
    )
    def test_parse_result(self, params, result):
        assert ping_check._parse_response_text(*params) == result

    @pytest.mark.parametrize(
        argnames='config,event',
        argvalues=[
            (ping_check.Config(service='ping', host='http://coolservice', port=80, path='/ping'),
             ping_check.Event(status=ping_check.Status.OK, description='OK', service='ping')),
            (ping_check.Config(service='ping', host='http://notcoolservice', port=80, path='/ping'),
             ping_check.Event(
                 status=ping_check.Status.CRIT,
                 description='args: host=http://notcoolservice port=80 port_offset=0 path=/ping; desc: oops',
                 service='ping')),
            (ping_check.Config(service='ping', host='http://utf8service', port=80, path='/ping'),
             ping_check.Event(
                 status=ping_check.Status.WARN,
                 description=u'Упс',
                 service='ping')),
            (ping_check.Config(service='ping', host='http://notexists', port=80, path='/ping'),
             ping_check.Event(
                 status=ping_check.Status.CRIT,
                 description='args: host=http://notexists port=80 port_offset=0 path=/ping; desc: Not found',
                 service='ping'))
        ]
    )
    def test_do_ping(self, config, event):
        with mock.patch('requests.get', side_effect=_mocked_requests_get):
            assert ping_check._do_ping(config=config).to_dict() == event.to_dict()

    @mock.patch('requests.get', side_effect=_mocked_requests_get)
    def test_ping(self, manifest):
        assert manifest.execute('ping', ('http://coolservice', 80))
