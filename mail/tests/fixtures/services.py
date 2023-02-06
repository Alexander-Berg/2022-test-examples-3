from collections import namedtuple

import httpretty
import pytest

from calendar_attach_processor.calendar_attach_processor import CalendarMailhookProcessor

from calendar_attach_processor.service.blackbox import User
from calendar_attach_processor.service.calendar import Attach
from calendar_attach_processor.service.ml import Subscriber

TEST_HEADERS = '''Received: from test ([2a02:6b8:0:2309:d598:3acf:436:eeb1])
	by mxbacktst1o.cmail.yandex.net with LMTP id QQ7zjEpf
	for <pg.search@yandex.ru>; Mon, 11 Sep 2017 17:40:39 +0300
From: ololoev ololo <testix005@yandex.ru>
To: meta.user@ya.ru
Cc: some.user@ya.ru, hello <hello@yandex.ru>
Subject: =?UTF-8?Q?=D0=A1=D0=BF=D0=B8=D1=81=D0=BE=D0=BA_=D0=B4?=
 =?UTF-8?Q?=D0=B5=D0=BB_=C2=ABRRRRR?= =?UTF-8?Q?RRRRRRRRRRRRRRRRR?=
 =?UTF-8?Q?RRRRRRRRRRRRRRRRRRRRRRRRRRR=C2=BB_ILYRL?=
MIME-Version: 1.0
Content-Type: multipart/mixed; 
    boundary="----=_Part_46_274847243.1330951606814"
Date: Mon, 11 Sep 2017 17:40:39 +0300
Return-Path: pg.search@yandex.ru
Message-Id: jdskfjkdsjfkdjkfjksjfksjkf
X-Yandex-Forward: f923df77820aa0b51da0712f7fbce8b7'''

TEST_CONTENT = '''BEGIN:CAL'''

TEST_HEADERS_WRONG_FROM = '''From: wrong
Cc: some.user@ya.ru, hello <hello@yandex.ru>
Message-Id: jdskfjkdsjfkdjkfjksjfksjkf'''

mime_part = namedtuple('mime_part', [
    'hid',
    'content_type',
    'content_subtype',
    'boundary',
    'name',
    'charset',
    'encoding',
    'content_disposition',
    'filename',
    'cid',

    'offset_begin',
    'offset_end'
])

MIMES_PARTS = [mime_part(hid='1', content_type='multipart', content_subtype='mixed',
                         boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"', charset='US-ASCII',
                         encoding='7bit', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                         offset_begin='696', offset_end='13017'),
               mime_part(hid='1.2', content_type='multipart', content_subtype='alternative',
                         boundary='----=_Part_47_120288404.1330951606814', name='\\"\\"', charset='US-ASCII',
                         encoding='7bit', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                         offset_begin='832', offset_end='8094')]

ML_USERS = [User("1120000000001387", "maillist", email="login1"),
            User("1120000000017656", "not_maillist", email="login2")]

TVM_TICKETS = {
    'X-Ya-Service-Ticket': 'TVM2 ticket',
    'Ticket': 'TVM1 ticket',
    'TicketClId': 'client_id',
}


@pytest.fixture
def config():
    return {
        'blackbox': {
            'url': "http://pass.test.yandex.ru/blackbox",
            'self_ip': "127.0.0.0"
        },
        'sharpei': "http://sharpei.test.mail.yandex.net/conninfo",
        'user': "user",
        'password': "password",
        'storage': "http://localhost:10010",
        'ml': {
            'url': "http://test.ml.yandex-team.ru/apiv3/lists/subscribers",
            'tvm': {
                'client': '101',
                'secret_env': 'HOME'
            }
        },
        'calendar': "http://calendar.ru",
        'pg': {
            'user': 'lb-test-pg-user',
            'pwd_env': 'HOME'
        }
    }


@pytest.fixture
def mailhook(config):
    return CalendarMailhookProcessor.from_config(config, 'yt', 'production')


@pytest.fixture
def sharpei(mocker):
    sharpei_mock = mocker.patch("calendar_attach_processor.service.sharpei.Sharpei.get_conn_info")
    sharpei_mock.return_value = "host=xdb-test02e.test.yandex.net port=6432 dbname=maildb"
    return sharpei_mock


@pytest.fixture
def blackbox_by_uid(mocker):
    blackbox_mock = mocker.patch("calendar_attach_processor.service.blackbox.BlackboxService.resolve_recipient_by_uid")
    blackbox_mock.return_value = User("4000019104", "abdul", True)
    return blackbox_mock


@pytest.fixture
def blackbox_by_logins(mocker):
    blackbox_mock = mocker.patch("calendar_attach_processor.service.blackbox.BlackboxService.resolve_uids_by_logins")
    blackbox_mock.return_value = ML_USERS
    return blackbox_mock


@pytest.fixture
def mail_pg(mocker):
    mail_pg_mock = mocker.patch("calendar_attach_processor.service.mdb.MailPg.get_mime")
    mail_pg_mock.return_value = MIMES_PARTS
    return mail_pg_mock


@pytest.fixture
def storage(mocker):
    storage_mock = mocker.patch("calendar_attach_processor.service.storage.Storage.stid")
    mulca_mock = mocker.patch("calendar_attach_processor.service.storage.MulcaRequest")
    storage_mock.return_value = mulca_mock
    mulca_mock.headers.return_value = TEST_HEADERS
    mulca_mock.content.return_value = TEST_CONTENT
    mulca_mock.attach.return_value = Attach('text/calendar', TEST_CONTENT)

    return storage_mock


@pytest.fixture
def tvm(mocker):
    return mocker.patch('calendar_attach_processor.tvm.session.TvmSession.tvm_from')


@pytest.fixture
def ticket_headers(mocker):
    ticket_headers_mock = mocker.patch('calendar_attach_processor.tvm.session.TvmSession.get_ticket_headers')
    ticket_headers_mock.return_value = TVM_TICKETS
    return ticket_headers_mock


@pytest.fixture
def http_pretty():
    httpretty.enable()
    yield
    httpretty.disable()


@pytest.fixture
def ml(mocker):
    ml_mock = mocker.patch("calendar_attach_processor.service.ml.Maillist.expand_maillists")
    ml_mock.return_value = [Subscriber("user1@yandex-team.ru", False, "kateogar"),
                            Subscriber("test-maillist@yandex-team.ru", False, "furita-test-29"),
                            Subscriber("user2@yandex-team.ru", False, "stassiak")]
    return ml_mock


@pytest.fixture
def calendar(mocker):
    return mocker.patch("calendar_attach_processor.service.calendar.Calendar.mailhook")
