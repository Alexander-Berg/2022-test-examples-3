# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six
import threading

from search.martylib.proto.structures import auth_pb2

from search.martylib.test_utils import TestCase
from search.martylib.core.singleton import Singleton, ThreadLocalSingleton, InitSingleton


class TestSingleton(TestCase):
    def test_singleton(self):
        class C(six.with_metaclass(Singleton)):
            pass

        self.assertTrue(C() is C())

    def test_thread_local_singleton(self):
        class TLC(six.with_metaclass(ThreadLocalSingleton)):
            pass

        objects = {
            'a': TLC()
        }

        def f():
            objects['b'] = TLC()
            objects['c'] = TLC()
            self.assertFalse(objects['a'] is objects['b'])
            self.assertTrue(objects['b'] is objects['c'])

        def f2():
            objects['d'] = TLC()
            self.assertFalse(objects['a'] is objects['d'])
            self.assertFalse(objects['b'] is objects['d'])

        thread = threading.Thread(target=f)
        thread.start()
        thread.join()

        thread2 = threading.Thread(target=f2)
        thread2.start()
        thread2.join()

    def test_init_singleton(self):
        class IS(six.with_metaclass(InitSingleton)):
            def __init__(self, x, y=0):
                self.coordinates = (x, y)

        self.assertIs(IS(1), IS(1, 0))
        self.assertIs(IS(1), IS(1, y=0))
        self.assertIs(IS(1, 0), IS(1, y=0))
        self.assertIs(IS(1, 1), IS(1, y=1))

        self.assertIsNot(IS(1), IS(1, y=1))
        self.assertIsNot(IS(1, 0), IS(1, y=1))
        self.assertIsNot(IS(1, 0), IS(1, 1))

        self.assertIs(IS({}), IS(dict()))
        self.assertIs(IS({'x': 1}), IS(dict(x=1)))

        with self.assertRaises(TypeError):
            IS({'nested': {}})

    def test_init_singleton_with_protobuf(self):
        class IS(six.with_metaclass(InitSingleton)):
            def __init__(self, auth):
                self.auth = auth

        self.assertIs(IS(auth_pb2.AuthInfo()), IS(auth_pb2.AuthInfo()))
        self.assertIs(IS(auth_pb2.AuthInfo(login='foo')), IS(auth_pb2.AuthInfo(login='foo')))
        self.assertIs(IS(auth_pb2.AuthInfo(login='foo', email='foo-email')), IS(auth_pb2.AuthInfo(email='foo-email', login='foo')))

        self.assertIsNot(IS(auth_pb2.AuthInfo(login='foo')), IS(auth_pb2.AuthInfo(login='bar')))
