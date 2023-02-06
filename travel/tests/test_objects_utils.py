# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.suggests_tasks.suggests.objects_utils import ObjIdConverter, get_obj_type

from common.tester.factories import create_station, create_settlement, create_country
from common.tester.testcase import TestCase


class TestObjIdConverter(object):
    def test_init(self):
        db_id_to_local = {'station': {1: 11, 2: 22}, 'settlement': {3: 33}}
        local_id_to_db = {22: ('station', 2), 11: ('station', 1), 33: ('settlement', 3)}
        id_converter = ObjIdConverter(db_id_to_local_id=db_id_to_local, local_id_to_db_id=local_id_to_db)
        assert id_converter.db_id_to_local_id is db_id_to_local
        assert id_converter.local_id_to_db_id is local_id_to_db
        assert id_converter.freeze is False
        assert id_converter.current_local_id == 0

        id_converter = ObjIdConverter(freeze=True)
        assert id_converter.db_id_to_local_id == {}
        assert id_converter.local_id_to_db_id == {}
        assert id_converter.freeze is True

    def test_get_type_and_db_id(self):
        local_id_to_db = {22: ('station', 2), 11: ('station', 1), 33: ('settlement', 3)}
        id_converter = ObjIdConverter(local_id_to_db_id=local_id_to_db)

        assert id_converter.get_type_and_db_id(22) == ('station', 2)
        assert id_converter.get_type_and_db_id(33) == ('settlement', 3)

    def test_to_dict(self):
        db_id_to_local = {'station': {1: 11, 2: 22}, 'settlement': {3: 33}}
        local_id_to_db = {22: ('station', 2), 11: ('station', 1), 33: ('settlement', 3)}
        id_converter = ObjIdConverter(db_id_to_local_id=db_id_to_local, local_id_to_db_id=local_id_to_db)

        assert id_converter.to_dict() == {
            'local_to_db': local_id_to_db,
            'db_to_local': db_id_to_local,
        }

    def test_from_dict(self):
        db_id_to_local = {'station': {1: 11, 2: 22}, 'settlement': {3: 33}}
        local_id_to_db = {22: ('station', 2), 11: ('station', 1), 33: ('settlement', 3)}
        id_dict = {
            'local_to_db': local_id_to_db,
            'db_to_local': db_id_to_local,
        }

        id_converter = ObjIdConverter.from_dict(id_dict, freeze=False)
        assert id_converter.local_id_to_db_id is local_id_to_db
        assert id_converter.db_id_to_local_id is db_id_to_local
        assert id_converter.freeze is False
        assert id_converter.current_local_id == 33

        id_dict = {
            'local_to_db': {},
            'db_to_local': {},
        }

        id_converter = ObjIdConverter.from_dict(id_dict, freeze=True)
        assert id_converter.local_id_to_db_id == {}
        assert id_converter.db_id_to_local_id == {}
        assert id_converter.freeze is True
        assert id_converter.current_local_id == 0

    def test_get_local_id(self):
        db_id_to_local = {'station': {1: 11, 2: 22}, 'settlement': {3: 33}}
        id_converter = ObjIdConverter(db_id_to_local_id=db_id_to_local, freeze=True)
        assert id_converter.get_local_id(1, 'station') == 11
        assert id_converter.get_local_id(2, 'station') == 22
        assert id_converter.get_local_id(3, 'settlement') == 33
        assert id_converter.get_local_id(10, 'country') is None

        id_converter = ObjIdConverter(db_id_to_local_id=db_id_to_local)
        assert id_converter.get_local_id(10, 'country') == 1
        assert id_converter.current_local_id == 1
        assert id_converter.get_local_id(10, 'station') == 2
        assert id_converter.current_local_id == 2


class TestGetObjType(TestCase):
    def test_get_obj_type(self):
        assert get_obj_type(create_station()) == 'station'
        assert get_obj_type(create_settlement()) == 'settlement'
        assert get_obj_type(create_country()) == 'country'
