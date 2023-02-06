# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import os

import pytest
from django.conf import settings
from django.core import mail
from django.test.client import Client

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa
from common.tester import transaction_context
from tester.utils.replace_setting import replace_setting
from tests.utils import login_user
from travel.library.python.resource import extract_resources

pytest_plugins.extend(['travel.rasp.library.python.common23.tester.plugins.rasp_deprecation', 'common.tester.yaml_fixtures'])  # noqa


@pytest.hookimpl(tryfirst=True)
def pytest_configure(config):
    extract_resources('travel/rasp/admin/tests/', strip_prefix=False)


def pytest_sessionstart(session):
    mail._original_email_backend = settings.EMAIL_BACKEND
    settings.EMAIL_BACKEND = 'django.core.mail.backends.locmem.EmailBackend'
    settings.LOG_PATH = os.path.join(settings.PROJECT_PATH, 'log')


def pytest_sessionfinish(session):
    settings.EMAIL_BACKEND = mail._original_email_backend


@pytest.fixture(autouse=True, scope='function')
def django_clear_outbox(request):
    def clear_outbox():
        mail.outbox = []

    request.addfinalizer(clear_outbox)


@pytest.yield_fixture(scope='function')
def superuser_client(request, superuser, admin_client):
    login_user(admin_client, superuser)
    yield admin_client


@pytest.yield_fixture(scope='function')
def superuser(request):
    from django.contrib.auth.models import User

    user = User.objects.create_superuser(username='admin', password='', email='')
    try:
        yield user
    finally:
        user.delete()


@pytest.yield_fixture(scope='function')
def staff_user(request):
    from django.contrib.auth.models import User

    user = User.objects.create_user(username='staff', password='', email='')
    user.is_staff = True
    user.save()
    try:
        yield user
    finally:
        user.delete()


@pytest.yield_fixture()
def admin_client():
    """
    :return: Клиент который позволяет логиниться в обход яндексовской авторизации
    """
    client = Client()
    with replace_setting('DISABLE_YAUTH', True), replace_setting('YAUSER_ADMIN_LOGIN', False):
        yield client


@pytest.fixture(scope='class', autouse=True)
def setup_class_rasp(request):
    atomic = None
    cls = request.cls

    try:
        if hasattr(cls, 'setup_class_rasp'):
            atomic = transaction_context.enter_atomic()
            cls.setup_class_rasp()

        yield

        if hasattr(cls, 'teardown_class_rasp'):
            cls.teardown_class_rasp()
    finally:
        if atomic is not None:
            transaction_context.rollback_atomic(atomic)


@pytest.fixture(scope='module', autouse=True)
def af_setup(request):
    # Transaction only after condition because there are conflicts with other tests
    if 'admin/tests/scripts/schedule/af' not in request.module.__file__:
        return

    from common.models.schedule import Company
    from common.tester.factories import create_station, create_supplier

    def fin():
        transaction_context.rollback_atomic(atomic)

    atomic = transaction_context.enter_atomic()
    try:
        Company.objects.create(id=112, title='РЖД')
        create_supplier(id=4, code='af')
        create_station(id=1101, t_type='suburban', __={'codes': {'esr': '000001'}})
        create_station(id=1102, t_type='suburban', __={'codes': {'esr': '000002'}})
        create_station(id=1103, t_type='suburban', __={'codes': {'esr': '000003'}})
    except Exception:
        fin()
        raise
    else:
        request.addfinalizer(fin)
