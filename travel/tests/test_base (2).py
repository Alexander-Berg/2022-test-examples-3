# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

import mock
import pytest
from mongoengine import EmbeddedDocument, Document

from travel.rasp.library.python.common23.db.mongo.base import ensure_indexes, RaspDocument, MongoengineDeclarationError


class TestEnsureIndexes(object):
    @staticmethod
    def _build_test_module(base_cls, meta_data=None):
        class TestClass(base_cls):
            ensure_indexes = mock.Mock()
            meta = meta_data or {}

        class Module(object):
            some_class = TestClass

        return Module

    def test_embedded_document(self):
        module = self._build_test_module(EmbeddedDocument)
        ensure_indexes(module)
        assert not module.some_class.ensure_indexes.called

    def test_document(self):
        module = self._build_test_module(Document)
        with pytest.raises(MongoengineDeclarationError):
            ensure_indexes(module)
        assert not module.some_class.ensure_indexes.called

    def test_rasp_document(self):
        module = self._build_test_module(RaspDocument)
        ensure_indexes(module)
        assert module.some_class.ensure_indexes.called

    def test_rasp_document_migration_disabled(self):
        module = self._build_test_module(RaspDocument)
        os.environ['RASP_MIGRATION_ALLOWED'] = 'false'
        ensure_indexes(module)
        os.environ['RASP_MIGRATION_ALLOWED'] = '1'
        assert not module.some_class.ensure_indexes.called

    def test_abstract_document(self):
        module = self._build_test_module(RaspDocument, meta_data={'abstract': True})
        ensure_indexes(module)
        assert not module.some_class.ensure_indexes.called

    def test_additional_allowed_base_class(self):
        class MyBaseClass(Document):
            meta = {'abstract': True, 'auto_create_index': False}
        module = self._build_test_module(MyBaseClass, meta_data={'abstract': False})
        ensure_indexes(module, additional_allowed_base_classes=(MyBaseClass,))
        assert module.some_class.ensure_indexes.called

    def test_check_auto_create_index_in_additional_allowed_base_class(self):
        class MyBaseClass(Document):
            meta = {'abstract': True}
        module = self._build_test_module(MyBaseClass, meta_data={})
        with pytest.raises(MongoengineDeclarationError):
            ensure_indexes(module, additional_allowed_base_classes=(MyBaseClass,))
        assert not module.some_class.ensure_indexes.called

    def test_check_right_subclass_of_additional_allowed_base_class(self):
        class MyBaseClass(object):
            pass
        module = self._build_test_module(MyBaseClass)
        ensure_indexes(module)
        with pytest.raises(MongoengineDeclarationError):
            ensure_indexes(module, additional_allowed_base_classes=(MyBaseClass,))
        assert not module.some_class.ensure_indexes.called
