from datetime import datetime as dt
from unittest import TestCase
from extsearch.video.robot.youtube_grabber.ugc.lib.db import next_poll


class Test(TestCase):
    def test_next_poll_first(self):
        ts = int(dt.now().timestamp())
        assert next_poll(ts, None, True) is None
        assert next_poll(ts, 0, True) == ts
        assert next_poll(ts, 1, True) == ts

    def test_next_poll_next(self):
        ts = int(dt.now().timestamp())
        assert next_poll(ts, None) is None
        assert next_poll(ts, 0) is None
        assert next_poll(ts, 1) == ts + 1
