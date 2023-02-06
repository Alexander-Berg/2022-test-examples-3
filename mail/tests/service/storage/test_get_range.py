# coding=utf-8
import logging
import base64

import httpretty
import pytest
from hamcrest import assert_that, equal_to, instance_of, contains_string

from calendar_attach_processor.service.calendar import Attach
from calendar_attach_processor.service.storage import StorageError, Storage, MulcaRequest, MDSRequest
from tests.fixtures.services import mime_part

ICS_CONTENT_DECODED = u'''CATEGORIES:English Speaking Club
ORGANIZER;CN="Армине Ахвердян":mailto:hjlh@yandex-team.ru
ATTENDEE;CUTYPE=ROOM;PARTSTAT=ACCEPTED;CN="1.Мулен руж":mailto:bmnbmnb@yandex-team.ru
ATTENDEE;PARTSTAT=ACCEPTED;CN="Армине Ахвердян":mailto:hjlh@yandex-team.ru
ATTENDEE;PARTSTAT=NEEDS-ACTION;CN=englishclub:mailto:nmuooo@yandex-team.ru
X-MICROSOFT-CDO-BUSYSTATUS:TENTATIVE
LAST-MODIFIED:20180131T122557Z'''

logging.basicConfig(level=logging.DEBUG)

TEST_STID = "12345"
TEST_REQUEST = "/gate/get/%s" % TEST_STID

ICS_CONTENT = '''CATEGORIES:English Speaking Club
ORGANIZER;CN=3D"=D0=90=D1=80=D0=BC=D0=B8=D0=BD=D0=B5 =D0=90=D1=85=D0=B2=D0=
=B5=D1=80=D0=B4=D1=8F=D0=BD":mailto:hjlh@yandex-team.ru
ATTENDEE;CUTYPE=3DROOM;PARTSTAT=3DACCEPTED;CN=3D"1.=D0=9C=D1=83=D0=BB=D0=B5=
=D0=BD =D1=80=D1=83=D0=B6":mailto:bmnbmnb@yandex-team.ru
ATTENDEE;PARTSTAT=3DACCEPTED;CN=3D"=D0=90=D1=80=D0=BC=D0=B8=D0=BD=D0=B5 =D0=
=90=D1=85=D0=B2=D0=B5=D1=80=D0=B4=D1=8F=D0=BD":mailto:hjlh@yandex-team=
.ru
ATTENDEE;PARTSTAT=3DNEEDS-ACTION;CN=3Denglishclub:mailto:nmuooo@yandex=
-team.ru
X-MICROSOFT-CDO-BUSYSTATUS:TENTATIVE
LAST-MODIFIED:20180131T122557Z'''

CAL_DECODED = '''BEGIN:VCALENDAR
PRODID:-//Yandex LLC//Yandex Calendar//EN
VERSION:2.0
CALSCALE:GREGORIAN
METHOD:REQUEST
X-YANDEX_MAIL_TYPE:event_invitation
BEGIN:VTIMEZONE
TZID:Asia/Novosibirsk
TZURL:http://tzurl.org/zoneinfo-outlook/Asia/Novosibirsk
X-LIC-LOCATION:Asia/Novosibirsk
BEGIN:STANDARD
TZOFFSETFROM:+0700
TZOFFSETTO:+0700
TZNAME:+07
DTSTART:19700101T000000
END:STANDARD
END:VTIMEZONE
BEGIN:VEVENT
DTSTART;TZID=Asia/Novosibirsk:20180215T120000
DTEND;TZID=Asia/Novosibirsk:20180215T140000
SUMMARY:Встреча офиса с Наташей
UID:YkVzLzXnyandex.ru
SEQUENCE:0
DTSTAMP:20180205T132612Z
CREATED:20180123T133543Z
LOCATION:Обсерватория
DESCRIPTION:Коллеги\, привет!\n\nВстреча с'''

