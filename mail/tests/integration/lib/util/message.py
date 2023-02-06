from email.policy import Compat32
from email.mime.text import MIMEText
from email.utils import make_msgid
from random import choice
from time import time
from typing import Tuple

from mail.notsolitesrv.tests.integration.lib.util.user import DEFAULT_SENDER, DEFAULT_RCPT_0


def make_stid_prefix(uid, is_shared_stid):
    return "mail:0" if is_shared_stid else "mail:{}".format(uid)


def make_stid(stid_prefix):
    rnd = "".join(str(choice(range(5))) for _ in range(5))
    return f"320.{stid_prefix}.E0000000:0000000000000000000000000{rnd}"


def make_mid():
    rnd = "".join(str(choice(range(5))) for _ in range(5))
    return f"1700000000000{rnd}"


def make_imap_id():
    return "".join(str(choice(range(4))) for _ in range(1, 5))


def make_policy(linesep="\r\n"):
    """
        ..notes:: Use Compat32 to explicitly pass max_line_length to prevent breaking headers
        by `email.header.MAXLINELEN`.
    """
    return Compat32(max_line_length=0, linesep=linesep)


def make_message(
        sender=DEFAULT_SENDER,
        rcpts=[DEFAULT_RCPT_0],
        subject="subj",
        text="txt",
        headers=dict()) -> Tuple[str, MIMEText]:
    msg_id = make_msgid()
    msg = MIMEText(text, "plain", "utf-8", policy=make_policy())
    msg["Message-Id"] = msg_id
    msg["From"] = sender
    msg["To"] = ", ".join(rcpts)
    msg["Subject"] = subject
    for name, value in headers.items():
        msg[name] = value
    return msg_id, msg


def make_http_message_envelope(mimetext_message):
    return {
        "mail_from": {"email": mimetext_message["From"]},
        "remote_host": "0.1.2.3",
        "remote_ip": "0.1.2.3",
        "helo": "0.1.2.3",
        "session_id": "SessionId",
        "envelope_id": "EnvelopeId"
    }


def make_http_message_message(stid):
    return {
        "stid": stid,
        "timemark": round(time() * 1000),
        "front": "Front",
        "spam": False,
        "hints": [{"external_imap_id": [str(make_imap_id())]}]
    }


def make_http_message_notification():
    return {"success": False, "failure": False, "delay": False}


def make_http_message_recipient(user):
    return {
        "email": user.email,
        "uid": str(user.uid),
        "notify": make_http_message_notification(),
        "is_local": "yes",
        "is_mailish": user.is_mailish
    }


def make_http_message(stid, user, mimetext_message):
    return {
        "envelope": make_http_message_envelope(mimetext_message),
        "message": make_http_message_message(stid),
        "recipients": [make_http_message_recipient(user)]
    }
