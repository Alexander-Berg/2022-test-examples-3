# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, contains, has_entries

from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import create_static_page
from travel.rasp.morda_backend.morda_backend.article.serialization import ArticlesQuerySchema, ArticlesResponseSchema


pytestmark = pytest.mark.dbuser


def test_articles_query_schema():
    query_params = {
        'country': 'RU',
        'limit': 3,
        'settlement_id': 54,
        'settlement_slug': 'yekaterinburg',
        't_type': 'train',
    }

    context, errors = ArticlesQuerySchema().load(query_params)

    assert errors == {}
    assert context.country.id == Country.RUSSIA_ID
    assert context.limit == 3
    assert context.settlement_id == 54
    assert context.settlement_slug == 'yekaterinburg'
    assert context.transport_type == TransportType.get_train_type()


def test_articles_response_schema():
    articles = [
        create_static_page(id=101, slug='slug1', title_ru='Статья 1', meta_description_ru='Аннотация 1'),
        create_static_page(id=102, slug='slug2', title_ru='Статья 2', meta_description_ru='Аннотация 2'),
    ]

    result, errors = ArticlesResponseSchema().dump({
        'articles': articles,
    })

    assert not errors
    assert_that(result, has_entries({
        'articles': contains(
            has_entries({
                'id': articles[0].id,
                'slug': articles[0].slug,
                'title': articles[0].L_title(lang='ru'),
                'description': articles[0].L_meta_description(lang='ru')
            }),
            has_entries({
                'id': articles[1].id,
                'slug': articles[1].slug,
                'title': articles[1].L_title(lang='ru'),
                'description': articles[1].L_meta_description(lang='ru')
            }),
        )
    }))


def test_articles_response_schema_empty_data():
    result, errors = ArticlesResponseSchema().dump({
        'articles': [],
    })

    assert not errors
    assert_that(result, has_entries({
        'articles': []
    }))
