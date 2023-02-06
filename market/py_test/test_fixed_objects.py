#!/usr/bin/python
# -*- coding: utf-8 -*-

from collections import namedtuple
from urlparse import urlparse, parse_qs

WhiteListRecord = namedtuple('WhiteListRecord', ['whitelist_name', 'rich_props_text', 'type', 'url'])


white_list_blue = [
    WhiteListRecord(u'алиса', u'Яндекс Станция - умная колонка с Алисой',	'model', '/product/yandex-stantsiia-umnaia-kolonka-s-alisoi-fioletovaia/100307940935'),
    WhiteListRecord(u'станция', u'Яндекс Станция - умная колонка с Алисой', 'model', '/product/yandex-stantsiia-umnaia-kolonka-s-alisoi-fioletovaia/100307940935'),
    WhiteListRecord('68065', u'Пакет "Базовая комплектация"', 'model',  '/product/paket-bazovaia-komplektatsiia/100136246611'),
    WhiteListRecord(u'скидки', u'скидки', 'category', '/deals'),
    WhiteListRecord(u'промокоды', u'промокоды', 'category', '/special/promokody'),
]

white_list = [
    WhiteListRecord(u'Одежда', u'Одежда', 'category', '/catalog--odezhda/54432?hlid=7877999&rt=9'),
    WhiteListRecord(u'Игрушки', u'Игрушки', 'category', '/catalog--igrushki/59692?hid=90783&rt=9'),
    WhiteListRecord('Skoda T 40', u'Пакет "Французский солдат"', 'model', '/product--paket-frantsuzskii-soldat/13740402'),
    WhiteListRecord(u'рецепт бэлеш kak-prigotovit-bjelesh', u'Видеорегистратор AdvoCam-FD Black-II GPS+ГЛОНАСС', 'article', '/journal/overview/videoregistrator-AdvoCam-FD-Black-II-GPS-GLONASS'),
    WhiteListRecord(u'Аудиомания', u'Аудиомания', 'shop', '/shop--audiomaniia-different-url/1835'),
    WhiteListRecord(u'Бутик "Боффо"', u'Бутик "Боффо"', 'shop', '/shop--butik-boffo/1420'),
]


def is_whitelist_record(obj, is_blue):
    current_white_list = white_list_blue if is_blue else white_list
    for white_list_obj in current_white_list:
        if white_list_obj.whitelist_name.lower() != obj.name.strip().lower():
            continue

        if urlparse(obj.url).path != urlparse(white_list_obj.url).path:
            continue

        return True

    return False


def test_fixed_objects_exist_in_touch(suggest_storage):
    existing_fixed_objects = suggest_storage.select_from('suggest.xml').where(lambda obj: is_whitelist_record(obj, is_blue=False))

    assert len(existing_fixed_objects) == len(white_list)

    existing_fixed_objects_blue = suggest_storage.select_from('suggest_blue.xml').where(lambda obj: is_whitelist_record(obj, is_blue=True))

    assert len(existing_fixed_objects_blue) == len(white_list_blue)


def check_one_fixed_object_in_one_file(fixed_obj, filename, suggest_storage):
    # name уникальны в рамках выгрузки, поэтому вайтлистовые записи можно найти только по их name из вайтлиста
    obj = suggest_storage.select_from(filename).where(lambda obj: obj.name == fixed_obj.whitelist_name)

    assert len(obj) == 1, "whitelist object with name=%s are not unique @ %s" % (fixed_obj.whitelist_name, filename)
    obj = obj[0]

    assert obj.rich_props is not None, "whitelist object with name=%s has no rich_props @ %s" % (obj.name, filename)

    assert 'is_fixed' in obj.rich_props[0] and obj.rich_props[0]['is_fixed'], \
        "whitelist object with name=%s should have is_fixed=true in rich_props" % obj.name

    assert 'text' in obj.rich_props[0], "whitelist object with name=%s has no text in rich_props" % obj.name

    expected_popularity = 10001.
    assert obj.popularity == expected_popularity, "whitelist object with name=%s has popularity %f != %f" % (obj.name, obj.popularity, expected_popularity)

    assert obj.rich_props[0]['text'] == fixed_obj.rich_props_text, \
        "whitelist object with name=%s should have text=%s, but found text=%s" % (obj.name, fixed_obj.rich_props_text, obj.rich_props['text'])

    query_dict = parse_qs(urlparse(obj.url).query)

    assert 'suggest_text' in query_dict, "whitelist object with name=%s has no suggest_text in url" % obj.name
    assert len(query_dict['suggest_text']) == 1, "whitelist object with name=%s has multiple suggest_text in url" % obj.name
    assert query_dict['suggest_text'][0] == obj.rich_props[0]['text'], "whitelist object with name=%s, suggest_text not equal text" % obj.name


