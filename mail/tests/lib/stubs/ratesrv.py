from aiohttp import web
from urllib.parse import urlparse
from json import dumps

from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub


def make_increase_response_for_counter(id, json):
    return {
        "status": "ok",
        "current": json["value"],
        "available": 10
    }


def mockable_make_increase_response(request):
    ratesrv_increase_counters_resp = {"counters": {}}
    for id, json in request["counters"].items():
        counter_response = make_increase_response_for_counter(id, json)
        ratesrv_increase_counters_resp["counters"][id] = counter_response
    return ratesrv_increase_counters_resp


def make_increase_response(request):
    """
    Parameters
    ----------
    request : json
        For example:
        {
            'counters': {
                '0': {
                    'name': 'mxcorp:greylisting:12447369857732515709',
                    'value': 1576514143
                }
            }
        }

    Returns
    ----------
    web.Response
    """
    return web.Response(text=dumps(mockable_make_increase_response(request)))


def mockable_get_current_counter_value(id):
    return 0


def make_get_response_for_counter(id, group, limit, hash):
    return {
        "status": "ok",
        "current": mockable_get_current_counter_value(id),
        "available": 10
    }


def make_get_response(request):
    """
    Parameters
    ----------
    request : json

    For example:
        {
            'counters': {
                '0': 'mxcorp:greylisting:12447369857732515709'
             }
        }

    Another example:
        {
            'counters': {
                '1': 'mxfront:msgs_for_rcpt_from_ip:1120000003066712##::1'
             }
        }

    Returns
    ----------
    web.Response
    """
    ratesrv_increase_counters_resp = {"counters": {}}
    for id, counter in request["counters"].items():
        group, limit, hash = counter.split(':', 2)
        counter_response = make_get_response_for_counter(id, group, limit, hash)
        ratesrv_increase_counters_resp["counters"][id] = counter_response
    return web.Response(text=dumps(ratesrv_increase_counters_resp))


class RateSrv(BaseHTTPStub):
    """
    Api description: https://wiki.yandex-team.ru/pochta/mx/RateSrv/
    """
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.requests = []

    def get_routes(self):
        return [
            web.post("/counters", self._handle_get_counters),
            web.post("/counters/increase", self._handle_increase_counters)
        ]

    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_base_path(self):
        return self.conf.http_client.hosts

    async def _handle_get_counters(self, request):
        json = await request.json()
        self.requests.append(json)
        return make_get_response(json)

    async def _handle_increase_counters(self, request):
        json = await request.json()
        self.requests.append(json)
        return make_increase_response(json)
