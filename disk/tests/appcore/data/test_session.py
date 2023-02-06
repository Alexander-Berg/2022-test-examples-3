# encoding: UTF-8

import unittest

import flask
from hamcrest import *

from appcore.data.session import SessionSelector
from appcore.data.session import master_session
from appcore.data.session import require_master_session

_SLAVE_SESSION = object()
_MASTER_SESSION = object()


class SessionSelectorTestCase(unittest.TestCase):
    def setUp(self):
        self.app = flask.Flask(__name__)
        self.selector = SessionSelector(
            slave_session_factory=lambda: _SLAVE_SESSION,
            master_session_factory=lambda: _MASTER_SESSION,
        )

    def test_returns_slave_session(self):
        with self.app.app_context():
            session = self.selector()
            assert_that(
                session,
                is_(_SLAVE_SESSION)
            )

    def test_returns_master_session(self):
        with self.app.app_context():
            require_master_session()
            session = self.selector()
            assert_that(
                session,
                is_(_MASTER_SESSION)
            )

    def test_require_master_session(self):
        @master_session
        def session_consumer():
            return self.selector()

        with self.app.app_context():
            assert_that(
                session_consumer(),
                is_(_MASTER_SESSION)
            )
