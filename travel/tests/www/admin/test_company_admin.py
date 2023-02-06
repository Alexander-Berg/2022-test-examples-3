# coding: utf-8

import pytest
from django.contrib.auth.models import Permission

from common.models.schedule import RThread, Route
from tester.factories import create_company, create_thread, create_route
from tests.utils import login_user


@pytest.mark.dbuser
@pytest.mark.parametrize('has_permissions', [True, False])
def test_get_remove_routes(admin_client, staff_user, has_permissions):
    """
    Проверяем права, и то что get не изменяет данные.
    """
    login_user(admin_client, staff_user)
    company = create_company()
    thread = create_thread(company=company)
    other_thread_ids = [create_thread().id, create_thread(company=create_company()).id]
    if has_permissions:
        staff_user.user_permissions.add(Permission.objects.get(codename='can_delete_company_routes'))

    response = admin_client.get('/www/company/{}/remove-routes/'.format(company.id))

    if has_permissions:
        assert response.status_code == 200
    else:
        assert response.status_code == 302

    assert RThread.objects.get(pk=thread.pk)
    assert RThread.objects.filter(pk__in=other_thread_ids).count() == 2


@pytest.mark.dbuser
def test_remove_routes_bad_post(admin_client, staff_user):
    """
    Проверяем необходимость POST параметра post=yes
    """
    login_user(admin_client, staff_user)
    company = create_company()
    thread = create_thread(company=company)
    other_thread_ids = [create_thread().id, create_thread(company=create_company()).id]

    staff_user.user_permissions.add(Permission.objects.get(codename='can_delete_company_routes'))

    response = admin_client.post('/www/company/{}/remove-routes/'.format(company.id))

    assert response.status_code == 400

    assert RThread.objects.get(pk=thread.pk)
    assert RThread.objects.filter(pk__in=other_thread_ids).count() == 2


@pytest.mark.dbuser
def test_remove_routes(admin_client, staff_user):
    login_user(admin_client, staff_user)
    company = create_company()
    other_route = create_route()
    thread = create_thread(company=company)
    other_thread_ids = [create_thread().id, create_thread(company=create_company()).id]

    staff_user.user_permissions.add(Permission.objects.get(codename='can_delete_company_routes'))

    response = admin_client.post('/www/company/{}/remove-routes/'.format(company.id), {'post': 'yes'})

    assert response.status_code == 302

    assert not RThread.objects.filter(pk=thread.pk)
    assert not Route.objects.filter(pk=thread.route_id)
    assert Route.objects.get(pk=other_route.pk)
    assert RThread.objects.filter(pk__in=other_thread_ids).count() == 2
