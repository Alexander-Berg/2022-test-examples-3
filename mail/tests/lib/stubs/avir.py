import logging

from asyncio import open_connection, ensure_future, start_server

from mail.nwsmtp.tests.lib import HOST
from mail.nwsmtp.tests.lib.stubs.base_stub import BaseStub
from mail.nwsmtp.tests.lib.util import get_port


log = logging.getLogger(__name__)


def is_virus():
    return False


def is_error():
    return False


def build_answer():
    http_response = "HTTP/1.0 403 Forbidden\r\n" if is_virus() else "HTTP/1.0 200 Ok\r\n"
    resolution = "infected\r\n" if is_virus() else "clean\r\n"

    response = ("ICAP/1.0 200 OK\r\n"
                "Server: C-ICAP/1.0\r\n"
                "Connection: close\r\n"
                "ISTag: CI0001YAVS\r\n"
                "Encapsulated: res-hdr=0, res-body=406\r\n\r\n"
                f"{http_response}"
                "X-Infection-Found: Type=0; Resolution=0; Threat=EICAR_test_file\r\n"
                "X-Virus-ID: EICAR_test_file\r\n"
                "Date: Mon Aug 30 2021 05:39:23 GMT\r\n"
                "Last-Modified: Mon Aug 30 2021 05:39:23 GMT\r\n"
                "Content-Length: 8\r\n"
                "Via: ICAP/1.0 (C-ICAP/1.0 Yandex AV Service)\r\n"
                "Content-Type: text/html\r\n"
                "Connection: close\r\n"
                "Via: ICAP/1.0 localhost (C-ICAP/1.0 Yandex AV Service )\r\n\r\n"
                f"{resolution}")

    error = "ICAP/1.0 400 Bad response\r\n"

    return bytes(error if is_error() else response, encoding="ascii")


async def handle_connection(reader, writer):
    try:
        await reader.readuntil(b"\r\n0\r\n\r\n")
        writer.write(build_answer())
        writer.write_eof()
        await writer.drain()
    except Exception as e:
        log.exception(e)


async def run_server(protocol, port):
    return await ensure_future(start_server(protocol, HOST, port, reuse_address=True))


class Avir(BaseStub):
    def __init__(self, *args, **kwargs):
        super(Avir, self).__init__(*args, **kwargs)
        self._connections = []

    def get_client(self):
        return open_connection(HOST, self.get_port())

    def get_port(self):
        return get_port(self.conf.client_settings.primary)

    async def start(self):
        await super().start()
        self.server = await run_server(self._handle_connection, self.get_port())

    async def stop(self):
        self.server.close()
        await self.server.wait_closed()
        await self._close_connections()

    async def _handle_connection(self, reader, writer):
        self._connections.append(writer.transport)
        await handle_connection(reader, writer)

    async def _close_connections(self):
        for conn in self._connections:
            conn.close()
        self._connections.clear()
