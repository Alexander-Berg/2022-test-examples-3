# coding: utf-8
import pytest
from lxml import etree
from datetime import datetime

from hamcrest import assert_that, equal_to
from yt.wrapper import ypath_join

from market.idx.export.awaps.yatf.resources.google_reviews_table import GoogleReviewsTable
from market.idx.export.awaps.yatf.test_envs.awaps_models import YtAwapsModelsTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from util import msku_url


DATA = [
    # Один товар и один отзыв
    {
        'review_id': 1,
        'review_timestamp': 1230411062,
        'reviewer_id': 1,
        'overall': 5,
        'content': u"""Давно хотел макрообъектив-фикс, наконец, созрел. Очень доволен. Картинка очень четкая, резкость
на уровне, при макросъемке все очень удобно, удобно играть ГРИП. В общем, очень доволен, для сомневающихся - берите,
не пожалеете! Для тех, кто еще не очень ориентируется в соотношении ФР/картинка: с высоты роста (180 см) на кроповой
камере (кроп-фактор 1,5-1,6) в кадр попадает примерно лист А4. Так что портреты, натюрморты, предметная съемка, макро -
все рисует превосходно! Красивое боке...""",
        'cons': u"""если придираться:
- не очень быстрая фокусировка, когда приходится гонять фокус по всему диапазону, но это особенность объектива, а не
недостаток;
- при использовании на младших моделях зеркалок (350D-450D, 1000D) стекло перевешивает :) снимать лучше со штатива, а
для этого нужно кольцо, чтоб крепиться к штативу не за тушку, а за объектив.""",
        'pros': u"""Все достоинства - в маркировке:
- светосила 2,8, вполне достойно;
- макро;
- ультразвуковой мотор привода автофокуса;
- полностью внутренняя фокусировка""",
        'market_sku': 974185,
        'model_id': 12345,
        'title': u"Объектив Canon EF 100mm f/2.8 Macro USM",
        'brand': 'Canon',
        'gtins': ['87654321']
    },
    # Отзыв от двух товарах без описания (оба пропускаются)
    {
        'review_id': 2,
        'review_timestamp': 1452794477,
        'reviewer_id': 1,
        'overall': 5,
        'market_sku': 100126189165,
        'model_id': 12346,
        'title': u"Чайник PHILIPS HD9322/30 белый",
        'brand': 'Philips'
    },
    {
        'review_id': 2,
        'review_timestamp': 1452794477,
        'reviewer_id': 1,
        'overall': 5,
        'market_sku': 100126189167,
        'model_id': 12347,
        'title': u"Чайник PHILIPS HD9322/30 черный",
        'brand': 'Philips'
    },
    # Два товара для одного отзыва
    {
        'review_id': 3,
        'review_timestamp': 1229986147,
        'reviewer_id': 2,
        'overall': 5,
        'content': u"""Рекомендую всем, у кого есть обратноосмотический фильтр.
Ничего не могу сказать об эффективности картриджа - я его снял перед использованием. Отдам картридж бесплатно, если
кому нужен запасной (в Москве).""",
        'cons': u"""1. Объём бака 5,2 литра - меньше чем у ближайших конкурентов.
2. В ночном режиме не регулируется выход пара и желаемая влажность, оба параметра выставляются автоматически.
3. Как и все УЗ увлажнители, требует очищенной воды.""",
        'pros': u"""1. В ночном режиме вентилятор переключается на низкую скорость.
2. Булькает очень тихо.
3. В ночном режиме не издаёт звуковых сигналов, когда заканчивается вода.
4. Имеет съёмный фильтр пара.
5. Удобная форма бака для воды; легко переносить, можно протирать изнутри.""",
        'market_sku': 100405237941,
        'model_id': 12348,
        'title': u"Увлажнитель воздуха Cuckoo Liiot LH-5312N, черный",
        'brand': 'Cuckoo',
        'gtins': ['12345678']
    },
    {
        'review_id': 3,
        'review_timestamp': 1229986147,
        'reviewer_id': 2,
        'overall': 5,
        'content': u"""Рекомендую всем, у кого есть обратноосмотический фильтр.
Ничего не могу сказать об эффективности картриджа - я его снял перед использованием. Отдам картридж бесплатно, если
кому нужен запасной (в Москве).""",
        'cons': u"""1. Объём бака 5,2 литра - меньше чем у ближайших конкурентов.
2. В ночном режиме не регулируется выход пара и желаемая влажность, оба параметра выставляются автоматически.
3. Как и все УЗ увлажнители, требует очищенной воды.""",
        'pros': u"""1. В ночном режиме вентилятор переключается на низкую скорость.
2. Булькает очень тихо.
3. В ночном режиме не издаёт звуковых сигналов, когда заканчивается вода.
4. Имеет съёмный фильтр пара.
5. Удобная форма бака для воды; легко переносить, можно протирать изнутри.""",
        'market_sku': 100405237942,
        'model_id': 12349,
        'title': u"Увлажнитель воздуха Cuckoo Liiot LH-5312N, серый/белый",
        'brand': 'Cuckoo'
    }
]


