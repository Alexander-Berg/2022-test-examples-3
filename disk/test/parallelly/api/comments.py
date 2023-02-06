# -*- coding: utf-8 -*-
from mpfs.platform.v1.comments.handlers import CommentsProxyHandler
from test.parallelly.api.base import ApiTestCase
from mpfs.common.static import tags
from mpfs.core.services import comments_service


class CommentsProxyTestCase(ApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    uid = 4002630069
    login = ""

    def setup_method(self, method):
        super(CommentsProxyTestCase, self).setup_method(method)
        comments_service.comments.log = self.log

    def test_mocked_response(self):
        entity_url = 'commentaries/mpfs-test/authentication/comments'
        response = self.client.request('GET', entity_url)

        self.assertEqual(200, response.status_code)
        self.assertEqual(CommentsProxyHandler.mocked_response, response.content)
