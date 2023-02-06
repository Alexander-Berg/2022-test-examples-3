import datetime as dt

from extsearch.video.robot.tools.library.python import utils


class TestTryParseDatetime(object):

    def test_empty(self):
        ts_datetime = utils.try_parse_datetime('')
        assert ts_datetime is None

    def test_iso_string(self):
        ts_datetime = utils.try_parse_datetime('2020-08-14T08:15:30Z')
        assert ts_datetime.replace(tzinfo=dt.timezone.utc).timestamp() == 1597392930

    def test_timestamp(self):
        ts_datetime = utils.try_parse_datetime(1598523028)
        assert ts_datetime.replace(tzinfo=dt.timezone.utc).timestamp() == 1598523028

    def test_timestamp_string(self):
        ts_datetime = utils.try_parse_datetime('1598523028')
        assert ts_datetime.replace(tzinfo=dt.timezone.utc).timestamp() == 1598523028

    def test_broken(self):
        ts_datetime = utils.try_parse_datetime('abc')
        assert ts_datetime is None
