from importlib import import_module

from django.conf import settings
from django.contrib.auth import login
from django.http import HttpRequest

from fan.testutils.pytest_fixtures import *  # NOQA
from fan.testutils.mocks import *  # NOQA

import pytest


@pytest.fixture
def django_session(user):
    engine = import_module(settings.SESSION_ENGINE)
    session = engine.SessionStore()
    session.save()
    return session


@pytest.fixture
def auth_session(user, django_session):
    user.backend = settings.AUTHENTICATION_BACKENDS[0]

    request = HttpRequest()
    request.session = django_session

    login(request, user)
    request.user = user
    django_session.save()

    return django_session
