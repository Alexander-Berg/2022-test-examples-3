# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from os.path import dirname, join, realpath
import pytest

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa
os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.rasp.wizards.proxy_api.tests.tester_settings'

from travel.rasp.library.python.common23.tester.transaction_context import transaction_fixture
from travel.rasp.wizards.wizard_lib.utils.resource_extractor import extract_resources

extract_resources(realpath(dirname(dirname(__file__))), 'resfs/file/travel/rasp/wizards/proxy_api')
extract_resources(
    os.path.join(dirname(dirname(realpath(dirname(__file__)))), 'wizard_lib'),
    'resfs/file/travel/rasp/wizards/wizard_lib'
)


@pytest.fixture()
def httpretty(httpretty):
    # httpretty fake sockets cannot be reused, we have to clean the requests pool to create a new socket for each test
    try:
        yield httpretty
    finally:
        # this import requires loaded apps
        from travel.rasp.wizards.proxy_api.lib.requests_pool import default_pool
        if not default_pool.empty:
            default_pool.cleanup()


@pytest.fixture
@transaction_fixture
def rur(request):
    from common.tester.factories import create_currency
    return create_currency(template_whole=u'%d руб.')
