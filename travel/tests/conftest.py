# -*- coding: utf-8 -*-
import os
import pytest

os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.avia.admin.tests.tests_settings'

from travel.avia.library.python.tester import hacks
hacks.apply_format_explanation()

pytest_plugins = [
    'travel.avia.library.python.tester.initializer',
    'travel.avia.library.python.tester.plugins.transaction',
]

from travel.avia.library.python.tester import hacks  # noqa
hacks.apply_format_explanation()


@pytest.yield_fixture(scope='function')
def superuser_client(request):
    from django.contrib.auth.models import User
    from django.test.client import Client

    from travel.avia.library.python.tester.utils.replace_setting import replace_setting

    superuser = User.objects.create_superuser(username='admin', password='', email='')
    client = Client()

    with replace_setting('DISABLE_YAUTH', True), replace_setting('YAUTH_USE_NATIVE_USER', False):
        client.login(username='admin', password='')
        yield client

    superuser.delete()  # transaction_context не работает с yield_fixture


@pytest.fixture
def rasp_repositories():
    from travel.library.python.dicts.factories.rasp_repositories import RaspRepositories

    return RaspRepositories()


_original_email_backend = None


def pytest_sessionstart(session):
    from django.conf import settings
    from django.core import mail  # noqa

    global _original_email_backend

    _original_email_backend = settings.EMAIL_BACKEND
    settings.EMAIL_BACKEND = 'django.core.mail.backends.locmem.EmailBackend'


def pytest_sessionfinish(session):
    from django.conf import settings
    from django.core import mail  # noqa

    settings.EMAIL_BACKEND = _original_email_backend


@pytest.fixture(scope='class', autouse=True)
def setup_class_rasp(request):
    from travel.avia.library.python.tester import transaction_context

    atomic = None
    cls = request.cls

    if hasattr(cls, 'setup_class_rasp'):
        atomic = transaction_context.enter_atomic()
        cls.setup_class_rasp()

    def fin():
        if hasattr(cls, 'teardown_class_rasp'):
            cls.teardown_class_rasp()

        if atomic is not None:
            transaction_context.rollback_atomic(atomic)

    request.addfinalizer(fin)


@pytest.fixture(autouse=True, scope='function')
def django_clear_outbox(request):
    from django.core import mail

    def clear_outbox():
        mail.outbox = []

    request.addfinalizer(clear_outbox)
