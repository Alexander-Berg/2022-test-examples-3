import pytest
import os

import asyncio

from bot.config.config import _default_logging
del _default_logging['handlers']['file']
import logging.config
logging.config.dictConfig(_default_logging)

from bot import modules

from mocks.zk import MockZK
from mocks.bot import MockBot
from mocks.clients.beholder import MockBeholder
from mocks.clients.aiostaff import MockStaff
from mocks.clients.aioabc import MockABC
from mocks.telegram import MockTelegram
from mocks.context.screenshoter import Screenshoter
from mocks.clients.aiojuggler import Juggler
from mocks.clients.aioshiftinator import Shiftinator
from mocks.clients.aiostartrek import Startrek
from mocks.clients.aiotickenator import Tickenator

from functools import partial

import tracemalloc

tracemalloc.start()


@pytest.fixture(scope='session')
def get_context() -> modules.Context:
    class MockContext(modules.Context):
        def __init__(self):
            super().__init__()

            self.deployment = False
            self.prestable = False
            self.site = 'localhost'
            self.tz = 'utc'
            self.loop = self.loop or asyncio.get_event_loop()
            self.zk = MockZK()

            self.bot = MockBot()
            self.beholder_proto = MockBeholder()

            self.warden = modules.Warden(self, {})
            self.warden._init = None

            self.juggler = Juggler()
            self.screenshoter = Screenshoter()
            self.shiftinator = Shiftinator()
            self.startrek = Startrek()
            self.tickenator = Tickenator()

            self.telegram = MockTelegram(self)
            self.staff = MockStaff()
            self.abc = MockABC()
            self.infra = modules.Infra(self, {})
            self.deployments = modules.Deployments(self, {})
            self.morty = modules.Morty(self, {})
            self.auth = modules.Auth(self, {})
            self.modules.support = modules.Support(self, {})
            self.modules.marty = modules.Marty(self, {})

            async def wait_for_data(*args, **kwargs):
                return

            self.modules.marty.wait_for_marty = wait_for_data

            # Mock some of functions
            async def get_person(self, *, login=None, telegram=None, tgid=None, ctx=None, logger=None):
                """Мокает метод get_person, для получения юзеров только из кэша, чтобы не ходить клиентом."""
                login = login.lower() if login else None
                telegram = telegram.lower() if telegram else None

                record = (
                    None or
                    self._by_login.get(login) or
                    self._by_username.get(telegram)
                )

                return record

            self.auth.get_person = partial(get_person, self=self.auth)

    context = MockContext()
    try:
        yield context
    finally:
        pass


@pytest.fixture(scope='session', autouse=True)
def initialize_db(get_context):
    # https://a.yandex-team.ru/arc/trunk/arcadia/quality/ab_testing/tools/postgres_local/README.md
    data = modules.Data(get_context, dict(
        hosts=[os.environ['POSTGRES_RECIPE_HOST']],
        port=os.environ['POSTGRES_RECIPE_PORT'],
        dbname=os.environ['POSTGRES_RECIPE_DBNAME'],
        user=os.environ['POSTGRES_RECIPE_USER'],
        password=None,
        minsize=1,
        maxsize=int(os.environ['POSTGRES_RECIPE_MAX_CONNECTIONS']),
        timeout=5,
        master_as_replica_weight=0.5,
    ))
    get_context.data = data


@pytest.fixture(scope='function', autouse=True)
async def prepare_database(get_context):
    data = get_context.data
    try:
        await data.initialize()
        yield
    finally:
        async with await data.connect() as conn:
            await conn.execute('DROP SCHEMA public CASCADE;\nCREATE SCHEMA public;')

        await data.close()