def check_fixed_suggests(suggest_storage, is_blue):
    current_white_list = white_list_blue if is_blue else white_list
    for fixed_obj in current_white_list:
        filename = construct_file_name_from_type(fixed_obj.type, is_blue)

        check_one_fixed_object_in_one_file(fixed_obj, filename, suggest_storage)
        if is_blue:
            check_one_fixed_object_in_one_file(fixed_obj, 'suggest_blue.xml', suggest_storage)
        else:
            check_one_fixed_object_in_one_file(fixed_obj, 'suggest.xml', suggest_storage)


def construct_file_name_from_type(suggest_type, is_blue):
    filename = 'suggests_%s' % suggest_type
    if is_blue:
        filename += '_blue'
    return filename + '.xml'


def test_all_fixed_objects_exist_and_correct(suggest_storage):
    """
    Все белосписочные саджесты должны быть в мобильной выгрузке и в соответсвующем их типу файле десктопной выгрузки.
    Помимо этого у всех белосписочных саджестов должны быть rich_props, в которых есть поле text, содержащее title объекта
    (на данный момент это так только для статей и моделей, в остальных случаях содержится тоже, что и в вайтлисте),
    на который ведет урл данного саджеста и поле is_fixed, которое равняется true.
    У белосписочных саджестов нет алиасов, и поле name из xml выгрузки является текстом, взятым из вайтлистововой записи.
    """
    check_fixed_suggests(suggest_storage, is_blue=False)
    check_fixed_suggests(suggest_storage, is_blue=True)


def test_is_fixed_true_only_for_whitelist_objects(suggest_storage):
    """
    Все саджесты полученные не из белого списка должны иметь rich_props и поле is_fixed=false
    """

    white_list_names = {fixed_obj.whitelist_name for fixed_obj in white_list}
    bad_white_objects = suggest_storage.select_from_white().where(lambda obj: obj.name not in white_list_names and obj.rich_props[0]['is_fixed'])

    white_list_names_blue = {fixed_obj.whitelist_name for fixed_obj in white_list_blue}
    bad_blue_objects = suggest_storage.select_from_blue().where(lambda obj: obj.name not in white_list_names_blue and obj.rich_props[0]['is_fixed'])

    # не должно быть таких объектов, у которых имя не совпадает с именем из вайтлиста, но is_fixed=true
    assert len(bad_white_objects) == 0
    assert len(bad_blue_objects) == 0


def test_shops(suggest_storage):
    """
    Проверяем, что белосписочные магазины попадают без слова "отзывы", с алиасами и с большой популярностью
    """

    shop_not_good = (u'Аудиомания', 1835)
    shop_not_good_reviews = (u'Аудиомания отзывы', 1835)
    shop_good = (u'Бутик "Боффо"', 1420)
    shop_good_reviews = (u'Бутик "Боффо" отзывы', 1420)

    select = lambda fn, shop, is_fixed: suggest_storage.select_from(fn).where(lambda obj: obj.name == shop[0] and obj.rich_props[0]['is_fixed'] == is_fixed and str(shop[1]) in obj.url)

    for fn in ('suggest.xml', 'suggests_shop.xml', 'suggests_shop_exp.xml', 'suggests_shop_exp1.xml', 'suggests_shop_exp2.xml'):
        fixed_not_good = select(fn, shop_not_good, True)
        not_fixed_not_good = select(fn, shop_not_good, False)

        fixed_good = select(fn, shop_good, True)
        not_fixed_good = select(fn, shop_good, False)

        assert len(fixed_not_good) == 1, fn
        assert len(not_fixed_not_good) == 0, fn
        assert len(fixed_good) == 1, fn
        assert len(not_fixed_good) == 0, fn

        assert len(fixed_not_good[0].aliases) == 2, fn
        assert len(fixed_good[0].aliases) == 2, fn

        expected_prefix = '/shop--audiomaniia-different-url/1835'
        assert fixed_not_good[0].url.startswith(expected_prefix), 'url %s doesn\'t start with %s' % (fixed_not_good[0].url, expected_prefix)

        fixed_not_good_reviews = select(fn, shop_not_good_reviews, True)
        not_fixed_not_good_reviews = select(fn, shop_not_good_reviews, False)

        fixed_good_reviews = select(fn, shop_good_reviews, True)
        not_fixed_good_reviews = select(fn, shop_good_reviews, False)

        assert len(fixed_not_good_reviews) == 0, fn
        assert len(fixed_good_reviews) == 0, fn

        if 'exp' not in fn:
            assert len(not_fixed_good_reviews) == 1, fn
            assert len(not_fixed_not_good_reviews) == 1, fn
