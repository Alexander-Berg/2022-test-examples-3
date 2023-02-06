import functools

import pytest_bdd
import yatest.common

from .steps import *  # noqa


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=yatest.common.source_path('mail/sharpei/tests/integration/feature')
)


@scenario('org_conninfo.feature', 'New organization conninfo')
def test_new_organization_conninfo():
    pass


@scenario('org_conninfo.feature', 'Existing organization conninfo')
def test_existing_organization_conninfo():
    pass
