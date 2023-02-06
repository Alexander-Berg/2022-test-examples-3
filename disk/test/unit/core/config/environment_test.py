# -*- coding: utf-8 -*-

from test.unit.base import NoDBTestCase
from mpfs.config.environment import *


class EnvironmentTestCase(NoDBTestCase):
    def test_base(self):
        env_obj = BaseEnvironmentVariable("1 2")
        assert env_obj.settings == "1 2"
        assert env_obj.type_cast is None
        self.assertRaises(TypeError, env_obj.cast, "1")

    def test_string(self):
        env_obj = StringEnvironmentVariable("1 2")
        assert env_obj.settings == "1 2"
        assert env_obj.type_cast == str
        assert env_obj.cast("1") == "1"
        assert env_obj.cast("test") == "test"

    def test_lower(self):
        env_obj = LowerStringEnvironmentVariable("1 2")
        assert env_obj.settings == "1 2"
        assert env_obj.type_cast == str
        assert env_obj.cast("1") == "1"
        assert env_obj.cast("TEsT") == "test"

    def test_int(self):
        env_obj = IntEnvironmentVariable("1 2")
        assert env_obj.settings == "1 2"
        assert env_obj.type_cast == int
        assert env_obj.cast("1") == 1
        self.assertRaises(ValueError, env_obj.cast, "test")

    def test_float(self):
        env_obj = FloatEnvironmentVariable("1 2")
        assert env_obj.settings == "1 2"
        assert env_obj.type_cast == float
        assert env_obj.cast("1.0") == 1.0
        self.assertRaises(ValueError, env_obj.cast, "test")


