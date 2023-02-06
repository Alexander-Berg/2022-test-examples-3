# -*- coding: utf-8 -*-
import socket
import uuid
import mock
import pytest

from gevent import monkey, spawn, wait

from test.base_suit import UserTestCaseMixin
from test.parallelly.api.base import ApiTestCase
from mpfs.common.static import tags

from mpfs.engine.process import get_cloud_req_id
from mpfs.engine.request_globals import enable_greenlet_local_data

from mpfs.platform import async_mode


class AsyncApiModeTestCase(ApiTestCase, UserTestCaseMixin):
    """Тесты ручек для работы с БД"""
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        monkey.patch_socket()
        enable_greenlet_local_data()

    def teardown_method(self, method):
        reload(socket)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_local_gevent_storage(self):
        with mock.patch.object(async_mode, 'PLATFORM_ENABLE_ASYNC_MODE', True):
            req_ids = [uuid.uuid4().hex for _ in xrange(5)]

            found_req_ids = []

            def make_request(req_id):
                resp = self.client.request('GET', 'disk/resources', uid=self.uid, query={'path': '/'},
                                           headers={'Yandex-Cloud-Request-ID': req_id})
                self.assertEqual(resp.status_code, 200)
                cloud_req_id = get_cloud_req_id()
                found_req_ids.append(cloud_req_id.split('-')[1])  # ycrid выглядит как mpfs-93481739487-host. а мы достаем значение из центра, которое и сгенерировали для заголовка Yandex-Cloud-Request-ID

            jobs = [spawn(make_request, req_id) for req_id in req_ids]
            wait(jobs)

            for req_id in req_ids:
                if req_id not in found_req_ids:
                    self.assertTrue(False, 'Req ID %s not found in %s' % (req_id, found_req_ids))
