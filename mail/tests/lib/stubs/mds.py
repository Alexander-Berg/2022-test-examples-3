import logging

from random import choice
from urllib.parse import urlparse

from aiohttp import web

from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub

log = logging.getLogger(__name__)


def make_stid(uid):
    rnd = "".join(str(choice(range(5))) for _ in range(5))
    return f"320.mail:{uid}.E0000000:0000000000000000000000000{rnd}"


def make_message():
    return "Subject: Hello\r\n\r\nText"


class MDS(BaseHTTPStub):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.messages = []
        self.requests = []
        self.stid = []

    def get_routes(self):
        return [
            web.post(r"/gate/put/mail:{uid:\d+}", self.handle_put),
            web.get(r"/gate/get/{stid}", self.handle_get),
            # web.get("/hostlist", self.handle_hostlist)  # TODO(MAILDLV-3265)
        ]

    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_base_path(self):
        return self.conf.url

    async def handle_put(self, request):
        uid = request.match_info["uid"]
        self.requests.append(request)
        self.messages.append(await request.read())
        return web.Response(text=make_stid(uid))

    async def handle_get(self, request):
        stid = request.match_info["stid"]
        self.requests.append(request)
        self.stid.append(stid)
        return web.Response(text=make_message())
