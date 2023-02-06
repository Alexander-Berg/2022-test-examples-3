# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from six import with_metaclass
from ylog.context import get_log_context

from yabus.common.client import ClientMeta
from yabus.common.fields import ValidationErrorBundle, ValidationWarning


class TestClientMeta(object):
    def test_validation_intercepter(self, caplog):
        class DummyClient(with_metaclass(ClientMeta)):
            warn_exceptions = ValidationWarning

            def endpoints(self):
                raise ValidationErrorBundle(instance=None, exceptions=[ValidationWarning(msg='this is a warning')])

        with caplog.at_level('WARNING', logger='yabus.common.client'):
            assert DummyClient().endpoints() is None

        assert [r.message for r in caplog.records if r.name == 'yabus.common.client'] == ['this is a warning']

    def test_connector_method_log_context(self):
        class DummyClient(with_metaclass(ClientMeta)):
            def endpoints(self):
                assert get_log_context()['method_name'] == 'endpoints'

        DummyClient().endpoints()