BAD_BASE64 = '''QkVHSU46VkNBTEVOREFSDQpQUk9ESUQ6LS8vWWFuZGV4IExMQy8vWWFuZGV4IENhbGVuZGFyLy9F
Tg0KVkVSU0lPTjoyLjANCkNBTFNDQUxFOkdSRUdPUklBTg0KTUVUSE9EOlJFUVVFU1QNClgtWUFO
REVYX01BSUxfVFlQRTpldmVudF9pbnZpdGF0aW9uDQpCRUdJTjpWVElNRVpPTkUNClRaSUQ6QXNp
YS9Ob3Zvc2liaXJzaw0KVFpVUkw6aHR0cDovL3R6dXJsLm9yZy96b25laW5mby1vdXRsb29rL0Fz
aWEvTm92b3NpYmlyc2sNClgtTElDLUxPQ0FUSU9OOkFzaWEvTm92b3NpYmlyc2sNCkJFR0lOOlNU
QU5EQVJEDQpUWk9GRlNFVEZST006KzA3MDANClRaT0ZGU0VUVE86KzA3MDANClRaTkFNRTorMDcN
CkRUU1RBUlQ6MTk3MDAxMDFUMDAwMDAwDQpFTkQ6U1RBTkRBUkQNCkVORDpWVElNRVpPTkUNCkJF
R0lOOlZFVkVOVA0KRFRTVEFSVDtUWklEPUFzaWEvTm92b3NpYmlyc2s6MjAxODAyMTVUMTIwMDAw
DQpEVEVORDtUWklEPUFzaWEvTm92b3NpYmlyc2s6MjAxODAyMTVUMTQwMDAwDQpTVU1NQVJZOtCS
0YHRgtGA0LXRh9CwINC+0YTQuNGB0LAg0YEg0J3QsNGC0LDRiNC10LkNClVJRDpZa1Z6THpYbnlh
bmRleC5ydQ0KU0VRVUVOQ0U6MA0KRFRTVEFNUDoyMDE4MDIwNVQxMzI2MTJaDQpDUkVBVEVEOjIw
MTgwMTIzVDEzMzU0M1oNCkxPQ0FUSU9OOtCe0LHRgdC10YDQstCw0YLQvtGA0LjRjw0KREVTQ1JJ
UFRJT0460JrQvtC70LvQtdCz0LhcLCDQv9GA0LjQstC10YIhXG5cbtCS0YHRgtGA0LXRh9CwINGB
INCd0LDRgtCw0YjQtdC5INC/0YDQvtC50LTQtdGCIDE1INGE0LXQstGA0LDQu9GPINGBIDEyOjAw
LTE0OjAwLiDQktGL0YjQu9C40YLQtVwsINC/0L7QttCw0LvRg9C50YHRgtCwXCwg0L/RgNC40LPQ
u9Cw0YjQtdC90LjQtSDQvtGB0YLQsNC70YzQvdGL0Lwg0YPRh9Cw0YHRgtC90LjQutCw0Lwg0LLR
gdGC0YDQtdGH0LguIA0KVVJMOmh0dHBzOi8vY2FsZW5kYXIueWFuZGV4LXRlYW0ucnUvZXZlbnQ/
ZXZlbnRfaWQ9MzI0MTI2OTANCkNBVEVHT1JJRVM60JjQu9GM0Y8g0KjRgtCw0L3RjA0KT1JHQU5J
WkVSO0NOPSLQktC40LrRgtC+0YDQuNGPINCS0L7RgNC+0LHRjNC10LLQsCI6bWFpbHRvOnlhemhl
dmlrYUB5YW5kZXgtdGVhbS5ydQ0KQVRURU5ERUU7Q1VUWVBFPVJPT007UEFSVFNUQVQ9QUNDRVBU
RUQ7Q049ItCe0LHRgdC10YDQstCw0YLQvtGA0LjRjyI6bWFpbHRvOmNvbmZfbnNrX29ic2VydmF0
b3J5QHlhbmRleC10ZWFtLnJ1DQpBVFRFTkRFRTtQQVJUU1RBVD1BQ0NFUFRFRDtDTj0i0JLQuNC6
0YLQvtGA0LjRjyDQktC+0YDQvtCx0YzQtdCy0LAiOm1haWx0bzp5YXpoZXZpa2FAeWFuZGV4LXRl
YW0ucnUNCkFUVEVOREVFO1BBUlRTVEFUPU5FRURTLUFDVElPTjtDTj0i0JjQu9GM0Y8g0KjRgtCw
0L3RjCI6bWFpbHRvOnNodGFuMTVAeWFuZGV4LXRlYW0ucnUNCkFUVEVOREVFO1BBUlRTVEFUPU5F
RURTLUFDVElPTjtDTj0i0J3QsNGC0LDQu9GM0Y8g0JrQvtCy0LDQu9C10LLQsCI6bWFpbHRvOm5h
dGFsaUB5YW5kZXgtdGVhbS5ydQ0KQVRURU5ERUU7UEFSVFNUQVQ9TkVFRFMtQUNUSU9OO0NOPSLQ
lNC80LjRgtGA0LjQuSDQmtC+0LfQu9C40LrQuNC9IjptYWlsdG86a296bGlraW5AeWFuZGV4LXRl
YW0ucnUNCkFUVEVOREVFO1BBUlRTVEFUPU5FRURTLUFDVElPTjtDTj0i0JzQsNGA0LPQsNGA0LjR
gtCwINCh0LrQvtGA0LjQvdC+0LLQsCAo0KfRg9GA0LrQuNC90LApIjptYWlsdG86bWVjaEB5YW5k
ZXgtdGVhbS5ydQ0KQVRURU5ERUU7UEFSVFNUQVQ9TkVFRFMtQUNUSU9OO0NOPSLQmNGA0LjQvdCw
INCa0LDQt9Cw0LoiOm1haWx0bzpzaGlueUB5YW5kZXg'''

