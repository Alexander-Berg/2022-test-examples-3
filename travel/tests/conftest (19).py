# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from django.db import transaction

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa
from travel.library.python.resource import extract_resources


pytest_plugins.extend(['travel.rasp.library.python.common23.tester.plugins.rasp_deprecation', 'common.tester.yaml_fixtures'])


@pytest.hookimpl(tryfirst=True)  # extracting resource should be extracted before common.tester.yaml_fixtures
def pytest_configure(config):
    extract_resources('travel/rasp/admin/cysix/tests/', strip_prefix=False)
    extract_resources('travel/rasp/admin/tester/fixtures/')


@pytest.yield_fixture(scope='class')
def setup_cysix_test_case():
    from cysix.tests.utils import CysixTestCase

    with transaction.atomic():
        CysixTestCase.setUpTestData()

        yield CysixTestCase

        transaction.set_rollback(True)
