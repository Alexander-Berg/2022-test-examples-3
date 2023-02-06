# coding: utf8
from django.test.client import RequestFactory


def create_request(url='', attribs=None, headers=None):
    attribs = attribs if attribs else {}
    attribs.setdefault('tld', 'ru')
    attribs.setdefault('national_version', 'ru')
    attribs.setdefault('language_code', 'ru')

    request = RequestFactory().get(url)
    for atrr_name, attr_value in attribs.items():
        setattr(request, atrr_name, attr_value)

    if headers:
        for key, value in headers.items():
            # Django конвертит все http-хедеры в вид HTTP_SOME_HEADER и кладет в META
            key = key.replace('-', '_').upper()
            request.META['HTTP_' + key] = value

    return request
