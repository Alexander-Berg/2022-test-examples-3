# coding: utf-8

"""Тест проверяет, что у книг выставляется поисковый литерал contex=green

Этот поисковый литерал нужен, чтобы искать модели экспериментальные модели.
Для книг это все не поддерживается, тем не менее, раз книги это модели,
то мы должны иметь в них литерал, который обозначает, что они не участвуют
в эксперименте

https://wiki.yandex-team.ru/users/yuraaka/Contex/#logikarabotyindeksatora
"""

import pytest
import six
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.generation.yatf.resources.books_indexer.yt_book_stuff import YTBookStuff
from market.idx.generation.yatf.test_envs.books_indexer import BooksIndexerTestEnv
from market.proto.content.mbo.MboParameters_pb2 import Category

from model_params import (
    make_model,
    string_param,
)


BOOK_CATEGORY_ID = 90881
BOOK_CATEGORY = Category(
    hid=BOOK_CATEGORY_ID,
    process_as_book_category=True,
    models_enrich_type=Category.BOOK,
)
BOOK_MODELS = [
    make_model(
        1046101,
        BOOK_CATEGORY_ID,
        {
            'name': string_param('Mega Book'),
        },
        [
            # This is a legacy pipeline port so no MBO pictures
        ],
    ),
]


@pytest.fixture(scope="module")
def yt_book_stuff():
    return []


@pytest.fixture(scope="module")
def books_xml():
    return {
        'books': [
            {
                'name': 'Mega Book'
            }
        ]
    }


@pytest.yield_fixture(scope="module")
def workflow(yt_server, yt_book_stuff, books_xml):
    resources = {
        'yt_book_stuff': YTBookStuff.from_list(
            yt_server.get_server(),
            yt_server.get_yt_client(),
            '//home/bookstuff',
            yt_book_stuff,
        ),
        'book_parameters': ParametersPb(BOOK_CATEGORY),
        'book_models': ModelsPb(BOOK_MODELS, BOOK_CATEGORY_ID),
    }
    with BooksIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_contex(workflow):
    assert_that(
        workflow,
        HasDocs().literals(contex='green'),
        six.u('В книжной коллекции все зеленое'),
    )
