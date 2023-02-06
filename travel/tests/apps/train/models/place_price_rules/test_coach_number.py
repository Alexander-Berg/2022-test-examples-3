# coding: utf8

from __future__ import unicode_literals

from common.apps.train.models import PlacePriceRules
from common.tester.testcase import TestCase


class TestCoachNumbers(TestCase):
    def test_none(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte')

        assert rule.coach_numbers == []

    def test_whitespace(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', _coach_numbers='   ')

        assert rule.coach_numbers == []

    def test_single_coach_number(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', _coach_numbers='    13 ')

        assert rule.coach_numbers == [13]

    def test_multiple_coach_numbers(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', _coach_numbers='  13 ,6,27, 05  ')

        assert rule.coach_numbers == [13, 6, 27, 5]


class TestCheckCoachNumber(TestCase):
    def test_no_coach_numbers(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte')

        assert not rule.check_coach_number(5)

    def test_check_fail(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', _coach_numbers='05, 06, 27')

        assert not rule.check_coach_number(7)

    def test_check_success(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', _coach_numbers='05, 06, 27')

        assert rule.check_coach_number(5)
        assert rule.check_coach_number(6)
        assert rule.check_coach_number(27)
