import logging

from asyncio import wait_for, sleep, get_event_loop, ensure_future
from collections import defaultdict
from email import message_from_bytes
from email.mime.text import MIMEText
from functools import partial
from typing import List, Tuple
from urllib.parse import urlparse

from aiosmtplib.smtp import SMTP
from aiosmtpd import handlers
from aiosmtpd.smtp import Envelope, SMTP as SMTPD

from mail.nwsmtp.tests.lib import HOST
from mail.nwsmtp.tests.lib.stubs.base_stub import BaseStub

log = logging.getLogger(__name__)


class Message:
    def __init__(self, envelope: Envelope):
        self.envelope = envelope
        self.mime = message_from_bytes(envelope.content)
        self.msg_id = self.mime["Message-Id"]


class WaitMsgsHandler(handlers.AsyncMessage):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.__msgs = defaultdict(list)
        self.__futures = defaultdict(list)

    def handle_message(self, message: Message):
        msg_id = message.msg_id
        self.__msgs[msg_id].append(message)
        if self.__futures[msg_id]:
            msg = self.pop_msg(msg_id)
            for fut in self.__futures.pop(msg_id):
                fut.set_result(msg)

    def prepare_message(self, _, envelope) -> Message:
        return Message(envelope)

    async def handle_DATA(self, _, session, envelope):
        message = self.prepare_message(session, envelope)
        self.handle_message(message)
        return "250 OK"

    def pop_msg(self, msg_id):
        msg = self.__msgs[msg_id].pop(0)
        if not self.__msgs[msg_id]:
            del self.__msgs[msg_id]
        return msg

    async def wait_msg(self, msg_id) -> Message:
        if self.__msgs[msg_id]:
            return self.pop_msg(msg_id)
        future = self.loop.create_future()
        self.__futures[msg_id].append(future)
        await future

    async def wait_msgs(self, msg_id=None, timeout=1.0) -> List[Tuple[str, Message]]:
        await sleep(timeout)
        if msg_id is None:
            return [self.__msgs.popitem() for _ in range(len(self.__msgs))]
        if self.__msgs[msg_id]:
            return [(msg_id, self.__msgs.pop(msg_id))]
        return []


class LMTPD(SMTPD):
    async def smtp_LHLO(self, *args, **kwargs):
        return await super().smtp_HELO(*args, **kwargs)


class LMTPWaitMsgsHandler(WaitMsgsHandler):
    async def handle_DATA(self, server, session, envelope):
        await super().handle_DATA(server, session, envelope)
        return "\r\n".join("250 OK" for _ in envelope.rcpt_tos)


def get_relay_impl(conf):
    if get_url(conf).startswith("lmtp://"):
        return LMTPD, LMTPWaitMsgsHandler
    return SMTPD, WaitMsgsHandler


def get_url(conf):
    if hasattr(conf, "addr"):
        return conf.addr
    return conf["addr"]


class Relay(BaseStub):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        # TODO(mafanasev) Is handler concurrent safe ?
        relay_cls, handler_cls = get_relay_impl(self.conf)
        self.handler = handler_cls()
        self.relay_cls = relay_cls
        self.handler_cls = handler_cls

    async def wait_msg(self, msg_id, timeout=1.0) -> MIMEText:
        return await wait_for(self.handler.wait_msg(msg_id), timeout=timeout)

    async def wait_msgs(self, msg_id=None, timeout=1.0) -> List[Tuple[str, MIMEText]]:
        return await self.handler.wait_msgs(msg_id, timeout)

    def get_port(self):
        url = get_url(self.conf)
        port = urlparse(url).port
        return port

    def get_bypass_port(self):
        try:
            return self.conf.targeting.bypass_port
        except (KeyError, AttributeError):
            return None

    def get_client(self):
        return SMTP(hostname=HOST, port=self.get_port())

    async def start(self):
        await super().start()
        port = self.get_port()
        bypass_port = self.get_bypass_port()

        if bypass_port and bypass_port != port:
            self.targeting_server = await ensure_future(
                get_event_loop().create_server(
                    partial(self.relay_cls, self.handler_cls(), hostname=HOST),
                    host=HOST, port=port, reuse_address=True)
            )
        else:
            self.targeting_server = None

        self.server = await ensure_future(
            get_event_loop().create_server(
                partial(self.relay_cls, self.handler),
                host=HOST, port=(bypass_port or port), reuse_address=True)
        )

    async def stop(self):
        self.server.close()
        await self.server.wait_closed()

        if self.targeting_server:
            self.targeting_server.close()
            await self.targeting_server.wait_closed()
