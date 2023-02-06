# coding: utf8

from __future__ import unicode_literals

from common.apps.train.models import PlacePriceRules
from common.tester.testcase import TestCase


class TestCheckTrainNumber(TestCase):
    def test_empty(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte')

        assert not rule.check_train_number('123Б')

    def test_check_fail(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', train_number='(123[БФГ])|(456[ЦЩ])')

        assert not rule.check_train_number('123П')
        assert not rule.check_train_number('321Б')

    def test_check_success(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', train_number='(123[БФГ])|(456[ЦЩ])')

        assert rule.check_train_number('123Б')
        assert rule.check_train_number('123Ф')
        assert rule.check_train_number('123Г')
        assert rule.check_train_number('456Ц')
        assert rule.check_train_number('456Щ')
