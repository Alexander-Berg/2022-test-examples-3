# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from urllib import urlencode

import pytest
from django.test import Client

from common.models.geo import Country, Settlement
from common.tester.factories import create_static_page


pytestmark = pytest.mark.dbuser


def get_articles_response(query, lang='ru'):
    qs = urlencode(query)
    response = Client().get('/{lang}/articles/?{qs}'.format(
        lang=lang,
        qs=qs
    ))
    return response.status_code, json.loads(response.content)


@pytest.mark.parametrize('query, expected_result', (
    (
        {'settlement_id': 213, 'country': 'ru', 't_type': 'train'},
        {'articles': [
            {
                'id': 100,
                'slug': 'ru_msk_train',
                'title': 'Статья ru msk train',
                'description': 'Аннотация ru msk train',
            },
            {
                'id': 110,
                'slug': 'ru_train',
                'title': 'Статья ru train',
                'description': 'Аннотация ru train',
            },
        ]}
    ),
    (
        {'settlement_slug': 'moscow', 'country': 'ru', 't_type': 'train', 'limit': 1},
        {'articles': [
            {
                'id': 100,
                'slug': 'ru_msk_train',
                'title': 'Статья ru msk train',
                'description': 'Аннотация ru msk train',
            },
        ]}
    ),
    (
        {'country': 'ru', 't_type': 'train'},
        {'articles': [
            {
                'id': 110,
                'slug': 'ru_train',
                'title': 'Статья ru train',
                'description': 'Аннотация ru train',
            },
        ]}
    ),
))
def test_articles_view(query, expected_result):
    msk = Settlement.objects.get(id=Settlement.MOSCOW_ID)
    create_static_page(
        id=100, slug='ru_msk_train', title_ru='Статья ru msk train', meta_description_ru='Аннотация ru msk train',
        article_bindings=[
            {'country': Country.RUSSIA_ID, 'settlement': msk, 't_type': 'train', 'priority': 0}
        ]
    )
    create_static_page(
        id=110, slug='ru_train', title_ru='Статья ru train', meta_description_ru='Аннотация ru train',
        article_bindings=[
            {'country': Country.RUSSIA_ID, 'settlement': None, 't_type': 'train', 'priority': 1}
        ]
    )

    status_code, response = get_articles_response(query)

    assert status_code == 200
    assert response['result'] == expected_result


def get_staticpage_response(query, lang='ru'):
    qs = urlencode(query)
    response = Client().get('/{lang}/staticpage/?{qs}'.format(
        lang=lang,
        qs=qs
    ))
    return response.status_code, json.loads(response.content)


@pytest.mark.parametrize('query', (
    {'slug': 'covid-19'},
    {'slug': 'covid-19', 'country': 'ru'},
    {'slug': '666'},
))
def test_staticpage_view(query):
    root_page = create_static_page(id=111, slug='root', title_ru='Корень')
    mid_page = create_static_page(id=333, slug='mid', title_ru='Середина', parent=root_page)
    page = create_static_page(
        id=666,
        slug='covid-19',
        title_ru='Статья про covid-19',
        content=r'<h3>COVID</h3>',
        meta_title_ru='meta Статья ru covid-19',
        meta_description_ru='meta Аннотация ru covid-19',
        is_published=1,
        parent=mid_page
    )
    create_static_page(id=701, slug='child1', title_ru='Глава 1', is_published=1, parent=page)
    create_static_page(id=702, slug='child2', title_ru='Глава 2', is_published=1, parent=page)
    create_static_page(id=709, slug='child9', title_ru='Черновик', is_published=0, parent=page)

    status_code, response = get_staticpage_response(query)

    assert status_code == 200
    assert response['result'] == {
        'id': 666,
        'slug': 'covid-19',
        'title': 'Статья про covid-19',
        'content': r'<h3>COVID</h3>',
        'meta_title': 'meta Статья ru covid-19',
        'meta_description': 'meta Аннотация ru covid-19',
        'parents': [
            {'id': 111, 'slug': 'root', 'title': 'Корень'},
            {'id': 333, 'slug': 'mid', 'title': 'Середина'},
        ],
        'children': [
            {'id': 701, 'slug': 'child1', 'title': 'Глава 1'},
            {'id': 702, 'slug': 'child2', 'title': 'Глава 2'},
        ],
    }
