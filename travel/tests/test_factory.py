# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, timedelta

import pytest
from django.conf import settings

from common.models.geo import StationMajority
from common.models.transport import TransportType
from travel.rasp.library.python.common23.date import environment
from common.utils.fields import MemoryFile
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import CysixGroupFilter
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.lib.unittests.server import serve_file
from travel.rasp.admin.lib.unittests.utils import use_tmp_data_path
from travel.rasp.admin.scripts.schedule.utils import RaspImportError
from common.tester.factories import create_station, create_supplier
from cysix.filter_parameters import FilterParameters
from cysix.models import PackageGroupFilter
from cysix.tests.utils import get_test_filepath
from cysix.two_stage_import.factory import CysixTSIGroupFactory


@pytest.mark.usefixtures('http_allowed')
class FactoryTest(TestCase):
    def setUp(self):
        self.package = create_tsi_package()
        first_station = create_station(t_type=TransportType.PLANE_ID, title=u'Начало',
                                       majority=StationMajority.IN_TABLO_ID, time_zone='Europe/Moscow')
        second_station = create_station(t_type=TransportType.BUS_ID, title=u'Конец',
                                        majority=StationMajority.NOT_IN_TABLO_ID, time_zone='UTC')
        last_station = create_station(t_type=TransportType.BUS_ID, title=u'Еще дальше',
                                      majority=StationMajority.NOT_IN_TABLO_ID, time_zone='Asia/Yekaterinburg')
        StationMapping.objects.create(station=first_station, code='g1_vendor_1', title=u'Начало',
                                      supplier=self.package.supplier)
        StationMapping.objects.create(station=second_station, code='g1_vendor_2', title=u'Конец',
                                      supplier=self.package.supplier)
        StationMapping.objects.create(station=last_station, code='g1_vendor_3', title=u'Еще дальше',
                                      supplier=self.package.supplier)
        self.factory = self.package.get_two_stage_factory()

    def testGetFactory(self):
        self.package.url = 'test_url'

        factory = self.package.get_two_stage_factory()

        factory.get_two_stage_importer()
        provider = factory.get_data_provider()
        station_finder = factory.get_station_finder()
        settings = factory.get_settings()

        self.assertEqual(provider, factory.get_data_provider())
        self.assertEqual(station_finder, factory.get_station_finder())
        self.assertEqual(settings.set_number, False)

    @use_tmp_data_path
    def testHTTPFileProvider(self):
        with serve_file(get_test_filepath('data', 'test_factory', 'test.txt'), 'text/xml') as self.package.url:
            file_provider = self.factory.get_file_provider()
            filepath = file_provider.get_cysix_file()

            with open(filepath) as f:
                self.assertEqual(f.read().strip(), 'test_data')

    def test_build_request_yt(self):
        self.package.url = 'https://hahn.yt.yandex-team.ru/api/v3/read_table?path=//home'
        file_provider = self.factory.get_file_provider()
        req = file_provider.build_request()
        assert req.headers == {'Authorization': 'OAuth {}'.format(settings.YT_TOKEN)}
        assert req.origin_req_host == settings.YT_FILE_HOST

    @use_tmp_data_path
    def test_build_request(self):
        with serve_file(get_test_filepath('data', 'test_factory', 'test.txt'), 'text/xml') as self.package.url:
            file_provider = self.factory.get_file_provider()
            req = file_provider.build_request()
            assert req.headers == {}
            assert req.origin_req_host == 'localhost'

    @use_tmp_data_path
    def testPackageFileProvider(self):
        factory = self.package.get_two_stage_factory()

        self.package.package_file = MemoryFile(b'file.xml', b'text/xml', b'test_data22')

        file_provider = factory.get_file_provider()
        filepath = file_provider.get_cysix_file()

        with open(filepath) as f:
            self.assertEqual(f.read().strip(), b'test_data22')

    @replace_now(datetime(2013, 2, 1))
    def testGetMaskBuilderSimple(self):
        factory = self.package.get_two_stage_factory()
        mask_builder = factory.get_mask_builder()

        dates = mask_builder.daily_mask().dates()

        start_date, last_date = dates[0], dates[-1]

        self.assertEqual(start_date, date(2013, 1, 18))
        self.assertEqual(last_date, environment.today() + timedelta(days=self.package.tsisetting.max_forward_days))

    @replace_now(datetime(2013, 2, 1))
    def testGetMaskBuilderStartDate(self):
        factory = self.package.get_two_stage_factory()
        mask_builder = factory.get_mask_builder(start_date=date(2013, 1, 30))

        dates = mask_builder.daily_mask().dates()

        start_date, last_date = dates[0], dates[-1]

        self.assertEqual(start_date, date(2013, 1, 30))
        self.assertEqual(last_date, environment.today() + timedelta(days=self.package.tsisetting.max_forward_days))

    @replace_now(datetime(2013, 2, 1))
    def testGetMaskBuilderEndDate(self):
        factory = self.package.get_two_stage_factory()
        factory.max_forward_days = 10

        mask_builder = factory.get_mask_builder(start_date=date(2013, 1, 30), end_date=date(2013, 4, 1))

        dates = mask_builder.daily_mask().dates()

        start_date, last_date = dates[0], dates[-1]

        self.assertEqual(start_date, date(2013, 1, 30))
        self.assertEqual(last_date, date(2013, 2, 11))

        mask_builder = factory.get_mask_builder(end_date=date(2013, 2, 5))

        dates = mask_builder.daily_mask().dates()

        start_date, last_date = dates[0], dates[-1]

        self.assertEqual(start_date, date(2013, 1, 18))
        self.assertEqual(last_date, date(2013, 2, 5))


