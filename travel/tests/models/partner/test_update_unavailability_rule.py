# -*- coding=utf-8 -*-

from datetime import datetime

from travel.avia.library.python.common.models.partner import Partner, UpdateHistoryRecord

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_partner


class TestUpdateUnavailabilityRule(TestCase):
    def setUp(self):
        partner = create_partner()
        self.partner_id = partner.id

    def test_update_twice_on_same_range(self):
        partner = Partner.objects.get(id=self.partner_id)

        assert partner.start_unavailability_datetime is None
        assert partner.end_unavailability_datetime is None
        assert UpdateHistoryRecord.objects.all().count() == 0

        start = datetime(2017, 1, 1)
        end = datetime(2017, 1, 2)

        partner.update_unavailability_range(start, end, 'yandex_login', 'admin', db_name='default')
        partner.update_unavailability_range(start, end, 'yandex_login', 'admin', db_name='default')

        partner = Partner.objects.get(id=self.partner_id)

        assert partner.start_unavailability_datetime == start
        assert partner.end_unavailability_datetime == end
        assert UpdateHistoryRecord.objects.all().count() == 1
        record = UpdateHistoryRecord.objects.all()[0]
        assert record.partner_id == partner.id

    def test_update_twice_on_different_range(self):
        partner = Partner.objects.get(id=self.partner_id)
        assert partner.start_unavailability_datetime is None
        assert partner.end_unavailability_datetime is None
        assert UpdateHistoryRecord.objects.all().count() == 0

        start = datetime(2017, 1, 1)
        end = datetime(2017, 1, 2)
        partner.update_unavailability_range(start, end, 'yandex_login', 'admin', db_name='default')

        partner = Partner.objects.get(id=self.partner_id)
        assert partner.start_unavailability_datetime == start
        assert partner.end_unavailability_datetime == end
        assert UpdateHistoryRecord.objects.all().count() == 1
        record = UpdateHistoryRecord.objects.all()[0]
        assert record.partner_id == partner.id

        other_end = datetime(2017, 1, 3)
        partner.update_unavailability_range(start, other_end, 'yandex_login', 'admin', db_name='default')

        partner = Partner.objects.get(id=self.partner_id)
        assert partner.start_unavailability_datetime == start
        assert partner.end_unavailability_datetime == other_end
        assert UpdateHistoryRecord.objects.all().count() == 2
        record = UpdateHistoryRecord.objects.all()[0]
        assert record.partner_id == partner.id
