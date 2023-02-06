import os

from asyncio import sslproto

from json import dumps
from typing import Union, Tuple, Text, Dict
from email import message_from_string
from email.policy import Compat32
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
from email.mime.multipart import MIMEMultipart
from email.mime.message import MIMEMessage
from email.message import Message
from email.utils import make_msgid

from mail.nwsmtp.tests.lib.users import User

from yatest.common import source_path


def make_policy(linesep="\r\n"):
    """
        ..notes:: Use Compat32 to explicitly pass max_line_length to prevent breaking headers
        by `email.header.MAXLINELEN`.
    """
    return Compat32(max_line_length=0, linesep=linesep)


def make_plain_message(
        sender: Union[str, User],
        rcpt: Union[str, User],
        text: str = "Hello",
        subject: str = "My Subject",
        headers=()) -> Tuple[str, MIMEText]:

    if isinstance(sender, User):
        sender = sender.email
    if isinstance(rcpt, User):
        rcpt = rcpt.email

    msg_id = make_msgid()
    msg = MIMEText(text, "plain", "utf-8", policy=make_policy())
    msg["Message-Id"] = msg_id
    msg["From"] = sender
    msg["To"] = rcpt
    msg["Subject"] = subject
    for name, value in headers:
        msg[name] = value
    return msg_id, msg


def make_message(to_email: Union[str, User], from_email: Union[str, User] = None,
                 text="Hello", subject="My Subject", headers=()):
    if isinstance(to_email, User):
        to_email = to_email.email
    if isinstance(from_email, User):
        from_email = from_email.email

    msg = Message(policy=make_policy())
    msg.set_payload(text)
    msg_id = make_msgid()
    msg["Message-Id"] = msg_id
    msg["From"] = from_email
    msg["To"] = to_email
    msg["Subject"] = subject
    for k, v in headers:
        msg[k] = v
    return msg_id, msg


def make_json_part(json: Text):
    json_part = MIMEBase("application", "json", policy=make_policy())
    json_part.set_payload(json)
    return json_part


def make_mime_message(*args, **kwargs):
    msg_id, msg = make_message(*args, **kwargs)
    return msg_id, MIMEMessage(msg)


def make_multipart(parts):
    multipart = MIMEMultipart(policy=make_policy())
    for part in parts:
        multipart.attach(part)
    return multipart


def make_multipart_body(json: Dict, message: MIMEBase):
    json_part = make_json_part(dumps(json))
    return make_multipart([json_part, message])


def get_port(url):
    return int(url.split(":", 1)[-1])


def is_corp(cluster):
    return "corp" in cluster


def read_data(name):
    path = source_path(os.path.join("mail/nwsmtp/tests/lib/data", name))
    with open(path) as fd:
        return fd.read()


def get_cluster_name(backend_name):
    return backend_name if backend_name[-4:] != "-out" else backend_name[:-4]


def has_ssl_transport(client) -> bool:
    return isinstance(client.transport, sslproto._SSLProtocolTransport)


def get_session_id(reply):
    """
    ({}, '2.0.0 Ok: queued on notebook as 1565859027-PHOa4eN6tg-oQc8tkC9')
    """
    bad, good = reply
    return good.rsplit(" ", 1)[-1].split("-", 1)[-1]


def make_raw_multipart(json, msg, boundary):
    if isinstance(json, dict):
        json = dumps(json)
    if isinstance(msg, Message):
        msg = msg.as_string(policy=make_policy())

    # Multipart body syntax is described in https://tools.ietf.org/html/rfc2046#section-5.1.1
    return (
        f"\r\n--{boundary}\r\n"
        f"Content-Type: application/json\r\n\r\n"
        f"{json}"
        f"\r\n--{boundary}\r\n"
        f"Content-Type: message/rfc822\r\n\r\n"
        f"{msg}"
        f"\r\n--{boundary}--\r\n").encode()


def make_raw_message(msg):
    return msg.as_string(policy=make_policy())


def make_message_from_string(message):
    return message_from_string(message)
