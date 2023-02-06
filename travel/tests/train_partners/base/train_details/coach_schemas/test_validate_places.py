# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.dynamic_settings.default import conf
from common.tester.testcase import TestCase
from travel.rasp.train_api.train_partners.base.factories.coach_schemas import (
    create_usual_platzkarte_schema, create_kryukov_compartment_schema, create_kryukov_platzkarte_schema,
    create_usual_compartment_schema
)
from travel.rasp.train_api.train_partners.base.train_details.coach_schemas import validate_places

PLATZKARTE = 'platzkarte'
COMPARTMENT = 'compartment'


class TestValidatePlaces(TestCase):
    """
    Для тестов используются укороченные схемы вагонов. Схемы нарисованы в файле ./factories.py
    """
    @classmethod
    def setUpClass(cls):
        super(TestValidatePlaces, cls).setUpClass()

        cls._orig_proto_setting_value = conf.TRAIN_BACKEND_USE_PROTOBUFS['schemas']
        conf.TRAIN_BACKEND_USE_PROTOBUFS['schemas'] = False

        cls.usual_platzkarte_schema = create_usual_platzkarte_schema()
        cls.kryukov_platzkarte_schema = create_kryukov_platzkarte_schema()

        cls.usual_compartment_schema = create_usual_compartment_schema()
        cls.kryukov_compartment_schema = create_kryukov_compartment_schema()

    @classmethod
    def tearDownClass(cls):
        conf.TRAIN_BACKEND_USE_PROTOBUFS['schemas'] = cls._orig_proto_setting_value
        super(TestValidatePlaces, cls).tearDownClass()

    def test_platzkarte_1_2_3_4(self):
        place_numbers = {1, 2, 3, 4}

        assert validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_9_10_17_18(self):
        place_numbers = {9, 10, 17, 18}

        assert validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_1_2_19_20(self):
        place_numbers = {1, 2, 19, 20}

        assert not validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_11_12_13_14_usual(self):
        place_numbers = {11, 12, 13, 14}

        assert validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_11_12_13_14_kryukov(self):
        place_numbers = {11, 12, 13, 14}

        assert validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_81_82(self):
        place_numbers = {81, 82}

        assert not validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert not validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_compartment_1_2_3_4(self):
        place_numbers = {1, 2, 3, 4}

        assert validate_places(self.usual_compartment_schema, place_numbers)[0]
        assert validate_places(self.kryukov_compartment_schema, place_numbers)[0]

    def test_compartment_9_10_11_12(self):
        place_numbers = {9, 10, 11, 12}

        assert not validate_places(self.usual_compartment_schema, place_numbers)[0]
        assert validate_places(self.kryukov_compartment_schema, place_numbers)[0]

    def test_compartment_81_82(self):
        place_numbers = {81, 82}

        assert not validate_places(self.usual_compartment_schema, place_numbers)[0]
        assert not validate_places(self.kryukov_compartment_schema, place_numbers)[0]


class TestValidatePlacesWithProtobufs(TestCase):
    """
    Для тестов используются укороченные схемы вагонов. Схемы нарисованы в файле ./factories.py
    """
    @classmethod
    def setUpClass(cls):
        super(TestValidatePlacesWithProtobufs, cls).setUpClass()

        cls._orig_proto_setting_value = conf.TRAIN_BACKEND_USE_PROTOBUFS['schemas']
        conf.TRAIN_BACKEND_USE_PROTOBUFS['schemas'] = True

        cls.usual_platzkarte_schema = create_usual_platzkarte_schema()
        cls.kryukov_platzkarte_schema = create_kryukov_platzkarte_schema()

        cls.usual_compartment_schema = create_usual_compartment_schema()
        cls.kryukov_compartment_schema = create_kryukov_compartment_schema()

    @classmethod
    def tearDownClass(cls):
        conf.TRAIN_BACKEND_USE_PROTOBUFS['schemas'] = cls._orig_proto_setting_value
        super(TestValidatePlacesWithProtobufs, cls).tearDownClass()

    def test_platzkarte_1_2_3_4(self):
        place_numbers = {1, 2, 3, 4}

        assert validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_9_10_17_18(self):
        place_numbers = {9, 10, 17, 18}

        assert validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_1_2_19_20(self):
        place_numbers = {1, 2, 19, 20}

        assert not validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_11_12_13_14_usual(self):
        place_numbers = {11, 12, 13, 14}

        assert validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_11_12_13_14_kryukov(self):
        place_numbers = {11, 12, 13, 14}

        assert validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_platzkarte_81_82(self):
        place_numbers = {81, 82}

        assert not validate_places(self.usual_platzkarte_schema, place_numbers)[0]
        assert not validate_places(self.kryukov_platzkarte_schema, place_numbers)[0]

    def test_compartment_1_2_3_4(self):
        place_numbers = {1, 2, 3, 4}

        assert validate_places(self.usual_compartment_schema, place_numbers)[0]
        assert validate_places(self.kryukov_compartment_schema, place_numbers)[0]

    def test_compartment_9_10_11_12(self):
        place_numbers = {9, 10, 11, 12}

        assert not validate_places(self.usual_compartment_schema, place_numbers)[0]
        assert validate_places(self.kryukov_compartment_schema, place_numbers)[0]

    def test_compartment_81_82(self):
        place_numbers = {81, 82}

        assert not validate_places(self.usual_compartment_schema, place_numbers)[0]
        assert not validate_places(self.kryukov_compartment_schema, place_numbers)[0]
