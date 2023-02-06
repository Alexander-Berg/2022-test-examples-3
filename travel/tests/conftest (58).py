# coding: utf-8

import pytest
from django.test.client import Client
from django.test.utils import override_settings

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa


@pytest.fixture(scope='function')
def rasp_client():
    from common.models.geo import Settlement

    with override_settings(NATIONAL_VERSION_DEFAULT_SEARCH_CITIES={'ru': (Settlement.MOSCOW_ID,  # для search_samples
                                                                          Settlement.MOSCOW_ID)}), \
            override_settings(CURRENCY_RATES_URL=None):
        yield Client(HTTP_HOST='rasp.yandex.ru')
