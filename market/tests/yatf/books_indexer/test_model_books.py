# coding: utf-8
import pytest
import six

from hamcrest import (
    all_of,
    is_not,
    assert_that,
    starts_with,
)
from market.idx.yatf.matchers.env_matchers import (
    DoesntHaveDocsWithValues,
    HasDocs,
    HasDocsWithValues,
    HasDocsWithValuesMatching,
)

from market.idx.generation.yatf.resources.books_indexer.yt_book_stuff import YTBookStuff
from market.idx.yatf.resources.mbo.global_vendors_xml import GlobalVendorsXml
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.generation.yatf.test_envs.books_indexer import BooksIndexerTestEnv

from market.proto.content.mbo.MboParameters_pb2 import Category

from model_params import (
    make_enum,
    make_model,
    enum_param,
    make_xl_picture,
    make_picrobot_picture_proto,
    make_picrobot_picture_mbo_proto,
    make_pic_id,
    string_param,
)


BOOK_CATEGORY_ID = 90881
BOOK_CATEGORY2_ID = 90882
NON_BOOK_CATEGORY_ID = 90883
BOOK_MODEL1_ID = 2441135
BOOK_MODEL2_ID = 2441136
BOOK_MODEL3_ID = 2441137
NON_BOOK_MODEL_ID = 2441138
VENDOR_ID_1 = 112233
VENDOR_ID_2 = 445566

BOOK_AUTHOR_PARAM = 1
BOOK_AUTHOR = 'Bar'
BOOK_PUBLISHER_PARAM = 2
BOOK_PUBLISHER = 'Baz'
BOOK_YEAR_PARAM = 3
BOOK_YEAR = '2007'
BOOK_SERIES_PARAM = 4
BOOK_SERIES = 'Awol'
ISBNS = ['978-1-56619-909-4', '1-56619-909-3', '1-4028-9462-7']
BOOK_MODEL1_EXTRA_BARCODES = ['1234567890']

BOOK_GOOD_DESCRIPTION = 'Good description'
BOOK_BAD_DESCRIPTION = 'Bad description'

BOOK_MODEL1_STUFF = {
    'model_id': BOOK_MODEL1_ID,
    'description': 'You are reading an arbitrary boring test book descripion',
    'url': 'https://my-shop.ru/shop/books/1.html',
    'shop_name': 'My-shop.ru',
    'pic': [
        {
            'md5': make_pic_id('my-shop.ru/pics/1.jpg'),
            'group_id': 1234,
            'width': 200,
            'height': 200,
            'thumb_mask': 0,
        },
    ],
    'barcodes': ['9785458169387', '978-5-458-16938-7'],
}

BOOK_MODEL2_STUFF = {
    'model_id': BOOK_MODEL2_ID,
    'description': 'You are reading a very interesting book description',
    'url': 'https://my-shop.ru/shop/books/2.html',
    'shop_name': 'My-shop2.ru',
    'pic': [
        {
            'md5': make_pic_id('my-shop.ru/pics/2.jpg'),
            'group_id': 1234,
            'width': 200,
            'height': 200,
            'thumb_mask': 0,
        },
    ],
    'barcodes': []
}

BOOK_MODEL1 = {
    'name': string_param('Foo'),
    'author': enum_param(BOOK_AUTHOR_PARAM, 1),
    'vendor': enum_param(BOOK_PUBLISHER_PARAM, 1),
    'year': enum_param(BOOK_YEAR_PARAM, 1),
    'series': enum_param(BOOK_SERIES_PARAM, 1),
    'description': string_param(BOOK_BAD_DESCRIPTION, auto=True),
    # both ISBN params will end up in the isbn search literal
    # after dashes are stripped from their values
    'uisbn': string_param(ISBNS[:2]),
    'nid_isbn': string_param(ISBNS[2:]),
    'BarCode': string_param(BOOK_MODEL1_EXTRA_BARCODES),
}

BOOK_MODEL2 = {
    'name': string_param('Foo 2'),
    'description': string_param(BOOK_GOOD_DESCRIPTION),
}

BOOK_MODEL3 = {
    'name': string_param('Foo 3'),
    # must be rejected because of the lacking enrich type
    'description': string_param(BOOK_BAD_DESCRIPTION),
}

BOOK_MODEL1_PICTURES = [
    make_xl_picture('//model1_xl_picture', auto=True)
]

BOOK_MODEL2_PICTURES = [
    make_xl_picture('//model2_xl_picture')
]

NON_BOOK_MODEL = {
    'name': string_param(
        'Grays Sports Almanac: Complete Sports Statistics 1950-2000'
    ),
    'author': enum_param(BOOK_AUTHOR_PARAM, 1),
    'vendor': enum_param(BOOK_PUBLISHER_PARAM, 1),
    'year': enum_param(BOOK_YEAR_PARAM, 1),
    'series': enum_param(BOOK_SERIES_PARAM, 1),
    'uisbn': string_param(ISBNS[:2]),
    'nid_isbn': string_param(ISBNS[2:]),
}

NON_BOOK_CATEGORY = Category(
    hid=NON_BOOK_CATEGORY_ID,
)

