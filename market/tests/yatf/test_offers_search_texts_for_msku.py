# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to, has_entries, contains_string, has_key


from market.idx.streams.src.streams_converter.yatf.test_env import StreamsConverterTestEnv, StreamsConverterMode
from market.idx.streams.src.streams_converter.yatf.resources.streams_converter_input import JoinOffersTextsToMskuInput
from market.idx.streams.src.streams_converter.yatf.resources.streams_converter_output import JoinOffersTextsToMskuOutput
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

from mapreduce.yt.python.table_schema import extract_column_attributes

"""
Тест проверяет YT джобу, которая джойнит поисковые офферные тексты из таблицы (blue_)offers_search_texts к их мску
для последующего добавления в факторанн в виде стримов двух типов - тайтлы и все остальные поисковые тексты.
Дублирующиеся тексты (в рамках определенного типа поискового текста, напр. category-literals или title+category+venor_code) - удаляются.
"""


ROOT_HID = 90401
DEFAULT_HID = 13
HID_NAME = 'Leaf_hid'
HID_NAME_ALIAS = 'Leaf_hid_alias'

NO_SEARCH_HID = 14
NO_SEARCH_HID_NAME = 'No_search_hid'


@pytest.yield_fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=ROOT_HID,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
            no_search=True,
        ),
        MboCategory(
            hid=DEFAULT_HID,
            tovar_id=1,
            parent_hid=ROOT_HID,
            unique_name=HID_NAME,
            name=HID_NAME,
            output_type=MboCategory.GURULIGHT,
            aliases=[HID_NAME_ALIAS],
        ),
        MboCategory(
            hid=NO_SEARCH_HID,
            tovar_id=1,
            parent_hid=ROOT_HID,
            unique_name=NO_SEARCH_HID_NAME,
            name=NO_SEARCH_HID_NAME,
            output_type=MboCategory.GURULIGHT,
            no_search=True,
        ),
    ]


offer_texts_table_schema = [
    dict(name='ware_md5', type='string'),
    dict(name='table_index', type='uint64'),
    dict(name='msku', type='int64'),
    dict(name='is_fake_msku_offer', type='boolean'),
    dict(name='category_id', type='int64'),
    dict(name='title', type='string'),
    dict(name='description', type='string'),
    dict(name='supplier_description', type='string'),
    dict(name='vendor_code', type='string'),
    dict(name='additional_search_text', type='string'),
    dict(name='book_authors', type='string'),
    dict(name='url', type='string'),
]


def __get_title(id):
    return 'title_' + id


def __get_description(id):
    return 'description_' + id


def __get_suppl_description(id):
    return 'suppl_description_' + id


def __get_add_search_text(id):
    return 'add_search_text_' + id


def __get_book_authors(id):
    return 'book_authors_' + id


def __get_vendor_code(id):
    return 'vendor_code_' + id


def __get_url(id):
    return 'url_' + id


def __make_title_category_vendor_code(title, category_title, vendor_code, no_search=False):
    if no_search:
        category_title = ""
    return title + ' ' + category_title + ' ' + vendor_code


def __make_category_literals(category_title, category_alias, no_search=False):
    if no_search:
        return ""
    return category_title + ' ' + category_alias


MSKU1 = 'msku1'
MSKU1_WARE_MD5 = 'hc1cVZiClnllcxjhGX0_m1'
MSKU3 = 'msku3'
MSKU4 = 'msku4'
MSKU4_WARE_MD5 = 'hc1cVZiClnllcxjhGX0_m4'

OFFER1_1 = 'offer1_1'
OFFER1_2 = 'offer1_2'
OFFER2_2 = 'offer2_1'


MSKU1_RESULT = {
    'ware_md5': MSKU1_WARE_MD5,
    'url': __get_url(MSKU1),
    'titles': [
        # offer1_1
        __get_title(OFFER1_1),
        # offer1_2 - тайтл дубликат
        # offer1_3 - тайтл пустой
    ],
    'texts': [
        # offer1_1
        __get_description(OFFER1_1), __get_add_search_text(OFFER1_1), __get_url(OFFER1_1),
        __get_book_authors(OFFER1_1), __make_title_category_vendor_code(__get_title(OFFER1_1), HID_NAME, __get_vendor_code(OFFER1_1)),
        __make_category_literals(HID_NAME, HID_NAME_ALIAS), __get_suppl_description(OFFER1_1),
        # __make_no_plus_minus_alias(__get_title(OFFER1_1)) - пустой, если в тайтле нет "+"/"-"

        # offer1_2
        __get_description(OFFER1_2), __get_url(OFFER1_2),
        __get_book_authors(OFFER1_2), __make_title_category_vendor_code(__get_title(OFFER1_1), HID_NAME, __get_vendor_code(OFFER1_2)),
        # __get_add_search_text(OFFER1_1) - дубликат

        # offer1_3 (пустые тексты)
        __make_title_category_vendor_code('', HID_NAME, ''),
    ],
}


