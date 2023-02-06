import os

from urllib.parse import urlparse

from jinja2 import StrictUndefined
from jinja2.environment import Template

from aiohttp import web

from yatest.common import work_path

from mail.nwsmtp.tests.lib.dkim_domains import DKIMDomains
from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub
from mail.nwsmtp.tests.lib.util import read_data


def load_keys(name):
    return read_data(work_path(os.path.join("fouras", name)))


def render_response(name, *args, **kwargs):
    template = Template(read_data(name), undefined=StrictUndefined)
    return template.render(*args, **kwargs)


async def handle_request(dkim_domains, request):
    if "domain" not in request.query:
        return web.Response(
            status=404
        )

    domain = request.query["domain"]
    dkim_domain = dkim_domains.get(domain)
    if not dkim_domain:
        return web.Response(
            text=read_data("fouras_not_found.json"), status=404
        )

    dkim_public_key = load_keys("dkim_public_key")
    dkim_private_key = load_keys("dkim_private_key")
    if dkim_domain.is_incorrect:
        dkim_private_key = ""

    return web.Response(
        text=render_response(
            "fouras_found.json",
            dkim_domain=dkim_domain,
            dkim_public_key=dkim_public_key,
            dkim_private_key=dkim_private_key
        )
    )


class Fouras(BaseHTTPStub):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.dkim_domains = DKIMDomains()
        self.requests = []

    def add_dkim_domains(self, domain):
        self.dkim_domains.add(domain)

    def get_port(self):
        return urlparse(self.conf.http_client.hosts).port

    def get_base_path(self):
        return self.conf.http_client.hosts

    def get_routes(self):
        return [web.get("/smtp/key", self.handle_request)]

    async def handle_request(self, request):
        self.requests.append(request)
        return await handle_request(self.dkim_domains, request)