BOOK_CATEGORY = Category(
    hid=BOOK_CATEGORY_ID,
    process_as_book_category=True,
    models_enrich_type=Category.BOOK,
    parameter=[
        make_enum(
            'author',
            BOOK_AUTHOR_PARAM,
            {
                1: BOOK_AUTHOR,
            }
        ),
        make_enum(
            'vendor',
            BOOK_PUBLISHER_PARAM,
            {
                1: BOOK_PUBLISHER,
            }
        ),
        make_enum(
            'year',
            BOOK_YEAR_PARAM,
            {
                1: BOOK_YEAR,
            }
        ),
        make_enum(
            'series',
            BOOK_SERIES_PARAM,
            {
                1: BOOK_SERIES,
            }
        ),
    ]
)

BOOK_CATEGORY2 = Category(
    hid=BOOK_CATEGORY2_ID,
    process_as_book_category=True,
)

BOOK_MODELS = [
    make_model(
        BOOK_MODEL1_ID,
        BOOK_CATEGORY_ID,
        BOOK_MODEL1,
        BOOK_MODEL1_PICTURES,
        vendor_id=VENDOR_ID_1,
    ),
    make_model(
        BOOK_MODEL2_ID,
        BOOK_CATEGORY_ID,
        BOOK_MODEL2,
        BOOK_MODEL2_PICTURES,
        published_on_market=False,
        published_on_blue_market=True,
        vendor_id=VENDOR_ID_2,
    )
]

BOOK_MODELS2 = [
    make_model(BOOK_MODEL3_ID, BOOK_CATEGORY2_ID, BOOK_MODEL3),
]

NON_BOOK_MODELS = [
    make_model(NON_BOOK_MODEL_ID, NON_BOOK_CATEGORY_ID, NON_BOOK_MODEL),
]


@pytest.fixture(scope="module")
def yt_book_stuff(yt_server):
    return YTBookStuff.from_list(
        yt_server.get_server(), yt_server.get_yt_client(),
        '//home/bookstuff',
        [BOOK_MODEL1_STUFF, BOOK_MODEL2_STUFF]
    )


@pytest.fixture(scope='module')
def global_vendors():
    return '''
        <global-vendors>
          <vendor id="112233" name="name1">
            <site>site</site>
            <picture>picture</picture>
          </vendor>
          <vendor id="445566" name="yandex">
            <is-fake-vendor>true</is-fake-vendor>
          </vendor>
        </global-vendors>
    '''


@pytest.yield_fixture(scope="module")
def workflow(yt_book_stuff, global_vendors):
    resources = {
        'yt_book_stuff': yt_book_stuff,
        'book_models': ModelsPb(BOOK_MODELS, BOOK_CATEGORY_ID),
        'book_models2': ModelsPb(BOOK_MODELS2, BOOK_CATEGORY2_ID),
        'non_book_models': ModelsPb(NON_BOOK_MODELS, NON_BOOK_CATEGORY_ID),
        'book_parameters': ParametersPb(BOOK_CATEGORY),
        'book_parameters2': ParametersPb(BOOK_CATEGORY2),
        'non_book_parameters': ParametersPb(NON_BOOK_CATEGORY),
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors)
    }
    with BooksIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_book_hid(workflow):
    """hyper_id книги должен прокидываться
    через группировочный атрибут hyper.
    """
    expected_ids = [BOOK_MODEL1_ID, BOOK_MODEL2_ID, BOOK_MODEL3_ID]
    unexpected_ids = [NON_BOOK_MODEL_ID]
    assert_that(
        workflow,
        all_of(
            HasDocsWithValues('hyper', expected_ids),
            DoesntHaveDocsWithValues('hyper', unexpected_ids),
        ),
        six.u('Модели книг совпадают с заданными в выгрузке MBO'),
    )


def test_book_category(workflow):
    """hyper_id категории книги должен прокидываться
    через группировочный атрибут hidd.
    """
    assert_that(
        workflow,
        all_of(
            HasDocs().attributes(hidd=str(BOOK_CATEGORY_ID)).count(2),
            HasDocs().attributes(hidd=str(BOOK_CATEGORY2_ID)).count(1),
            is_not(HasDocs().attributes(hidd=str(NON_BOOK_CATEGORY_ID)).count(None)),
        ),
        six.u('Категория книги совпадает с заданной в выгрузке MBO'),
    )


def test_book_description(workflow):
    """Книга должна обогащаться описанием из YT.
    """
    assert_that(
        workflow,
        all_of(
            HasDocsWithValues(
                'description',
                [BOOK_MODEL1_STUFF['description']],
            ),
            is_not(HasDocsWithValuesMatching(
                'description',
                starts_with(BOOK_MODEL2_STUFF['description']),
            )),
            HasDocsWithValues(
                'description',
                [BOOK_GOOD_DESCRIPTION],
            ),
            is_not(HasDocsWithValuesMatching(
                'description',
                starts_with(BOOK_BAD_DESCRIPTION),
            )),
        ),
        six.u('Описание книги совпадает с ожидаемым'),
    )


