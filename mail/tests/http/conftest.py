import pytest

from lib.nwsmtp.http_client import HTTPClient
from lib.random_generator import get_random_number
from lib.users import get_user


@pytest.fixture
def http_client():
    return HTTPClient()


@pytest.fixture
def sender():
    return get_user("DefaultSender")


@pytest.fixture
def rcpt():
    return get_user("DefaultRcpt")


@pytest.fixture
def mailish_rcpt():
    return get_user("MailishUser")


@pytest.fixture
def external_imap_id():
    return get_random_number(6)


@pytest.fixture
def rfc822_part():
    return "From: from@ya.ru\r\nTo: to@ya.ru\r\n\r\nText Body"
