# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from builtins import map
from builtins import range
from datetime import time, date, datetime, timedelta

from unittest import TestCase
import pytz
from freezegun import freeze_time


from travel.rasp.library.python.common23.tester.utils.datetime import replace_now

from travel.rasp.library.python.common23.date import environment
from travel.rasp.library.python.common23.date.run_mask import RunMask

from travel.rasp.library.python.common23.date.tz_mask_split import (
    calculate_mask_for_tz_and_station, StationForMaskSplit, ThreadForMaskSplit,
    mask_split, thread_mask_split, MaskSplitter
)


def build_time(s):
    return time(*[int(x, 10) for x in s.split(':')])


def build_date(s):
    return date(*[int(x, 10) for x in s.split('-')])


def build_datetime(s):
    return datetime.strptime(s, '%Y-%m-%d %H:%M:%S')


def shifted(dates, shift):
    return [d + timedelta(shift) for d in dates]


class TestMaskSplit(TestCase):
    @freeze_time("2012-02-01 00:00:00")
    def testMaskSplit_1(self):
        """
        2012г.

        Europe/Kiev +3/+2
        utc_transition
        +2
        2012-03-25 01:00 utc
        +3
        2012-10-28 01:00 utc
        +2

        Europe/Moscow +4 no transitions
        """

        today = environment.now()

        msk_start_time = build_time("10:00")
        msk_mask = RunMask(
            today=today,
            days=list(map(build_date, [
                "2012-03-24",
                "2012-03-25",
                "2012-03-26",

                "2012-10-27",
                "2012-10-28",
                "2012-10-29",
            ]))
        )

        out_winter = list(map(build_date, [
            "2012-03-24",

            "2012-10-28",
            "2012-10-29",
        ]))
        out_winter_kiv_time = build_time("08:00")

        out_summer = list(map(build_date, [
            "2012-03-25",
            "2012-03-26",

            "2012-10-27",
        ]))

        out_summer_kiv_time = build_time("09:00")

        split_results = mask_split(msk_mask, msk_start_time, 'Europe/Moscow', 'Europe/Kiev')
        split_results.sort(key=lambda sr: next(sr.result_mask.iter_dates()))

        self.assertEqual(len(split_results), 2)
        self.assertListEqual(split_results[0].result_mask.dates(), out_winter)
        self.assertListEqual(split_results[1].result_mask.dates(), out_summer)

        self.assertEqual(split_results[0].out_time, out_winter_kiv_time)
        self.assertEqual(split_results[1].out_time, out_summer_kiv_time)

    @freeze_time("2012-02-01 00:00:00")
    def testMaskSplit_2(self):
        """
        2012г.

        Europe/Kiev +3/+2
        utc_transition
        +2
        2012-03-25 01:00 utc
        +3
        2012-10-28 01:00 utc
        +2

        Australia/South +9:30/+10:30
        utc_transition
        +10:30
        2012-03-31 16:30
        +9:30
        2012-10-06 16:30
        +10:30
        """

        today = environment.now()

        kiv_start_time = build_time("17:00")
        kiv_mask = RunMask(
            today=today,
            days=list(map(build_date, [
                "2012-03-24",  # +2, +10:30, au_t = 01:30, shift 1
                "2012-03-25",  # +3, +10:30, au_t = 00:30, shift 1
                "2012-03-26",  # +3, +10:30, au_t = 00:30, shift 1

                "2012-03-30",  # +3, +10:30, au_t = 00:30, shift 1
                "2012-03-31",  # +3, +10:30, au_t = 00:30, shift 1
                "2012-04-01",  # +3, +09:30, au_t = 23:30, shift 0

                "2012-10-05",  # +3, +09:30, au_t = 23:30, shift 0
                "2012-10-06",  # +3, +09:30, au_t = 23:30, shift 0
                "2012-10-07",  # +3, +10:30, au_t = 00:30, shift 1

                "2012-10-27",  # +3, +10:30, au_t = 00:30, shift 1
                "2012-10-28",  # +2, +10:30, au_t = 01:30, shift 1
                "2012-10-29",  # +2, +10:30, au_t = 01:30, shift 1
            ]))
        )

        au_01_30_shift_1 = list(map(build_date, [
            "2012-03-24",
            "2012-10-28",
            "2012-10-29",
        ]))

        au_00_30_shift_1 = list(map(build_date, [
            "2012-03-25",
            "2012-03-26",
            "2012-03-30",
            "2012-03-31",
            "2012-10-07",
            "2012-10-27",
        ]))

        au_23_30_shift_0 = list(map(build_date, [
            "2012-04-01",
            "2012-10-05",
            "2012-10-06",
        ]))

        split_results = mask_split(kiv_mask, kiv_start_time, 'Europe/Kiev', 'Australia/South')
        split_results.sort(key=lambda sr: next(sr.result_mask.iter_dates()))

        self.assertEqual(len(split_results), 3)

        au_01_30_splt_res = split_results[0]
        au_00_30_splt_res = split_results[1]
        au_23_30_splt_res = split_results[2]

        self.assertListEqual(au_01_30_splt_res.event_mask.dates(), au_01_30_shift_1)
        self.assertEqual(au_01_30_splt_res.out_time, build_time("01:30"))
        self.assertEqual(au_01_30_splt_res.shift, 1)
        self.assertListEqual(au_01_30_splt_res.result_mask.dates(), shifted(au_01_30_shift_1, 1))

        self.assertListEqual(au_00_30_splt_res.event_mask.dates(), au_00_30_shift_1)
        self.assertEqual(au_00_30_splt_res.out_time, build_time("00:30"))
        self.assertEqual(au_00_30_splt_res.shift, 1)
        self.assertListEqual(au_00_30_splt_res.result_mask.dates(), shifted(au_00_30_shift_1, 1))

        self.assertListEqual(au_23_30_splt_res.event_mask.dates(), au_23_30_shift_0)
        self.assertEqual(au_23_30_splt_res.out_time, build_time("23:30"))
        self.assertEqual(au_23_30_splt_res.shift, 0)
        self.assertListEqual(au_23_30_splt_res.result_mask.dates(), shifted(au_23_30_shift_0, 0))


