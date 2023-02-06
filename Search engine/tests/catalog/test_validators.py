from search.martylib.core.exceptions import ValidationError
from search.martylib.test_utils import TestCase
from search.mon.workplace.protoc.structures.catalog_pb2 import AbcServiceFilter
from search.mon.workplace.protoc.structures.catalog_pb2 import Functionality
from search.mon.workplace.protoc.structures.report_spec_pb2 import (
    ReportsSettings,
    WeeklyReportSettings,
    QuarterlyReportSettings,
    GoalsStatusReportSettings,
)
from search.mon.workplace.protoc.structures import vertical_spec_pb2
from search.mon.workplace.src.libs.catalog.validators import (
    validate_url,
    validate_abc_filter,
    validate_owner,
    validate_vertical_item,
    weight_is_valid,
)


class TestWorkplaceValidators(TestCase):
    def test_validate_url(self):
        with self.assertRaises(ValidationError):
            validate_url('ya.ru')
        with self.assertRaises(ValidationError):
            validate_url('kek://lol.mda')

        validate_url('https://mail.yandex-team.ru')

    def test_validate_abc_filter(self):
        value = AbcServiceFilter()
        with self.assertRaises(ValidationError):
            validate_abc_filter(value)
        value.slug = 'test_slug'

        validate_abc_filter(value)

    def test_validate_login(self):
        with self.assertRaises(ValidationError):
            validate_owner('epsilond1@')
        with self.assertRaises(ValidationError):
            validate_owner('my login')
        with self.assertRaises(ValidationError):
            validate_owner('nika,mvel')
        with self.assertRaises(ValidationError):
            validate_owner('nika mvel')

        validate_owner('epsilond1')
        validate_owner('Talion')
        validate_owner('mrt0rtikize')
        validate_owner('new-login')

    def test_validate_vertical_item(self):
        obj = vertical_spec_pb2.CatalogVertical(name='WEB',
                                                target=1000,
                                                weight=1,
                                                kpi=99.9999,
                                                comment='test',
                                                ticket_age=30,
                                                reports_settings=ReportsSettings(
                                                    weekly_report_settings=WeeklyReportSettings(
                                                        enabled=True,
                                                        queue='TEST',
                                                    ),
                                                    quarterly_report_settings=QuarterlyReportSettings(
                                                        enabled=True,
                                                        queue='TEST',
                                                    ),
                                                    goals_status_report_settings=GoalsStatusReportSettings(
                                                        enabled=True,
                                                        queue='TEST',
                                                    ),
                                                ))

        validate_vertical_item(obj)

        tmp = obj
        with self.assertRaises(ValueError):  # ValueError: Value out of range
            tmp.target = -1

        tmp = obj
        tmp.weight = -1
        with self.assertRaises(ValidationError):
            validate_vertical_item(tmp)

        tmp = obj
        tmp.weight = 0.0
        validate_vertical_item(tmp)

        tmp = obj
        tmp.comment = ''
        with self.assertRaises(ValidationError):
            validate_vertical_item(tmp)

        tmp = obj
        tmp.name = ''
        with self.assertRaises(ValidationError):
            validate_vertical_item(tmp)

        tmp = obj
        tmp.reports_settings.goals_status_report_settings.queue = ''
        with self.assertRaisesWithMessage(ValidationError,
                                          message='Report Goals status contains empty fields: [\'queue\'] '):
            validate_vertical_item(tmp)

    def test_functionality_weight(self):
        obj = Functionality(
            name='Test',
            weight=0.5,
            comment='Descr',
            vertical='web',
        )
        tmp = obj
        tmp.weight = 1.1
        with self.assertRaisesWithMessage(ValidationError,
                                          message='weight should be between [0;1]'):
            weight_is_valid(tmp.weight)

    def validate_ticket_age_in_vertical(self):
        with self.assertRaises(ValidationError):
            obj = vertical_spec_pb2.CatalogVertical(name='WEB',
                                                    target=1000,
                                                    weight=1,
                                                    kpi=99.9999,
                                                    comment='test')

            tmp = obj
            validate_vertical_item(tmp)

            tmp.ticket_age = 29

            with self.assertRaises(ValidationError):
                validate_vertical_item(tmp)
