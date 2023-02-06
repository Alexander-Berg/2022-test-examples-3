# coding: utf-8

from __future__ import absolute_import, unicode_literals

from xml.etree.ElementTree import fromstring

from common.models.schedule import Company
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.af_thread_parser import AfThreadParser
from tester.factories import create_supplier


def process_xml_thread(thread_xml, default_supplier=None, default_company=None):
    if not default_supplier:
        default_supplier = create_supplier(code='test_supplier_for_af')
    if not default_company:
        default_company = Company.objects.create(title=u'Компания для тестов А.Ф.')

    parser = AfThreadParser(default_supplier=default_supplier,
                            default_company_id=default_company.id,
                            default_t_type_code='suburban')

    thread_el =fromstring(thread_xml)
    thread = parser.parse_thread(fromstring(thread_xml))
    suburban.process_thread(thread, thread_el, 'test.thread_xml', environment.today())


def assert_has_facilities(thread, facilities, dates):
    thread_facility = get_thread_facility(thread, facilities)
    mask = RunMask(thread_facility.year_days)
    assert all(mask[d] for d in dates), 'Удобства не совпадают по дням'


def assert_has_not_facilities(thread, facilities, dates):
    thread_facility = get_thread_facility(thread, facilities)
    mask = RunMask(thread_facility.year_days)
    assert not any(mask[d] for d in dates), 'Не должно быть данных удобств по этим дням'


def get_thread_facility(thread, facilities):
    for thread_facility in thread.suburbanthreadfacility_set.prefetch_related('facilities'):
        if set(facilities) == set(thread_facility.facilities.all()):
            return thread_facility
