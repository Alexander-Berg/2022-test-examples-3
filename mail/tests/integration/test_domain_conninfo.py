import functools

import pytest_bdd
import yatest.common

from .steps import *  # noqa


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=yatest.common.source_path('mail/sharpei/tests/integration/feature')
)


@scenario('domain_conninfo.feature', 'New domain conninfo')
def test_new_domain_conninfo():
    pass


@scenario('domain_conninfo.feature', 'Existing domain conninfo')
def test_existing_domain_conninfo():
    pass


@scenario('domain_conninfo.feature', 'Absent domain conninfo')
def test_absent_domain_conninfo():
    pass
