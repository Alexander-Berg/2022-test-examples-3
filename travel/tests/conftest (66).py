# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from os.path import dirname, join, realpath
import pytest

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa
from common.tester.transaction_context import transaction_fixture
from common.tester.utils.datetime import replace_now
from travel.rasp.wizards.wizard_lib.utils.resource_extractor import extract_resources


extract_resources(realpath(dirname(dirname(__file__))), 'resfs/file/travel/rasp/wizards/train_wizard_api')
extract_resources(
    os.path.join(dirname(dirname(realpath(dirname(__file__)))), 'wizard_lib'),
    'resfs/file/travel/rasp/wizards/wizard_lib'
)


@pytest.fixture
def fixed_now():
    with replace_now('2000-01-01'):
        yield


@pytest.fixture(autouse=True)
def deluxe_train_without_cache():
    from common.models.schedule import DeLuxeTrain

    DeLuxeTrain._number2deluxe_train = {}
    yield


@pytest.fixture
@transaction_fixture
def rur(request):
    from common.tester.factories import create_currency
    return create_currency(template_whole=u'%d руб.')
