from aiohttp import web
from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub
from urllib.parse import urlparse
from collections import defaultdict
import json


class SoRbl(BaseHTTPStub):
    """
        SO RBL http mock
        MAILDLV-4983
        https://wiki.yandex-team.ru/ps/SO/RBL/
    """
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.known_ips = defaultdict(dict)  # e.g. {'::1': {'is_spam': True}}
        self.requests = []

    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_base_path(self):
        return self.conf.hosts

    def mark_ip_as_spam(self, ip: str):
        self.known_ips[ip]['is_spam'] = True

    def mark_ip_as_not_spam(self, ip: str):
        self.known_ips[ip]['is_spam'] = False

    def get_routes(self):
        return [web.get("/check", self.handle_check)]

    def get_requests(self):
        return self.requests

    def reset_requests(self):
        self.requests = []

    def handle_check(self, request):
        """
            This method checks only combined-bl

            Json answer example
            {
                "infos": {
                },
                "checks": {
                    "combined-bl": false
                }
            }
        """
        requested_ip = request.query['ip']
        self.requests.append(requested_ip)

        response = {}
        response['infos'] = {}
        response['checks'] = {}
        found_in_bl = False
        if requested_ip in self.known_ips:
            found_in_bl = self.known_ips[requested_ip]['is_spam']
        response['checks']['spamsource'] = found_in_bl
        return web.Response(text=json.dumps(response))