@replace_now('2012-02-01')
def test_thread_mask_split():
    """
    2012г.

    Europe/Kiev +3/+2
    utc_transition
    +2
    2012-03-25 01:00 utc
    +3
    2012-10-28 01:00 utc
    +2

    Europe/Moscow +4 no transitions
    """

    today = environment.today()
    kiev_pytz = pytz.timezone('Europe/Kiev')
    moscow_pytz = pytz.timezone('Europe/Moscow')
    mask_splitter = MaskSplitter()

    stations = [
        StationForMaskSplit(moscow_pytz, None, 0,  time(0), 0),
        StationForMaskSplit(kiev_pytz, time(0), 1, time(10), 1),
        StationForMaskSplit(kiev_pytz, time(0), 2, time(10), 2),
        StationForMaskSplit(moscow_pytz, time(0), 3, time(10), 3),
        StationForMaskSplit(moscow_pytz, time(0), 4, None, 0),
    ]

    def split_and_check_mask(init_mask_days, result_masks_days):
        mask = RunMask(today=today, days=init_mask_days)
        thread_for_split = ThreadForMaskSplit(mask, stations)
        masks = thread_mask_split(thread_for_split, mask_splitter, moscow_pytz)

        assert len(masks) == len(result_masks_days)
        for index in range(0, len(masks)):
            assert masks[index].dates() == result_masks_days[index]

    split_and_check_mask(
        [date(2012, 4, 1)],
        [[date(2012, 4, 1)]]
    )

    split_and_check_mask(
        [date(2012, 2, 1), date(2012, 2, 3)],
        [[date(2012, 2, 1), date(2012, 2, 3)]]
    )

    split_and_check_mask(
        [
            date(2012, 3, 21), date(2012, 3, 22), date(2012, 3, 23),
            date(2012, 3, 24), date(2012, 3, 25), date(2012, 3, 26)
        ],
        [
            [date(2012, 3, 21), date(2012, 3, 22)],
            [date(2012, 3, 23)],
            [date(2012, 3, 24)],
            [date(2012, 3, 25), date(2012, 3, 26)]
        ]
    )

    split_and_check_mask(
        [
            date(2012, 10, 24), date(2012, 10, 25), date(2012, 10, 26),
            date(2012, 10, 27), date(2012, 10, 28), date(2012, 10, 29)
        ],
        [
            [date(2012, 10, 24), date(2012, 10, 25)],
            [date(2012, 10, 26)],
            [date(2012, 10, 27)],
            [date(2012, 10, 28), date(2012, 10, 29)]
        ]
    )

    split_and_check_mask(
        [date(2012, 3, 20), date(2012, 3, 23), date(2012, 6, 1), date(2012, 10, 27), date(2012, 10, 30)],
        [
            [date(2012, 3, 20), date(2012, 10, 30)],
            [date(2012, 3, 23)],
            [date(2012, 6, 1)],
            [date(2012, 10, 27)]
        ]
    )

    split_and_check_mask(
        [
            date(2012, 3, 21), date(2012, 3, 22), date(2012, 3, 23),
            date(2012, 3, 24), date(2012, 3, 25), date(2012, 3, 26),
            date(2012, 10, 24), date(2012, 10, 25), date(2012, 10, 26),
            date(2012, 10, 27), date(2012, 10, 28), date(2012, 10, 29)
        ],
        [
            [date(2012, 3, 21), date(2012, 3, 22), date(2012, 10, 28), date(2012, 10, 29)],
            [date(2012, 3, 23)],
            [date(2012, 3, 24)],
            [date(2012, 3, 25), date(2012, 3, 26), date(2012, 10, 24), date(2012, 10, 25)],
            [date(2012, 10, 26)],
            [date(2012, 10, 27)],
        ]
    )


@replace_now('2020-09-01')
def test_calculate_mask_for_tz_and_station():
    init_mask = RunMask(today=environment.today(), days=[date(2020, 9, 3), date(2020, 9, 5)])
    moscow_pytz = pytz.timezone('Etc/GMT-3')
    ekb_pytz = pytz.timezone('Etc/GMT-5')

    mask = calculate_mask_for_tz_and_station(init_mask, time(0), 0, moscow_pytz, ekb_pytz)
    assert mask.dates() == [date(2020, 9, 3), date(2020, 9, 5)]

    mask = calculate_mask_for_tz_and_station(init_mask, time(0), 0, ekb_pytz, moscow_pytz)
    assert mask.dates() == [date(2020, 9, 2), date(2020, 9, 4)]

    mask = calculate_mask_for_tz_and_station(init_mask, time(23), 0, moscow_pytz, ekb_pytz)
    assert mask.dates() == [date(2020, 9, 4), date(2020, 9, 6)]

    mask = calculate_mask_for_tz_and_station(init_mask, time(23), 2, moscow_pytz, ekb_pytz)
    assert mask.dates() == [date(2020, 9, 6), date(2020, 9, 8)]

    mask = calculate_mask_for_tz_and_station(init_mask, time(0), 1, ekb_pytz, moscow_pytz)
    assert mask.dates() == [date(2020, 9, 3), date(2020, 9, 5)]
