from hamcrest import assert_that, equal_to
from psycopg2 import DatabaseError

from calendar_attach_processor.calendar_attach_processor import CalendarMailhookProcessor
from calendar_attach_processor.service.blackbox import BlackboxError, User
from calendar_attach_processor.service.calendar import CalendarError, Attach, Envelope
from calendar_attach_processor.service.ml import MlError
from calendar_attach_processor.service.sharpei import SharpeiError
from calendar_attach_processor.service.storage import StorageError
from tests.fixtures.services import config, mailhook, blackbox_by_uid, sharpei, tvm, ml, mail_pg, storage, calendar, \
    blackbox_by_logins, TEST_HEADERS, TEST_CONTENT, ML_USERS, mime_part

HEADERS_WITH_CALENDAR = '''Received: from test ([2a02:6b8:0:2309:d598:3acf:436:eeb1])
	by mxbacktst1o.cmail.yandex.net with LMTP id QQ7zjEpf
	for <pg.search@yandex.ru>; Mon, 11 Sep 2017 17:40:39 +0300
From: ololoev ololo <testix005@yandex.ru>
To: meta.user@ya.ru
Subject: =?UTF-8?Q?=D0=A1=D0=BF=D0=B8=D1=81=D0=BE=D0=BA_=D0=B4?=
 =?UTF-8?Q?=D0=B5=D0=BB_=C2=ABRRRRR?= =?UTF-8?Q?RRRRRRRRRRRRRRRRR?=
 =?UTF-8?Q?RRRRRRRRRRRRRRRRRRRRRRRRRRR=C2=BB_ILYRL?=
MIME-Version: 1.0
Content-Type: multipart/mixed; 
    boundary="----=_Part_46_274847243.1330951606814"
X-Calendar-Request-Id: wHkMq9dQ
Date: Mon, 11 Sep 2017 17:40:39 +0300
Return-Path: pg.search@yandex.ru
X-Yandex-Forward: f923df77820aa0b51da0712f7fbce8b7'''


def test_bb_error(blackbox_by_uid, sharpei, tvm, mailhook):
    blackbox_by_uid.side_effect = BlackboxError()
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert sharpei.call_count == 0


def test_sharpei_error(blackbox_by_uid, sharpei, tvm, mailhook):
    sharpei.side_effect = SharpeiError()
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")


def test_pg_error(blackbox_by_uid, mail_pg, storage, tvm, mailhook):
    mail_pg.side_effect = DatabaseError()
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert storage.call_count == 0


def test_storage_error(blackbox_by_uid, sharpei, tvm, mail_pg, storage, ml, mailhook):
    storage.side_effect = StorageError()
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert ml.call_count == 0


def test_ml_error(blackbox_by_uid, sharpei, tvm, mail_pg, storage, ml, calendar, mailhook):
    ml.side_effect = MlError()
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert calendar.call_count == 0


def test_calendar_error(blackbox_by_uid, sharpei, tvm, mail_pg, storage, ml, blackbox_by_logins, calendar,
                        mailhook):
    calendar.side_effect = CalendarError()
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")


def test_empty_mime_parts(blackbox_by_uid, sharpei, tvm, mail_pg, storage, mailhook):
    mail_pg.return_value = []
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert storage.call_count == 0


def test_mime_part_not_found(blackbox_by_uid, sharpei, tvm, mail_pg, storage, mailhook):
    mail_pg.return_value = [mime_part(hid='1', content_type='multipart', content_subtype='mixed',
                                      boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"',
                                      charset='US-ASCII',
                                      encoding='7bit', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                                      offset_begin='696', offset_end='13017')]
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert storage.call_count == 0


def test_calendar_req_id_in_headers(blackbox_by_uid, sharpei, tvm, mail_pg, storage, calendar, mailhook):
    storage.return_value.headers.return_value = HEADERS_WITH_CALENDAR
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert calendar.call_count == 0


def test_ml_list_empty(blackbox_by_uid, sharpei, tvm, mail_pg, storage, ml, calendar, mailhook):
    ml.return_value = []
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert calendar.call_count == 0


def test_uid_is_ml(blackbox_by_uid, sharpei, tvm, mail_pg, storage, ml, blackbox_by_logins, calendar, mailhook):
    assert mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert calendar.call_count == 1

    calendar_event = calendar.call_args[0][0]
    assert_that(calendar_event.attach, equal_to(Attach("text/calendar", TEST_CONTENT)))
    expected_envelope = Envelope('testix005@yandex.ru', ['some.user@ya.ru', 'hello@yandex.ru'],
                                 'jdskfjkdsjfkdjkfjksjfksjkf')
    assert_that(calendar_event.envelope, equal_to(expected_envelope))
    assert_that(calendar_event.users, equal_to(ML_USERS))


def test_uid_is_ml_on_prod(blackbox_by_uid, sharpei, tvm, mail_pg, storage, ml, blackbox_by_logins, calendar,
                           config):
    assert CalendarMailhookProcessor.from_config(config, 'public', 'production') \
                                    .process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert calendar.call_count == 1

    calendar_event = calendar.call_args[0][0]
    assert_that(calendar_event.attach, equal_to(Attach("text/calendar", TEST_CONTENT)))
    expected_envelope = Envelope('testix005@yandex.ru', ['some.user@ya.ru', 'hello@yandex.ru'],
                                 'jdskfjkdsjfkdjkfjksjfksjkf')
    assert_that(calendar_event.envelope, equal_to(expected_envelope))
    assert_that(calendar_event.users, equal_to([User("4000019104", "abdul", email="meta.user@ya.ru")]))


def test_uid_is_not_ml(blackbox_by_uid, sharpei, tvm, mail_pg, storage, ml, calendar, mailhook):
    test_user = User("4000019104", "abdul", email="meta.user@ya.ru")
    blackbox_by_uid.return_value = test_user
    assert mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert calendar.call_count == 1

    calendar_event = calendar.call_args[0][0]
    assert_that(calendar_event.attach, equal_to(Attach("text/calendar", TEST_CONTENT)))
    expected_envelope = Envelope('testix005@yandex.ru', ['some.user@ya.ru', 'hello@yandex.ru'],
                                 'jdskfjkdsjfkdjkfjksjfksjkf')
    assert_that(calendar_event.envelope, equal_to(expected_envelope))
    assert_that(calendar_event.users, equal_to([test_user]))


def test_wrong_from_headers(blackbox_by_uid, sharpei, tvm, mail_pg, storage, ml, calendar, mailhook):
    import tests
    storage.return_value.headers.return_value = tests.fixtures.services.TEST_HEADERS_WRONG_FROM
    blackbox_by_uid.return_value = User("4000019104", "abdul", email="meta.user@ya.ru")
    assert mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")

    calendar_event = calendar.call_args[0][0]
    expected_envelope = Envelope(None, ['some.user@ya.ru', 'hello@yandex.ru'],
                                 'jdskfjkdsjfkdjkfjksjfksjkf')
    assert_that(calendar_event.envelope, equal_to(expected_envelope))


def test_uid_is_not_pg(blackbox_by_uid, sharpei, tvm, mailhook):
    test_user = User("4000019104", "abdul", email="meta.user@ya.ru", is_pg=False)
    blackbox_by_uid.return_value = test_user
    assert not mailhook.process_calendar_attach("4001", "163255", "1000013", "1.2", "text/calendar")
    assert sharpei.call_count == 0
