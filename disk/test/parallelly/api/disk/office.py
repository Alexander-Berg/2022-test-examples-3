# -*- coding: utf-8 -*-
import base64
import json
import random

import mock
import pytest
import re

from copy import deepcopy

from mpfs.core.albums.static import GeneratedAlbumType
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from test.parallelly.social_suit import CommonSocialMethods
from mpfs.common.util import from_json, to_json
from mpfs.common.static import tags


class OrganizationsTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'POST'

    def setup_method(self, method):
        super(OrganizationsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_only_office(self):
        with self.specified_client(scopes=['cloud_api:disk.read']), \
             mock.patch('mpfs.platform.v1.disk.handlers.OnlyOfficeCallbackHandler.request_service') as callback:
            status = random.randrange(10)
            key = 'key%i' % random.randrange(10)
            oid = 'oid%i' % random.randrange(10)
            token = base64.b64encode(
                to_json({'key': key, 'raw_resource_id': 'raw_resource_id%s' % random.randrange(10)}))
            data = {'key': key, 'status': status, 'url': 'url'}
            response = self.client.post('disk/only-office/%s' % key, uid=self.uid, data=data,
                                        query={'token': token, 'uid': self.uid, 'subdomain': 'domain'})
            self.assertEqual(response.status, 200, response.result)
            assert callback.called
            assert response.status == 200
            call_args = callback.call_args.args[0]
            assert self.uid in call_args
            assert 'subdomain=domain' in call_args
            assert token in call_args
            assert 'oid' not in call_args
            assert from_json(callback.call_args[1]['data']) == data
            response = self.client.post('disk/only-office/%s' % key, uid=self.uid, data=data,
                             query={'token': token, 'uid': self.uid, 'oid': oid, 'subdomain': 'domain'})
            assert callback.called
            assert response.status == 200
            assert oid in callback.call_args.args[0]
