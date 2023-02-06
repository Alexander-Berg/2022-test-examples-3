# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext
from hamcrest import assert_that, has_property

from common.models.geo import Country, Settlement
from common.models.transport import TransportType
from common.tester.factories import create_country, create_settlement, create_static_page
from travel.rasp.morda_backend.morda_backend.article.data_layer import get_articles


pytestmark = pytest.mark.dbuser


@pytest.mark.parametrize('settlement_slug, transport_type_code, country_code, limit, expected_article_slugs', [
    # order in expected_article_slugs is important
    ['yekaterinburg', 'train', 'RU', 5, ['ru_ekb_train', 'ru_ekb_msk_train1', 'ru_train0', 'ru_train1']],
    ['yekaterinburg', 'train', 'RU', 3, ['ru_ekb_train', 'ru_ekb_msk_train1', 'ru_train0']],
    ['yekaterinburg', 'train', 'RU', 1, ['ru_ekb_train']],
    [None, 'train', 'RU', 5, ['ru_train0', 'ru_train1']],
    [None, 'train', 'RU', 1, ['ru_train0']],
])
def test_get_articles(settlement_slug, transport_type_code, country_code, limit, expected_article_slugs):
    def settlement_by_slug(slug):
        return Settlement.objects.get(slug=slug)

    ekb = create_settlement(id=54, slug='yekaterinburg')
    msk = settlement_by_slug('moscow')

    create_country(id=Country.BELARUS_ID, code='BY')

    create_static_page(id=101, slug='ru_ekb_train', title_ru='Статья RU ЕКБ train', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': ekb, 't_type': 'train', 'priority': 0},
    ])
    create_static_page(id=102, slug='ru_ekb', title_ru='Статья RU ЕКБ', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': ekb, 't_type': None, 'priority': 0},
    ])
    create_static_page(id=103, slug='ru_ekb_msk_train1', title_ru='Статья RU ЕКБ МСК train', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': ekb, 't_type': 'train', 'priority': 1},
        {'country': Country.RUSSIA_ID, 'settlement': msk, 't_type': 'train', 'priority': 1},
    ])
    create_static_page(id=104, slug='ru_msk_train', title_ru='Статья RU МСК train', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': msk, 't_type': 'train', 'priority': 0},
    ])
    create_static_page(id=105, slug='ru_msk', title_ru='Статья RU МСК', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': msk, 't_type': None, 'priority': 0},
    ])
    create_static_page(id=106, slug='ru_train1', title_ru='Статья RU train 1', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': None, 't_type': 'train', 'priority': 1},
    ])
    create_static_page(id=107, slug='ru_train0', title_ru='Статья RU train 0', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': None, 't_type': 'train', 'priority': 0},
    ])
    create_static_page(id=108, slug='ru_plane', title_ru='Статья RU plane', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': None, 't_type': 'plane', 'priority': 0},
    ])
    create_static_page(id=109, slug='ru_ekb_plane', title_ru='Статья RU ekb plane', article_bindings=[
        {'country': Country.RUSSIA_ID, 'settlement': ekb, 't_type': 'plane', 'priority': 0},
    ])
    create_static_page(id=110, slug='by_ekb_train', title_ru='Статья BY ЕКБ train', article_bindings=[
        {'country': Country.BELARUS_ID, 'settlement': ekb, 't_type': 'train', 'priority': 0},
    ])
    create_static_page(id=111, slug='by_ekb', title_ru='Статья BY ЕКБ', article_bindings=[
        {'country': Country.BELARUS_ID, 'settlement': ekb, 't_type': None, 'priority': 0},
    ])

    settlement = settlement_by_slug(settlement_slug) if settlement_slug else None
    transport_type = TransportType.objects.get(code=transport_type_code) if transport_type_code else None
    country = Country.objects.get(code=country_code) if country_code else None

    with CaptureQueriesContext(connection) as captured_queries:
        articles = get_articles(settlement, transport_type, country, limit)

    assert len(captured_queries) <= 2
    for query in captured_queries:
        assert query['sql'].find('content') == -1  # w/o content fields

    # validate existence and order
    assert len(expected_article_slugs) == len(articles)
    for i in range(0, len(expected_article_slugs)):
        assert_that(articles[i], has_property('slug', expected_article_slugs[i]))