MSKU4_RESULT = {
    'ware_md5': MSKU4_WARE_MD5,
    'url': __get_url(MSKU4),
    'texts': [
        __make_title_category_vendor_code('', NO_SEARCH_HID_NAME, '', no_search=True),

        # __make_category_literals(HID_NAME, '') - пустой, т.к. no_search=True
        # __make_no_plus_minus_alias(title)) - пустой, если в тайтле нет "+"/"-"
    ],
}


@pytest.fixture(scope='module')
def offer_texts_table(yt_stuff):
    data = [
        # мску и офферы - мску попадает в стримы с уникальными офферными текстами  TODO - проверить None, хоть и не модет быть
        dict(msku=1, is_fake_msku_offer=True, ware_md5=MSKU1_WARE_MD5, table_index=0L, category_id=DEFAULT_HID,
             title=__get_title(MSKU1), description=__get_description(MSKU1), vendor_code=__get_vendor_code(MSKU1),
             additional_search_text=__get_add_search_text(MSKU1), book_authors=__get_book_authors(MSKU1), url=__get_url(MSKU1),
             supplier_description=__get_suppl_description(MSKU1)),

        dict(msku=1, is_fake_msku_offer=False, ware_md5='white_offer1_hc1cVZiCl', table_index=0L, category_id=DEFAULT_HID,
             title=__get_title(OFFER1_1), description=__get_description(OFFER1_1), vendor_code=__get_vendor_code(OFFER1_1),
             additional_search_text=__get_add_search_text(OFFER1_1), book_authors=__get_book_authors(OFFER1_1), url=__get_url(OFFER1_1),
             supplier_description=__get_suppl_description(OFFER1_1)),

        # title, additional_search_text - дубликаты с OFFER1_1, supplier_description - совпадает с description
        dict(msku=1, is_fake_msku_offer=False, ware_md5='white_offer2_hc1cVZiCl', table_index=0L, category_id=DEFAULT_HID,
             title=__get_title(OFFER1_1), description=__get_description(OFFER1_2), vendor_code=__get_vendor_code(OFFER1_2),
             additional_search_text=__get_add_search_text(OFFER1_1), book_authors=__get_book_authors(OFFER1_2), url=__get_url(OFFER1_2),
             supplier_description=__get_description(OFFER1_2)),

        dict(msku=1, is_fake_msku_offer=False, ware_md5='white_offer3_hc1cVZiCl', table_index=0L, category_id=DEFAULT_HID,
             title='', description='', vendor_code='', additional_search_text='', book_authors='', url=''),

        # оффер без мску - мску не попадает в стримы
        dict(msku=2, is_fake_msku_offer=False, ware_md5='white_offer4_hc1cVZiCl', table_index=0L, category_id=DEFAULT_HID,
             title=__get_title(OFFER2_2), description=__get_description(OFFER2_2), vendor_code=__get_vendor_code(OFFER2_2),
             additional_search_text=__get_add_search_text(OFFER2_2), book_authors=__get_book_authors(OFFER2_2), url=__get_url(OFFER2_2)),

        # мску без офферов - мску не попадает в стримы
        dict(msku=3, is_fake_msku_offer=True, ware_md5='hc1cVZiClnllcxjhGX0_m3', table_index=0L, category_id=DEFAULT_HID,
             title=__get_title(MSKU3), description=__get_description(MSKU3), vendor_code=__get_vendor_code(MSKU3),
             additional_search_text=__get_add_search_text(MSKU3), book_authors=__get_book_authors(MSKU3), url=__get_url(MSKU3)),

        # мску с оффером почти без текстов
        dict(msku=4, is_fake_msku_offer=True, ware_md5='hc1cVZiClnllcxjhGX0_m4', table_index=0L, category_id=NO_SEARCH_HID,
             title=__get_title(MSKU4), description=__get_description(MSKU4), vendor_code=__get_vendor_code(MSKU4),
             additional_search_text=__get_add_search_text(MSKU4), book_authors=__get_book_authors(MSKU4), url=__get_url(MSKU4)),

        dict(msku=4, is_fake_msku_offer=False, ware_md5='white_offer5_hc1cVZiCl', table_index=0L, category_id=NO_SEARCH_HID,
             title='', description='', vendor_code='', additional_search_text='', book_authors='', url=''),
        dict(msku=4, is_fake_msku_offer=False, ware_md5='white_offer5_hc1cVZiCl', table_index=0L, category_id=NO_SEARCH_HID,
             title=None, description=None, vendor_code=None, additional_search_text=None, book_authors=None, url=None),
    ]
    table = YtTableResource(yt_stuff, "//indexer/mi3/main/offers_search_texts", data, attributes={'schema': offer_texts_table_schema})
    return table


