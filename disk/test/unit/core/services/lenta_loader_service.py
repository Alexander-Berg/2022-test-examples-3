# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import mock

from mpfs.core.services.lenta_loader_service import LentaLoaderService


def test_lenta_loader_service_open_url_passes_yandex_cloud_request_id():
    service = LentaLoaderService()
    with mock.patch('urllib2.urlopen') as mocked_urlopen:
        with mock.patch(
            'mpfs.engine.process.get_cloud_req_id',
            return_value=b'Place your Ad here.'
        ):
            with mock.patch(
                'mpfs.engine.process.get_tvm_ticket',
                return_value=None
            ):
                service.open_url('http://www.example.com')
                mocked_urlopen.assert_called_once()
                args, kwargs = mocked_urlopen.call_args
                (request,) = args
                assert 'Yandex-cloud-request-id' in request.headers
                assert request.headers['Yandex-cloud-request-id'] == b'Place your Ad here.'
