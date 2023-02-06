import pytest
from yatest.common import network

from mail.devpack.lib.components.postgres import Postgres
from mail.devpack.lib.coordinator import Coordinator
from mail.devpack.tests.helpers.env import TestEnv
from mail.ipa.ipa.conf import settings
from mail.ipa.ipa.tests.utils import dummy_async_context_manager, dummy_async_function
from mail.ipa.ipa.utils.db import create_configured_engine


class IpaDB(Postgres):
    NAME = 'ipa_db'

    def __init__(self, env, components):
        self.init_from_conf(
            config=env.get_config(),
            dbname=self.NAME,
            users=('ipa',),
            ddl_prefixes=('resfs/file/migrations',),
        )


@pytest.fixture(autouse=True, scope='session')
def configure_db():
    if not settings.DB_USE_DEVPACK:
        yield
    else:
        port_manager = network.PortManager()
        coordinator = Coordinator(
            TestEnv(port_manager, IpaDB),
            top_comp_cls=IpaDB,
        )
        coordinator.start()
        db = coordinator.components[IpaDB]
        settings.DATABASE = {
            'NAME': db.pg.dbname,
            'USER': None,
            'PASSWORD': None,
            'HOST': '0.0.0.0',
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
def raw_db_engine(loop):
    engine = create_configured_engine()
    loop.run_until_complete(engine.connect())
    yield engine
    engine.close()
    loop.run_until_complete(engine.wait_closed())


@pytest.fixture
def mocked_db_engine(loop, monkeypatch, mocker, raw_db_engine):
    MOCK_TR = True
    conn = loop.run_until_complete(raw_db_engine.acquire().__aenter__())
    tr = loop.run_until_complete(conn.begin())

    def mock_acquire():
        return dummy_async_context_manager(conn)

    def mock_tr():
        tr = mocker.MagicMock
        tr.close = tr.commit = tr.rollback = dummy_async_function()
        tr.is_active = True
        return dummy_async_context_manager(tr)

    if MOCK_TR:
        for subengine in raw_db_engine:
            monkeypatch.setattr(subengine, 'acquire', mock_acquire)
        monkeypatch.setattr(conn, 'begin', mock_tr)

    yield raw_db_engine

    if not MOCK_TR:
        loop.run_until_complete(tr.commit())
    else:
        loop.run_until_complete(tr.rollback())