@pytest.fixture(scope='module')
def input_data(offer_texts_table):
    tmp_path = '//indexer/streams/offers/msku/tmp'
    res = JoinOffersTextsToMskuInput(offer_texts_table, tmp_path)
    return res


# output info and YT tables
@pytest.fixture(scope='module')
def output_data():
    out_offer_titles_stream = '//indexer/streams/offers/msku/titles'
    out_offer_texts_stream = '//indexer/streams/offers/msku/texts'
    return JoinOffersTextsToMskuOutput(out_offer_titles_stream, out_offer_texts_stream)


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, output_data, tovar_tree):
    resources = {
        "input": input_data,
        "output": output_data,
        "tovar_tree_pb":  TovarTreePb(tovar_tree),
    }

    with StreamsConverterTestEnv(**resources) as env:
        env.execute(StreamsConverterMode.JOIN_OFFERS_TEXTS_TO_MSKU, yt_stuff)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def offer_titles_stream_yt_table(workflow):
    return workflow.outputs.get('offer_titles_stream_table')


@pytest.fixture(scope='module')
def offer_texts_stream_yt_table(workflow):
    return workflow.outputs.get('offer_texts_stream_table')


@pytest.fixture(scope='module')
def offer_titles_stream_yt_data(workflow):
    return workflow.outputs.get('offer_titles_stream_data')


@pytest.fixture(scope='module')
def offer_texts_stream_yt_data(workflow):
    return workflow.outputs.get('offer_texts_stream_data')


def check_result_table_schema(table):
    assert_that(extract_column_attributes(list(table.schema)),
                equal_to([
                    {'required': False, "name": "ware_md5", "type": "string"},
                    {'required': False, "name": "part", "type": "uint64"},
                    {'required': False, "name": "region_id", "type": "uint64"},
                    {'required': False, "name": "text", "type": "string"},
                    {'required': False, "name": "url", "type": "string"},
                    {'required': False, "name": "value", "type": "string"},
                ]), "Schema is incorrect")


def test_offer_titles_stream_table_schema(offer_titles_stream_yt_table):
    check_result_table_schema(offer_titles_stream_yt_table)


def test_offer_texts_stream_table_schema(offer_texts_stream_yt_table):
    check_result_table_schema(offer_texts_stream_yt_table)


def test_offer_titles_stream_table_len(offer_titles_stream_yt_table, yt_stuff):
    assert_that(len(offer_titles_stream_yt_table.data), equal_to(1))  # MSKU1 (MSKU4 - без тайтлов)


def test_offer_texts_stream_table_len(offer_texts_stream_yt_table, yt_stuff):
    assert_that(len(offer_texts_stream_yt_table.data), equal_to(2))   # MSKU1 + MSKU4


@pytest.mark.parametrize('msku', [MSKU1_RESULT])
def test_offer_titles_stream(offer_titles_stream_yt_data, msku):

    assert_that(offer_titles_stream_yt_data, has_key(msku['ware_md5']), "No expected msku")
    actual_data = offer_titles_stream_yt_data[msku['ware_md5']]
    assert_that(
        actual_data,
        has_entries({'ware_md5': msku['ware_md5'], 'region_id': 225, 'url': msku['url'], 'value': '1', 'part': 0}),
        "No expected msku data"
    )

    actual_titles_str = actual_data['text']
    expected_titles = msku['titles']

    for title in expected_titles:
        assert_that(actual_titles_str, contains_string(title), 'No expected search title {}'.format(title))

    assert_that(
        len(actual_titles_str),
        equal_to(len(' '.join(expected_titles))),
        'Actual data has extra search titles, actual titles = {}'.format(actual_titles_str)
    )


@pytest.mark.parametrize('msku', [MSKU1_RESULT, MSKU4_RESULT])
def test_offer_texts_stream(offer_texts_stream_yt_data, msku):

    assert_that(offer_texts_stream_yt_data, has_key(msku['ware_md5']), "No expected msku")
    actual_data = offer_texts_stream_yt_data[msku['ware_md5']]
    assert_that(
        actual_data,
        has_entries({'ware_md5': msku['ware_md5'], 'region_id': 225, 'url': msku['url'], 'value': '1', 'part': 0}),
        "No expected msku data"
    )

    actual_texts_str = actual_data['text']
    expected_texts = msku['texts']

    for text in expected_texts:
        assert_that(actual_texts_str, contains_string(text), 'No expected search text {}'.format(text))

    assert_that(
        len(actual_texts_str),
        equal_to(len(' '.join(expected_texts))),
        'Actual data has extra search texts, actual texts = {}'.format(actual_texts_str)
    )
