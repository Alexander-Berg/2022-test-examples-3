# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import


def test_shutdown(client):
    response = client.post('/shutdown')
    assert response.status_code == 200

    response = client.get('/ping')
    assert response.status_code == 503
