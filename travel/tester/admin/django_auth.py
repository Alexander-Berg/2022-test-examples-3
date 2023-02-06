# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.test.client import Client


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


@pytest.yield_fixture(scope='function')
def superuser(request):
    from django.contrib.auth.models import User

    user = User.objects.create_superuser(username='admin', password='', email='')
    try:
        yield user
    finally:
        user.delete()


@pytest.yield_fixture()
def admin_client():
    """
    :return: Клиент который позволяет логиниться в обход яндексовской авторизации
    """
    # чтобы точно импортнулись настройки YATEAM_LOGIN_ENABLED
    from common.middleware import yateamuser  # noqa

    client = Client()
    from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting
    with replace_setting('DISABLE_YAUTH', True), replace_setting('YATEAM_LOGIN_ENABLED', False):
        yield client


def login_user(admin_client, user, password=''):
    admin_client.login(username=user.username, password=password)


@pytest.yield_fixture(scope='function')
def staff_client(request, staff_user, admin_client):
    login_user(admin_client, staff_user)
    yield admin_client


@pytest.yield_fixture(scope='function')
def superuser_client(request, superuser, admin_client):
    login_user(admin_client, superuser)
    yield admin_client
