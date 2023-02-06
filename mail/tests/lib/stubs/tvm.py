import json

from urllib.parse import parse_qs

from aiohttp import web

from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub
from mail.nwsmtp.tests.lib.util import get_port, read_data


class TVM(BaseHTTPStub):
    def get_routes(self):
        return [
            web.get("/2/keys", self.keys),
            web.get("/2/keys/", self.keys),
            web.post("/2/ticket", self.ticket),
            web.post("/2/ticket/", self.ticket)
        ]

    async def keys(self, _):
        return web.Response(text=read_data("public_key"))

    async def ticket(self, request):
        body = await request.text()
        params = parse_qs(body)

        dsts = params["dst"][0].split(",") if len(params["dst"]) > 0 else []

        ret = {}
        for dst in dsts:
            ret[dst] = {
                "ticket": read_data("service_ticket").strip()
            }

        return web.Response(text=json.dumps(ret))

    def get_port(self):
        return get_port(self.get_base_path())

    def get_base_path(self):
        return self.conf.tvm_host
