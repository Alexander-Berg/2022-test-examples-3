# -*- coding: utf-8 -*-

import mock
import os.path

from datetime import datetime

from parameterized import parameterized

from cysix.models import PackageGroupFilter
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import CysixGroupFilter, TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase

data_file_path = os.path.join('travel', 'rasp', 'admin', 'tests', 'importinfo', 'data', 'test_tsi_thread_station_flags',
                              'threads.xml')


class TestGroupPackageFilters(TestCase):
    class_fixtures = ['travel.rasp.admin.tests.importinfo:test_tsi_thread_station_flags.yaml']

    def setUp(self):
        self.package = TwoStageImportPackage.objects.get(title=u"Test Package")
        TSISetting.objects.get_or_create(package=self.package)

        self.group_code = 'g1'
        self.group_filter = CysixGroupFilter.objects.create(code=self.group_code, import_order_data=True,
                                                            package=self.package,
                                                            title=u'Барнаул', tsi_import_available=True,
                                                            tsi_middle_available=True,
                                                            use_thread_in_station_code=True,
                                                            use_package_group_filters=True)

        self.group_filter.add_default_filters()

        self.factory = self.package.get_two_stage_factory()

    def _set_package_group_filter_use(self, filter_name, use):
        filter_ = PackageGroupFilter.objects.get(
            filter__code=filter_name,
            cysix_group_filter__package=self.package,
            cysix_group_filter__code=self.group_code
        )

        filter_.use = use
        filter_.save()

    @parameterized.expand([
        ('correct_departure_and_arrival',
         'cysix.filters.correct_departure_and_arrival.Filter.correct_departure_and_arrival'),
        ('correct_stop_time', 'cysix.filters.correct_stop_time.Filter.correct_stop_time'),
        ('sort_stoppoints_by_supplier_distance',
         'cysix.filters.sort_stoppoints_by_supplier_distance.Filter.sort_by_supplier_distance'),
        ('import_geometry', 'cysix.filters.import_geometry.write_geometry'),
        ('fuzzy_flag_interpretation', 'cysix.filters.fuzzy_flag_interpretation.Filter._update_flags'),
        ('timezone_override', 'cysix.two_stage_import.xml_thread.CysixRTStation.get_timezone'),
        ('replace_local_time_by_shift', 'cysix.two_stage_import.CysixRTSParser.replace_local_time_by_shift'),
        ('cysix_xml_thread.skip_station_without_times',
         'cysix.filters.cysix_xml_thread.skip_station_without_times.SkipStationWithoutTimes.times_is_filled'),
        ('check_path_geometry', 'cysix.filters.check_path_geometry.Filter.check_path_geometry'),
        ('correct_arrival_time_by_map', 'cysix.filters.correct_arrival_time_by_map.Filter.can_correct_path')

        # Нельзя переопределить для групп пакета
        # ('text_schedule_and_extrapolation', 'cysix.filters.text_schedule_and_extrapolation.Filter.process'),
        # ('thread_comment', 'cysix.filters.thread_comment.Filter._set_comment'),
    ])
    @replace_now(datetime(2013, 2, 4))
    def test_filter_use(self, filter_name, filter_method):
        self.package.set_filter_use(filter_name, False)
        self._set_package_group_filter_use(filter_name, True)

        with open(data_file_path) as f:
            self.package.package_file = f

            with mock.patch(filter_method) as m_filter:
                tsi_importer = self.factory.get_two_stage_importer()
                tsi_importer.reimport_package()

            assert m_filter.called

    @parameterized.expand([
        ('allow_station_mapping_by_code',
         'travel.rasp.admin.importinfo.two_stage_import.TwoStageImportStationFinder.find_exact_mappings_by_code_only'),
        ('set_default_flags', 'cysix.filters.set_default_flags.Filter.set_default_flags'),
    ])
    @replace_now(datetime(2013, 2, 4))
    def test_filter_use_on_reimport_package_into_middle_base(self, filter_name, filter_method):
        StationMapping.objects.all().delete()
        self.package.set_filter_use(filter_name, False)
        self._set_package_group_filter_use(filter_name, True)

        with open(data_file_path) as f:
            self.package.package_file = f

            with mock.patch(filter_method) as m_filter:
                tsi_importer = self.factory.get_two_stage_importer()
                tsi_importer.reimport_package_into_middle_base()

            assert m_filter.called
