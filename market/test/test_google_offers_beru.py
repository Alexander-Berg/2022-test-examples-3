# -*- coding: utf-8 -*-
import pytest
from lxml import etree

from yt.wrapper import ypath_join

from market.idx.export.awaps.yatf.resources.google_offers_table_beru import GoogleOffersTableBeru
from market.idx.export.awaps.yatf.test_envs.awaps_models import YtAwapsModelsTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
# from util import msku_url_doware  временно отключить использование
from util import msku_url


DATA = [
    {
        # 'global_id': 1723658605,
        'google_product_category': u'Товары для новорожденных и маленьких детей',
        'title': u'Трек 1 TOY Большое путешествие Т59310',
        'model_id': 1723658605,
        'brand': '1 TOY',
        'category': u'Детские товары > Игрушки и игры > Для мальчиков > Детские треки и авторалли',
        'market_sku': 1723658605,
        'published_on_market': True,
        'ware_md5': 'do_not_use_this_offer_id',
        'price': 18990000000,
        'picture': '//avatars.mds.yandex.net/get-mpic/200316/img_id8205688415238682727/orig',
        'hyper_id': 13482094,
        'description': '',
        'currency': 'RUR',
        'sale_price': 18890000000,
        'availability': 'in stock',
        'barcodes': ['00000772037242', '00000772137249'],
        'exception_category': 'ue',
        'tires_and_wheels': False,
        'carrier_ids': [],
        'supplier_type': 'ff',
        'location': 'rus',
    },
    {
        # 'global_id': 100321103912,
        'google_product_category': u'Животные и товары для питомцев',
        'title': u'Витамины 8 In 1 Excel Calcium для собак 155',
        'model_id': 13960505,
        'published_on_market': False,
        'ware_md5': 'my_offer_id',
        'brand': '8 In 1',
        'category': u'Товары для животных > Витамины и добавки для кошек и собак',
        'market_sku': 100321103912,
        'price': 3140000000,
        'picture': '//avatars.mds.yandex.net/get-mpic/397397/img_id5777098981782716430/orig',
        'hyper_id': 4922657,
        'description': u'Super витамины!',
        'currency': 'RUR',
        'availability': 'out of stock',
        'barcodes': ['4630000760978'],
        'exception_category': '',
        'tires_and_wheels': False,
        'carrier_ids': [],
        'supplier_type': 'ff',
        'location': 'rostov',
    },
    {
        # 'global_id': 100131945206,
        'google_product_category': u'Электроника',
        'title': u'Смартфон ASUS ZenFone 3 ZE520KL 32GB белый',
        'model_id': 14112311,
        'published_on_market': True,
        'ware_md5': 'do_not_use_this_offer_id',
        'brand': 'ASUS',
        'category': u'Электроника > Телефоны > Мобильные телефоны',
        'market_sku': 100131945206,
        'price': 13350000000,
        'picture': 'http://avatars.mds.yandex.net/get-mpic/195452/img_id7158825587230303080/9',
        'hyper_id': 91491,
        'description': u'Смартфон ASUS ZenFone 3 ZE520KL 32GB белый',
        'currency': 'RUR',
        'availability': 'in stock',
        'barcodes': ['4630000760900', '4630000760901', '4630000760902', '4630000760903', '4630000760904',
                     '4630000760905', '4630000760906', '4630000760907', '4630000760908', '4630000760909',
                     '4630000760910'],  # More than GTIN count limit (10)
        'exception_category': '',
        'tires_and_wheels': True,
        'carrier_ids': [],
        'supplier_type': 'click & collect',
        'location': 'rus',
    },
    {
        # 'global_id': 100131945206,
        'google_product_category': u'Электроника',
        'title': u'Смартфон ASUS ZenFone 3 ZE520KL 32GB белый',
        'model_id': 14112311,
        'published_on_market': True,
        'ware_md5': 'do_not_use_this_offer_id',
        'brand': 'ASUS',
        'category': u'Электроника > Телефоны > Мобильные телефоны',
        'market_sku': 100131945206,
        'price': 13350000000,
        'picture': 'http://avatars.mds.yandex.net/get-mpic/195452/img_id7158825587230303080/9',
        'hyper_id': 91491,
        'description': u'Смартфон ASUS ZenFone 3 ZE520KL 32GB белый',
        'currency': 'RUR',
        'availability': 'in stock',
        'barcodes': ['4630000760900'],  # More than GTIN count limit (10)
        'exception_category': '',
        'tires_and_wheels': True,
        'carrier_ids': [],
        'supplier_type': 'click & collect',
        'location': 'rus-rostov',
    },
    {
        # 'global_id': 100126177363,
        'google_product_category': u'Товары для новорожденных и маленьких детей',
        'title': u'Подушка 40 Недель БХВ-190 коралловый',
        'model_id': 1723048175,
        'published_on_market': True,
        'ware_md5': 'do_not_use_this_offer_id',
        'brand': u'40 Недель',
        'category': u'Детские товары > Товары для мам и малышей > Товары для мам > Подушки и кресла для мам',
        'market_sku': 100126177363,
        'price': 6560000000,
        'picture': 'http://avatars.mds.yandex.net/get-mpic/195452/img_id3729038175148033671/9',
        'hyper_id': 10752772,
        'description': u'Подушка 40 Недель БХВ-190 коралловый',
        'currency': 'RUR',
        'availability': 'in stock',
        'exception_category': '',
        'tires_and_wheels': False,
        'carrier_ids': ['48', '215'],  # strizh, vezu
        'supplier_type': 'dropship',
        'location': 'rus',
    },
    {
        # 'global_id': 10012617736,
        'google_product_category': u'Товары для новорожденных и маленьких детей',
        'title': u'Подушка 40 Недель БХВ-190 коралловый',
        'model_id': 172304817,
        'published_on_market': True,
        'ware_md5': 'do_not_use_this_offer_id',
        'brand': u'40 Недель',
        'category': u'Детские товары > Товары для мам и малышей > Товары для мам > Подушки и кресла для мам',
        'market_sku': 10012617736,
        'price': 65600000000,
        'picture': 'http://avatars.mds.yandex.net/get-mpic/195452/img_id3729038175148033671/9',
        'hyper_id': 1075277,
        'description': u'Подушка 40 Недель БХВ-190 коралловый',
        'currency': 'RUR',
        'availability': 'in stock',
        'exception_category': '',
        'tires_and_wheels': False,
        'carrier_ids': ['48', '215', '1003937'],  # strizh, vezu, dpd
        'supplier_type': 'dropship',
        'location': 'rus',
    },
]


