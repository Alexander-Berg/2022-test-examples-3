import logging

import httpretty
import pytest
from hamcrest import assert_that, equal_to, has_entries, has_length

from calendar_attach_processor.service.blackbox import User
from calendar_attach_processor.service.calendar import Calendar, CalendarEvent, Envelope, Attach, CalendarError

logging.basicConfig(level=logging.DEBUG)

ICS_CONTENT = '''CATEGORIES:English Speaking Club
ORGANIZER;CN=3D"=D0=90=D1=80=D0=BC=D0=B8=D0=BD=D0=B5 =D0=90=D1=85=D0=B2=D0=
=B5=D1=80=D0=B4=D1=8F=D0=BD":mailto:llll@yandex-team.ru
ATTENDEE;CUTYPE=3DROOM;PARTSTAT=3DACCEPTED;CN=3D"1.=D0=9C=D1=83=D0=BB=D0=B5=
=D0=BD =D1=80=D1=83=D0=B6":mailto:lllll@yandex-team.ru
ATTENDEE;PARTSTAT=3DACCEPTED;CN=3D"=D0=90=D1=80=D0=BC=D0=B8=D0=BD=D0=B5 =D0=
=90=D1=85=D0=B2=D0=B5=D1=80=D0=B4=D1=8F=D0=BD":mailto:lllll@yandex-team=
.ru
ATTENDEE;PARTSTAT=3DNEEDS-ACTION;CN=3Denglishclub:mailto:lllll@yandex=
-team.ru
X-MICROSOFT-CDO-BUSYSTATUS:TENTATIVE
LAST-MODIFIED:20180131T122557Z'''

TEST_EVENT = CalendarEvent(users=[User("12345", login="test1", email="test1@yandex.ru"),
                                  User("123456", login="test12", email="test12@yandex.ru"),
                                  User("123457", login="test13", email="test13@yandex.ru")],
                           envelope=Envelope(from_email="some@email",
                                             cc=["cc1@email", "cc2@email"],
                                             message_id="1234567890"),
                           attach=Attach('text/calendar', ICS_CONTENT.encode('utf-8')))


@httpretty.activate
def test_calendar_request_failed():
    httpretty.register_uri(httpretty.POST, "http://calendar-request-failed/mailhook",
                           responses=[httpretty.Response(body="error", status=502)])
    with pytest.raises(CalendarError):
        Calendar("http://calendar-request-failed/mailhook").mailhook(TEST_EVENT)


@httpretty.activate
def test_calendar_request():
    calendar_url = "http://calendar-request-succ/mailhook"
    httpretty.register_uri(httpretty.POST, calendar_url, status=200)
    calendar = Calendar(calendar_url)
    calendar.mailhook(TEST_EVENT)

    assert_that(httpretty.httpretty.latest_requests, has_length(len(TEST_EVENT.users)))
    for user in TEST_EVENT.users:
        req = next(r for r in httpretty.httpretty.latest_requests if r.querystring['to'] == [user.email])
        assert_that(req.querystring, has_entries({
            "from": ['some@email'],
            "to": [user.email],
            "cc": ["cc1@email,cc2@email"],
            "message-id": ['1234567890'],
            "content-type": ['text/calendar']
        }))
        assert_that(req.headers['Content-Type'], equal_to("text/calendar"))
        assert_that(req.body, equal_to(ICS_CONTENT))
        assert_that(req.headers['Content-Length'], equal_to(str(len(ICS_CONTENT.encode('utf-8')))))
