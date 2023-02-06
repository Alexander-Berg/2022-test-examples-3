from datetime import datetime, timedelta

from travel.rasp.admin.scripts.export.export_nearest_suburban import ToFromCenterJsonWriter

from tester.factories import create_rthread_segment
from tester.testcase import TestCase


class TestToFromCenterJsonWriter(TestCase):
    def test_export_json_with_uid_travel_time(self):
        departure = datetime.now()
        duration = timedelta(hours=11, minutes=42, seconds=10)

        segment = create_rthread_segment(
            departure=departure,
            arrival=departure + duration,
        )

        result = ToFromCenterJsonWriter.export_json_with_uid(segment)
        assert result['travel_time'] == '11:42'
