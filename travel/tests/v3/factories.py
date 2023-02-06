# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from django.http import HttpRequest, QueryDict


def create_request(**kwargs):
    request = HttpRequest()
    if kwargs.get('GET'):
        request.GET = QueryDict('', mutable=True)
        request.GET.update(kwargs.get('GET'))
    request.national_version = kwargs.get('NATIONAL_VERSION', 'ru')
    request.tld = kwargs.get('tld', 'ru')
    request.client_city = None
    return request
