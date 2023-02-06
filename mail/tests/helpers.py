import logging
import os
from typing import Awaitable, Callable, List, Dict

import mock
from hamcrest import is_

from library.python.testing.pyremock.lib.pyremock import MockHttpServer, MatchRequest, MockResponse
from mail.borador.devpack.components.borador import Borador
from mail.borador.src.aqua import AquaApi


def is_arcadia():
    try:
        import library.python.resource as resource
        del resource
        return True
    except ImportError:
        return False


def data_path(*path) -> str:
    if is_arcadia():
        from yatest.common import source_path
        return source_path(os.path.join('mail/borador/tests/data', *path))
    else:
        return os.path.join(*path)


def add_import_prefix(path: str) -> str:
    if is_arcadia():
        return 'mail.borador.' + path
    else:
        return path


def async_value(val) -> Awaitable:
    async def inner():
        return val
    return inner()


def async_raise(val) -> Awaitable:
    async def inner():
        raise val
    return inner()


def make_api(launch, show, wait, restart=None) -> AquaApi:
    a = mock.MagicMock()
    a.launch_pack = mock.MagicMock()
    a.launch_pack.return_value = launch
    a.show_launch = mock.MagicMock()
    a.show_launch.side_effect = show
    a.wait_for_next_show = mock.MagicMock()
    a.wait_for_next_show.return_value = wait
    a.restart_launch = mock.MagicMock()
    a.restart_launch.side_effect = restart

    return a


class Waitable(object):
    def __init__(self, callable: Callable[[], object], info: Callable[[object], str]):
        self.callable = callable
        self.info = info
        self.res = None
        self.logger = logging.getLogger('borador')

    def __str__(self):
        return self.info(self.res)

    def __call__(self):
        self.res = self.callable()

        self.logger.info(f'waitable response is got: {str(self)}')

        return self.res


def get_log_lines(path: str, req_id: str) -> List[Dict[str, str]]:
    with open(path) as f:
        data = [
            dict({
                (pair.split('=')[0], pair.split('=')[1]) if '=' in pair else (pair, '')
                for pair in pairs
            }) for pairs in [
                line.strip().split('\t') for line in f.readlines()
            ]
        ]

    return list(filter(lambda x: 'x_request_id' in x and x['x_request_id'] == req_id, data))


def waitable_file(borador: Borador, x_request_id: str):
    return Waitable(callable=lambda: get_log_lines(path=borador.tskv_log(), req_id=x_request_id),
                    info=lambda x: f'filtered file with lines: {x}')


def aqua_will_response(aqua_mock: MockHttpServer, responses: List):
    aqua_mock.reset()
    for command, response in responses:
        if isinstance(response, dict):
            text = response['text']
            status = response['status']
        else:
            text = response
            status = 200

        aqua_mock.expect(request=MatchRequest(path=is_('/run'), body=is_(command)),
                         response=MockResponse(status=status, body=text))
