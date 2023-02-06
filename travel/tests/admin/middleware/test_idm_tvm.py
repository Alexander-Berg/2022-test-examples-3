# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from django.test.client import RequestFactory

from common.tester.utils.replace_setting import replace_setting
from travel.rasp.admin.admin.middleware.idm_tvm import IdmTvmMiddleware


def check_service_ticket(self, service_ticket):
    ticket = mock.Mock()
    ticket.src = service_ticket
    return ticket


def test_idm_tvm_middleware():
    factory = RequestFactory()
    middleware = IdmTvmMiddleware()
    with replace_setting('TVM_IDM_CLIENT_IDS', {123}), \
            mock.patch('travel.library.python.tvm_ticket_provider.FakeTvmTicketProvider.check_service_ticket',
                       autospec=True, side_effect=check_service_ticket):
        request = factory.get('/dostup/info', HTTP_X_YA_SERVICE_TICKET=123)
        response = middleware.process_request(request)
        assert response is None

        request = factory.get('/dostup/info', HTTP_X_YA_SERVICE_TICKET=456)
        response = middleware.process_request(request)
        assert response.status_code == 403

        request = factory.get('/dostup/info')
        response = middleware.process_request(request)
        assert response.status_code == 403

        request = factory.get('/admin/', HTTP_X_YA_SERVICE_TICKET=456)
        response = middleware.process_request(request)
        assert response is None

        request = factory.get('/admin/')
        response = middleware.process_request(request)
        assert response is None
