import json
import pytest

from ora2pg.reminders import make_reminders, get_callback_url_mid


def test():
    data = json.loads('''
    {
        "invocation-info": {
            "hostname": "reminders-test-back.cmail.yandex.net",
            "action": "GET_ReminderActions.findReminder/v1/{uid}/reminders/{cid}/",
            "app-name": "api",
            "app-version": "15.3.3.fdc3235",
            "req-id": "JswkIfe7",
            "exec-duration-millis": "2"
        },
        "result": {
            "reminders": [
                {
                    "id": "delayed_message_2160000000013431519",
                    "clientId": "yandex-wmi",
                    "name": "send_delayed_message",
                    "reminderDate": "2016-01-01T00:00:00+03:00",
                    "channels": {
                        "callback": {
                            "url": "http://web-tst1j.yandex.ru:9090/send_delayed_message?mdb=mdb302&mid=2160000000013431519&suid=3002406291&uid=4001671419"
                        }
                    }
                }
            ]
        }
    }
    ''')
    result = list(make_reminders(data))
    assert len(result) == 1
    assert result[0].id == 'delayed_message_2160000000013431519'
    assert result[0].client_id == 'yandex-wmi'
    assert result[0].name == 'send_delayed_message'
    assert result[0].date == '2016-01-01T00:00:00+03:00'
    assert result[0].callback_url == 'http://web-tst1j.yandex.ru:9090/send_delayed_message?mdb=mdb302&mid=2160000000013431519&suid=3002406291&uid=4001671419'


class TestGetCallbackUrlMid(object):
    DELAYED_URL = 'http://meta.mail.yandex.net:9090/send_delayed_message?mdb=mdb302&mid=2160000000013431519&suid=3002406291&uid=4001671419'
    NON_NUMBER_MIDS = 'http://meta.mail.yandex.net:9090/send_delayed_message?mdb=mdb302&mid=FOO&suid=3002406291&uid=4001671419'
    EMPTY_MID = 'http://meta.mail.yandex.net:9090/send_delayed_message?mdb=mdb302&mid=&suid=3002406291&uid=4001671419'
    NO_ANSWER_URL = 'http://meta.mail.yandex.net:9090/no_answer_remind?date=02.02.2016&lang=ru&message_id=%3C3304501454391055%40web5h.yandex.ru%3E&uid=55322177'

    def test_callback_url_with_mid(self):
        assert get_callback_url_mid(self.DELAYED_URL) == 2160000000013431519

    def test_callback_url_without_mid(self):
        assert get_callback_url_mid(self.NO_ANSWER_URL) is None

    @pytest.mark.parametrize('bad_url', [
        NON_NUMBER_MIDS,
        EMPTY_MID
    ])
    def test_callback_url_with_bronken_mid(self, bad_url):
        assert get_callback_url_mid(bad_url) is None
