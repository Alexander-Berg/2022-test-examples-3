# coding: utf-8

import logging

from pytest_bdd import parsers

from tests_common.pytest_bdd import then
from .folder import parse_counter

from hamcrest import (
    assert_that,
    has_properties,
)

log = logging.getLogger(__name__)


ATTACH_COUNTERS_RE = r'''
user has
\s*
(?:(?P<has_attaches_count>({0})) messages? with attaches)
(?:
(,| and)?\s*
(?:
(?:(?P<has_attaches_unseen>({0})) unseen)|
(?:(?P<has_attaches_seen>({0})) seen)
)
)*
'''.format(r'zero|no|not|one|"\d+"').strip().replace('\n', '')


@then(ATTACH_COUNTERS_RE, parse_builder=parsers.re)
def step_check_attach_counters(context):
    kwargs = context.args
    counters = {}
    for k in [
            'has_attaches_count',
            'has_attaches_seen',
            'has_attaches_unseen']:
        val = parse_counter(kwargs[k])
        if val is not None:
            counters[k] = val

    assert_that(context.qs.attach_counters(), has_properties(counters))
