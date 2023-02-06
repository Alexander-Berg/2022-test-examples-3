# -*- coding: utf-8 -*-
import urlparse

from hamcrest import has_entries
from hamcrest.core.base_matcher import BaseMatcher

from test.helpers.utils import URLHelper


class HasSameURLParams(BaseMatcher):
    def __init__(self, params):
        self.dict_matcher = has_entries(params)
        self.mismatch_descriptions = []

    def matches(self, url, mismatch_description=None):
        parsed = urlparse.urlparse(url)
        actual_params = urlparse.parse_qs(parsed.query)

        return self.dict_matcher.matches(actual_params, mismatch_description=mismatch_description)

    def describe_mismatch(self, item, mismatch_description):
        self.matches(item, mismatch_description)

    def describe_to(self, description):
        self.dict_matcher.describe_to(description)


def has_same_url_params(params):
    """Matches if URL has same querystring as provided URL.

    Example:
          >>> assert_that('https://ya.ru?text=meow&from=7', has_same_url_params('?from=7&text=meow'))
          >>> assert_that('https://ya.ru?text=meow&from=7', has_same_url_params('http://example.com/path?from=7&text=meow'))
          >>> assert_that('https://ya.ru?text=meow&from=7', has_same_url_params({'from': [7], 'text': ['meow']}))

          >>> assert_that('https://ya.ru/nya?sound=meow&type=homyak', has_same_url_params('/path?sound=phyr&type=homyak'))
          E       AssertionError:
          E       Expected: a dictionary containing {'sound': <['phyr']>, 'type': <['homyak']>}
          E            but: value for 'sound' was <['meow']>

    """
    if isinstance(params, basestring):
        params = URLHelper(params).query_string
    return HasSameURLParams(params)
