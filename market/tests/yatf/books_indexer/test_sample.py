# coding: utf-8
import base64
import hashlib
import pytest
import six

from hamcrest import assert_that
from market.idx.yatf.matchers.env_matchers import HasDocsWithValues, DoesntHaveDocsWithValues

from market.idx.generation.yatf.resources.books_indexer.yt_book_stuff import YTBookStuff, YTBookStuffWithoutTable
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.generation.yatf.test_envs.books_indexer import BooksIndexerTestEnv
from market.proto.content.mbo.MboParameters_pb2 import Category
from market.proto.content.pictures_pb2 import Picture

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
BOOK_MODEL_ID = 1046101
BOOK_NAME = 'Mega Book'
BOOK_UISBN = '0-597-84062-8'
BOOK_MODELS = [
    make_model(
        BOOK_MODEL_ID,
        BOOK_CATEGORY_ID,
        {
            'name': string_param(BOOK_NAME),
            'uisbn': string_param(BOOK_UISBN),
        },
        [
            # This is a legacy pipeline port so no MBO pictures
        ],
    ),
]


def make_b64(proto_str):
    return base64.b64encode(
        six.ensure_binary(proto_str),
        altchars=six.ensure_binary('-_')
    ).replace(
        six.ensure_binary('='),
        six.ensure_binary(',')
    )


def make_uid_from_binary(binary):
    return base64.b64encode(
        six.ensure_binary(binary),
        altchars=six.ensure_binary('-_')
    )[:22]


def make_pic_id(url):
    h = hashlib.md5()
    h.update(six.ensure_binary(url))
    return make_uid_from_binary(h.digest())


@pytest.fixture(scope="module")
def yt_book_stuff():
    return [{
        'model_id': BOOK_MODEL_ID,
        'description': 'You are reading an arbitrary boring test book descripion',
        'url': 'https://my-shop.ru/shop/books/1.html',
        'shop_name': 'My-shop.ru',
        'pic': [{
            'md5': make_pic_id('my-shop.ru/pics/1.jpg'),
            'group_id': 1234,
            'width': 200,
            'height': 200,
            'thumb_mask': 0
            }],
        'barcodes': ['9785458169387', '978-5-458-16938-7']
        }]


@pytest.fixture(scope="module")
def yt_client(yt_server):
    return yt_server.get_yt_client()


@pytest.yield_fixture(scope="module")
def workflow(yt_book_stuff, yt_server, yt_client):
    resources = {
        'yt_book_stuff': YTBookStuff.from_list(
            yt_server.get_server(), yt_client,
            '//home/bookstuff',
            yt_book_stuff
        ),
        'book_parameters': ParametersPb(BOOK_CATEGORY),
        'book_models': ModelsPb(BOOK_MODELS, BOOK_CATEGORY_ID),
    }
    with BooksIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def yt_workflow_without_table(yt_book_stuff, yt_server, yt_client):
    resources = {
        'yt_book_stuff': YTBookStuffWithoutTable.from_list(
            yt_server.get_server(), yt_client,
            '//home/not_created_table',
            yt_book_stuff
        ),
        'book_parameters': ParametersPb(BOOK_CATEGORY),
        'book_models': ModelsPb(BOOK_MODELS, BOOK_CATEGORY_ID),
    }
    with BooksIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def do_test_book_names(workflow):
    expected_names = [BOOK_NAME]
    assert_that(workflow, HasDocsWithValues('maliases', expected_names),
                six.u('Название книги совпадает с заданным'))


def test_yt_book_names(workflow):
    do_test_book_names(workflow)


def test_yt_book_names_without_table(yt_workflow_without_table):
    do_test_book_names(yt_workflow_without_table)


def do_test_book_hid(workflow):
    expected_names = [str(BOOK_MODEL_ID)]
    assert_that(workflow, HasDocsWithValues('hyper', expected_names),
                six.u('Модель книги совпадает с заданной'))


def test_yt_book_hid(workflow):
    do_test_book_hid(workflow)


def test_yt_book_hid_without_table(yt_workflow_without_table):
    do_test_book_hid(yt_workflow_without_table)


def do_test_book_descr(workflow, description):
    exp_descr = [description]
    assert_that(workflow, HasDocsWithValues('description', exp_descr),
                six.u('Описание книги совпадает с ожидаемым'))


def do_negative_test_book_descr(workflow, description):
    assert_that(workflow, DoesntHaveDocsWithValues('description', description),
                six.u('Описание книги нет потому что нет таблиц'))


def test_yt_book_descr(workflow, yt_book_stuff):
    do_test_book_descr(workflow, yt_book_stuff[0]['description'])


def test_yt_book_isbn(workflow):
    isbns = [BOOK_UISBN]
    expected = [isbns[-1]]
    assert_that(workflow, HasDocsWithValues('ISBN', expected),
                 six.u('ISBN книги совпадает с ожидаемым'))


def test_yt_book_descr_without_table(yt_workflow_without_table, yt_book_stuff):
    do_negative_test_book_descr(yt_workflow_without_table,
                                yt_book_stuff[0]['description'])


def do_test_book_pictures(workflow, proto_picture):
    proto_str = proto_picture.SerializeToString()
    b64_str = make_b64(proto_str)
    expected = [b64_str]
    assert_that(workflow, HasDocsWithValues('PicturesProtoBase64', expected),
                six.u('Картинка книги совпадает с ожидаемой'))


def do_negative_test_book_pictures(workflow, proto_picture):
    proto_str = proto_picture.SerializeToString()
    b64_str = make_b64(proto_str)
    expected = [b64_str]
    assert_that(workflow, DoesntHaveDocsWithValues('PicturesProtoBase64', expected),
                six.u('Картинка нет так как нет таблиц'))


def test_yt_book_pictures(workflow, yt_book_stuff):
    p = Picture()
    for (k, v) in yt_book_stuff[0]['pic'][0].items():
        setattr(p, k, v)
    do_test_book_pictures(workflow, p)


def test_yt_book_pictures_without_table(yt_workflow_without_table, yt_book_stuff):
    p = Picture()
    for (k, v) in yt_book_stuff[0]['pic'][0].items():
        setattr(p, k, v)
    do_negative_test_book_pictures(yt_workflow_without_table, p)
