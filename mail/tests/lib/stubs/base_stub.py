import logging

from typing import Optional, List

from aiohttp.web import AppRunner, Application, TCPSite, RouteDef

from mail.nwsmtp.tests.lib.http_client import HTTPClient
from mail.nwsmtp.tests.lib.config import Conf
from mail.nwsmtp.tests.lib.users import Users

log = logging.getLogger(__name__)


class BaseStub:
    def __init__(self, conf: Conf):
        self.conf = conf
        self.server = None

    def __str__(self):
        s = super().__str__()
        name = s.split(" ", 1)[0]
        return "%s at %s" % (name.split(".")[-1], hex(id(self)))

    def get_port(self) -> int:
        raise NotImplementedError()

    def get_alt_port(self) -> Optional[int]:
        return None

    def get_client(self):
        raise NotImplementedError()

    async def start(self):
        log.info("%s listen %d (%s)",
                 self, self.get_port(), self.get_alt_port())

    async def stop(self):
        raise NotImplementedError()

    async def __aenter__(self):
        await self.start()
        return self

    async def __aexit__(self, *args, **kwargs):
        await self.stop()


class BaseHTTPStub(BaseStub):
    def __init__(self, conf: Conf, users: Users):
        super().__init__(conf)
        self.users = users

    def get_base_path(self) -> str:
        raise NotImplementedError()

    def get_routes(self) -> List[RouteDef]:
        raise NotImplementedError()

    def get_app(self) -> Application:
        app = Application()
        app.add_routes(self.get_routes())
        return app

    def get_client(self) -> HTTPClient:
        return HTTPClient(self.get_base_path())

    async def start(self):
        await super().start()
        self.server = AppRunner(self.get_app())
        await self.server.setup()
        await TCPSite(self.server, port=self.get_port(), reuse_address=True).start()
        if self.get_alt_port():
            await TCPSite(self.server, port=self.get_alt_port(), reuse_address=True).start()

    async def stop(self):
        await self.server.cleanup()
