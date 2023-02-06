# encoding: UTF-8

import unittest

import flask
from hamcrest import *

from appcore.injection import Injected, InjectionError, inject, \
    register_injection


class ClassWithInjection(object):
    custom_ext = Injected('custom_ext')


class InjectionTestCase(unittest.TestCase):
    def setUp(self):
        self.app = flask.Flask(__name__)

    def test_register_works(self):
        ext = object()

        register_injection(self.app, 'custom_ext', ext)

        assert_that(
            self.app.extensions,
            has_entries(
                custom_ext=is_(ext),
            ),
        )

    def test_register_fails_on_duplicates(self):
        ext = object()

        register_injection(self.app, 'custom_ext', ext)

        assert_that(
            calling(register_injection).with_args(self.app, 'custom_ext', ext),
            raises(ValueError, 'duplicates'),
        )

    def test_injection_works(self):
        ext = object()

        register_injection(self.app, 'custom_ext', ext)

        with self.app.app_context():
            value = inject('custom_ext')

            assert_that(
                value,
                is_(ext),
            )

    def test_injection_raises_outside_of_context(self):
        assert_that(
            calling(inject).with_args('custom_ext'),
            raises(RuntimeError, 'Working outside of application context.'),
        )

    def test_injection_raises_when_not_found(self):
        with self.app.app_context():
            assert_that(
                calling(inject).with_args('custom_ext'),
                raises(InjectionError),
            )


class InjectionDescriptorTestCase(unittest.TestCase):
    def setUp(self):
        self.app = flask.Flask(__name__)
        self.obj_with_injection = ClassWithInjection()

    def test_working_as_expected(self):
        ext = object()

        register_injection(self.app, 'custom_ext', ext)

        with self.app.app_context():
            value = self.obj_with_injection.custom_ext

            assert_that(
                value,
                is_(ext),
            )

    def test_raises_outside_of_context(self):
        def access_custom_ext():
            _ = self.obj_with_injection.custom_ext

        assert_that(
            calling(access_custom_ext),
            raises(RuntimeError, 'Working outside of application context.'),
        )

    def test_raises_not_found(self):
        def access_custom_ext():
            _ = self.obj_with_injection.custom_ext

        with self.app.app_context():
            assert_that(
                calling(access_custom_ext),
                raises(InjectionError),
            )

    def test_returns_itself(self):
        assert_that(
            ClassWithInjection.custom_ext,
            instance_of(Injected),
        )
