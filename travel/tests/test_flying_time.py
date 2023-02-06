# coding=utf-8
import logging
import pytz
from datetime import date, datetime
from freezegun import freeze_time

from travel.avia.shared_flights.tasks.flying_time.flying_time import FlyingTimeTool, DATE_FORMAT, TEXT_OUTPUT_MODE


logger = logging.getLogger(__name__)


class TestFlyingTime:
    def test_parse_time(self):
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        result = flying_time_tool.parse_time(
            datetime.strptime('2011-11-07', DATE_FORMAT),
            0,  # not an overnight arrival
            1234,  # 12:34 local time
            pytz.UTC,
        )
        assert result == datetime(2011, 11, 7, 12, 34, 0, 0, pytz.UTC)

        result = flying_time_tool.parse_time(
            datetime.strptime('2011-11-07', DATE_FORMAT),
            1,  # overnight arrival
            1,  # 00:01 local time
            pytz.UTC,
        )
        assert result == datetime(2011, 11, 8, 0, 1, 0, 0, pytz.UTC)

    def test_calculate_flying_time(self):
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        # Same time zone
        result = flying_time_tool.calculate_flying_time(
            datetime.strptime('2011-11-07', DATE_FORMAT),
            0,
            1259,
            1400,
            pytz.UTC,
            pytz.UTC,
        )
        assert result == 61

        # Different time zones
        result = flying_time_tool.calculate_flying_time(
            datetime.strptime('2011-11-07', DATE_FORMAT),
            0,
            1259,
            1400,
            pytz.timezone('Asia/Yekaterinburg'),
            pytz.timezone('Europe/Moscow'),
        )
        assert result == 61 + 120

    def test_valid_time(self):
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        assert flying_time_tool.valid_time(0)
        assert flying_time_tool.valid_time(1130)
        assert flying_time_tool.valid_time(2359)
        assert not flying_time_tool.valid_time(1160)
        assert not flying_time_tool.valid_time(199)
        assert not flying_time_tool.valid_time(2400)

    @freeze_time(datetime(2021, 11, 1))
    def test_get_max_flight_date(self):
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        assert date(2022, 9, 27) == flying_time_tool.get_max_flight_date(date(2049, 11, 1))
        assert date(2021, 12, 1) == flying_time_tool.get_max_flight_date(date(2021, 12, 1))

    @freeze_time(datetime(2021, 11, 1))
    def test_get_min_flight_date(self):
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        assert date(2049, 11, 1) == flying_time_tool.get_min_flight_date(date(2049, 11, 1))
        assert date(2021, 12, 1) == flying_time_tool.get_min_flight_date(date(2021, 12, 1))
        assert date(2021, 9, 30) == flying_time_tool.get_min_flight_date(date(2001, 12, 1))

    def test_update_flying_time(self):
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        flight_date = datetime(2021, 11, 1)
        flying_time_tool.update_flying_time(1, 1, flight_date, 100)
        assert flying_time_tool._flying_time_per_day.get((1, 1, '2021-11-01')) == 100
        assert flying_time_tool._flying_time_min.get((1, 1)) == 100

        flight_date = datetime(2021, 11, 2)
        flying_time_tool.update_flying_time(1, 1, flight_date, 90)
        assert flying_time_tool._flying_time_per_day.get((1, 1, '2021-11-01')) == 100
        assert flying_time_tool._flying_time_per_day.get((1, 1, '2021-11-02')) == 90
        assert flying_time_tool._flying_time_min.get((1, 1)) == 90

        flight_date = datetime(2021, 11, 3)
        flying_time_tool.update_flying_time(1, 1, flight_date, 110)
        assert flying_time_tool._flying_time_per_day.get((1, 1, '2021-11-01')) == 100
        assert flying_time_tool._flying_time_per_day.get((1, 1, '2021-11-02')) == 90
        assert flying_time_tool._flying_time_per_day.get((1, 1, '2021-11-03')) == 110
        assert flying_time_tool._flying_time_min.get((1, 1)) == 90

    @freeze_time(datetime(2021, 11, 1))
    def test_process_flight_record(self):
        # valid record
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        flying_time_tool.process_flight_record(
            date(2021, 11, 1),
            date(2021, 11, 7),
            67,  # flying only on Saturdays and Sundays
            0,
            213,  # departure settlement
            54,  # arrival settlement
            1200,
            1700,
            'Europe/Moscow',
            'Asia/Yekaterinburg',
        )
        assert flying_time_tool._skipped_invalid_records == 0
        assert flying_time_tool._flying_time_min == {(213, 54): 180}
        assert flying_time_tool._flying_time_per_day == {(213, 54, '2021-11-06'): 180, (213, 54, '2021-11-07'): 180}

        # invalid record: invalid departure time
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        flying_time_tool.process_flight_record(
            date(2021, 11, 1),
            date(2021, 11, 7),
            67,  # flying only on Saturdays and Sundays
            0,
            213,  # departure settlement
            54,  # arrival settlement
            9999,
            1700,
            'Europe/Moscow',
            'Asia/Yekaterinburg',
        )
        assert flying_time_tool._skipped_invalid_records == 1
        assert flying_time_tool._flying_time_min == {}
        assert flying_time_tool._flying_time_per_day == {}

        # invalid record: invalid arrival time
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        flying_time_tool.process_flight_record(
            date(2021, 11, 1),
            date(2021, 11, 7),
            67,  # flying only on Saturdays and Sundays
            0,
            213,  # departure settlement
            54,  # arrival settlement
            1200,
            9999,
            'Europe/Moscow',
            'Asia/Yekaterinburg',
        )
        assert flying_time_tool._skipped_invalid_records == 1
        assert flying_time_tool._flying_time_min == {}
        assert flying_time_tool._flying_time_per_day == {}

        # invalid record: no operating_from
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        flying_time_tool.process_flight_record(
            None,
            date(2021, 11, 7),
            67,  # flying only on Saturdays and Sundays
            0,
            213,  # departure settlement
            54,  # arrival settlement
            1200,
            1700,
            'Europe/Moscow',
            'Asia/Yekaterinburg',
        )
        assert flying_time_tool._skipped_invalid_records == 1
        assert flying_time_tool._flying_time_min == {}
        assert flying_time_tool._flying_time_per_day == {}

        # invalid record: no operating_until
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        flying_time_tool.process_flight_record(
            date(2021, 11, 1),
            None,
            67,  # flying only on Saturdays and Sundays
            0,
            213,  # departure settlement
            54,  # arrival settlement
            1200,
            1700,
            'Europe/Moscow',
            'Asia/Yekaterinburg',
        )
        assert flying_time_tool._skipped_invalid_records == 1
        assert flying_time_tool._flying_time_min == {}
        assert flying_time_tool._flying_time_per_day == {}

        # invalid record: no departure tz
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        flying_time_tool.process_flight_record(
            date(2021, 11, 1),
            date(2021, 11, 7),
            67,  # flying only on Saturdays and Sundays
            0,
            213,  # departure settlement
            54,  # arrival settlement
            1200,
            1700,
            '',
            'Asia/Yekaterinburg',
        )
        assert flying_time_tool._skipped_invalid_records == 1
        assert flying_time_tool._flying_time_min == {}
        assert flying_time_tool._flying_time_per_day == {}

        # invalid record: no arrival tz
        flying_time_tool = FlyingTimeTool('testing', TEXT_OUTPUT_MODE, None, 0)
        flying_time_tool.process_flight_record(
            date(2021, 11, 1),
            date(2021, 11, 7),
            67,  # flying only on Saturdays and Sundays
            0,
            213,  # departure settlement
            54,  # arrival settlement
            1200,
            1700,
            'Europe/Moscow',
            '',
        )
        assert flying_time_tool._skipped_invalid_records == 1
        assert flying_time_tool._flying_time_min == {}
        assert flying_time_tool._flying_time_per_day == {}
