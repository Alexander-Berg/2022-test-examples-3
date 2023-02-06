# -*- coding: utf-8 -*-
import pytest
from django.http import HttpRequest

from travel.rasp.api_public.api_public.old_versions.core.decorators import check_country
from travel.rasp.api_public.api_public.old_versions.core.api_errors import ApiError

from common.tester.testcase import TestCase


class TestCheckCountry(TestCase):
    def create_request(self, country):
        request = HttpRequest()
        if country is not None:
            request.GET['country'] = country

        return request

    def test_valid(self):
        # no country specified
        with pytest.raises(ApiError):
            check_country(self.create_request(None))

        # known tld and localization
        request = self.create_request('TR')
        check_country(request)
        assert request.tld == 'com.tr'
        assert request.NATIONAL_VERSION == 'tr'

        # known tld but unknown localization
        request = self.create_request('KZ')
        check_country(request)
        assert request.tld == 'kz'
        assert request.NATIONAL_VERSION == 'ru'

        # unknown country
        request = self.create_request('US')
        with pytest.raises(ApiError):
            check_country(request)
