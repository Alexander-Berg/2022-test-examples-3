from json import loads, dumps

from email.mime.base import MIMEBase

from socket import AF_INET6, AF_INET
from typing import Tuple, Union, Any
from urllib.parse import urljoin, urlencode

from aiohttp import request, TCPConnector, ClientTimeout

from mail.nwsmtp.tests.lib.util import make_raw_multipart, make_raw_message


class HTTPClient:
    def __init__(self, base_url: str, ipv6=False):
        self.base_url = base_url
        self.ipv6 = ipv6

    def get_connector(self):
        if self.ipv6:
            return TCPConnector(family=AF_INET6)
        return TCPConnector(family=AF_INET)

    async def fetch(self, path: str, method: str = "GET",
                    data: Any = None, headers: Any = None, timeout=1.0) \
            -> Tuple[int, Union[dict, bytes]]:

        if not path.startswith("/"):
            raise RuntimeError("path should starts with /")
        url = urljoin(self.base_url, path)

        timeout = ClientTimeout(total=timeout)
        async with request(method, url, data=data, headers=headers,
                           connector=self.get_connector(), timeout=timeout) as resp:
            body = await resp.read()
            if resp.headers["Content-Type"] == "application/json":
                return resp.status, loads(body)
            return resp.status, body

    async def get(self, path: str) -> Tuple[int, Union[dict, bytes]]:
        return await self.fetch(path)

    async def post(self, path: str, data: Any = None, headers: dict = None) \
            -> Tuple[int, Union[dict, bytes]]:

        headers = headers or {}
        if isinstance(data, MIMEBase):
            # Do not change order of calls,
            #  otherwise Content-type would not contain boundary field
            data_bytes = data.as_bytes()
            headers["Content-Type"] = data["Content-Type"]
            return await self.fetch(path, method="POST", data=data_bytes, headers=headers)
        return await self.fetch(path, method="POST", data=data, headers=headers)

    async def store(self, user, json, msg, fid=None, service="sendbernar", headers=None):
        body, headers = self._make_multipart_body_and_headers(json, msg, headers)
        params = {"fid": fid, "service": service, "uid": user.uid}
        if fid is None:
            params.pop("fid")
        return await self.post("/mail/store?" + urlencode(params), body, headers=headers)

    async def store_mailish(self, user, json, msg, fid=1, external_imap_id=1, headers=None):
        body, headers = self._make_multipart_body_and_headers(json, msg, headers)
        url = f"/mail/store_mailish?uid={user.uid}&fid={fid}&external_imap_id={external_imap_id}"
        return await self.post(url, body, headers=headers)

    async def send_mail(self, user, json, msg,
                        detect_spam="0", detect_virus="0",  service="sendbernar", headers=None):
        body, headers = self._make_multipart_body_and_headers(json, msg, headers)
        url = (
            f"/mail/send_mail?uid={user.uid}&from_email={user.email}&"
            f"service={service}&detect_spam={detect_spam}&detect_virus={detect_virus}"
        )
        return await self.post(url, body, headers=headers)

    async def send_system_mail(self, user, msg, to: list, labels: list = [], lids: list = [],
                               service="sendbernar", headers=None):
        to_parameter = self._make_url_parameter_from_list("to", to)
        labels_parameter = self._make_url_parameter_from_list("label", labels)
        lids_parameter = self._make_url_parameter_from_list("lid", lids)
        body, headers = self._make_body_and_headers(msg, headers)
        url = (
            f"/mail/send_system_mail?uid={user.uid}&from={user.email}&"
            f"service={service}&{to_parameter}&{labels_parameter}&{lids_parameter}"
        )
        return await self.post(url, body, headers=headers)

    async def restore(self, user, json, service="barbet", headers=None):
        url = f"/mail/restore?uid={user.uid}&service={service}&request_id=request_id"
        headers = headers or {}
        headers["Content-Type"] = "application/json"
        return await self.post(url, dumps(json), headers=headers)

    async def save(self, user, msg, received_date="0", fid="fid", old_mid="old_mid", system: list = [],
                   symbol: list = [], lids: list = [], detect_virus="0",
                   detect_spam="0", service="sendbernar", headers=None):
        system_parameter = self._make_url_parameter_from_list("system", system)
        symbol_parameter = self._make_url_parameter_from_list("symbol", symbol)
        lids_parameter = self._make_url_parameter_from_list("lid", lids)
        body, headers = self._make_body_and_headers(msg, headers)
        url = (
            f"/mail/save?uid={user.uid}&email={user.email}&fid={fid}&"
            f"service={service}&{system_parameter}&{symbol_parameter}&{lids_parameter}&"
            f"old_mid={old_mid}&detect_spam={detect_spam}&detect_virus={detect_virus}&"
            f"received_date={received_date}&request_id=request_id"
        )
        return await self.post(url, body, headers=headers)

    @staticmethod
    def _make_multipart_body_and_headers(json, msg, headers):
        boundary = "===============1=="
        body = make_raw_multipart(json, msg, boundary)
        headers = headers or {}
        headers["Content-Type"] = f"multipart/mixed; boundary=\"{boundary}\""
        return body, headers

    @staticmethod
    def _make_body_and_headers(msg, headers):
        body = make_raw_message(msg)
        headers = headers or {}
        headers["Content-Type"] = "message/rfc822"
        return body, headers

    @staticmethod
    def _make_url_parameter_from_list(name, parameters: list):
        return "&".join(name + "=" + parameter for parameter in parameters)