def test_book_fields(workflow):
    """Книга должна обогащаться параметрами из YT.
    """
    # TODO(@bzz13): some mistakes
    # diff between py2 and py3
    # ISBN 1-56619-909-3 1-4028-9462-7
    # MboModel
    # UkcIwbGvBxAEKj4KAnJ1EjhZb3UgYXJlIHJlYWRpbmcgYW4gYXJiaXRyYXJ5IGJvcmluZyB0ZXN0IGJvb2sgZGVzY3JpcGlvblIcCOGxrwcQBCoTCgJydRINMS01NjYxOS05MDktMw==
    # UkcIwbGvBxAEKj4KAnJ1EjhZb3UgYXJlIHJlYWRpbmcgYW4gYXJiaXRyYXJ5IGJvcmluZyB0ZXN0IGJvb2sgZGVzY3JpcGlvblIcCOGxrwcQBCoTCgJydRINMS00MDI4LTk0NjItNw==
    assert_that(
        workflow,
        all_of(
            # HasDocsWithValues(
            #     'ISBN',
            #     ISBNS[1:2],
            # ),
            HasDocsWithValues(
                'publishing_year',
                [BOOK_YEAR],
            ),
            HasDocsWithValues(
                'book_authors',
                [BOOK_AUTHOR],
            ),
            HasDocsWithValues(
                'publisher',
                [BOOK_PUBLISHER],
            ),
            HasDocsWithValues(
                'book_series',
                [BOOK_SERIES],
            )
        ),
        six.u('Параметры книги совпадают с ожидаемым'),
    )


def test_book_isbn_literals(workflow):
    """Все ISBN должны прокидываться через литералы isbn.
    """
    assert_that(
        workflow,
        all_of(*(
            HasDocs().literals(isbn=isbn.replace('-', '')).count(1)
            for isbn in ISBNS
        )),
        six.u('ISBN совпдают с ожидаемыми'),
    )


def test_book_pictures(workflow):
    """Книги должны обогащаться картинками из YT.
    """
    bs_model1_picture = make_picrobot_picture_proto(BOOK_MODEL1_STUFF['pic'][0])
    bs_model2_picture = make_picrobot_picture_proto(BOOK_MODEL2_STUFF['pic'][0])

    model1_picture_big = make_picrobot_picture_mbo_proto(BOOK_MODEL1_PICTURES[0])
    model2_picture_big = make_picrobot_picture_mbo_proto(BOOK_MODEL2_PICTURES[0])

    assert_that(
        workflow,
        all_of(
            # У первой модели value_source == AUTO, а значит картинка берется из YT
            HasDocsWithValues('PicturesProtoBase64', [bs_model1_picture]),
            DoesntHaveDocsWithValues('PicturesProtoBase64', [model1_picture_big]),
            # У второй модели картинка берется из данных MBO
            HasDocsWithValues('ProtoPicInfo', [model2_picture_big]),
            DoesntHaveDocsWithValues('ProtoPicInfo', [bs_model2_picture]),
        ),
        six.u('Картинки книг совпадают с ожидаемыми'),
    )


def test_book_barcode_literals(workflow):
    """Все баркоды должны прокидываться через литералы barcode.
    """
    barcodes = (
        ISBNS +
        BOOK_MODEL1_STUFF['barcodes'] +
        BOOK_MODEL1_EXTRA_BARCODES
    )

    assert_that(
        workflow,
        all_of(*(
            HasDocs().literals(barcode=barcode.replace('-', '')).count(1)
            for barcode in barcodes
        )),
        six.u('Баркоды совпадают с ожидаемыми'),
    )


def test_book_model_color(workflow):
    """Опубликованность на белом маркете должна правильно прокидываться в model_color.
    """
    assert_that(
        workflow,
        all_of(
            HasDocs().attributes(hyper=str(BOOK_MODEL1_ID)).literals(model_color='white').count(1),
            HasDocs().attributes(hyper=str(BOOK_MODEL2_ID)).literals(model_color=None).count(1),
        ),
        six.u('model_color корректно отражает published_on_market'),
    )


def test_book_vendor_id_literal(workflow):
    """Проверяем, что для книг с не файковым вендором проставляется литерал vendor_id
    """
    assert_that(
        workflow,
        all_of(
            HasDocs().attributes(hyper=str(BOOK_MODEL1_ID)).literals(vendor_id=str(VENDOR_ID_1)).count(1),
            HasDocs().attributes(hyper=str(BOOK_MODEL2_ID)).literals(vendor_id=None).count(1),
        ),
        six.u('литерал vendor_id корректно заполняется по vendor_id и только для не фейковых вендоров'),
    )


def test_book_vendor_id_attribute(workflow):
    """Проверяем, что для книг проставляется атрибут vendor_id
    """
    assert_that(
        workflow,
        all_of(
            HasDocsWithValues('vendor_id', [VENDOR_ID_1, VENDOR_ID_2]),
            DoesntHaveDocsWithValues('vendor_id', [98374742]),
        ),
        six.u('атрибут vendor_id проставляется корректно'),
    )