@pytest.mark.usefixtures('http_allowed')
class CysixTSIGroupFactoryTest(TestCase):
    def setUp(self):
        self.supplier = create_supplier(code='test_sup')
        self.package = create_tsi_package(supplier=self.supplier)
        self.package.add_default_filters()

        self.group_code_use_filters = 'p_group_use_filters'
        self.group_code_not_use_filters = 'p_group_defaut'

        group_filter = CysixGroupFilter.objects.create(code=self.group_code_use_filters, import_order_data=True,
                                        package=self.package,
                                        title=u'Барнаул', tsi_import_available=True, tsi_middle_available=True,
                                        use_thread_in_station_code=True, use_package_group_filters=True)

        group_filter.add_default_filters()

        group_filter = CysixGroupFilter.objects.create(code=self.group_code_not_use_filters, import_order_data=True,
                                        package=self.package,
                                        title=u'Владивосток', tsi_import_available=True, tsi_middle_available=True,
                                        use_thread_in_station_code=False, use_package_group_filters=False)

        group_filter.add_default_filters()

        self.package.tsisetting.filter_by_group = True
        self.package.tsisetting.save()

        self.factory = self.package.get_two_stage_factory()
        self.group_use_filters_factory = CysixTSIGroupFactory(self.factory, self.group_code_use_filters)
        self.group_not_use_filters_factory = CysixTSIGroupFactory(self.factory, self.group_code_not_use_filters)

    def test_factory_methods(self):
        factories = (self.factory, self.group_not_use_filters_factory, self.group_use_filters_factory)

        def assert_results_equal(func):
            assert func(factories[0]) == func(factories[1]) == func(factories[2])

        assert_results_equal(lambda f: f.get_two_stage_importer())
        assert_results_equal(lambda f: f.get_company_finder())
        assert_results_equal(lambda f: f.get_transport_model_finder())
        assert_results_equal(lambda f: f.get_data_provider())
        assert_results_equal(lambda f: f.get_settings())
        assert_results_equal(lambda f: f.get_context())
        assert_results_equal(lambda f: f.get_station_finder())
        assert_results_equal(lambda f: f.get_file_provider())
        assert_results_equal(lambda f: f.get_supplier_route_class())

        start_date, end_date = date.today(), date.today() + timedelta(days=5)
        assert_results_equal(lambda f: f.get_mask_builder(start_date=start_date, end_date=end_date))

        def assert_results_not_equal(func):
            assert func(factories[0]) != func(factories[1]) != func(factories[2])

        assert_results_not_equal(lambda f: f.get_route_importer())
        assert_results_not_equal(lambda f: f.get_package_file_provider())
        assert_results_not_equal(lambda f: f.get_download_file_provider())

    def test_get_filter_use(self):
        filter_code = 'correct_departure_and_arrival'
        filter_ = PackageGroupFilter.objects.get(
            filter__code=filter_code,
            cysix_group_filter__code=self.group_code_use_filters
        )

        filter_.use = False
        filter_.save()

        package_filter = self.factory.get_filter(filter_code)

        package_group_filter = self.group_not_use_filters_factory.get_filter(filter_code)
        assert package_filter.params == package_group_filter.params

        package_group_filter = self.group_use_filters_factory.get_filter(filter_code)
        assert package_filter.params != package_group_filter.params

    def test_get_filter_params(self):
        filter_code = 'correct_departure_and_arrival'
        filter_ = PackageGroupFilter.objects.get(
            filter__code=filter_code,
            cysix_group_filter__code=self.group_code_use_filters
        )

        filter_.parameters = FilterParameters('''
            [
                {
                    "code": "departure",
                    "type": "string",
                    "value": "",
                    "title": "text"
                }
            ]
        ''')

        filter_.save()

        package_filter = self.factory.get_filter(filter_code)

        package_group_filter = self.group_not_use_filters_factory.get_filter(filter_code)
        assert package_filter.params == package_group_filter.params

        package_group_filter = self.group_use_filters_factory.get_filter(filter_code)
        assert package_filter.params != package_group_filter.params
        assert package_group_filter.params == filter_.parameters.get_parameters_as_dict()

    def test_import_error(self):
        with pytest.raises(RaspImportError):
            self.group_use_filters_factory.get_filter('some_unknown_filter')

        with pytest.raises(RaspImportError):
            self.group_not_use_filters_factory.get_filter('some_unknown_filter')

    def test_get_package_filter_obj(self):
        filter_code = 'allow_station_mapping_by_code'
        filter_ = PackageGroupFilter.objects.get(
            filter__code=filter_code,
            cysix_group_filter__code=self.group_code_use_filters
        )

        filter_.use = True
        filter_.save()

        package_filter = self.factory.get_package_filter_obj(filter_code)
        assert not package_filter.use

        package_filter = self.group_use_filters_factory.get_package_filter_obj(filter_code)
        assert package_filter.use
