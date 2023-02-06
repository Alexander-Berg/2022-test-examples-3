from aiohttp import web
from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub
from urllib.parse import urlparse
from random import randint
from json import dumps


def make_mid():
    return str(randint(1000, 2000))


def make_imap_id():
    return str(randint(1000, 2000))


def make_nsls_store_resp(result_type, mid=None, imap_id=None):
    return {
        "email": "",
        "uid": "",
        "is_local": "yes",
        "notify": {
            "success": False,
            "failure": False,
            "delay": False,
        },
        "is_mailish": True,
        "delivery_result": {
            "mid": mid or make_mid(),
            "imap_id": imap_id or make_imap_id(),
            "stid": "",
            "mail_from": "",
            "hints": [],
        },
        "status": result_type,
    }


def make_store_resp(result_type="stored", mid=None, imap_id=None):
    return {
        "recipients": [make_nsls_store_resp(result_type, mid, imap_id)]
    }


class Nsls(BaseHTTPStub):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.requests = []

    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_base_path(self):
        return self.conf.hosts

    def get_routes(self):
        return [web.post("/store", self.handle_store)]

    async def handle_store(self, request):
        json = await request.json()
        self.requests.append(json)
        return web.Response(text=dumps(make_store_resp()))
