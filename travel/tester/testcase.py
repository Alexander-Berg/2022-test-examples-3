# -*- coding: utf-8 -*-
from django.test import testcases


class TestCase(testcases.TestCase):
    @classmethod
    def tearDownClass(cls):
        # Т.к. джанга закрывает коннекты в этом месте, приходится хачить дажнговский TestCase
        # При закрытии коннектов, ломаются питестовские фикстуры с моделями scope=module or class or session.
        cls._rollback_atomics(cls.cls_atomics)

        super(testcases.TestCase, cls).tearDownClass()
