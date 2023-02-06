# coding=utf-8
import pytest
from lxml import etree
from urllib import quote_plus

from yt.wrapper import ypath_join

from market.idx.export.awaps.yatf.resources.blue_vendor_category_table import BlueVendorCategoryTable
from market.idx.export.awaps.yatf.resources.categories_table import CategoriesTable
from market.idx.export.awaps.yatf.test_envs.awaps_models import YtAwapsModelsTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.pylibrary import slug
from util import cantor_pairing
from library.python import resource


DATA = [
    {
        'category_id': 90564,
        'category_nid': 12345,
        'blue_category_nid': 7777777,
        'negativekw': ['first', 'second'],
        'vendor': 'Ardor',
        'category_name': 'Scarlett',
        'price': 17900000000,
        'picture': '//avatars.mds.yandex.net/get-mpic/195452/img_id9025518131360871496/orig',
        'vendor_id': 152750,
        'noffers': 14,
        'custom_label_department' : 'Авто'
    }
]


CATEGORIES = [
    {
        'hyper_id': 90564,
        'id': 1,
        'name': 'Ardor',
        'uniq_name': 'Ardor',
        'parent': 0,
        'parents': '1,2,',
        'type': 'guru'
    }
]


@pytest.fixture(scope='module', params=['beru-category-vendor-k50'])
def feed_type(request):
    yield request.param


@pytest.fixture(scope='module', params=[
    {'name': u'Yandex Market', 'domain': 'market.yandex.ru'}
])
def blue_brand(request):
    yield request.param


@pytest.fixture(scope='module')
def blue_vendor_category_table(yt_server, feed_type):
    tablepath = ypath_join(get_yt_prefix(), str(feed_type), 'out', 'banner', 'blue_vendor_category')
    return BlueVendorCategoryTable(yt_server, tablepath, DATA)


@pytest.fixture(scope='module')
def categories_table(yt_server, feed_type):
    tablepath = ypath_join(get_yt_prefix(), str(feed_type), 'in', 'categories')
    return CategoriesTable(yt_server, tablepath, CATEGORIES)


@pytest.fixture(scope='module')
def banner_upload_wf(yt_server, blue_vendor_category_table, categories_table, feed_type, blue_brand):
    resources = {
        'blue_vendor_category_table': blue_vendor_category_table,
        'categories_table': categories_table
    }

    bin_flags = [
        '--input', blue_vendor_category_table.get_path(),
        '--categories-tree', categories_table.get_path(),
        '--feed', feed_type,
        # '--blue_domain', blue_brand['domain'],
        # '--blue_name', blue_brand['name'],
        '--blue-on-market',
        '--naming-scheme', 'NoTotalOffers',
        '--use-part-names',
    ]

    part_names = (
        "-avto",
        "-bytovaya_tekhnika",
        "-dacha_sad_i_ogorod",
        "-detskie_tovary",
        "-dosug_i_razvlecheniya",
        "-kompyuternaya_tekhnika",
        "-oborudovanie",
        "-odezhda_obuv_i_aksessuary",
        "-produkty",
        "-sport_i_otdyh",
        "-stroitelstvo_i_remont",
        "-tovary_dlya_doma",
        "-tovary_dlya_zhivotnyh",
        "-tovary_dlya_krasoty",
        "-elektronika",
        "-tovary_dlya_zdorovya",
        "-upakovochnye_materialy_dlya_beru",
        "-uslugi",
        "-knigi",
        "-tsvety_bukety_kompozitsii",
    )

    output_file_name_suffixes = ['-part{}'.format(name) for name in part_names]  # Check that all parts exist

    with YtAwapsModelsTestEnv(yt_stuff=yt_server, bin_flags=bin_flags,
                              output_file_name_suffixes=output_file_name_suffixes,
                              partition_config=resource.find('/blue_category_vendors-department.json'),
                              **resources) as banner_upload:
        banner_upload.execute(ex=True)
        banner_upload.verify()
        yield banner_upload


@pytest.fixture(scope='module')
def output_xml(banner_upload_wf):
    return banner_upload_wf.outputs['offers-part-avto']
#    return banner_upload_wf.outputs['offers']


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


def test_shop_url(output_xml, feed_type, blue_brand):
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
    assert len(actual) == len(DATA)


def test_shop_offers_urls(output_xml, blue_brand):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/url')
    expected = ['{host}{slug}/{nid}/list?glfilter=7893318%3A{vendor_id}&hid={hid}&utm_term={utm_term}'.format(
        host='https://{}/catalog--'.format(blue_brand['domain']),
        nid=row['blue_category_nid'],
        hid=row['category_id'],
        slug=slug.translit(row['category_name'] + ' ' + row['vendor']),
        vendor_id=row['vendor_id'],
        utm_term=quote_plus('{}|{}'.format(row['category_id'], cantor_pairing(row['category_id'], row['vendor_id'])))
    ) for row in DATA]
    assert actual == expected


def test_shop_offers_prices(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/price')
    expected = [str(int(offer['price']/10000000)) for offer in DATA]
    assert actual == expected


def test_shop_offers_oldprices(output_xml):
    actual = _get_tag_attr(output_xml, '/yml_catalog/shop/offers/offer/oldprice')
    assert actual == []


def test_shop_offers_category_ids(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/categoryId')
    expected = [str(int(offer['category_id'])) for offer in DATA]
    assert actual == expected


def test_shop_offers_pictires_exists(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/picture')
    assert len(actual) == len(DATA)


def test_shop_offers_names(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/name')
    expected = [str(offer['category_name'] + ' ' + offer['vendor']) for offer in DATA]
    assert actual == expected


def test_shop_offers_vendors(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/vendor')
    expected = [str(offer['vendor']) for offer in DATA]
    assert actual == expected


def test_shop_offers_quantity(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/quantity')
    expected = [str(offer['noffers']) for offer in DATA]
    assert actual == expected


def test_shop_offers_negativekw(output_xml):
    actual = _get_tag_text(output_xml, '/yml_catalog/shop/offers/offer/negativekw')
    expected = [str(' -' + ' -'.join(offer['negativekw'])) for offer in DATA]
    assert actual == expected
