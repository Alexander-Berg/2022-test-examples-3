from urllib.parse import urlparse

from aiohttp import web

from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub


def get_is_internal():
    return False


def get_readonly():
    return False


async def handle_delivery_info(users, request):
    email = request.query["email"]
    user = users.get(email)

    if not user or not user.is_corp:
        return web.json_response(status=410, data={
            "status": "error",
            "error": "no entity with email {}".format(email)
        })

    if user.is_ml:
        return web.json_response({
            "status": "ok",
            "is_open": True,
            "is_internal": get_is_internal(),
            "type": "maillist",
            "subscribers": {
                "inbox": list(user.subscribers.keys())
            },
            "readonly": get_readonly(),
            "who_can_write": []
        })

    return web.json_response({
        "emails": [user.email],
        "login": user.login,
        "status": "ok",
        "type": "user"
    })


class CorpML(BaseHTTPStub):

    def get_port(self):
        return urlparse(self.conf.addr).port

    def get_base_path(self):
        return self.conf.addr

    def get_routes(self):
        return [web.get("/apiv2/delivery/info", self.handle_delivery_info)]

    async def handle_delivery_info(self, request):
        return await handle_delivery_info(self.users, request)
