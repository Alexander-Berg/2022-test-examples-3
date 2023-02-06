from urllib.parse import urlparse

from aiohttp import web

from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub


def to_subscriptions(big_ml):
    subscriptions = []
    subscriptions.extend(
        {"email": email, "uid": user.uid} for
        email, user in big_ml.subscribers.items())
    return subscriptions


def get_additional_subscribers():
    return []


async def handle_recipients(users, request):
    email = request.query["email_to"]
    user = users.get(email)
    if not user or user.is_corp or not user.is_ml:
        return web.json_response(status=404, data={
            "status": "error",
            "response": {
                "code": "not_found",
                "message": "Maillist with this email {} does not exist".format(email),
                "params": {}}
        })

    return web.json_response({
        "response": {
            "subscriptions": [*to_subscriptions(user), *get_additional_subscribers()]
        },
        "status": "ok"
    })


class BigML(BaseHTTPStub):
    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_base_path(self):
        return self.conf.hosts

    def get_routes(self):
        return [web.get("/api/v1/recipients", self.handle_recipients)]

    async def handle_recipients(self, request):
        return await handle_recipients(self.users, request)
