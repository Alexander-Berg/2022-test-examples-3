# encoding: UTF-8

import unittest

import flask
import mock
from hamcrest import *

from appcore.plugin import PluginBase


class PluginbaseTestCase(unittest.TestCase):
    def setUp(self):
        self.app = flask.Flask(__name__)

    def test_explicit_initialization(self):
        with mock.patch.object(PluginBase, '_do_init_app'):
            plugin = PluginBase()

            plugin._do_init_app.assert_not_called()

            plugin.init_app(self.app)

            plugin._do_init_app.assert_called_once()

            assert_that(
                plugin,
                has_properties(
                    app=is_(self.app),
                )
            )

    def test_implicit_initialization(self):
        with mock.patch.object(PluginBase, '_do_init_app'):
            plugin = PluginBase(self.app)

            plugin._do_init_app.assert_called_once()

            assert_that(
                plugin,
                has_properties(
                    app=is_(self.app),
                )
            )

    def test_raises_not_implemented(self):
        assert_that(
            calling(PluginBase).with_args(self.app),
            raises(NotImplementedError),
        )
