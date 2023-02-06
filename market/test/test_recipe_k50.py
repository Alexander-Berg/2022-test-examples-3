# -*- coding: utf-8 -*-
import pytest
from lxml import etree

from yt.wrapper import ypath_join

from market.idx.export.awaps.yatf.resources.white_recipe_k50 import WhiteRecipeK50
from market.idx.export.awaps.yatf.resources.categories_table import CategoriesTable
from market.idx.export.awaps.yatf.test_envs.awaps_models import YtAwapsModelsTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.pylibrary import slug


DATA = [
    {
        'hyper_id': 123,
        'nid': 45,
        'recipe_id': 456,
        'recipe_header': 'Telefony 256Gb',
        'price': 8,
        'picture_url': 'http://picture.ru',
        'recipe_filters_list': [
            "5127597:1"
        ],
    },
]


CATEGORIES = [
    {
        'hyper_id': 1,
        'id': 1,
        'name': 'Base',
        'uniq_name': 'Base',
        'parent': 0,
        'parents': '1,',
        'type': 'simple'
    },
    {
        'hyper_id': 123,
        'id': 2,
        'name': 'Derived',
        'uniq_name': 'Derived',
        'parent': 1,
        'parents': '1,2,',
        'type': 'guru'
    }
]

PICROBOT_THUMBS = ['600x600', '1x1']


@pytest.fixture(scope='module')
def white_recipe_k50(yt_server):
    tablepath = ypath_join(get_yt_prefix(), 'out', 'banner', 'white_recipe_k50')
    return WhiteRecipeK50(yt_server, tablepath, DATA)


@pytest.fixture(scope='module')
def categories_table(yt_server):
    tablepath = ypath_join(get_yt_prefix(), 'in', 'categories')
    return CategoriesTable(yt_server, tablepath, CATEGORIES)


@pytest.fixture(scope='module')
def banner_upload_wf(yt_server, white_recipe_k50, categories_table):
    resources = {
        'white_recipe_k50': white_recipe_k50,
        'categories_table': categories_table,
    }
    bin_flags = [
        '--input', white_recipe_k50.get_path(),
        '--categories-tree', categories_table.get_path(),
        '--feed', 'recipe-k50',
        '--naming-scheme', 'SinglePart',
    ]

    with YtAwapsModelsTestEnv(yt_stuff=yt_server, bin_flags=bin_flags, **resources) as banner_upload:
        banner_upload.execute(ex=True)
        banner_upload.verify()
        yield banner_upload


@pytest.fixture(scope='module')
def output_xml(banner_upload_wf):
    return banner_upload_wf.outputs['offers']


def _get_tag_text(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str)
    return [tag.text for tag in xpath(root_etree)]


def _get_tag_attr(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str)
    return [tag for tag in xpath(root_etree)]


def test_shop_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop')
    assert len(actual) == 1


def test_shop_name(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/name')
    assert len(actual) == 1 and actual[0] == 'Yandex Market'


def test_shop_company(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/company')
    assert len(actual) == 1 and actual[0] == 'Yandex'


def test_shop_url(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/url')
    assert len(actual) == 1 and actual[0] == 'https://market.yandex.ru'


def test_shop_curency_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/currencies/currency')
    assert len(actual) == 1


def test_shop_curency_id(output_xml):
    actual = _get_tag_attr(output_xml, '/yml_catalog/shop/currencies/currency/@id')
    assert len(actual) == 1 and actual[0] == 'RUB'


def test_shop_currency_rate(output_xml):
    actual = _get_tag_attr(output_xml, '/yml_catalog/shop/currencies/currency/@rate')
    assert len(actual) == 1 and actual[0] == '1'


def test_shop_categories_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/categories/category')
    assert len(actual) == len(CATEGORIES)


def test_shop_categories_ids(output_xml):
    actual = _get_tag_attr(output_xml, '/yml_catalog/shop/categories/category/@id')
    expected = [str(cat['hyper_id']) for cat in CATEGORIES]
    assert actual == expected


def test_shop_categories_name(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/categories/category')
    expected = [str(cat['name']) for cat in CATEGORIES]
    assert actual == expected


def test_shop_offers_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer')
    assert len(actual) == len(DATA)


def test_shop_offers_ids(output_xml):
    actual = _get_tag_attr(output_xml, '/yml_catalog/shop/offers/offer/@id')
    expected = [str(row['recipe_id']) for row in DATA]
    assert actual == expected


def test_shop_offers_available(output_xml):
    actual = _get_tag_attr(output_xml, '/yml_catalog/shop/offers/offer/@available')
    expected = ['true'] * len(DATA)
    assert actual == expected


def test_shop_offers_urls(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/url')
    expected = ['https://market.yandex.ru/catalog--{slug}/{nid}/list?hid={hid}{glfilter}&utm_term={utm_term}'.format(
        nid=row['nid'],
        hid=row['hyper_id'],
        slug=slug.translit(row['recipe_header']),
        glfilter=''.join(map(lambda recipe_filter: '&glfilter=%s' % recipe_filter.replace(':', '%3A'), row['recipe_filters_list'])),
        utm_term='{}|{}'.format(row['hyper_id'], row['recipe_id']),
    ) for row in DATA]
    assert actual == expected


def test_shop_offers_prices(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/price')
    expected = [str(int(row['price']/10000000.0)) for row in DATA]
    assert actual == expected


def test_shop_offers_currency_ids(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/currencyId')
    expected = ['RUB'] * len(DATA)
    assert actual == expected


def test_shop_offers_category_ids(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/categoryId')
    expected = [str(row['hyper_id']) for row in DATA]
    assert actual == expected


def test_shop_offers_pictires_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/picture')
    assert len(actual) == len(DATA)


def test_shop_offers_names(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/name')
    expected = [str(row['recipe_header']) for row in DATA]
    assert actual == expected
