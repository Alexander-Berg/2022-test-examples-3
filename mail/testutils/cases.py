from importlib import import_module

from django.conf import settings
from django.contrib.auth import login
from django.http import HttpRequest
from django.test import TestCase

from exam import Exam, before
from fan.utils.cached_getters import cache_clear

from .fixtures import Fixtures


class BaseTestCase(Fixtures, Exam):
    @before
    def clear_db_cache(self):
        cache_clear()

    @before
    def setup_session(self):
        engine = import_module(settings.SESSION_ENGINE)

        session = engine.SessionStore()
        session.save()

        self.session = session

    def save_session(self):
        self.session.save()

        cookie_data = {
            "max-age": None,
            "path": "/",
            "domain": settings.SESSION_COOKIE_DOMAIN,
            "secure": settings.SESSION_COOKIE_SECURE or None,
            "expires": None,
        }

        session_cookie = settings.SESSION_COOKIE_NAME
        self.client.cookies[session_cookie] = self.session.session_key
        self.client.cookies[session_cookie].update(cookie_data)

    def login_as(self, user):
        user.backend = settings.AUTHENTICATION_BACKENDS[0]

        request = HttpRequest()
        request.session = self.session

        login(request, user)
        request.user = user

        # Save the session values.
        self.save_session()


class TestCase(BaseTestCase, TestCase):
    pass
