# -*- coding: utf-8 -*-

from common.models.geo import District, Settlement, Station
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.www.utils.district_import import import_district


module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml'
]


class DistrictLoaderTest(TestCase):
    class_fixtures = module_fixtures

    test_data = [
        'station_id;settlement_id;title',
        ';213;Москва',
        '9600366;;Пулково'
    ]

    def testLoadDistrict(self):

        district = District.objects.create(title=u"test", region_id=1)

        import_district(district, self.test_data)

        msk = Settlement.objects.get(id=213)
        plk = Station.objects.get(id=9600366)

        self.assertEqual(msk.district, district)
        self.assertEqual(plk.district, district)

    def testDistrictLog(self):
        district = District.objects.create(title=u"test", region_id=1)

        self.assertFalse(district.get_log())

        import_district(district, self.test_data)

        self.assertTrue(district.get_log().count(u"Москва"))
