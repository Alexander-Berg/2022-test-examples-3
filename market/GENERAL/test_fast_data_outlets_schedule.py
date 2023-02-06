#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import DynamicOutletDate, DynamicOutletInfo, DynamicOutletTime, DynamicOutletWorkingTime, Outlet
from core.types.delivery import OutletWorkingTime


OUTLETS_REQUEST = (
    'place=outlets&'
    'bsformat=2&'
    'rgb={color}&'
    'pp=18&'
    'outlets={outlets}&'
    'compress-working-time={compress}&'
    'rearr-factors=market_blue_use_dynamic_for_calendar={use_dynamic}'
)

WORKING_DAYS = [
    OutletWorkingTime.MONDAY,
    OutletWorkingTime.TUESDAY,
    OutletWorkingTime.WEDNESDAY,
    OutletWorkingTime.THURSDAY,
    OutletWorkingTime.FRIDAY,
]
WEEK_DAYS = [OutletWorkingTime.SUNDAY] + WORKING_DAYS + [OutletWorkingTime.SATURDAY]


class UseFastDataOutlets(object):
    UFDO_USE_INDEX = 0
    UFDO_USE_DYNAMIC_WITH_FALLBACK = 1
    UFDO_USE_DYNAMIC_ONLY = 2


class T(TestCase):
    @classmethod
    def prepare_outlets(cls):
        working_times = [
            OutletWorkingTime(
                days_from=OutletWorkingTime.MONDAY,
                days_till=OutletWorkingTime.SUNDAY,
                hours_from='09:00',
                hours_till='13:00',
            ),
            OutletWorkingTime(
                days_from=OutletWorkingTime.MONDAY,
                days_till=OutletWorkingTime.SUNDAY,
                hours_from='14:00',
                hours_till='17:00',
            ),
        ]
        working_times_friday_off = [
            OutletWorkingTime(
                days_from=OutletWorkingTime.MONDAY,
                days_till=OutletWorkingTime.THURSDAY,
                hours_from='00:00',
                hours_till='23:55',
            ),
            OutletWorkingTime(
                days_from=OutletWorkingTime.SATURDAY,
                days_till=OutletWorkingTime.SUNDAY,
                hours_from='00:00',
                hours_till='23:55',
            ),
        ]

        outlet_info = [
            # (id, alias_id, working_time)
            # ПВЗ, информация о которых есть в fast_data_outlets.pb.sn
            (1997, None, working_times_friday_off),
            (1998, None, working_times),
            (1999, None, working_times),
            (2000, None, working_times),
            (2001, None, working_times),
            (10000000000, 1997, working_times_friday_off),
            (10000000001, 1998, working_times),
            (10000000002, 1999, working_times),
            # ПВЗ, отсутствующие в fast_data_outlets.pb.sn
            (2002, None, working_times),
            (10000000003, 2002, working_times),
        ]
        cls.index.outlets += [
            Outlet(point_id=id, mbi_alias_point_id=alias_id, working_times=working_times)
            for id, alias_id, times in outlet_info
        ]

    @classmethod
    def prepare_fast_data_outlets(cls):
        time_from = DynamicOutletTime(hour=8, min=0)
        time_to = DynamicOutletTime(hour=20, min=0)

        working_times = [
            DynamicOutletWorkingTime(week_day=day, time_from=time_from, time_to=time_to) for day in WORKING_DAYS
        ]

        working_times_ext = [
            DynamicOutletWorkingTime(week_day=day, time_from=time_from, time_to=time_to) for day in WEEK_DAYS
        ]

        working_times_friday_off = [
            DynamicOutletWorkingTime(
                week_day=day, time_from=DynamicOutletTime(hour=0, min=0), time_to=DynamicOutletTime(hour=23, min=55)
            )
            for day in [
                OutletWorkingTime.SUNDAY,
                OutletWorkingTime.MONDAY,
                OutletWorkingTime.TUESDAY,
                OutletWorkingTime.WEDNESDAY,
                OutletWorkingTime.THURSDAY,
                OutletWorkingTime.SATURDAY,
            ]
        ]

        outlet_info = [
            # (id, alias_id, working_time)
            (2000, None, working_times),
            (2001, None, working_times_ext),
            (10000000000, 1997, working_times_friday_off),
            (10000000001, 1998, working_times),
            (10000000002, 1999, working_times_ext),
        ]
        cls.dynamic.fast_data_outlets += [
            DynamicOutletInfo(
                id=id,
                is_active=True,
                last_update_time='1985-06-24 05:07:33.419708',
                working_time=working_time,
                calendar_holidays=[],
                start_day=DynamicOutletDate(year=1985, month=6, day=24),
                end_day=DynamicOutletDate(year=1985, month=9, day=24),
                mbi_id=alias_id,
            )
            for id, alias_id, working_time in outlet_info
        ]

    def test_place_outlets_use_dynamic(self):
        """
        Проверяем, что расписание ПВЗ на выдаче в 'place=outlets' формируется
        в корректном формате с учетом параметра 'compress-working-time', если
        ПВЗ есть в динамике
        """
        outlet_info = [
            # (id, start_day, end_day)
            (1998, OutletWorkingTime.MONDAY, OutletWorkingTime.FRIDAY),
            (1999, OutletWorkingTime.MONDAY, OutletWorkingTime.SUNDAY),
            (10000000001, OutletWorkingTime.MONDAY, OutletWorkingTime.FRIDAY),
            (10000000002, OutletWorkingTime.MONDAY, OutletWorkingTime.SUNDAY),
        ]
        outlets_str = ','.join(str(id) for id, _, _ in outlet_info)

        for color in ['white', 'blue']:
            for use_dynamic in [
                UseFastDataOutlets.UFDO_USE_DYNAMIC_ONLY,
                UseFastDataOutlets.UFDO_USE_DYNAMIC_WITH_FALLBACK,
            ]:
                response = self.report.request_json(
                    OUTLETS_REQUEST.format(color=color, outlets=outlets_str, compress=0, use_dynamic=use_dynamic)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'outlet',
                                'id': str(id),
                                'workingTime': [
                                    {'daysFrom': str(day), 'daysTo': str(day), 'hoursFrom': '08:00', 'hoursTo': '20:00'}
                                    for day in range(start, end + 1)
                                ],
                            }
                            for id, start, end in outlet_info
                        ]
                    },
                    allow_different_len=False,
                )

                response = self.report.request_json(
                    OUTLETS_REQUEST.format(color=color, outlets=outlets_str, compress=1, use_dynamic=use_dynamic)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'outlet',
                                'id': str(id),
                                'workingTime': [
                                    {
                                        'daysFrom': str(start),
                                        'daysTo': str(end),
                                        'hoursFrom': '08:00',
                                        'hoursTo': '20:00',
                                    }
                                ],
                            }
                            for id, start, end in outlet_info
                        ]
                    },
                    allow_different_len=False,
                )

    def test_place_outlets_use_index(self):
        """
        Проверяем, что расписание ПВЗ на выдаче в 'place=outlets' при использовании
        только данных из индекса формируется в корректном формате
        с учетом параметра 'compress-working-time'
        """
        outlet_ids = [1998, 2001, 10000000001]
        outlets_str = ','.join(str(id) for id in outlet_ids)

        for color in ['white', 'blue']:
            response = self.report.request_json(
                OUTLETS_REQUEST.format(
                    color=color, outlets=outlets_str, compress=0, use_dynamic=UseFastDataOutlets.UFDO_USE_INDEX
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'outlet',
                            'id': str(id),
                            'workingTime': [
                                {'daysFrom': str(day), 'daysTo': str(day), 'hoursFrom': '09:00', 'hoursTo': '13:00'}
                                for day in WEEK_DAYS
                            ]
                            + [
                                {'daysFrom': str(day), 'daysTo': str(day), 'hoursFrom': '14:00', 'hoursTo': '17:00'}
                                for day in WEEK_DAYS
                            ],
                        }
                        for id in outlet_ids
                    ]
                },
                allow_different_len=False,
            )

            response = self.report.request_json(
                OUTLETS_REQUEST.format(
                    color=color, outlets=outlets_str, compress=1, use_dynamic=UseFastDataOutlets.UFDO_USE_INDEX
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'outlet',
                            'id': str(id),
                            'workingTime': [
                                {
                                    'daysFrom': str(OutletWorkingTime.MONDAY),
                                    'daysTo': str(OutletWorkingTime.SUNDAY),
                                    'hoursFrom': '09:00',
                                    'hoursTo': '17:00',
                                    'breaks': [{'hoursFrom': '13:00', 'hoursTo': '14:00'}],
                                }
                            ],
                        }
                        for id in outlet_ids
                    ]
                },
                allow_different_len=False,
            )

    def test_place_outlets_use_dynamic_with_fallback(self):
        """
        Проверяем, что расписание ПВЗ на выдаче в 'place=outlets' формируется
        в корректном формате с учетом параметра 'compress-working-time'

        Если данных в динамике недостаточно, используем fallback на индекс
        """
        outlet_ids = [2002, 10000000003]
        outlets_str = ','.join(str(id) for id in outlet_ids)

        for color in ['white', 'blue']:
            response = self.report.request_json(
                OUTLETS_REQUEST.format(
                    color=color,
                    outlets=outlets_str,
                    compress=1,
                    use_dynamic=UseFastDataOutlets.UFDO_USE_DYNAMIC_WITH_FALLBACK,
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'outlet',
                            'id': str(id),
                            'workingTime': [
                                {
                                    'daysFrom': str(OutletWorkingTime.MONDAY),
                                    'daysTo': str(OutletWorkingTime.SUNDAY),
                                    'hoursFrom': '09:00',
                                    'hoursTo': '17:00',
                                    'breaks': [{'hoursFrom': '13:00', 'hoursTo': '14:00'}],
                                }
                            ],
                        }
                        for id in outlet_ids
                    ]
                },
                allow_different_len=False,
            )

            response = self.report.request_json(
                OUTLETS_REQUEST.format(
                    color=color,
                    outlets=outlets_str,
                    compress=0,
                    use_dynamic=UseFastDataOutlets.UFDO_USE_DYNAMIC_WITH_FALLBACK,
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'outlet',
                            'id': str(id),
                            'workingTime': [
                                {'daysFrom': str(day), 'daysTo': str(day), 'hoursFrom': '09:00', 'hoursTo': '13:00'}
                                for day in WEEK_DAYS
                            ]
                            + [
                                {'daysFrom': str(day), 'daysTo': str(day), 'hoursFrom': '14:00', 'hoursTo': '17:00'}
                                for day in WEEK_DAYS
                            ],
                        }
                        for id in outlet_ids
                    ]
                },
                allow_different_len=False,
            )

    def test_place_outlets_friday_off(self):
        """
        Проверяем, что расписание ПВЗ на выдаче в 'place=outlets' формируется
        корректно, если:
            - строим выдачу в сжатом формате (параметр 'compress-working-time')
            - для рассчета используется только динамик или динамик + индекс
            - у ПВЗ "нетипичное" расписание (например, все дни рабочие за исключением пятницы)
        """
        outlet_ids = [1997, 10000000000]
        outlets_str = ','.join(str(id) for id in outlet_ids)
        for color in ['white', 'blue']:
            for use_dynamic in [
                UseFastDataOutlets.UFDO_USE_DYNAMIC_WITH_FALLBACK,
                UseFastDataOutlets.UFDO_USE_DYNAMIC_ONLY,
            ]:
                response = self.report.request_json(
                    OUTLETS_REQUEST.format(color=color, outlets=outlets_str, compress=1, use_dynamic=use_dynamic)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'outlet',
                                'id': str(id),
                                'workingTime': [
                                    {'daysFrom': str(day), 'daysTo': str(day), 'hoursFrom': '00:00', 'hoursTo': '23:55'}
                                    for day in [
                                        OutletWorkingTime.MONDAY,
                                        OutletWorkingTime.TUESDAY,
                                        OutletWorkingTime.WEDNESDAY,
                                        OutletWorkingTime.THURSDAY,
                                        OutletWorkingTime.SATURDAY,
                                        OutletWorkingTime.SUNDAY,
                                    ]
                                ],
                            }
                            for id in outlet_ids
                        ]
                    },
                    allow_different_len=False,
                )


if __name__ == '__main__':
    main()
