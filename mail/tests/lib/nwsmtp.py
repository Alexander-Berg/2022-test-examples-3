import asyncio
import logging
import os
import aiosmtplib

from bisect import bisect_left
from datetime import datetime
from ssl import SSLContext, PROTOCOL_SSLv23
from typing import List, Tuple, Optional

from aiosmtplib.smtp import SMTP

from yatest.common import binary_path, execute, process

from mail.nwsmtp.tests.lib.config import Conf
from mail.nwsmtp.tests.lib.http_client import HTTPClient

log = logging.getLogger(__name__)

DATE_TIME_FMT = "%Y-%m-%d %H:%M:%S.%f"


class NwSMTP:
    def __init__(self, conf: Conf):
        self.conf = conf
        self.proc = None  # type: process._Execution
        self.logs = {
            "yplatform": Log(os.path.join(conf.base_path, conf.log["global"]["sinks"][0]["path"])),
            "nwsmtp": Log(os.path.join(conf.base_path, conf.log["tskv"]["sinks"][0]["path"]))
        }

    async def wait_for_str_in_log(self, log_type, waited_string, start=None):
        return await self.logs[log_type].wait_for_str(waited_string, start)

    def get_log(self, log_type, start=None) -> List[str]:
        return self.logs[log_type].get(start)

    def is_running(self):
        return self.proc and self.proc.running

    async def start(self) -> None:
        nwsmtp_bin = binary_path("mail/nwsmtp/bin/nwsmtp")
        self.proc = execute([nwsmtp_bin, self.conf.path],
                            close_fds=True, wait=False, check_exit_code=False)
        if not await wait_nwsmtp_ready(self):
            try:
                self.proc.kill()
            except process.InvalidExecutionStateError:
                pass
            raise RuntimeError("Unable to run NwSMTP, see 'nwsmtp.err'")

    def stop(self) -> None:
        try:
            self.proc.kill()
        except process.InvalidExecutionStateError:
            pass

    async def __aenter__(self):
        await self.start()
        return self

    async def __aexit__(self, *args, **kwargs):
        self.stop()

    def is_auth_required(self) -> bool:
        return self.conf.nwsmtp.auth.use

    @staticmethod
    def make_client(use_tls, host, port) -> SMTP:
        ctx = SSLContext(PROTOCOL_SSLv23)
        return SMTP(host, port, use_tls=use_tls, tls_context=ctx)

    def get_endpoints(self) -> List[Tuple[bool, str, str]]:
        if self.conf.ymod_smtp_server:
            return [(ep["ssl"], ep["addr"], ep["port"]) for ep in
                    self.conf.ymod_smtp_server.endpoints.listen]
        return [(ep["ssl"], ep["addr"], ep["port"]) for ep in
                self.conf.nwsmtp.endpoints.listen]

    async def get_client(self, connect=True, ipv6=True, use_ssl=True) -> SMTP:
        endpoints = self.get_endpoints()
        is_ssl, host, port = endpoints[0]  # default
        ssl_ep = next((ep for ep in endpoints if ep[0] is True), None)
        if use_ssl and ssl_ep:
            is_ssl, host, port = ssl_ep

        if not ipv6:
            host = "127.0.0.1"

        smtp = self.make_client(is_ssl, host, port)
        if connect:
            await smtp.connect()
        return smtp

    def get_http_client(self, ipv6=True):
        ep = self.conf.web_server.endpoints.listen[0]
        return HTTPClient(f"http://localhost:{ep['port']}", ipv6=ipv6)


async def wait_nwsmtp_ready(nw, timeout=15.0) -> bool:
    total_time = 0
    step = 0.5
    while total_time < timeout:
        await asyncio.sleep(step)
        try:
            client = await nw.get_client(use_ssl=False)
            client.close()
            return True
        except aiosmtplib.errors.SMTPConnectError:
            pass
        total_time += step
    return False


def get_host_port(conf) -> Tuple[str, str]:
    endpoint = conf.nwsmtp.endpoints.listen[0]
    return endpoint["addr"], endpoint["port"]


def parse_log_line(line: str) -> Tuple[datetime, str]:
    raw_date, raw_time, message = line.split(" ", 2)
    raw_date = raw_date[1:]
    raw_time = raw_time[:-1]
    dateobj = datetime.strptime(raw_date + " " + raw_time, DATE_TIME_FMT)
    return dateobj, raw_time + " " + message


def parse_tskv_line(line):
    pairs = dict(field.split("=", 1) for field in line.split("\t") if "=" in field)
    raw_date, raw_time = pairs["timestamp"].split("T", 1)
    raw_time = raw_time.split("+", 1)[0]
    dateobj = datetime.strptime(raw_date + " " + raw_time, DATE_TIME_FMT)
    return dateobj, raw_time + " " + pairs["message"]


class Log:
    def __init__(self, path):
        self.path = path
        if os.path.splitext(path)[1] == '.tskv':
            self.parse_line = parse_tskv_line
        else:
            self.parse_line = parse_log_line
        self.fd = None
        self.lines = []

    def update_log(self) -> None:
        if not self.fd:
            self.fd = open(self.path)
        else:
            self.fd.seek(self.fd.tell())
        for line in self.fd:
            line = line.strip()
            if not line:
                continue
            try:
                self.lines.append(self.parse_line(line))
            except Exception as e:
                log.error(e)

    def get(self, start: Optional[datetime] = None) -> List[str]:
        self.update_log()
        if start is None:
            return [item[1] for item in self.lines]
        idx = bisect_left(self.lines, (start, ""))
        return [item[1] for item in self.lines[idx:]]

    async def wait_for_str(self, waited_string, start: Optional[datetime] = None, timeout=15.0):
        start_time_point = datetime.now()
        while (datetime.now() - start_time_point).total_seconds() < timeout:
            if next((line for line in self.get(start) if waited_string in line), None) is not None:
                return True
            await asyncio.sleep(0.5)
        return False
