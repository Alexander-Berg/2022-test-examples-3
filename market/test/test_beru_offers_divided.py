# -*- coding: utf-8 -*-
import pytest
from lxml import etree

from yt.wrapper import ypath_join

from market.idx.export.awaps.yatf.resources.beru_offers_table import BeruOffersTable
from market.idx.export.awaps.yatf.resources.categories_table import CategoriesTable
from market.idx.export.awaps.yatf.test_envs.awaps_models import YtAwapsModelsTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from library.python import resource
from util import msku_url_doware


DATA = [
    {
        'category_id': 1,
        'vendor': 'VendorA',
        'title': 'Some test',
        'market_sku': 100210851212,
        'model_id': 1717054632,
        'published_on_market': True,
        'ware_md5': 'do_not_use_this_offer_id',
        'price': 5025000000,
        'picture': '//avatars.mds.yandex.net/get-mpic/372220/img_id8062181699821043420/orig',
        'currency': 'RUB',
        'oldprice': 5026000000,
        'available': True,
        'custom_label_category_id': 'msk_city'
    },
    {
        'market_sku': 100309815654,
        'model_id': 1717054631,
        'published_on_market': False,
        'ware_md5': 'my_offer_id',
        'price': 5026000000,
        'currency': 'RUB',
        'category_id': 2,
        'vendor': 'VendorB',
        'title': 'Some test 2',
        'picture': '//avatars.mds.yandex.net/get-mpic/1246680/img_id2358682066965280419.jpeg/orig',
        'oldprice': 5027000000,
        'available': False,
        'custom_label_category_id' : 'msk_city'
    }
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
        'hyper_id': 2,
        'id': 2,
        'name': 'Derived',
        'uniq_name': 'Derived',
        'parent': 1,
        'parents': '1,2,',
        'type': 'guru'
    }
]


@pytest.fixture(scope='module', params=['beru-offers'])
def feed_type(request):
    yield request.param


@pytest.fixture(scope='module', params=[
    {'name': u'Yandex Market', 'domain': 'market.yandex.ru'}
])
def blue_brand(request):
    yield request.param


@pytest.fixture(scope='module')
def beru_offers_table(yt_server, feed_type):
    tablepath = ypath_join(get_yt_prefix(), str(feed_type), 'out', 'banner', 'beru_offers_divided')
    return BeruOffersTable(yt_server, tablepath, DATA)


@pytest.fixture(scope='module')
def categories_table(yt_server, feed_type):
    tablepath = ypath_join(get_yt_prefix(), str(feed_type), 'in', 'categories')
    return CategoriesTable(yt_server, tablepath, CATEGORIES)


