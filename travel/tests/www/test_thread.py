# -*- coding: utf-8 -*-

from datetime import time, date
from itertools import permutations

from common.models.schedule import RThread
from common.utils.date import RunMask
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml'
]


class ThreadImportUidTest(TestCase):
    class_fixtures = ['travel.rasp.admin.tester.fixtures.www:thread.yaml'] + module_fixtures

    def testThreadImportUid(self):
        route = self.get_obj_from_fixture('travel.rasp.admin.tester.fixtures.www:thread.yaml', 'www.route', '1')
        thread = route.rthread_set.all()[0]
        thread.supplier = route.supplier
        thread.t_type = route.t_type

        route.route_uid = thread.gen_route_uid(use_start_station=True, use_company=True)
        route.save()

        import_uids = [(thread.gen_import_uid(), u"basic")]

        # change route
        thread.number = u"10"
        route.route_uid = thread.gen_route_uid(use_start_station=True, use_company=True)
        thread.route = route

        import_uids.append((thread.gen_import_uid(), u"platform"))

        # change arrival
        thread.rtstations = list(thread.rtstation_set.all().select_related('station'))
        thread.rtstations[1].tz_arrival = 1000

        import_uids.append((thread.gen_import_uid(), u"arrival"))

        # change platform
        thread.rtstations[0].platform = "25"

        import_uids.append((thread.gen_import_uid(), u"platform"))

        for (uid1, type1_), (uid2, type2_) in permutations(import_uids, 2):
            self.assertNotEqual(uid1, uid2, u"{} {} == {} {}".format(uid1, type1_, uid2, type2_))

    @replace_now('2013-03-10 00:00:00')
    def testCalcMskToLocalShift(self):
        thread = RThread()
        thread.tz_start_time = time(23, 00)
        thread.time_zone = 'Europe/Moscow'

        thread.year_days = str(RunMask(days=[date(2013, 3, 10)]))

        self.assertEqual(thread.calc_mask_shift('Asia/Yekaterinburg'), 1)
