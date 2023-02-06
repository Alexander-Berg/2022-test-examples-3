#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import unittest
import yatest.common

from market.tools.update_dynamic_filter_outlet.test.util.runners import (
    prepare_json_data,
    run_pbsncat,
    run_update_dynamic_filter_outlet
)
from market.tools.update_dynamic_filter_outlet.test.util.output_formatters import (
    Date,
    WorkingTime,
    YtOutlet,
    FastDataOutlet
)


YT_OUTLETS_DATA = [
    i.to_json() for i in [
        YtOutlet(
            lms_id=10000000001,
            mbi_id=1,
            is_active=True,
            updated_at='2020-04-17T19:10:18Z',
            working_time=[
                WorkingTime(week_day=1, hour_from=10, min_from=0, hour_to=20, min_to=0),
                WorkingTime(week_day=5, hour_from=12, min_from=0, hour_to=21, min_to=0)
            ],
            start_date=Date(year=2020, month=4, day=24),
            end_date=Date(year=2020, month=7, day=23),
            calendar_holidays=[
                Date(year=2020, month=3, day=1),
                Date(year=2020, month=4, day=25),
                Date(year=2021, month=1, day=1)
            ]
        ),
        YtOutlet(
            lms_id=10000000002,
            mbi_id=2,
            is_active=False,
            updated_at='2020-04-17T19:10:18Z',
            working_time=[
                WorkingTime(week_day=1, hour_from=10, min_from=0, hour_to=20, min_to=0),
                WorkingTime(week_day=5, hour_from=15, min_from=0, hour_to=21, min_to=0),
                WorkingTime(week_day=5, hour_from=12, min_from=0, hour_to=14, min_to=0)
            ],
            start_date=Date(year=2020, month=4, day=24),
            end_date=Date(year=2020, month=7, day=23),
            calendar_holidays=[]
        ),

        # This point has no mbi_id, so we expect warning
        YtOutlet(
            lms_id=10000000003,
            is_active=True,
            updated_at='2020-04-17T19:10:18Z',
            working_time=[
                WorkingTime(week_day=1, hour_from=10, min_from=0, hour_to=20, min_to=0),
                WorkingTime(week_day=5, hour_from=12, min_from=0, hour_to=21, min_to=0)
            ],
            start_date=Date(year=2020, month=4, day=24),
            end_date=Date(year=2020, month=7, day=23),
            calendar_holidays=[]
        )
    ]
]

FAST_DATA_OUTLETS_DATA = {
    'fast_data_outlets': [
        i.to_json() for i in [
            FastDataOutlet(
                id=10000000001,
                last_update_time='2020-04-17T19:10:18Z',
                is_active=True,
                start_day=Date(year=2020, month=4, day=24),
                end_day=Date(year=2020, month=7, day=23),
                calendar_holidays=[Date(year=2020, month=4, day=25)],
                working_time=[
                    WorkingTime(week_day=1, hour_from=10, min_from=0, hour_to=20, min_to=0),
                    WorkingTime(week_day=5, hour_from=12, min_from=0, hour_to=21, min_to=0)
                ],
                mbi_id=1
            ),
            FastDataOutlet(
                id=10000000002,
                last_update_time='2020-04-17T19:10:18Z',
                is_active=False,
                start_day=Date(year=2020, month=4, day=24),
                end_day=Date(year=2020, month=7, day=23),
                working_time=[
                    WorkingTime(week_day=1, hour_from=10, min_from=0, hour_to=20, min_to=0),
                    WorkingTime(week_day=5, hour_from=12, min_from=0, hour_to=14, min_to=0),
                    WorkingTime(week_day=5, hour_from=15, min_from=0, hour_to=21, min_to=0)
                ],
                mbi_id=2
            ),
            FastDataOutlet(
                id=10000000003,
                last_update_time='2020-04-17T19:10:18Z',
                is_active=True,
                start_day=Date(year=2020, month=4, day=24),
                end_day=Date(year=2020, month=7, day=23),
                working_time=[
                    WorkingTime(week_day=1, hour_from=10, min_from=0, hour_to=20, min_to=0),
                    WorkingTime(week_day=5, hour_from=12, min_from=0, hour_to=21, min_to=0)
                ]
            )
        ]
    ]
}

ERR_LOG_LINES = {
    # line number : content
    '1': "Total number of records read: 3",
    '2': "Errors occurred while parsing YT table:",
    '3': "[WRN] MBI ID corrupted (total: 1):\t10000000003"
}


class T(unittest.TestCase):
    def _check_stderr(self, err, expected):
        actual = {}
        with open(err) as err_file:
            i = 0
            line = err_file.readline()
            while line:
                actual[str(i)] = line.rstrip()
                i += 1
                line = err_file.readline()
        self.assertDictContainsSubset(expected, actual)

    def _check_json_files_equal(self, lhs, rhs):
        with open(lhs) as lhs_file:
            lhs_str = json.dumps(json.load(lhs_file), sort_keys=True)
        with open(rhs) as rhs_file:
            rhs_str = json.dumps(json.load(rhs_file), sort_keys=True)
        self.assertEqual(lhs_str, rhs_str)

    def test_update_dynamic_filter_outlet(self):
        fast_data_outlets_pb_sn, out, err = run_update_dynamic_filter_outlet(yt_outlet_data=YT_OUTLETS_DATA)

        expected_fast_data_outlets = yatest.common.output_path('expected_fast_data_outlets.json')
        actual_fast_data_outlets = yatest.common.output_path('actual_fast_data_outlets.json')

        prepare_json_data(
            output_filename=expected_fast_data_outlets,
            data=FAST_DATA_OUTLETS_DATA
        )
        run_pbsncat(
            magic='FDOU',
            input_filename=fast_data_outlets_pb_sn,
            input_format='pbsn',
            output_filename=actual_fast_data_outlets,
            output_format='json'
        )

        self._check_json_files_equal(lhs=actual_fast_data_outlets, rhs=expected_fast_data_outlets)
        self._check_stderr(err=err, expected=ERR_LOG_LINES)


if __name__ == '__main__':
    unittest.main()