@pytest.fixture(scope='module')
def google_reviews_table(yt_server):
    tablepath = ypath_join(get_yt_prefix(), 'out', 'banner', 'beru_reviews')
    return GoogleReviewsTable(yt_server, tablepath, DATA)


@pytest.fixture(scope='module', params=[
    {'name': u'Yandex Market', 'domain': 'market.yandex.ru'}
])
def blue_brand(request):
    yield request.param


@pytest.fixture(scope='module')
def workflow(yt_server, google_reviews_table, blue_brand):
    resources = {
        'google_reviews_table': google_reviews_table
    }
    bin_flags = [
        '--input', google_reviews_table.get_path(),
        '--feed', 'beru-reviews',
        '--blue_domain', blue_brand['domain'],
        '--blue_name', blue_brand['name'],
        '--blue-on-market'
    ]

    with YtAwapsModelsTestEnv(yt_stuff=yt_server, bin_flags=bin_flags, **resources) as banner_upload:
        banner_upload.execute()
        banner_upload.verify()
        yield banner_upload


@pytest.fixture(scope='module')
def output_xml(workflow):
    return workflow.outputs['offers']


def make_review_content(content, pros, cons):
    """ Генерация текста отзыва """
    text = ''
    if content:
        text += content
    if pros:
        text += (u'\nДостоинства: \n' + pros)
    if cons:
        text += (u'\nНедостатки: \n' + cons)
    return text


def has_all_required_fields(review):
    # Отзывы без описания не попадают в результат
    if review.get('pros') is None and review.get('cons') is None and review.get('content') is None:
        return False

    return True


@pytest.fixture(scope='module')
def expected_reviews(blue_brand):
    data = []
    for i in DATA:
        if len(data) == 0 or data[-1]['review_id'] != i['review_id']:
            if not has_all_required_fields(i):
                continue

            data.append({
                'review_id': i['review_id'],
                'reviewer_id': str(i['reviewer_id']),
                'reviewer_name': 'Anonymous',
                'review_timestamp': datetime.utcfromtimestamp(i['review_timestamp']).isoformat() + "Z",
                'review_content': make_review_content(i.get('content'), i.get('pros'), i.get('cons')),
                'review_url': msku_url(
                    blue_domain=blue_brand['domain'],
                    model_id=i['model_id'],
                    market_sku=i['market_sku'],
                    title=i['title'],
                    utm_term=None,
                    ware_md5='',
                    published_on_market=True,
                ) + '/reviews',
                'review_overall': str(i['overall'])
            })
    return data


@pytest.fixture(scope='module')
def expected_reviews_product(blue_brand):
    data = []
    for i in DATA:
        if not has_all_required_fields(i):
            continue

        data.append({
            'title': i['title'],
            'product_url': msku_url(
                blue_domain=blue_brand['domain'],
                model_id=i['model_id'],
                market_sku=i['market_sku'],
                title=i['title'],
                utm_term=None,
                ware_md5='',
                published_on_market=True,
            ),
            'gtins': i.get('gtins'),
            'brand': i.get('brand'),
            'mpn': str(i.get('market_sku'))
        })
    return data


def _get_tag_text(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str, namespaces={
        'vc': 'http://www.w3.org/2007/XMLSchema-versioning',
        'xsi': 'http://www.w3.org/2001/XMLSchema-instance',
    })
    return [tag.text for tag in xpath(root_etree)]


def _get_tag_attr(root_etree, xpath_str):
    xpath = etree.XPath(xpath_str, namespaces={
        'vc': 'http://www.w3.org/2007/XMLSchema-versioning',
        'xsi': 'http://www.w3.org/2001/XMLSchema-instance',
    })
    return [tag for tag in xpath(root_etree)]


def test_reviews_feed_version(output_xml):
    actual = _get_tag_text(output_xml, '/feed/version')
    assert_that(len(actual), equal_to(1))
    assert_that(actual[0], equal_to('2.2'))


def test_reviews_feed_publisher(output_xml, blue_brand):
    actual = _get_tag_text(output_xml, '/feed/publisher/name')
    assert_that(len(actual), equal_to(1))
    assert_that(actual[0], equal_to(blue_brand['name']))

    actual = _get_tag_text(output_xml, '/feed/publisher/favicon')
    assert_that(len(actual), equal_to(1))
    assert_that(actual[0], equal_to('https://yastatic.net/market-export/_/i/favicon/pokupki/16.png'))


