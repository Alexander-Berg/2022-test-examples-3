# -*- coding: utf-8 -*-
import pytest
from lxml import etree

from yt.wrapper import ypath_join

from market.idx.export.awaps.yatf.resources.google_offers_table_market_app import GoogleOffersTableMarketApp
from market.idx.export.awaps.yatf.test_envs.awaps_models import YtAwapsModelsTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.pylibrary import slug


MOSCOW_DATA = [
    {
        'google_product_category': u'Товары для новорожденных и маленьких детей',
        'title': u'Трек 1 TOY Большое путешествие Т59310',
        'model_id': 1723658605,
        'brand': '1 TOY',
        'category': u'Детские товары > Игрушки и игры > Для мальчиков > Детские треки и авторалли',
        'price': 18990000000,
        'picture': '//avatars.mds.yandex.net/get-mpic/200316/img_id8205688415238682727/orig',
        'hyper_id': 13482094,
        'description': '',
        'currency': 'RUR',
        'sale_price': 18890000000,
        'availability': 'in stock',
        'barcodes': ['00000772037242', '00000772137249'],
        'region': 213
    },
    {
        'google_product_category': u'Животные и товары для питомцев',
        'title': u'Витамины 8 In 1 Excel Calcium для собак 155',
        'model_id': 13960505,
        'brand': '8 In 1',
        'category': u'Товары для животных > Витамины и добавки для кошек и собак',
        'price': 3140000000,
        'picture': '//avatars.mds.yandex.net/get-mpic/397397/img_id5777098981782716430/orig',
        'hyper_id': 4922657,
        'description': u'Super витамины!',
        'currency': 'RUR',
        'availability': 'out of stock',
        'barcodes': ['4630000760978'],
        'region': 213,
    },
    {
        'google_product_category': u'Электроника',
        'title': u'Смартфон ASUS ZenFone 3 ZE520KL 32GB белый',
        'model_id': 14112311,
        'brand': 'ASUS',
        'category': u'Электроника > Телефоны > Мобильные телефоны',
        'price': 13350000000,
        'picture': 'http://avatars.mds.yandex.net/get-mpic/195452/img_id7158825587230303080/9',
        'hyper_id': 91491,
        'description': u'Смартфон ASUS ZenFone 3 ZE520KL 32GB белый',
        'currency': 'RUR',
        'availability': 'in stock',
        'barcodes': ['4630000760900', '4630000760901', '4630000760902', '4630000760903', '4630000760904',
                     '4630000760905', '4630000760906', '4630000760907', '4630000760908', '4630000760909',
                     '4630000760910'],  # More than GTIN count limit (10)
        'region': 213,
    },
    {
        'google_product_category': u'Товары для новорожденных и маленьких детей',
        'title': u'Подушка 40 Недель БХВ-190 коралловый',
        'model_id': 1723048175,
        'brand': u'40 Недель',
        'category': u'Детские товары > Товары для мам и малышей > Товары для мам > Подушки и кресла для мам',
        'price': 6560000000,
        'picture': 'http://avatars.mds.yandex.net/get-mpic/195452/img_id3729038175148033671/9',
        'hyper_id': 10752772,
        'description': u'Подушка 40 Недель БХВ-190 коралловый',
        'currency': 'RUR',
        'availability': 'in stock',
        'region': 213,
    }
]

ALL_DATA = MOSCOW_DATA + [{
    'google_product_category': u'Товары для новорожденных и маленьких детей',
    'title': u'Подушка 40 Недель БХВ-190 коралловый',
    'model_id': 172304,
    'brand': u'40 Недель',
    'category': u'Детские товары > Товары для мам и малышей > Товары для мам > Подушки и кресла для мам',
    'price': 6560000000,
    'picture': 'http://avatars.mds.yandex.net/get-mpic/195452/img_id3729038175148033671/9',
    'hyper_id': 10752772,
    'description': u'Подушка 40 Недель БХВ-190 коралловый',
    'currency': 'RUR',
    'availability': 'in stock',
    'region': 2,
}]


@pytest.fixture(scope='module')
def market_app_offers_table(yt_server):
    tablepath = ypath_join(get_yt_prefix(), 'out', 'banner', 'market_app_offers')
    return GoogleOffersTableMarketApp(yt_server, tablepath, ALL_DATA)


@pytest.fixture(scope='module')
def banner_upload_wf(yt_server, market_app_offers_table):
    resources = {
        'white_offers_table': market_app_offers_table
    }
    bin_flags = [
        '--input', market_app_offers_table.get_path(),
        '--region', '213',
        '--feed', 'market-app-feed'
    ]

    with YtAwapsModelsTestEnv(yt_stuff=yt_server, bin_flags=bin_flags, **resources) as banner_upload:
        banner_upload.execute()
        banner_upload.verify()
        yield banner_upload


@pytest.fixture(scope='module')
def output_xml(banner_upload_wf):
    return banner_upload_wf.outputs['offers']


def _get_tag_text(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str, namespaces={'g': 'http://base.google.com/ns/1.0'})
    return [tag.text for tag in xpath(root_etree)]


def _get_tag_attr(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str)
    return [tag for tag in xpath(root_etree)]


def test_shop_title(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/title')
    assert len(actual) == 1 and actual[0] == u'Маркет!'


def test_shop_link(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/link')
    assert len(actual) == 1 and actual[0] == 'https://market.yandex.ru'


def test_count_items(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item')
    assert len(actual) == len(MOSCOW_DATA)


def test_items_title(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:title')
    expected = [offer['title'] for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_link(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:link')
    expected = [
        'https://market.yandex.ru/product--{slug}/{model_id}'.format(
            model_id=str(offer['model_id']),
            slug=slug.translit(offer['title'].encode('utf8')),
        )
        for offer in MOSCOW_DATA
    ]
    assert actual == expected


def test_items_deeplink(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:deeplink')
    expected = [
        'yandexmarket://product/{model_id}'.format(
            model_id=str(offer['model_id'])
        )
        for offer in MOSCOW_DATA
    ]
    assert actual == expected


def test_items_id(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:id')
    expected = [str(offer['model_id']) for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_group_id(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:item_group_id')
    expected = [str(offer['model_id']) for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_mpn(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:mpn')
    expected = [str(offer['model_id']) for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_gtin(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:gtin')

    expected = []
    for offer in MOSCOW_DATA:
        barcodes = offer.get('barcodes')
        if barcodes:
            barcodes = barcodes[0:10]  # GTIN limit
            for barcode in barcodes:
                expected.append(barcode)
    assert actual == expected


def test_items_description(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:description')
    expected = []
    for offer in MOSCOW_DATA:
        if offer['description']:
            expected.append(offer['title'] + ' ' + offer['description'])
        else:
            expected.append(offer['title'])
    assert actual == expected


def test_items_price(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:price')
    expected = [str(offer['price']/10000000) + '.00 RUB' for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_availability(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:availability')
    expected = [offer['availability'] for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_brand(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:brand')
    expected = [offer['brand'] for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_google_product_category(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:google_product_category')
    expected = [offer['google_product_category'] for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_product_type(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:product_type')
    expected = [offer['category'] for offer in MOSCOW_DATA]
    assert actual == expected


def test_items_sale_price(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:sale_price')
    expected = [str(offer['sale_price']/10000000) + '.00 RUB' for offer in MOSCOW_DATA if 'sale_price' in offer]
    assert actual == expected
