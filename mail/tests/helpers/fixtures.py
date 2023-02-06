import os
from contextlib import contextmanager

from mail.devpack.lib import config_master
from mail.devpack.lib.coordinator import Coordinator
from mail.devpack.lib.env import DevEnv
from yatest.common import network
from .env import TestEnv


@contextmanager
def test_coordinator_factory(top_comp_cls, config_path=None, devpack_root=None):
    pm = network.PortManager()
    config = config_master.read(config_path) if config_path else None
    env = TestEnv(pm, top_comp_cls=top_comp_cls, devpack_root=devpack_root, config=config)
    coord = Coordinator(env, top_comp_cls=top_comp_cls)
    try:
        yield coord
    finally:
        coord.purge()
        coord.hard_purge()
        pm.release()


@contextmanager
def dev_coordinator_factory(top_comp_cls, config_path=None):
    if config_path is None:
        config_path = config_master.DEFAULT_CONFIG_PATH
    config = config_master.read(config_path)
    env = DevEnv(config)
    yield Coordinator(env, top_comp_cls=top_comp_cls)


@contextmanager
def coordinator_factory(top_comp_cls, use_test_env=True, config_path=None, devpack_root=None):
    cf = test_coordinator_factory(top_comp_cls, config_path, devpack_root) if use_test_env else dev_coordinator_factory(top_comp_cls)
    with cf as coord:
        yield coord


@contextmanager
def coordinator_context(top_comp_cls, use_test_env=None, config_path=None, devpack_root=None):
    if use_test_env is None:
        use_test_env = 'DEVPACK_USE_DEV_ENV' not in os.environ
    with coordinator_factory(top_comp_cls, use_test_env, config_path, devpack_root) as coord:
        with coord.context(top_comp_cls, do_cleanup=use_test_env) as ctx_coord:
            yield ctx_coord
