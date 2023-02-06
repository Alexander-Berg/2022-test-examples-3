# -*- coding: utf-8 -*-
import mock
import pytest

from nose_parameterized import parameterized
from lxml import etree

from mpfs.core.factory import get_resource
from mpfs.metastorage.mongo.collections.all_user_data import AllUserDataCollection
from test.base import DiskTestCase
from test.conftest import INIT_USER_IN_POSTGRES
from test.fixtures.kladun import KladunMocker
from test.helpers.stubs.services import MulcaServiceStub


class ImageSizeMixin(object):

    def _check_width_height(self, uid, _path, width, height, angle):
        db_record = AllUserDataCollection().find_one_on_uid_shard(uid, {'uid': uid, 'key': _path}).record
        assert 'width' in db_record['data']
        assert 'height' in db_record['data']
        assert 'angle' in db_record['data']
        assert width == db_record['data']['width']
        assert height == db_record['data']['height']
        assert angle == db_record['data']['angle']
        resource = get_resource(uid, _path)
        assert 'width' in resource.meta
        assert 'height' in resource.meta
        assert 'angle' in resource.meta
        assert width == resource.meta['width']
        assert height == resource.meta['height']
        assert angle == resource.meta['angle']

    def _make_kladun_callbacks_with_image_size(self, uid, oid, width, height, angle):
        body_1 = open('fixtures/xml/kladun_store_1.xml').read()
        body_2 = open('fixtures/xml/kladun_store_2.xml').read()
        body_3_dom = etree.fromstring(open('fixtures/xml/kladun_store_3.xml').read())

        if width is not None:
            body_3_dom.find('stages').find('generate-image-one-preview').find('result').set('original-width', str(width))
        if height is not None:
            body_3_dom.find('stages').find('generate-image-one-preview').find('result').set('original-height', str(height))
        if angle is not None:
            body_3_dom.find('stages').find('generate-image-one-preview').find('result').set('rotate-angle', str(angle))

        KladunMocker().do_three_callbacks(uid, oid, body_1, body_2, etree.tostring(body_3_dom))


class ImageSizeSuit(ImageSizeMixin, DiskTestCase):
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_store_with_image_size(self):
        width = 569
        height = 1782
        angle = 90

        path = '/disk/test.jpg'
        path2 = '/disk/test2.jpg'
        path3 = '/disk/test3.jpg'
        path4 = '/disk/test4.jpg'
        opts = {'uid': self.uid, 'path': path}
        res = self.json_ok('store', opts)
        oid = res['oid']
        self._make_kladun_callbacks_with_image_size(self.uid, oid, width, height, angle)
        resp = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert 'DONE' == resp['status']

        # проверяем, что width / height установились при загрузке файла
        self._check_width_height(self.uid, path, width, height, angle)

        # проверяряем, что при хардлинке сохраняется width / height
        self.hardlink_file(self.uid, path, path2)
        self._check_width_height(self.uid, path2, width, height, angle)

        # проверяряем, что при копировании и перемещении сохраняется width / height
        self.async_ok('async_copy', {'uid': self.uid, 'src': path, 'dst': path3})
        self._check_width_height(self.uid, path3, width, height, angle)
        self.async_ok('async_move', {'uid': self.uid, 'src': path, 'dst': path4})
        self._check_width_height(self.uid, path4, width, height, angle)

    def test_store_image_size_when_regenerating_preview(self):
        width = 569
        height = 1782
        angle = 270
        path = '/disk/test.jpg'
        self.upload_file(self.uid, path)

        resource = get_resource(self.uid, path)
        meta = resource.meta
        assert 'width' not in meta
        assert 'height' not in meta
        assert 'angle' not in meta

        kladun_response = '''
<regenerate-preview>
	<mulca-id>320.yadisk:preview.E1572:297259517234115421721216885512</mulca-id>
	<image-info format="jpeg" width="1280" height="720" original-width="{width}" original-height="{height}" rotate-angle="{angle}"></image-info>
</regenerate-preview>'''.format(width=width, height=height, angle=angle)
        self.stubs_manager.disable_stubs(scope='function')
        with MulcaServiceStub(), \
             mock.patch('mpfs.engine.http.client.open_url', return_value=kladun_response) as open_url_stub:
                resource.regenerate_preview()
        assert 1 == open_url_stub.call_count
        self._check_width_height(self.uid, path, width, height, angle)

    invalid_cases = [(1, 0), (0, 1), (1, None), (None, 1), (0, 0), (None, None), (1, -1), ('a', 1)]

    @parameterized.expand(invalid_cases)
    def test_does_not_store_invalid_or_incomplete_width_height(self, width, height):
        path = '/disk/test.jpg'
        opts = {'uid': self.uid, 'path': path}
        res = self.json_ok('store', opts)
        oid = res['oid']

        self._make_kladun_callbacks_with_image_size(self.uid, oid, width, height, None)
        resp = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert 'DONE' == resp['status']
        db_record = AllUserDataCollection().find_one_on_uid_shard(self.uid, {'uid': self.uid, 'key': path}).record
        assert 'width' not in db_record['data']
        assert 'height' not in db_record['data']

    @parameterized.expand(invalid_cases)
    def test_does_not_store_invalid_or_incomplete_width_height_when_regenerating_preview(self, width, height):
        path = '/disk/test.jpg'
        self.upload_file(self.uid, path)

        resource = get_resource(self.uid, path)
        meta = resource.meta
        assert 'width' not in meta
        assert 'height' not in meta
        assert 'angle' not in meta

        kladun_response = '''
<regenerate-preview>
    <mulca-id>320.yadisk:preview.E1572:297259517234115421721216885512</mulca-id>
    <image-info format="jpeg" width="1280" height="720" original-width="{width}" original-height="{height}"></image-info>
</regenerate-preview>'''.format(width=width, height=height)
        self.stubs_manager.disable_stubs(scope='function')
        with MulcaServiceStub(), \
             mock.patch('mpfs.engine.http.client.open_url', return_value=kladun_response) as open_url_stub:
                resource.regenerate_preview()
        assert 1 == open_url_stub.call_count
        db_record = AllUserDataCollection().find_one_on_uid_shard(self.uid, {'uid': self.uid, 'key': path}).record
        assert 'width' not in db_record['data']
        assert 'height' not in db_record['data']
