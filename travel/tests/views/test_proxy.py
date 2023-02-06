# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from django.test.client import Client


@pytest.mark.dbuser
def test_proxy_view_no_content():
    response = Client().get('/api/proxy/?transport=suburban&point=departure_point&point_to=arrival_point')
    assert response.status_code == 204