MIME_PART_1 = mime_part(hid='1', content_type='multipart', content_subtype='mixed',
                        boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"',
                        charset='US-ASCII',
                        encoding='7bit', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                        offset_begin='696', offset_end='13017')

MIME_PART_1_1 = mime_part(hid='1.1', content_type='text', content_subtype='calendar',
                          boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"',
                          charset='utf-8',
                          encoding='quoted-printable', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                          offset_begin='696', offset_end='13017')

MIME_PART_1_2 = mime_part(hid='1.2', content_type='text', content_subtype='calendar',
                          boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"',
                          charset='utf-8',
                          encoding='base64', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                          offset_begin='696', offset_end='13017')

MIME_PART_1_3 = mime_part(hid='1.3', content_type='text', content_subtype='calendar',
                          boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"',
                          charset='utf-8',
                          encoding='7BIT', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                          offset_begin='696', offset_end='13017')

MIME_PART_1_4 = mime_part(hid='1.4', content_type='text', content_subtype='calendar',
                          boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"',
                          charset='utf-8',
                          encoding='8bit', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                          offset_begin='696', offset_end='13017')

MIME_PART_1_5 = mime_part(hid='1.5', content_type='text', content_subtype='calendar',
                          boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"',
                          charset='utf-8',
                          encoding='gzip-unexpected', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                          offset_begin='696', offset_end='13017')

MIME_PART_1_6 = mime_part(hid='1.6', content_type='text', content_subtype='calendar',
                          boundary='----=_Part_46_274847243.1330951606814', name='\\"\\"',
                          charset='utf-8',
                          encoding='binary', content_disposition='\\"\\"', filename='\\"\\"', cid='\\"\\"',
                          offset_begin='696', offset_end='13017')


def test_create_mds_request():
    assert_that(Storage('').stid('', []), instance_of(MDSRequest))


@httpretty.activate
def test_failed_resp_mulca():
    url = "http://test-storage-request-failed-mulca.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, status=500)
    with pytest.raises(StorageError):
        Storage(url).stid(TEST_STID, [MIME_PART_1]).headers()


@httpretty.activate
def test_failed_resp_mds():
    url = "http://test-storage-request-failed-mds.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, status=500)
    with pytest.raises(StorageError):
        Storage(url).stid(TEST_STID, [MIME_PART_1]).headers()


@httpretty.activate
def test_mds_wrong_status_headers_resp():
    url = "http://request-failed-headers.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, status=200)
    with pytest.raises(StorageError):
        Storage(url).stid(TEST_STID, [MIME_PART_1]).headers()


@httpretty.activate
def test_mds_wrong_status_content_resp():
    url = "http://test-storage-request-failed-content-200.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, status=200)
    with pytest.raises(StorageError):
        Storage(url).stid(TEST_STID, [MIME_PART_1]).content('1')


