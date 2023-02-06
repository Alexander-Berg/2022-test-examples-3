# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty
import json
from django.conf import settings


HOTEL_CITY_STATIC_PAGE_URL = settings.TRAVEL_API_URL + 'hotels_portal/v1/does_city_static_page_exist'
DEFAULT_HOTEL_CITY_STATIC_PAGE_RESPONSE = {'exists': True}


def mock_does_hotel_city_static_page_exist_request(status=200, body=DEFAULT_HOTEL_CITY_STATIC_PAGE_RESPONSE):
    def request_callback(request, uri, response_headers):
        return [status, response_headers, json.dumps(body)]
    httpretty.register_uri(httpretty.GET, HOTEL_CITY_STATIC_PAGE_URL, body=request_callback)
