# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from travel.rasp.suggests_tasks.suggests.objects_utils import ObjIdConverter
from travel.rasp.suggests_tasks.suggests.storage import Storage


class TestStorage(object):
    path = 'data_path'
    filename = 'filename'
    data = {'data'}

    def test_init(self):
        storage = Storage(self.path)
        assert storage.data_path == self.path

    def test_save_data(self):
        with mock.patch('__builtin__.open') as m_open_file, \
                mock.patch('msgpack.dump') as m_dump:
            m_file = m_open_file.return_value.__enter__ = mock.Mock()

            storage = Storage(self.path)
            storage._save_data(self.filename, self.data)

            m_open_file.assert_called_once_with('{}/{}'.format(self.path, self.filename), 'w')
            m_dump.assert_called_once_with(self.data, m_file.return_value, encoding='utf8')

    def test_load_data(self):
        with mock.patch('__builtin__.open') as m_open_file, \
                mock.patch('msgpack.load', return_value=self.data) as m_load:
            storage = Storage(self.path)
            data = storage._load_data(self.filename)
            assert data is data
            m_open_file.assert_called_once_with('{}/{}'.format(self.path, self.filename))
            m_load.assert_called_once_with(m_open_file.return_value, encoding='utf8', use_list=False)

    def test_load_id_converter(self):
        with mock.patch.object(Storage, '_load_data', autospec=True) as m_load_data, \
                mock.patch.object(ObjIdConverter, 'from_dict') as m_obj_conv:
            storage = Storage(self.path)
            id_converter = storage.load_id_converter(freeze=True, can_create=False)
            m_load_data.assert_called_once_with(storage, storage.id_conv_file)
            m_obj_conv.assert_called_once_with(m_load_data.return_value, freeze=True)
            assert id_converter is m_obj_conv.return_value

        with pytest.raises(IOError):
            with mock.patch.object(Storage, '_load_data', side_effect=IOError) as m_load_data:
                storage = Storage(self.path)
                id_converter = storage.load_id_converter(freeze=True, can_create=False)

        with mock.patch.object(Storage, '_load_data', autospec=True, side_effect=[IOError, mock.Mock()]) as m_load_data, \
                mock.patch('travel.rasp.suggests_tasks.suggests.storage.ObjIdConverter.__init__', autospec=True, side_effect=ObjIdConverter.__init__) as m_storage:
            instance = m_storage.return_value
            instance.suggests_data = mock.Mock()
            storage = Storage(self.path)
            id_converter = storage.load_id_converter(freeze=True)
            m_load_data.assert_called_once_with(storage, storage.id_conv_file)
            assert m_storage.call_count == 1
            assert isinstance(m_storage.call_args[0][0], ObjIdConverter)
            assert id_converter is m_storage.call_args[0][0]

    def test_save_objs_data(self):
        data = {}
        expected_data = {}
        data['titles'] = {'ru': {'all': {'title': {(11, False), (22, False)}}}}
        expected_data['titles'] = {'ru': {'all': {'title': [(11, False), (22, False)]}}}
        with mock.patch.object(Storage, '_save_data', autospec=True) as m_save_data:
            storage = Storage(self.path)
            id_converter = storage.save_objs_data(data)  # noqa
            m_save_data.assert_called_once_with(storage, storage.objs_data_file, expected_data)

    def test_save_id_converter(self):
        with mock.patch.object(Storage, '_save_data', autospec=True) as m_save_data, \
                mock.patch.object(ObjIdConverter, 'to_dict', autospec=True) as m_to_dict:
            storage = Storage(self.path)
            id_converter = ObjIdConverter()
            storage.save_id_converter(id_converter)
            m_to_dict.assert_called_once_with(id_converter)
            m_save_data.assert_called_once_with(storage, storage.id_conv_file, m_to_dict.return_value)

    def test_load_ttypes(self):
        with mock.patch.object(Storage, '_load_data', autospec=True) as m_load_data:
            storage = Storage(self.path)
            data = storage.load_ttypes()
            m_load_data.assert_called_once_with(storage, storage.t_types_file)
            assert data is m_load_data.return_value

    def test_save_ttypes(self):
        data = {
            'stations': {
                1: [1, 2],
            },
            'settlements': {
                2: [4, 5],
            }
        }
        with mock.patch.object(Storage, '_save_data', autospec=True) as m_save_data:
            storage = Storage(self.path)
            storage.save_ttypes(data)
            m_save_data.assert_called_once_with(storage, storage.t_types_file, data)

    def test_load_stat(self):
        with mock.patch.object(Storage, '_load_data', autospec=True) as m_load_data:
            storage = Storage(self.path)
            data = storage.load_stat()
            m_load_data.assert_called_once_with(storage, storage.stat_file)
            assert data is m_load_data.return_value

    def test_save_stat(self):
        data = {
            'by_obj': {},
            'routes': {},
        }
        with mock.patch.object(Storage, '_save_data', autospec=True) as m_save_data:
            storage = Storage(self.path)
            storage.save_stat(data)
            m_save_data.assert_called_once_with(storage, storage.stat_file, data)

    def test_load_stat_converted(self):
        with mock.patch.object(Storage, '_load_data', autospec=True) as m_load_data:
            storage = Storage(self.path)
            data = storage.load_stat_converted()
            m_load_data.assert_called_once_with(storage, storage.stat_converted_file)
            assert data is m_load_data.return_value

    def test_save_stat_converted(self):
        data = {
            'by_obj': {},
            'routes': {},
        }
        with mock.patch.object(Storage, '_save_data', autospec=True) as m_save_data:
            storage = Storage(self.path)
            storage.save_stat_converted(data)
            m_save_data.assert_called_once_with(storage, storage.stat_converted_file, data)

    def test_objs_data(self):
        with mock.patch.object(Storage, '_load_data', autospec=True) as m_load_data:
            storage = Storage(self.path)
            data = storage.load_objs_data()
            m_load_data.assert_called_once_with(storage, storage.objs_data_file)
            assert data is m_load_data.return_value
