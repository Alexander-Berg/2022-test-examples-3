# -*- coding: utf-8 -*-
import logging
import mock
import requests
import urlparse

from contextlib import contextmanager
from hamcrest import assert_that, has_entries, equal_to, contains
from httplib import OK

import mpfs.engine.process

from test.base import DiskTestCase
from mpfs.core.services.mulca_service import Mulca, http_client, StidNotFound
from mpfs.config import settings
from mpfs.core.filesystem.hardlinks.common import AbstractLink

# отключаем логирование requests
mpfs.engine.process.get_requests_log().setLevel(logging.WARNING)


@contextmanager
def upload2mulca(data=''):
    base_url = settings.services['mulca']['base_url']
    upload_url = base_url % ('put', 'test_yadisk_data', '')
    mid = None
    try:
        r = requests.post(upload_url, data=data, params={'ns': 'disk'})
        mid = r.text
        print 'Upload data to mulca. Code: %i, mid: %s' % (r.status_code, mid)
        yield mid
    finally:
        if mid:
            remove_url = base_url % ('del', mid, '')
            r = requests.get(remove_url, params={'ns': 'disk'})
            print 'Delete %s form mulca. Code: %i' % (mid, r.status_code)


class MulcaTestCase(DiskTestCase):
    def test_is_file_exist(self):
        mulca = Mulca()
        # файл существует
        with upload2mulca(data='test') as mid:
            assert mulca.is_file_exist(mid) is True
            assert AbstractLink.is_file_in_storage(mid) is True
        # файл не существует
        mid = '320.yadisk.E5531:1345800037437992843858042153'
        assert mulca.is_file_exist(mid) is False
        assert AbstractLink.is_file_in_storage(mid) is False
        # кривой запрос
        bad_mid = ''
        assert mulca.is_file_exist(bad_mid) is False
        assert AbstractLink.is_file_in_storage(bad_mid) is False

    def test_get_file_size(self):
        mulca = Mulca()
        # файл существует
        data = 'test_data'
        with upload2mulca(data=data) as mid:
            assert mulca.get_file_size(mid) == len(data)
        # файл не существует
        mid = '320.yadisk.E5531:1345800037437992843858042153'
        self.assertRaises(StidNotFound, mulca.get_file_size, mid)

    def test_remove(self):
        with mock.patch.object(http_client, 'open_url', return_value=(204, None, None)) as open_url_mock:
            assert Mulca().remove('test_stid') == 204

        open_url_mock.assert_called()
        url = open_url_mock.call_args[0][0]
        parsed_url = urlparse.urlparse(url)
        qs_params = urlparse.parse_qs(parsed_url.query, keep_blank_values=True)

        assert_that(parsed_url.path, equal_to('/gate/del/test_stid'))
        assert_that(qs_params, has_entries(ns=contains('disk'),
                                           service=contains('disk_clean')))

    def test_querystring_for_head_request(self):
        """Проверяем общий для запросов на чтение метод.

        Должны:
          * передаваться обязательные query string параметры запроса
        """
        with mock.patch.object(http_client, 'open_url', return_value=(OK, None, None)) as open_url_mock:
            Mulca()._make_head_request('test_stid')

        open_url_mock.assert_called()
        url = open_url_mock.call_args[0][0]
        parsed_url = urlparse.urlparse(url)
        qs_params = urlparse.parse_qs(parsed_url.query, keep_blank_values=True)

        assert_that(qs_params, has_entries(ns=contains('disk'),
                                           service=contains('disk')))

    def test_mulca_service_use_tvm(self):
        with mock.patch.object(http_client, 'open_url', return_value=(OK, None, None)) as open_url_mock:
            Mulca()._make_head_request('test_stid')
            open_url_mock.assert_called()
            headers = open_url_mock.call_args[1]['headers']
            assert 'X-Ya-Service-Ticket' in headers

        with mock.patch.object(http_client, 'open_url', return_value=(OK, None, None)) as open_url_mock:
            Mulca().remove('test_stid')
            open_url_mock.assert_called()
            headers = open_url_mock.call_args[1]['headers']
            assert 'X-Ya-Service-Ticket' in headers

        with mock.patch.object(http_client, 'open_url', return_value=(OK, None, {'content-length': 1})) as open_url_mock:
            Mulca().get_file_size('test_stid')
            open_url_mock.assert_called()
            headers = open_url_mock.call_args[1]['headers']
            assert 'X-Ya-Service-Ticket' in headers

        with mock.patch.object(http_client, 'open_url', return_value=(OK, None, None)) as open_url_mock:
            Mulca().is_file_exist('test_stid')
            open_url_mock.assert_called()
            headers = open_url_mock.call_args[1]['headers']
            assert 'X-Ya-Service-Ticket' in headers
