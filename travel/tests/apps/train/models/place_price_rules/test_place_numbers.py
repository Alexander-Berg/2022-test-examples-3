# coding: utf8

from common.apps.train.models import PlacePriceRules
from common.tester.testcase import TestCase


class TestPlaceNumbers(TestCase):
    def test_none(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte')

        assert rule.place_numbers_set == set()

    def test_whitespace(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', place_numbers='   ')

        assert rule.place_numbers_set == set()

    def test_single_number(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', place_numbers='  042 ')

        assert rule.place_numbers_set == {42}

    def test_multiple_number(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', place_numbers='  042 ,98,99, 100  ')

        assert rule.place_numbers_set == {100, 99, 98, 42}

    def test_singe_range(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', place_numbers=' 23-  26')
        assert rule.place_numbers_set == {23, 24, 25, 26}

    def test_incorrect_range(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', place_numbers=' 23-  22')
        assert rule.place_numbers_set == set()

    def test_multiple_ranges(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', place_numbers=' 23-  26, 4 -6')
        assert rule.place_numbers_set == {4, 5, 6, 23, 24, 25, 26}

    def test_complex_with_duplicates(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte',
                                              place_numbers='2, 4, 6, 8, 3 - 5, 4 - 6, 10, 18, 15-15, 13-11')
        assert rule.place_numbers_set == {2, 3, 4, 5, 6, 8, 10, 18}
