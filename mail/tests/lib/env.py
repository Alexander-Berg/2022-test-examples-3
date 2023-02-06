import asyncio
import logging

from datetime import datetime
from itertools import chain
from typing import List, Tuple, Optional

from contextlib import asynccontextmanager

from mail.nwsmtp.tests.lib.stubs.relay import Message
from mail.nwsmtp.tests.lib.stubs import (BigML, Blackbox, Relay, CorpML, StubsRunner,
                                         MDS, TVM, RateSrv, Yarm, Settings, Nsls, SoRbl,
                                         SO, Avir, Fouras)
from mail.nwsmtp.tests.lib.config import Conf
from mail.nwsmtp.tests.lib.users import Users
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.nwsmtp import NwSMTP

log = logging.getLogger(__name__)
_envs_cache = {}


class Relays(StubsRunner):
    def __init__(self, conf):
        super().__init__()
        self.local = Relay(conf.nwsmtp.delivery.relays.local)
        self.fallback = Relay(conf.nwsmtp.delivery.relays.fallback)
        if conf.nwsmtp.delivery.relays.external and conf.nwsmtp.delivery.relays.external.addr:
            self.external = Relay(conf.nwsmtp.delivery.relays.external)

        if 'mxcode_map' in conf.nwsmtp.delivery:
            for foreignMX in conf.nwsmtp.delivery.mxcode_map:
                if foreignMX['key'] == 'gre':
                    self.grey = Relay(foreignMX)
                if foreignMX['key'] == 'bla':
                    self.black = Relay(foreignMX)
                if foreignMX['key'] == 'whi':
                    self.white = Relay(foreignMX)

        if 'relays_map' in conf.nwsmtp.delivery.relays.sender_dependent:
            for relay in conf.nwsmtp.delivery.relays.sender_dependent.relays_map:
                if relay['key'] == 'mail.ru':
                    self.mail = Relay(relay)
                    break

    def get_relays_futures(self, msg_id: str, timeout=1.0):
        futures = [
            self.local.wait_msg(msg_id, timeout),
            self.fallback.wait_msg(msg_id, timeout),
        ]
        if hasattr(self, 'external'):
            futures.append(self.external.wait_msg(msg_id, timeout))
        if hasattr(self, 'grey'):
            futures.append(self.grey.wait_msg(msg_id, timeout))
        if hasattr(self, 'black'):
            futures.append(self.black.wait_msg(msg_id, timeout))
        if hasattr(self, 'white'):
            futures.append(self.white.wait_msg(msg_id, timeout))
        if hasattr(self, 'mail'):
            futures.append(self.mail.wait_msg(msg_id, timeout))
        return futures

    async def wait_msg(self, msg_id: str, timeout=1.0) -> Message:
        done, pending = await asyncio.wait(self.get_relays_futures(msg_id, timeout), return_when=asyncio.FIRST_COMPLETED)

        for task in pending:
            task.cancel()
        return (await asyncio.gather(*done))[0]

    async def wait_msgs(self, msg_id=None, timeout=1.0) -> List[Tuple[str, List[Message]]]:
        done, _ = await asyncio.wait(self.get_relays_futures(msg_id, timeout), return_when=asyncio.ALL_COMPLETED)
        list_of_results = (await asyncio.gather(*done))
        return list(chain(*list_of_results))


class Stubs(StubsRunner):
    def __init__(self, conf: Conf, users: Users):
        super().__init__()
        self.blackbox = Blackbox(conf.modules.blackbox_client.configuration, users)
        self.big_ml = BigML(conf.modules.big_ml_client.configuration, users)
        self.corp_ml = CorpML(conf.nwsmtp.corp_maillist, users)
        self.mds = MDS(conf.modules.mds_client.configuration, users)
        self.ratesrv = RateSrv(conf.modules.ratesrv_client.configuration, users)
        self.tvm = TVM(conf.tvm, users)
        self.yarm = Yarm(conf.modules.yarm_client.configuration, users)
        self.settings = Settings(conf.modules.settings_client.configuration, users)
        self.nsls = Nsls(conf.modules.nsls_client.configuration, users)
        self.so_rbl = SoRbl(conf.modules.rbl_http_client.configuration, users)
        self.so_in = SO(conf.modules.so_in_client.configuration, users)
        self.so_out = SO(conf.modules.so_out_client.configuration, users)
        self.avir = Avir(conf.nwsmtp.av)
        self.fouras = Fouras(conf.modules.dkim.configuration.fouras, users)


class Env:
    def __init__(self, back: str, conf: Optional[Conf] = None):
        self.back = back
        self.conf = conf
        self.nwsmtp: Optional[NwSMTP] = None
        self.stubs = None
        self.relays = None
        self.users = None
        self._make_conf = None

    def is_corp(self) -> bool:
        return self.conf.is_corp()

    def start_conf(self):
        if not self.conf:
            self._make_conf = make_conf(self.back)
            self.conf = self._make_conf.__enter__()

    def stop_conf(self):
        if self._make_conf:
            self._make_conf.__exit__(None, None, None)
            self._make_conf = None
        self.conf = None

    async def start_nwsmtp(self):
        if not self.nwsmtp or not self.nwsmtp.is_running():
            self.nwsmtp = NwSMTP(self.conf)
            await self.nwsmtp.start()

    def stop_nwsmtp(self):
        self.nwsmtp and self.nwsmtp.stop()

    async def start(self):
        self.start_conf()

    def stop(self):
        self.stop_nwsmtp()
        self.stop_conf()

    async def __aenter__(self):
        await self.start()
        return self

    async def __aexit__(self, *args, **kwargs):
        self.stop()


@asynccontextmanager
async def run_for_single_test(env: Env, users: Users) -> Env:
    start = datetime.now()
    try:
        env.users = users
        async with Stubs(env.conf, env.users) as stubs:
            env.stubs = stubs
            async with Relays(env.conf) as relays:
                env.relays = relays
                await env.start_nwsmtp()

                yield env

                if not env.nwsmtp.is_running():
                    raise RuntimeError("NwSMTP is down after test. Possibly crashed? "
                                       "See nwsmtp.{stderr,stdout}, nwsmtp.tskv or yplatform.log")
    finally:
        env.users = None
        env.stubs = None
        env.relays = None
        if env.nwsmtp:
            for line in env.nwsmtp.get_log("yplatform", start):
                log.info(line)


@asynccontextmanager
async def make_env(back: str, users: Users, conf: Optional[Conf] = None) -> Env:
    async with Env(back, conf) as env:
        async with run_for_single_test(env, users):
            yield env


@asynccontextmanager
async def get_env(back: str, users: Users) -> Env:
    if back not in _envs_cache:
        env = Env(back)
        try:
            await env.start()
        except Exception:
            env.stop()
            raise

        _envs_cache[back] = env

    env = _envs_cache[back]
    async with run_for_single_test(env, users) as env:
        yield env
