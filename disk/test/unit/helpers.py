# -*- coding: utf-8 -*-
import unittest

from hamcrest import assert_that, equal_to
from nose_parameterized import parameterized

from test.helpers.matchers import has_same_url_params


class MatchersTestCase(unittest.TestCase):
    @parameterized.expand([
        ('single', 'https://ya.ru/nya?sound=meow', '?sound=meow'),
        ('multiple', 'https://ya.ru/nya?sound=meow&type=homyak', '?sound=meow&type=homyak'),
        ('dict_multiple', 'https://ya.ru/nya?sound=meow&type=homyak', {'type': ['homyak'], 'sound': ['meow']}),
        ('dict_list', 'https://ya.ru/nya?sound=phyr&sound=meow&sound=lol', {'sound': ['phyr', 'meow', 'lol']}),
    ])
    def test_has_url_params(self, case_name, actual_url, expected_params):
        assert_that(actual_url, has_same_url_params(expected_params))

    @parameterized.expand([
        ('no_param', 'https://ya.ru/nya?param2=yes&'),
        ('wrong_value', 'https://ya.ru/nya?sound=meow'),
        ('extra_param', 'https://ya.ru/nya?sound=nya&k=2'),
    ])
    def test_mismatch_for_has_url_params(self, case_name, url):
        matcher = has_same_url_params({'sound': 'nya'})
        assert_that(matcher.matches(url), equal_to(False))
