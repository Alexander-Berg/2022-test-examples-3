# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date
import os

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_station, create_thread, create_region
from common.tester.utils.datetime import replace_now
from common.models.geo import Region
from common.models.schedule import RThread
from common.utils.date import RunMask
from travel.rasp.admin.scripts.moscow_quarantine_buses import main


pytestmark = [pytest.mark.dbuser]


@replace_now('2020-03-27')
def test_moscow_quarantine():
    moscow_region = create_region(id=Region.MOSCOW_REGION_ID)
    chukotka_region = create_region(id=999)
    st_moscow_1 = create_station(region=moscow_region, t_type=TransportType.BUS_ID)
    st_moscow_2 = create_station(region=moscow_region, t_type=TransportType.BUS_ID)
    st_moscow_3 = create_station(region=moscow_region, t_type=TransportType.BUS_ID)
    st_chukotka = create_station(region=chukotka_region, t_type=TransportType.BUS_ID)
    st_train_1 = create_station(region=moscow_region, t_type=TransportType.TRAIN_ID)
    st_train_2 = create_station(region=moscow_region, t_type=TransportType.TRAIN_ID)

    os.environ['RASP_QUARANTINE_BUSES_TEMPLATE_DATE'] = "2020-03-29"
    os.environ['RASP_QUARANTINE_BUSES_START_DATE'] = "2020-03-30"
    os.environ['RASP_QUARANTINE_BUSES_FINISH_DATE'] = "2020-04-03"

    create_thread(
        id=101, t_type=TransportType.BUS_ID,
        year_days=[date(2020, 3, 25), date(2020, 3, 29), date(2020, 4, 1), date(2020, 4, 10)],
        schedule_v1=[
            [None, 0, st_moscow_1], [10, 20, st_moscow_2], [30, None, st_moscow_3]
        ]
    )

    create_thread(
        id=102, t_type=TransportType.BUS_ID,
        year_days=[date(2020, 3, 25), date(2020, 3, 30), date(2020, 4, 1), date(2020, 4, 3), date(2020, 4, 10)],
        schedule_v1=[
            [None, 0, st_moscow_3], [30, None, st_moscow_1]
        ]
    )

    create_thread(
        id=201, t_type=TransportType.BUS_ID,
        year_days=[date(2020, 3, 25), date(2020, 3, 29), date(2020, 4, 1), date(2020, 4, 10)],
        schedule_v1=[
            [None, 0, st_moscow_1], [30, None, st_chukotka]
        ]
    )

    create_thread(
        id=202, t_type=TransportType.BUS_ID,
        year_days=[date(2020, 3, 25), date(2020, 3, 30), date(2020, 4, 1), date(2020, 4, 10)],
        schedule_v1=[
            [None, 0, st_moscow_1], [30, None, st_chukotka]
        ]
    )

    create_thread(
        id=301, t_type=TransportType.TRAIN_ID,
        year_days=[date(2020, 3, 25), date(2020, 3, 29), date(2020, 4, 1), date(2020, 4, 10)],
        schedule_v1=[
            [None, 0, st_train_1], [30, None, st_train_2]
        ]
    )

    main()

    thread = RThread.objects.get(id=101)
    assert RunMask(thread.year_days) == RunMask(days=[
        date(2020, 3, 25), date(2020, 3, 29), date(2020, 3, 30), date(2020, 3, 31),
        date(2020, 4, 1), date(2020, 4, 2), date(2020, 4, 3), date(2020, 4, 10)
    ])

    thread = RThread.objects.get(id=102)
    assert RunMask(thread.year_days) == RunMask(days=[
        date(2020, 3, 25), date(2020, 4, 10)
    ])

    thread = RThread.objects.get(id=201)
    assert RunMask(thread.year_days) == RunMask(days=[
        date(2020, 3, 25), date(2020, 3, 29), date(2020, 4, 1), date(2020, 4, 10)
    ])

    thread = RThread.objects.get(id=202)
    assert RunMask(thread.year_days) == RunMask(days=[
        date(2020, 3, 25), date(2020, 3, 30), date(2020, 4, 1), date(2020, 4, 10)
    ])

    thread = RThread.objects.get(id=301)
    assert RunMask(thread.year_days) == RunMask(days=[
        date(2020, 3, 25), date(2020, 3, 29), date(2020, 4, 1), date(2020, 4, 10)
    ])
