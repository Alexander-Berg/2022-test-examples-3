# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

from hamcrest.core.base_matcher import BaseMatcher
from hamcrest.core.helpers.wrap_matcher import wrap_matcher


class JSONMatcher(BaseMatcher):
    def __init__(self, data_matcher):
        self.data_matcher = data_matcher

    def _matches(self, item):
        try:
            json_data = json.loads(item)
        except (TypeError, ValueError):
            return False

        return self.data_matcher.matches(json_data)

    def describe_to(self, description):
        description.append_text('JSON string with ').append_description_of(self.data_matcher)


def has_json(match):
    return JSONMatcher(wrap_matcher(match))
