from aiohttp import web
from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub
from mail.so.api.so_api_pb2 import SoRequest
from urllib.parse import urlparse
from json import dumps


def get_so_cluster(env):
    return env.stubs.so_in if env.nwsmtp.conf.nwsmtp.so.client == 'so_in_client' else env.stubs.so_out


def get_so_resolution():
    return "SO_RESOLUTION_ACCEPT"


def get_so_types():
    return ["people", "trust_5", "domain_domain"]


def make_peronal_resolutions():
    return []


def get_deny_graylist():
    return False


def make_out_parameters():
    return {"forward_type": "FORWARD_TYPE_WHITE"}


def get_activity_infos():
    return []


def make_so_resp():
    return {
        "resolution": get_so_resolution(),
        "deny_graylist": get_deny_graylist(),
        "so_classes": get_so_types(),
        "personal_resolutions": make_peronal_resolutions(),
        "out_parameters": make_out_parameters(),
        "activity_infos": get_activity_infos()
    }


class SO(BaseHTTPStub):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.requests = []

    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_base_path(self):
        return self.conf.hosts

    def get_routes(self):
        return [web.post("/v3/antispam", self.handle_antispam)]

    def get_message(self):
        return self.requests[0].raw_email if len(self.requests) > 0 else None

    def get_request(self):
        return self.requests[0] if len(self.requests) > 0 else None

    async def handle_antispam(self, request):
        so_request = SoRequest()
        so_request.ParseFromString(await request.read())
        self.requests.append(so_request)
        return web.Response(text=dumps(make_so_resp()))