@httpretty.activate
def test_ok_resp():
    url = "http://test-storage-request-ok.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, body=ICS_CONTENT.encode('utf-8'), status=206)

    assert_that(Storage(url).stid(TEST_STID, [MIME_PART_1]).content('1'), equal_to(ICS_CONTENT))


# {'name': MDSRequest}

@pytest.mark.parametrize("hid,content,attach_expected", [
    ('1.1', ICS_CONTENT, Attach('text/calendar', ICS_CONTENT_DECODED)),
    ('1.2', base64.encodestring(CAL_DECODED), Attach('text/calendar', CAL_DECODED)),
    ('1.3', ICS_CONTENT, Attach('text/calendar', ICS_CONTENT)),
    ('1.4', ICS_CONTENT, Attach('text/calendar', ICS_CONTENT)),
    ('1.6', ICS_CONTENT, Attach('text/calendar', ICS_CONTENT)),
])
@pytest.mark.parametrize("req", [{'name': MDSRequest}, {'name': MulcaRequest}])
def test_ok_attach_resp(hid, content, attach_expected, req):
    httpretty.enable()
    url = "http://test-storage-request-ok.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, body=content, status=206)

    assert_that(req['name'](url + TEST_REQUEST, [MIME_PART_1,
                                                 MIME_PART_1_1,
                                                 MIME_PART_1_2,
                                                 MIME_PART_1_3,
                                                 MIME_PART_1_4,
                                                 MIME_PART_1_6
                                                 ]).attach(hid),
                equal_to(attach_expected))
    httpretty.disable()
    httpretty.reset()


@httpretty.activate
def test_bad_encoding_for_attach_mds():
    url = "http://test-attach-encoding-bad.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, body=ICS_CONTENT, status=206)

    with pytest.raises(StorageError, match=r'encoding'):
        MDSRequest(url + TEST_REQUEST, [MIME_PART_1_5]).attach('1.5')


@httpretty.activate
def test_decode_fail_for_attach_mds():
    url = "http://test-attach-encoding-bad.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, body=BAD_BASE64, status=206)

    with pytest.raises(StorageError, match=r'decode'):
        MDSRequest(url + TEST_REQUEST, [MIME_PART_1_2]).attach('1.2')


@httpretty.activate
def test_decode_fail_for_attach_mulca():
    url = "http://test-attach-encoding-bad.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, body=BAD_BASE64, status=206)

    with pytest.raises(StorageError, match=r'decode'):
        MulcaRequest(url + TEST_REQUEST, [MIME_PART_1_2]).attach('1.2')


@httpretty.activate
def test_bad_encoding_for_attach_mulca():
    url = "http://test-attach-encoding-bad.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, body=ICS_CONTENT, status=206)

    with pytest.raises(StorageError, match=r'encoding'):
        MulcaRequest(url + TEST_REQUEST, [MIME_PART_1_5]).attach('1.5')


@httpretty.activate
def test_mds_ok_resp_headers():
    url = "http://test-storage-request-ok.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, body=ICS_CONTENT.encode('utf-8'), status=206)

    assert_that(Storage(url).stid(TEST_STID, [MIME_PART_1]).headers(), equal_to(ICS_CONTENT))


@httpretty.activate
def test_not_found_mime():
    with pytest.raises(StorageError, match=r'mime part'):
        Storage('').stid(TEST_STID, [MIME_PART_1]).content('1.2')


@httpretty.activate
def test_ok_gettype_resp():
    url = "http://test-storage-gettype-request-ok.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, body=ICS_CONTENT.encode('utf-8'), status=206)

    assert_that(Storage(url).stid(TEST_STID, [MIME_PART_1]).content('1'), equal_to(ICS_CONTENT))

    assert_that(httpretty.last_request().querystring, equal_to({
        'service': ['calendar-mailhook']
    }))


@httpretty.activate
def test_stid_not_found_resp_headers():
    url = "http://test-storage-stid-not-found.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, status=404)
    with pytest.raises(StorageError):
        Storage(url).stid(TEST_STID, [MIME_PART_1]).headers()


@httpretty.activate
def test_stid_not_found_resp_content():
    url = "http://test-storage-stid-not-found.com"
    httpretty.register_uri(httpretty.GET, url + TEST_REQUEST, status=404)
    with pytest.raises(StorageError):
        Storage(url).stid(TEST_STID, [MIME_PART_1]).content('1')
