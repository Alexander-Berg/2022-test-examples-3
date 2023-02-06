# coding: utf-8
import pytest
import six

from hamcrest import (
    all_of,
    assert_that,
    equal_to
)
from market.idx.yatf.matchers.env_matchers import (
    HasDocsWithValues,
)

from market.idx.generation.yatf.resources.books_indexer.yt_book_stuff import YTBookStuff
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.generation.yatf.test_envs.books_indexer import BooksIndexerTestEnv

from market.proto.content.mbo.MboParameters_pb2 import Category

from model_params import (
    make_model,
    make_xl_picture,
    make_picrobot_picture_proto,
    make_picrobot_picture_mbo_proto,
    make_yt_record
)


BOOK_CATEGORY_ID = 90881
BOOK_MODEL_ID = 2442135


@pytest.fixture(scope="module")
def category():
    return Category(
        hid=BOOK_CATEGORY_ID,
        process_as_book_category=True,
        models_enrich_type=Category.BOOK,
    )


@pytest.fixture(scope="module")
def pictures(category):
    return [[
        make_xl_picture('//model1_xl_picture', auto=True)
    ], [
        make_xl_picture('//model2_xl_picture')
    ], [
    ], [
        make_xl_picture('null')
    ]]


@pytest.fixture(scope="module")
def models(category, pictures):
    return [make_model(
        model_id=BOOK_MODEL_ID + i,
        category_id=BOOK_CATEGORY_ID,
        pictures=pictures[i]
    ) for i in range(0, len(pictures))]


@pytest.fixture(scope="module")
def yt_book_stuff(pictures):
    return [make_yt_record(
        model_id=BOOK_MODEL_ID + i
    ) for i in range(0, len(pictures))]


@pytest.yield_fixture(scope="module")
def workflow(yt_book_stuff, yt_server, models, category):
    resources = {
        'yt_book_stuff': YTBookStuff.from_list(
            yt_server.get_server(), yt_server.get_yt_client(),
            '//home/bookstuff',
            yt_book_stuff
        ),
        'book_models': ModelsPb(models, category.hid),
        'book_parameters': ParametersPb(category),
    }
    with BooksIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_book_pictures_yt_data(workflow, yt_book_stuff, pictures):
    """Книги должны обогащаться картинками из YT
    Используется только информация из 'XL-Picture'
    """
    bs_model1_picture = make_picrobot_picture_proto(yt_book_stuff[0]['pic'][0])
    bs_model3_picture = make_picrobot_picture_proto(yt_book_stuff[2]['pic'][0])
    bs_model4_picture = make_picrobot_picture_proto(yt_book_stuff[3]['pic'][0])
    model2_picture_xl = make_picrobot_picture_mbo_proto(pictures[1][0])

    assert_that(len(workflow.offers), equal_to(len(pictures)))

    assert_that(
        workflow,
        all_of(
            # Картинка всегда берется из YT, если value_source == auto.
            HasDocsWithValues('PicturesProtoBase64', [bs_model1_picture]),
            # Если есть, используется картинка из 'XL-Picture'.
            HasDocsWithValues('ProtoPicInfo', [model2_picture_xl]),
            # Если нет картинки 'XL-Picture', то картинка берется из YT.
            HasDocsWithValues('PicturesProtoBase64', [bs_model3_picture]),
            # Если URL 'XL-Picture' имеет неверный формат, то картинка берется из YT.
            HasDocsWithValues('PicturesProtoBase64', [bs_model4_picture]),
        ),
        six.u('Картинки книг совпадают с ожидаемыми'),
    )
