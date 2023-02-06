# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from rest_framework.exceptions import PermissionDenied

from common.data_api.yasms.client import YaSMSClientError
from travel.rasp.train_api.helpers.error import (
    ErrorType, YASMS_USER_ERROR_DESCRIPTION_DEFAULT, build_error_info, rest_framework_exception_handler,
)
from travel.rasp.train_api.train_partners.im.base import ImNonCriticalError
from travel.rasp.train_api.train_partners.ufs.base import UfsError
from travel.rasp.train_api.train_purchase.core.models import ErrorInfo


@pytest.mark.dbuser
@pytest.mark.parametrize('error,expected', [
    (Exception('foo'), ErrorInfo(type=ErrorType.INTERNAL_ERROR)),
    (UfsError('code', 'descr_id', 'description'), ErrorInfo(
        type=ErrorType.PARTNER_ERROR,
        message='description',
        data={
            'code': 'code',
            'description_id': 'descr_id',
            'description': 'description'
        })),
    (YaSMSClientError(err_msg='1', err_code='2'), ErrorInfo(
        type=ErrorType.YASMS_ERROR,
        message=YASMS_USER_ERROR_DESCRIPTION_DEFAULT,
        data={
            'err_msg': '1',
            'err_code': '2'
        })),
])
def test_build_error_info(error, expected):
    assert build_error_info(error) == expected


@pytest.mark.dbuser
@pytest.mark.parametrize('error,expected,code', [
    (Exception('foo'), {
        'errors': {
            ErrorType.INTERNAL_ERROR: {
                'type': ErrorType.INTERNAL_ERROR,
                'message': None,
                'data': {}
            }
        }
    }, 500),
    (UfsError('code', 'descr_id', 'description'), {
        'errors': {
            ErrorType.PARTNER_ERROR: {
                'type': ErrorType.PARTNER_ERROR,
                'message': 'description',
                'data': {
                    'code': 'code',
                    'description_id': 'descr_id',
                    'description': 'description'
                }
            }
        }
    }, 500),
    (PermissionDenied('Achtung!'), {'detail': 'Achtung!'}, 403),
    (ImNonCriticalError(311, 'message', 'message_params'), {
        'errors': {
            ErrorType.PARTNER_ERROR: {
                'type': ErrorType.PARTNER_ERROR,
                'message': 'message',
                'data': {
                    'code': 311,
                    'message': 'message',
                    'message_params': 'message_params'
                }
            }
        }
    }, 400)
])
def test_rest_framework_exception_handler(error, expected, code):
    response = rest_framework_exception_handler(error, None)
    assert response.data == expected
    assert response.status_code == code
