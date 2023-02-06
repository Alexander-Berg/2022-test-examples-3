# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.test.client import Client
from django.test.utils import override_settings

from common.views import version


def test_version():
    with override_settings(ROOT_URLCONF=version.__name__, PKG_VERSION='3.1.1',
                           SUBMODULE_STATUS='dd\n22', MIDDLEWARE_CLASSES=[]):
        client = Client()
        response = client.get('/version')
        assert response.status_code == 200
        assert response.content == b'3.1.1\n\ndd\n22'
