from aiohttp import web
from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub
from urllib.parse import urlparse


async def handle_get():
    return web.json_response(status=200, data={
        "settings": {
            "parameters": {
                "single_settings": {
                    "enable_imap_auth_plain": "on"
                }
            },
            "profile": {
                "single_settings": {
                    "enable_pop": "",
                    "enable_imap": "on"
                }
            }
        }
    })


class Settings(BaseHTTPStub):
    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_base_path(self):
        return self.conf.hosts

    def get_routes(self):
        return [web.get("/get", self.handle_get)]

    async def handle_get(self, request):
        return await handle_get()
