import pytest
from yatest.common import network

from sendr_aiopg.engine.lazy import PgPingerClient

from mail.beagle.beagle.conf import settings
from mail.beagle.beagle.tests.utils import dummy_async_context_manager, dummy_async_function
from mail.beagle.beagle.utils.db import create_configured_engine
from mail.devpack.lib.components.postgres import Postgres
from mail.devpack.lib.coordinator import Coordinator
from mail.devpack.tests.helpers.env import TestEnv

DATABASE_HOST = '0.0.0.0'


@pytest.fixture
def mock_pg_pinger(mocker):
    async def dummy_hosts(self, preset):
        return {'hosts': [{'hostname': DATABASE_HOST}]}

    mocker.patch.object(PgPingerClient, 'hosts', dummy_hosts)


class BeagleDB(Postgres):
    NAME = 'beagle_db'

    def __init__(self, env, components):
        self.init_from_conf(
            config=env.get_config(),
            dbname=self.NAME,
            users=('beagle',),
            ddl_prefixes=('resfs/file/migrations',),
        )


@pytest.fixture(autouse=True, scope='session')
def configure_db():
    if not settings.DB_USE_DEVPACK:
        yield
    else:
        port_manager = network.PortManager()
        coordinator = Coordinator(
            TestEnv(port_manager, BeagleDB),
            top_comp_cls=BeagleDB,
        )
        coordinator.start()
        db = coordinator.components[BeagleDB]
        settings.DATABASE = {
            'NAME': db.pg.dbname,
            'USER': None,
            'PASSWORD': None,
            'HOST': DATABASE_HOST,
            'PORT': db.pg.port,
            'USE_SSL': None,
            'CONNECT_TIMEOUT': 2,
            'TIMEOUT': 6,
            'TARGET_SESSION_ATTRS': 'read-write',
        }
        yield
        coordinator.purge()
        coordinator.hard_purge()
        port_manager.release()


@pytest.fixture
def raw_db_engine(loop, mock_pg_pinger):
    engine = create_configured_engine()
    yield engine
    engine.close()
    loop.run_until_complete(engine.wait_closed())


@pytest.fixture
def mocked_db_engine(loop, monkeypatch, mocker, raw_db_engine):
    MOCK_TR = True
    conn = loop.run_until_complete(raw_db_engine.acquire().__aenter__())
    tr = loop.run_until_complete(conn.begin())

    def mock_acquire(preset=None):
        return dummy_async_context_manager(conn)

    def mock_tr():
        tr = mocker.MagicMock
        tr.close = tr.commit = tr.rollback = dummy_async_function()
        tr.is_active = True
        return dummy_async_context_manager(tr)

    if MOCK_TR:
        monkeypatch.setattr(raw_db_engine, 'acquire', mock_acquire)
        monkeypatch.setattr(conn, 'begin', mock_tr)

    yield raw_db_engine

    if not MOCK_TR:
        loop.run_until_complete(tr.commit())
    else:
        loop.run_until_complete(tr.rollback())
