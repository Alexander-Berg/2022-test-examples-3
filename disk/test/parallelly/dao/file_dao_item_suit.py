# -*- coding: utf-8 -*-
from test.base import DiskTestCase

from mpfs.core.filesystem.dao.resource import ResourceDAO
from mpfs.metastorage.mongo.util import decompress_data


class FileDAOItemTestCase(DiskTestCase):

    def test_custom_setprop_fields(self):
        """
        Тест проверяет, что кастомное поле, которое мы положили в базу, вернется нам в zdata.setprop
        """

        file_path = '/disk/test_file.txt'
        self.upload_file(self.uid, file_path)

        self.json_ok('setprop', {'uid': self.uid, 'path': file_path, 'special_nonexistent_field': 'value123'})
        mongo_dict = ResourceDAO().find_one({'uid': self.uid, 'path': file_path})

        zdata = decompress_data(mongo_dict['zdata'])
        assert 'special_nonexistent_field' in zdata['setprop']
        assert zdata['setprop']['special_nonexistent_field'] == 'value123'

    def test_ext_coordinates(self):
        """
        Тест проверяет, что поле возвращается и мы с ним можем работать
        """
        file_path = '/disk/test_file.jpg'
        self.upload_file_with_coordinates(self.uid, file_path)
        result = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ','})
        assert 'meta' in result
        assert 'ext_coordinates' in result['meta']
        coordinates = result['meta']['ext_coordinates'].split(',')
        assert len(coordinates) == 2
