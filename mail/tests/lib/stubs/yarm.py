import socket

from urllib.parse import urlparse

from aiohttp import web

from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub
from mail.nwsmtp.tests.lib.util import read_data


async def handle_smtp_data(users, request):
    email = request.query["email"]
    if 'bad' in email:
        return web.json_response(status=500, data={"error": {
            "method": "smtp_data",
            "reason": "internal api error",
            "description": "no such collector",
            "host": socket.gethostname(),
            "request_id": "N4KafXMRw8c1",
        }})
    return web.Response(text=read_data("smtp_auth_data.json"))


class Yarm(BaseHTTPStub):
    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_base_path(self):
        return self.conf.hosts

    def get_routes(self):
        return [web.get("/api/v2/smtp_data", self.handle_smtp_data)]

    async def handle_smtp_data(self, request):
        return await handle_smtp_data(self.users, request)