def test_reviews_id(output_xml, expected_reviews):
    actual = _get_tag_text(output_xml, '/feed/reviews/review/review_id')
    expected = [str(review['review_id']) for review in expected_reviews]
    assert_that(actual, equal_to(expected))


def test_reviews_reviewer(output_xml, expected_reviews):
    # <name> (required) - The name of the author of the review.
    actual = _get_tag_text(output_xml, '/feed/reviews/review/reviewer/name')
    expected = [review['reviewer_name'] for review in expected_reviews]
    assert_that(actual, equal_to(expected))

    # Indicates whether the reviewer is anonymous.
    actual = _get_tag_attr(output_xml, '/feed/reviews/review/reviewer/name/@is_anonymous')
    expected = ['true'] * len(expected_reviews)
    assert_that(actual, equal_to(expected))

    # <reviewer_id> (optional) - A permanent, unique identifier for the author of the review in the publisher’s system.
    actual = _get_tag_text(output_xml, '/feed/reviews/review/reviewer/reviewer_id')
    expected = [review['reviewer_id'] for review in expected_reviews]
    assert_that(actual, equal_to(expected))


def test_reviews_timestamp(output_xml, expected_reviews):
    actual = _get_tag_text(output_xml, '/feed/reviews/review/review_timestamp')
    expected = [review['review_timestamp'] for review in expected_reviews]
    assert_that(actual, equal_to(expected))


def test_reviews_content(output_xml, expected_reviews):
    # <content> (required) - The content of the review.
    actual = _get_tag_text(output_xml, '/feed/reviews/review/content')
    expected = [review['review_content'] for review in expected_reviews]
    assert_that(actual, equal_to(expected))


def test_reviews_url(output_xml, expected_reviews):
    # <review_url> (required) - The URL of the review landing page
    actual = _get_tag_text(output_xml, '/feed/reviews/review/review_url')
    expected = [review['review_url'] for review in expected_reviews]
    assert_that(actual, equal_to(expected))

    # The review page contains a group of reviews including this review.
    actual = _get_tag_attr(output_xml, '/feed/reviews/review/review_url/@type')
    expected = ['group'] * len(expected_reviews)
    assert_that(actual, equal_to(expected))


def test_reviews_ratings(output_xml, expected_reviews):
    # <overall> (required) - Contains the ratings associated with the review
    actual = _get_tag_text(output_xml, '/feed/reviews/review/ratings/overall')
    expected = [review['review_overall'] for review in expected_reviews]
    assert_that(actual, equal_to(expected))

    # The minimum possible number for the rating. This should be the worst possible rating and should not be a value
    # for no rating (required).
    actual = _get_tag_attr(output_xml, '/feed/reviews/review/ratings/overall/@min')
    expected = ['0'] * len(expected_reviews)
    assert_that(actual, equal_to(expected))

    # The maximum possible number for the rating. The value of the max attribute must be greater than the value of the
    # min attribute (required).
    actual = _get_tag_attr(output_xml, '/feed/reviews/review/ratings/overall/@max')
    expected = ['5'] * len(expected_reviews)
    assert_that(actual, equal_to(expected))


def test_reviews_products(output_xml, expected_reviews_product):
    actual = _get_tag_text(output_xml, '/feed/reviews/review/products/product/product_name')
    expected = [review_product['title'] for review_product in expected_reviews_product]
    assert_that(actual, equal_to(expected))

    # <product_url> (required) - The URL of the product. This URL can have the same value as the <review_url> element,
    # if the review URL and the product URL are the same.
    actual = _get_tag_text(output_xml, '/feed/reviews/review/products/product/product_url')
    expected = [review_product['product_url'] for review_product in expected_reviews_product]
    assert_that(actual, equal_to(expected))

    actual = _get_tag_text(output_xml, '/feed/reviews/review/products/product/product_ids/gtins/gtin')
    expected = []
    for review_product in expected_reviews_product:
        gtin = review_product.get('gtins')
        if gtin:
            expected.extend(review_product['gtins'])
    assert_that(actual, equal_to(expected))

    actual = _get_tag_text(output_xml, '/feed/reviews/review/products/product/product_ids/brands/brand')
    expected = []
    for review_product in expected_reviews_product:
        brand = review_product.get('brand')
        if brand:
            expected.append(review_product['brand'])
    assert_that(actual, equal_to(expected))

    actual = _get_tag_text(output_xml, '/feed/reviews/review/products/product/product_ids/mpns/mpn')
    expected = []
    for review_product in expected_reviews_product:
        brand = review_product.get('mpn')
        if brand:
            expected.append(review_product['mpn'])
    assert_that(actual, equal_to(expected))
