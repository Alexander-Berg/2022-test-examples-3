# -*- coding: utf-8 -*-
import pytest
from lxml import etree
from yt.wrapper import ypath_join

from market.idx.export.awaps.yatf.resources.blue_adult_offers_table import BlueAdultOffersTable
from market.idx.export.awaps.yatf.resources.categories_table import CategoriesTable
from market.idx.export.awaps.yatf.test_envs.awaps_models import YtAwapsModelsTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from util import msku_url

DATA = [
    {
        'market_sku': 100569398755,
        'model_id': 367872079,
        'published_on_market': True,
        'ware_md5': 'do_not_use_this_offer_id',
        'price': 11140000000,
        'category_id': 6290384,
        'vendor': 'Lola Toys',
        'title': 'Discovery Racer 6900',
        'picture': '//avatars.mds.yandex.net/get-mpic/1602935/img_id7229585067025374104.jpeg/orig'
    },
    {
        'market_sku': 100530039170,
        'model_id': 297534468,
        'published_on_market': False,
        'ware_md5': 'my_offer_id',
        'price': 8550000000,
        'category_id': 6290267,
        'vendor': u'МиФ',
        'title': u'Николай 16,5 см',
        'picture': '//avatars.mds.yandex.net/get-mpic/1081556/img_id822135451021393179.jpeg/orig'
    }
]


CATEGORIES = [
    {
        'hyper_id': 6091783,
        'id': 1,
        'name': 'Root',
        'uniq_name': 'Root',
        'parent': 0,
        'parents': '1,',
        'type': 'simple'
    },
    {
        'hyper_id': 6290384,
        'id': 2,
        'name': 'Base',
        'uniq_name': 'Base',
        'parent': 6091783,
        'parents': '6091783,',
        'type': 'simple'
    },
    {
        'hyper_id': 6290267,
        'id': 3,
        'name': 'Derived',
        'uniq_name': 'Derived',
        'parent': 6290384,
        'parents': '6091783,6290384,',
        'type': 'guru'
    }
]

PICROBOT_THUMBS = ['600x600', '1x1']


@pytest.fixture(scope='module', params=["blue-adult-offers"])
def feed_type(request):
    yield request.param


@pytest.fixture(scope='module', params=['market.yandex.ru'])
def blue_domain(request):
    yield request.param


@pytest.fixture(scope='module')
def blue_adult_offers_table(yt_server, feed_type):
    tablepath = ypath_join(get_yt_prefix(), str(feed_type), 'out', 'banner', 'blue-adult-offers')
    return BlueAdultOffersTable(yt_server, tablepath, DATA)


@pytest.fixture(scope='module')
def categories_table(yt_server, feed_type):
    tablepath = ypath_join(get_yt_prefix(), str(feed_type), 'in', 'categories')
    return CategoriesTable(yt_server, tablepath, CATEGORIES)


@pytest.fixture(scope='module')
def banner_upload_wf(yt_server, blue_adult_offers_table, categories_table, feed_type, blue_domain):
    resources = {
        'blue_adult_offers_table': blue_adult_offers_table,
        'categories_table': categories_table,
    }
    bin_flags = [
        '--input', blue_adult_offers_table.get_path(),
        '--categories-tree', categories_table.get_path(),
        '--feed', feed_type,
        '--blue_domain', blue_domain,
        '--blue-on-market',
    ]

    with YtAwapsModelsTestEnv(yt_stuff=yt_server, bin_flags=bin_flags, **resources) as banner_upload:
        banner_upload.execute()
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
    expected = [str(offer['market_sku']) for offer in DATA]
    assert actual == expected


def test_shop_offers_available(output_xml, feed_type):
    actual = _get_tag_attr(output_xml, '/yml_catalog/shop/offers/offer/@available')
    expected = ['true'] * len(DATA)
    assert actual == expected


def test_shop_offers_urls(output_xml, blue_domain):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/url')
    expected = [
        msku_url(
            blue_domain=blue_domain,
            model_id=offer['model_id'],
            market_sku=offer['market_sku'],
            title=offer['title'].encode('utf-8'),
            utm_term=None,
            ware_md5=offer['ware_md5'],
            published_on_market=offer['published_on_market'],
        )
        for offer in DATA
    ]
    assert actual == expected


def test_shop_offers_prices(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/price')
    expected = [str(offer['price'] / 10000000) for offer in DATA]
    assert actual == expected


def test_shop_offers_currency_ids(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/currencyId')
    expected = ["RUB"] * len(DATA)
    assert actual == expected


def test_shop_offers_category_ids(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/categoryId')
    expected = [str(offer['category_id']) for offer in DATA]
    assert actual == expected


def test_shop_offers_pictures_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/picture')
    assert len(actual) == len(DATA)


def test_shop_offers_names(output_xml):
    actual = map(lambda x: x.encode('utf-8'), _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/name'))
    expected = [offer['title'].encode('utf-8') for offer in DATA]
    assert actual == expected


def test_shop_offers_vendors(output_xml):
    actual = map(lambda x: x.encode('utf-8'), _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/vendor'))
    expected = [offer['vendor'].encode('utf-8') for offer in DATA]
    assert actual == expected
