import pytest

from time import time

from mail.nwsmtp.tests.lib.util import make_message


@pytest.fixture
def http_client(env):
    return env.nwsmtp.get_http_client()


@pytest.fixture
def message(sender, rcpt):
    msg_id, msg = make_message(sender, rcpt)
    return msg


@pytest.fixture
def store_info(rcpt):
    return {"user_info": {"email": rcpt.email}, "mail_info": {"received_date": int(time()), "labels": {"symbol": ["seen_label"]}}}
