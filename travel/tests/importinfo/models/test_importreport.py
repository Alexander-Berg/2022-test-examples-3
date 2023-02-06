# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.tester.factories import create_supplier
from travel.rasp.admin.importinfo.models import ImportReport


pytestmark = pytest.mark.dbuser('module')


class TestPickedObjectField(object):
    def test_save_and_get_not_empty(self):
        test_set = {'a', 'b', 'c'}
        ImportReport.objects.create(supplier=create_supplier(), route_set=test_set)

        ir = ImportReport.objects.get()
        assert ir.route_set == test_set

    def test_lookup_not_empty(self):
        test_set = {'a', 'b', 'c'}
        ImportReport.objects.create(supplier=create_supplier(), route_set=test_set)

        assert ImportReport.objects.filter(route_set=test_set).exists()
        assert ImportReport.objects.filter(route_set__in=[test_set]).exists()
        assert not ImportReport.objects.filter(route_set={0, 1, 2}).exists()
        assert not ImportReport.objects.filter(route_set__in=[{0, 1, 2}]).exists()

    def test_save_and_get_empty(self):
        ImportReport.objects.create(supplier=create_supplier())

        ir = ImportReport.objects.get()
        assert ir.route_set is None

    def test_lookup_empty(self):
        ImportReport.objects.create(supplier=create_supplier())

        assert ImportReport.objects.filter(route_set=None).exists()
        assert ImportReport.objects.filter(route_set__isnull=True).exists()
        assert not ImportReport.objects.filter(route_set={0, 1, 2}).exists()
        assert not ImportReport.objects.filter(route_set__isnull=False).exists()
