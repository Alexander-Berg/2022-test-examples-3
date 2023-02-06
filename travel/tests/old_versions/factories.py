# -*- coding: utf-8 -*-
from django.http import HttpRequest, QueryDict

from travel.rasp.api_public.api_public.old_versions.core.api_context import ApiContext


def create_request(**kwargs):
    request = HttpRequest()
    request.external_api_context = kwargs.get('external_api_context', ApiContext())
    if kwargs.get('GET'):
        request.GET = QueryDict('', mutable=True)
        request.GET.update(kwargs.get('GET'))
    request.NATIONAL_VERSION = kwargs.get('NATIONAL_VERSION', 'ru')
    request.tld = kwargs.get('tld', 'ru')
    request.client_city = None
    return request
