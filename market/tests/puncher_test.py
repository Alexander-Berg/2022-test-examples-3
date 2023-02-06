import datetime


from utils.puncher import handler_login
from utils.puncher import handler_age


class TestPuncher(object):

    @staticmethod
    def test_handler_login():
        assert handler_login('%le087%') == "le087"
        assert handler_login('le087%') == "le087"
        assert handler_login('%le087') == "le087"

    @staticmethod
    def test_handler_age():
        time_now = datetime.datetime.now()
        time_delta = datetime.timedelta(hours=5, minutes=16)
        time_ticket = time_now - time_delta
        time_as_string = time_ticket.strftime('%Y-%m-%dT%H:%M:%S') + '.000+0300'
        assert handler_age(time_as_string) == '02:16'
