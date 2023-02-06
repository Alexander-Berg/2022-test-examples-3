from datetime import datetime

import pytz
import mock

from common.tester.testcase import TestCase
from common.tester.factories import create_station
from route_search.base import PlainSegmentSearch
from route_search.models import RThreadSegment


class TestMaxCount(TestCase):
    def test_max_count(self):
        station_from = create_station()
        station_to = create_station()
        MSK_TZ = pytz.timezone('Europe/Moscow')

        def m_gen_from(plain_segment_search, from_dt_aware):
            for i in range(5):
                segment = RThreadSegment()
                segment.thread = 5 - i
                segment.departure = datetime(2015, 1, 10, hour=12, tzinfo=MSK_TZ)
                yield segment

        with mock.patch.object(PlainSegmentSearch, 'gen_from', m_gen_from):
            segments = PlainSegmentSearch(station_from, station_to).search(datetime(2015, 1, 10, tzinfo=MSK_TZ), datetime(2015, 1, 11, tzinfo=MSK_TZ), max_count=3)

            assert 3 == len(segments)

            assert 5 == segments[0].thread
            assert 4 == segments[1].thread
            assert 3 == segments[2].thread
