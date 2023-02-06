# coding: utf-8

from __future__ import unicode_literals

import os

from common.models.geo import Country
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, CysixGroupFilter
from tester.testcase import TestCase
from tester.factories import create_currency, create_station, create_region, create_supplier, create_settlement


def get_test_filepath(*path_parts):
    return os.path.join('travel', 'rasp', 'admin', 'cysix', 'tests', *path_parts)


class CysixTestCase(TestCase):
    @classmethod
    def setUpTestData(cls):
        super(CysixTestCase, cls).setUpTestData()

        create_currency(name='рубли', code='RUR', iso_code='RUB')

        cls.msk_region = create_region(pk=1, title='Московская область')
        cls.ekb_region = create_region(title='Свердловская область', time_zone='Asia/Yekaterinburg')
        cls.ekb_settlement = create_settlement(title='Екатеринбург', region=cls.ekb_region,
                                               time_zone='Asia/Yekaterinburg')

        station_1 = create_station(country=Country.RUSSIA_ID, region=cls.msk_region, title='Станция 1')
        station_2 = create_station(country=Country.RUSSIA_ID, region=cls.msk_region, title='Станция 2')
        station_3 = create_station(country=Country.RUSSIA_ID, region=cls.msk_region, title='Станция 3')
        station_4 = create_station(country=Country.RUSSIA_ID, region=cls.msk_region, title='Станция 4')
        station_5 = create_station(country=Country.RUSSIA_ID, region=cls.msk_region, title='Станция 5')

        supplier = create_supplier(code='supplier_1')

        tsi_package = TwoStageImportPackage.objects.create(supplier=supplier)
        CysixGroupFilter.objects.create(
            package=tsi_package, code='group1', import_order_data=True, title='Группа 1',
            tsi_import_available=True, tsi_middle_available=True, use_thread_in_station_code=False
        )

        StationMapping.objects.create(supplier=supplier, title='Станция 1', station=station_1, code='group1_vendor_1')
        StationMapping.objects.create(supplier=supplier, title='Станция 2', station=station_2, code='group1_vendor_2')
        StationMapping.objects.create(supplier=supplier, title='Станция 3', station=station_3, code='group1_vendor_3')
        StationMapping.objects.create(supplier=supplier, title='Станция 4', station=station_4, code='group1_vendor_4')
        StationMapping.objects.create(supplier=supplier, title='Станция 5', station=station_5, code='group1_vendor_5')