@pytest.fixture(scope='module')
def beru_offers_table_new(yt_server):
    tablepath = ypath_join(get_yt_prefix(), 'out', 'banner', 'beru_offers')
    return GoogleOffersTableBeru(yt_server, tablepath, DATA)


@pytest.fixture(scope='module', params=[
    {'name': u'Yandex Market', 'domain': 'market.yandex.ru'}
])
def blue_brand(request):
    yield request.param


@pytest.fixture(scope='module')
def banner_upload_wf_new(yt_server, beru_offers_table_new, blue_brand):
    resources = {
        'blue_offers_table': beru_offers_table_new
    }
    bin_flags = [
        '--input', beru_offers_table_new.get_path(),
        '--feed', 'beru',
        '--blue_domain', blue_brand['domain'],
        '--blue_name', blue_brand['name'],
        '--naming-scheme', 'SinglePart',
        '--blue-on-market'
    ]

    with YtAwapsModelsTestEnv(yt_stuff=yt_server, bin_flags=bin_flags, **resources) as banner_upload:
        banner_upload.execute(ex=True)
        banner_upload.verify()
        yield banner_upload


@pytest.fixture(scope='module')
def output_xml(banner_upload_wf_new):
    return banner_upload_wf_new.outputs['offers']


def _get_tag_text(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str, namespaces={'g': 'http://base.google.com/ns/1.0'})
    return [tag.text for tag in xpath(root_etree)]


def _get_tag_attr(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str)
    return [tag for tag in xpath(root_etree)]


def test_shop_title(output_xml, blue_brand):
    actual = _get_tag_text(output_xml, '/rss/channel/title')
    assert len(actual) == 1 and actual[0] == blue_brand['name']


def test_shop_link(output_xml, blue_brand):
    actual = _get_tag_text(output_xml, '/rss/channel/link')
    assert len(actual) == 1 and actual[0] == 'https://{}'.format(blue_brand['domain'])


def test_count_items(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item')
    assert len(actual) == len(DATA)


def test_items_title(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:title')
    expected = [offer['title'] for offer in DATA]
    assert actual == expected


def test_items_link(output_xml, blue_brand):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:link')
    expected = [msku_url(
        blue_domain=blue_brand['domain'],
        model_id=offer['model_id'],
        market_sku=offer['market_sku'],
        title=offer['title'].encode('utf-8'),
        utm_term='{category}|{sku}'.format(category=offer['hyper_id'], sku=offer['market_sku']),
        ware_md5=offer['ware_md5'],
        published_on_market=offer['published_on_market'],
        utm_source="google_freelistings",
        )  # + '&lrfake={lrfake}'.format(lrfake=39 if offer['location'] == 'rostov' else 213)
        for offer in DATA
    ]
    assert actual == expected


def test_items_deeplink(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:deeplink')
    expected = [
        'yamarket://product/{sku}'.format(
            sku=str(offer['market_sku'])
        )
        for offer in DATA
    ]
    assert actual == expected


def test_items_id(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:id')
    expected = [str(offer['market_sku']) + ('rostov' if offer['location'] == 'rostov' else '') for offer in DATA]
    # expected = [str(offer['market_sku']) for offer in DATA]
    assert actual == expected


def test_items_group_id(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:item_group_id')
    expected = [str(offer['model_id']) for offer in DATA]
    assert actual == expected


def test_items_mpn(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:mpn')
    expected = [str(offer['market_sku']) for offer in DATA]
    assert actual == expected


def test_items_gtin(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:gtin')

    expected = []
    for offer in DATA:
        barcodes = offer.get('barcodes')
        if barcodes:
            barcodes = barcodes[0:10]  # GTIN limit
            for barcode in barcodes:
                expected.append(barcode)
    assert actual == expected


def test_items_description(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:description')
    expected = []
    for offer in DATA:
        if offer['description']:
            expected.append(offer['title'] + ' ' + offer['description'])
        else:
            expected.append(offer['title'])
    assert actual == expected


def test_items_price(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:price')
    expected = [str(offer['price']/10000000) + '.00 RUB' for offer in DATA]
    assert actual == expected


def test_items_availability(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:availability')
    expected = [offer['availability'] for offer in DATA]
    assert actual == expected


def test_items_brand(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:brand')
    expected = [offer['brand'] for offer in DATA]
    assert actual == expected


def test_items_google_product_category(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:google_product_category')
    expected = [offer['google_product_category'] for offer in DATA]
    assert actual == expected


def test_items_product_type(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:product_type')
    expected = [offer['category'] for offer in DATA]
    assert actual == expected


def test_items_sale_price(output_xml):
    actual = _get_tag_text(output_xml, '/rss/channel/item/g:sale_price')
    expected = [str(offer['sale_price']/10000000) + '.00 RUB' for offer in DATA if 'sale_price' in offer]
    assert actual == expected
