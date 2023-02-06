# -*- coding: utf-8 -*-

from common.tester.factories import create_supplier, create_station
from common.tester.testcase import TestCase

from travel.rasp.admin.importinfo.models.mappings import StationMapping


class TestStationMapping(TestCase):
    def test_copy_mappings(self):
        supplier = create_supplier()
        station = create_station()
        StationMapping.objects.create(supplier=supplier, station=station, title='1', code='old_vendor_1')

        StationMapping.copy_mappings(supplier=supplier, old_group_code='old', new_group_code='new')

        mapping = StationMapping.objects.get(supplier=supplier, code='new_vendor_1')
        assert mapping.station_id == station.id

    def test_copy_mappings_different_length(self):
        supplier = create_supplier()
        station = create_station()
        StationMapping.objects.create(supplier=supplier, station=station, title='1', code='old_vendor_1')

        StationMapping.copy_mappings(supplier=supplier, old_group_code='old', new_group_code='newnew')

        mapping = StationMapping.objects.get(supplier=supplier, code='newnew_vendor_1')
        assert mapping.station_id == station.id

    def test_copy_mappings_different_length_unicode(self):
        supplier = create_supplier()
        station = create_station()
        StationMapping.objects.create(supplier=supplier, station=station, title='1', code=u'старый_vendor_1')

        StationMapping.copy_mappings(supplier=supplier, old_group_code=u'старый', new_group_code=u'новый')

        mapping = StationMapping.objects.get(supplier=supplier, code=u'новый_vendor_1')
        assert mapping.station_id == station.id
