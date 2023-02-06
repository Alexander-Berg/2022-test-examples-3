# -*- coding: utf-8 -*-
from __future__ import absolute_import

from copy import copy
from datetime import date

from common.db.switcher import switcher
from common.models.schedule import TrainSchedulePlan
from travel.rasp.admin.scripts.schedule.af_processors.suburban.affected_threads_finder import generate_possible_route_uids
from tester.factories import create_thread
from tester.testcase import TestCase


class TestPossibleRouteUidsGeneration(TestCase):
    def test_possible_route_uids_generation(self):
        TrainSchedulePlan.objects.create(
            title=u'2014',
            start_date=date(2014, 1, 1),
            end_date=date(2014, 12, 31),
            code='g14',
        )
        TrainSchedulePlan.objects.create(
            title=u'2015',
            start_date=date(2015, 1, 1),
            end_date=date(2015, 12, 31),
            code='g15',
        )
        thread = create_thread()
        start_station = thread.path[0].station

        rtstations = [1, 2, 3]
        thread.rtstations = copy(rtstations)
        schedule_plan = copy(thread.schedule_plan)

        # избавляемся от воздействия cache_until_switch
        switcher.data_updated.send(None)

        route_uids = generate_possible_route_uids(thread.number, thread.supplier, start_station)
        assert len(set(route_uids)) == 3

        # проверяем, что атрибуты нитки не пострадали
        assert thread.rtstations == rtstations
        assert thread.schedule_plan == schedule_plan