@pytest.fixture(scope='module')
def banner_upload_wf(yt_server, beru_offers_table, categories_table, feed_type, blue_brand):
    resources = {
        'beru_offers_table': beru_offers_table,
        'categories_table': categories_table
    }

    bin_flags = [
        '--input', beru_offers_table.get_path(),
        '--categories-tree', categories_table.get_path(),
        '--feed', feed_type,
        '--use-partition-by-field',
        '--naming-scheme', 'NoTotalOffers',
        '--use-part-names',
        # '--blue_domain', blue_brand['domain'],
        # '--blue_name', blue_brand['name'],
        '--blue-on-market',
    ]

    # зададим/проверим только первый части разбияния (наличие всех департаметнов), чтобы при пересотировке не "плыл" тест
    part_names = (
        "-ue",
        "-rus-rostov",
        "-rostov",
        "-msk_city",
        "-msk_obl",
        "-spb",
        "-msk_city+msk_obl",
        "-msk_city+spb",
        "-msk_obl+spb",
        "-msk_city+msk_obl+spb",
        "-rus-avto-1",
        "-rus-bytovaya_tekhnika-1",
        "-rus-dacha_sad_i_ogorod-1",
        "-rus-detskie_tovary-1",
        "-rus-dosug_i_razvlecheniya-1",
        "-rus-kompyuternaya_tekhnika-1",
        "-rus-oborudovanie-1",
        "-rus-odezhda_obuv_i_aksessuary-1",
        "-rus-produkty-1",
        "-rus-sport_i_otdyh-1",
        "-rus-stroitelstvo_i_remont-1",
        "-rus-tovary_dlya_doma-1",
        "-rus-tovary_dlya_zhivotnyh-1",
        "-rus-tovary_dlya_krasoty-1",
        "-rus-elektronika-1",
        "-rus-tovary_dlya_zdorovya-1",
        "-rus-upakovochnye_materialy_dlya_beru-1",
        "-rus-uslugi-1",
        "-rus-knigi-1",
        "-rus-tsvety_bukety_kompozitsii-1",
        "-not-have-config-part-warning"       # выделенная часть, для ловли офферов, которые не фильтруются конфигом
    )

    output_file_name_suffixes = ['-part{}'.format(name) for name in part_names]  # Check that all parts exist

    with YtAwapsModelsTestEnv(yt_stuff=yt_server,
                              bin_flags=bin_flags,
                              output_file_name_suffixes=output_file_name_suffixes,
                              partition_config=resource.find('/k50-beru-beru_offers-full.json'),
                              **resources) as banner_upload:
        banner_upload.execute(ex=True)
        banner_upload.verify()
        yield banner_upload


@pytest.fixture(scope='module')
def output_xml(banner_upload_wf):
    return banner_upload_wf.outputs['offers-part-msk_city']  # Test only one of the parts


def build_offer_url(blue_domain, offer):
    return msku_url_doware(
        blue_domain=blue_domain,
        model_id=offer['model_id'],
        market_sku=offer['market_sku'],
        title=offer['title'],
        utm_term='{}|{}'.format(offer['category_id'], offer['market_sku']),
        ware_md5=offer['ware_md5'],
        published_on_market=offer['published_on_market'],
    )


def _get_tag_text(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str)
    return [tag.text for tag in xpath(root_etree)]


def _get_tag_attr(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str)
    return [tag for tag in xpath(root_etree)]


def test_shop_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop')
    assert len(actual) == 1


def test_shop_name(output_xml, blue_brand):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/name')
    assert len(actual) == 1 and actual[0] == blue_brand['name']


def test_shop_company(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/company')
    assert len(actual) == 1 and actual[0] == 'Yandex'


def test_shop_url(output_xml, blue_brand):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/url')
    shop_url = 'https://{}'.format(blue_brand['domain'])
    assert len(actual) == 1 and actual[0] == shop_url


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


def test_shop_offers_available(output_xml):
    actual = _get_tag_attr(output_xml, '/yml_catalog/shop/offers/offer/@available')
    expected = ['true'] * len(DATA)
    assert actual == expected


def test_shop_offers_urls(output_xml, blue_brand):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/url')
    expected = [build_offer_url(blue_brand['domain'], offer) for offer in DATA]
    assert actual == expected


def test_shop_offers_prices(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/price')
    expected = [str(int(round(offer['price']/10000000.0))) for offer in DATA]
    assert actual == expected


def test_shop_offers_oldprices(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/oldprice')
    expected = [str(int(round(offer['oldprice']/10000000.0))) for offer in DATA]
    assert actual == expected


def test_shop_offers_currency_ids(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/currencyId')
    expected = [str(offer['currency']) for offer in DATA]
    assert actual == expected


def test_shop_offers_category_ids(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/categoryId')
    expected = [str(offer['category_id']) for offer in DATA]
    assert actual == expected


def test_shop_offers_pictires_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/picture')
    assert len(actual) == len(DATA)


def test_shop_offers_names(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/name')
    expected = [str(offer['title']) for offer in DATA]
    assert actual == expected


def test_shop_offers_vendors(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/vendor')
    expected = [str(offer['vendor']) for offer in DATA]
    assert actual == expected
