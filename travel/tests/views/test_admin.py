# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest
from django.contrib.auth.models import User, AnonymousUser
from django.http import HttpResponse
from django.test import RequestFactory
from hamcrest import assert_that, has_properties, has_entries

from travel.rasp.info_center.info_center.views import admin
from travel.rasp.info_center.info_center.views.admin import info_change_or_create, InfoForm
from common.apps.info_center.models import Info
from common.models.factories import create_info


pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


def test_info_view_get(staff_client):
    create_info(
        id=123,
        title='title123',
        national_versions=['ru', 'uk'],
        pages=[{'id': 12, 'title': 'my cool page'}],
        settlements=[{'id': 16, 'title_ru': 'title16'}, {'id': 15, 'title_ru': 'title15'}],
        messages=[
            {'dt_created': datetime(2017, 12, 31), 'text': 'aaa'},
            {'dt_created': datetime(2016, 12, 31, 5, 6, 7), 'text': 'bbb'},
        ]
    )

    resp = staff_client.get('/admin/info/123/')

    assert resp.status_code == 200
    info_id = resp.context_data['info_id']
    linked_objs = resp.context_data['linked_objs_groups'][0]
    linked_objs_types = resp.context_data['linked_objs_types']
    feed = resp.context_data['feed']

    form = resp.context_data['form']
    assert_that(form.initial, has_entries({
        'title': 'title123',
        'national_versions': ['ru', 'uk']
    }))

    assert info_id == '123'
    assert sorted(linked_objs_types) == sorted(Info.OBJ_LINKS_CONFIG.keys())
    assert linked_objs == [
        ('page', 12, 'my cool page'),
        ('settlement', 15, 'title15'),
        ('settlement', 16, 'title16'),
    ]
    assert feed == [
        {'dtCreated': '2016-12-31 05:06', 'text': 'bbb', 'type': 'message'},
        {'dtCreated': '2017-12-31 00:00', 'text': 'aaa', 'type': 'message'},
    ]


def test_info_view_post(staff_client):
    with mock.patch.object(admin, 'info_change_or_create') as m_change:
        expected_response = HttpResponse()
        m_change.return_value = expected_response

        resp = staff_client.post('/admin/info/{}/change/'.format(42), {'title': '123'})
        args = m_change.call_args_list[0]
        assert args[0][0].POST == {'title': ['123']}
        assert args[0][1] == '42'
        assert resp is expected_response


class InfoChangeOrCreate(object):
    def get_data(self):
        data = {
            'title': '123',
            'text': '456',
            'info_type': Info.Type.AHTUNG,
            'lang': 'ru',
            'national_versions': ['ru'],
            'importance': 1,
            'dt_from': '2017-08-09',
            'dt_to': '2018-09-11',
            'services': [Info.Service.WEB],
            'uuid': 1,
        }

        expected = dict(**data)
        expected['dt_from'] = datetime(2017, 8, 9)
        expected['dt_to'] = datetime(2018, 9, 11)
        expected['id'] = 42

        return data, expected

    def test_info_change_or_create(self):
        create_info(id=42, title='aaaaa')

        # нет суперюзера
        request = RequestFactory().post('/admin/info/add/', {'title': '123'})
        request.user = AnonymousUser()
        resp = info_change_or_create(request, 42)
        assert resp.status_code == 302
        assert Info.objects.get().title == 'aaaaa'

        # невалидная форма
        request.user = User()
        resp = info_change_or_create(request, 42)
        assert resp.status_code == 200
        assert resp.context_data['form'].errors
        assert resp.context_data['form'].data['title'] == '123'
        assert resp.context_data['info_id'] == 42
        assert Info.objects.get().title == 'aaaaa'

        data, expected = self.get_data()

        request = RequestFactory().post('/admin/info/add/', data)
        request.user = User()
        resp = info_change_or_create(request, 42)
        assert resp.status_code == 302

        info = Info.objects.get()
        assert_that(info, has_properties(expected))

    def test_info_change_or_create_new(self):
        data, expected = self.get_data()
        # создание нового info - вызываем без id
        expected.pop('id')
        request = RequestFactory().post('/admin/info/add/', data)
        request.user = User()
        resp = info_change_or_create(request, None)
        assert resp.status_code == 302
        info = Info.objects.get(id__ne=42)
        assert_that(info, has_properties(expected))


def test_info_add_get(staff_client):
    resp = staff_client.get('/admin/info/add/')
    assert resp.status_code == 200
    form = resp.context_data['form']
    assert isinstance(form, InfoForm)


def test_info_add_post(staff_client):
    with mock.patch.object(admin, 'info_change_or_create') as m_change:
        expected_response = HttpResponse()
        m_change.return_value = expected_response

        resp = staff_client.post('/admin/info/add/', {'title': '123'})
        args = m_change.call_args_list[0]
        assert args[0][0].POST == {'title': ['123']}
        assert args[0][1] is None
        assert resp is expected_response


def test_info_remove(staff_client):
    create_info(id=123)
    resp = staff_client.post('/admin/info/123/remove/')

    assert resp.status_code == 302
    with pytest.raises(Info.DoesNotExist):
        Info.objects.get(id=123)


def test_info_list(staff_client):
    info1 = create_info()
    info2 = create_info()
    info3 = create_info()

    resp = staff_client.get('/admin/info/')
    assert resp.status_code == 200
    assert resp.context_data['infos'] == [info3, info2, info1]


def test_send_push(staff_client):
    create_info(id=123)

    data = {
        'title': 'пушег',
        'text': 'няшный пушег',
        'url': 'yandextrains://favourites',
        'image_url': 'http://42',
    }
    resp = staff_client.post('/admin/info/123/send_push/', data)
    assert resp.status_code == 200


def test_add_yadm_news_item(staff_client):
    create_info(id=124)

    # Если так вызывать POST-запрос, то форма получается невалидной, и тест по своей сути не верен
    # Как правильно передавать словарь значений - не придумали
    data = {
        'title': 'новость',
        'text': 'Новость для ЯдМ',
        'importance': 0
    }
    resp = staff_client.post('/admin/info/124/add_yadm_news_item/', data)
    assert resp.status_code == 200

    # info = Info.objects.get(id=124)
    # assert len(info.yadm_news) == 1
    # assert_that(info.yadm_news[0], has_entries({
    #     'title': 'новость',
    #     'text': 'Новость для ЯдМ',
    #     'importance': 0,
    # }))
