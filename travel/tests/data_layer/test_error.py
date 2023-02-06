# coding: utf-8
from __future__ import unicode_literals

import pytest
from rest_framework.exceptions import PermissionDenied

from travel.rasp.morda_backend.morda_backend.data_layer.error import rest_framework_exception_handler


@pytest.mark.dbuser
@pytest.mark.parametrize('error,expected,code', [
    (Exception('foo'), {
        'errors': {
            'internal_error': {
                'type': 'internal_error',
                'message': 'foo',
                'data': {}
            }
        }
    }, 500),
    (PermissionDenied('Achtung!'), {'detail': 'Achtung!'}, 403),
])
def test_rest_framework_exception_handler(error, expected, code):
    response = rest_framework_exception_handler(error, None)
    assert response.data == expected
    assert response.status_code == code
