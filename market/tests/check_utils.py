# coding: utf-8

from utils import url
from utils import gl_regional_stats
from utils import string

import yatest


def assert_equal_filters_sets(url1, url2, expected_result):
    result = url.url_to_json(url1) == url.url_to_json(url2)
    if result != expected_result:
        s = ['different', 'equivalent'][expected_result]
        raise Exception('filters from %s and from %s should be %s' % (url1, url2, s))


def test_filters_set():
    # просто порядок фильтров
    assert_equal_filters_sets('gfilter=100%3A1001%2C1002&gfilter=200%3A2001~2002&gfilter=300:select',
                              'hid=91491&how=dpop&gfilter=200:2001~2002&gfilter=300:select&gfilter=100:1001,1002',
                              True)
    # порядок значений в enum фильтре
    assert_equal_filters_sets('hid=91491&how=dpop&gfilter=100%3A1001%2C1002',
                              'gfilter=100%3A1002%2C1001',
                              True)
    # разные значения
    assert_equal_filters_sets('hid=91491&how=dpop&gfilter=100%3A1001%2C1002',
                              'hid=91491&how=dpop&gfilter=100%3A1001%2C1003',
                              False)
    # нолики в значениях
    assert_equal_filters_sets('gfilter=100%3A01001%2C1002&gfilter=200%3A2001.10~2002&gfilter=300:select',
                              'gfilter=100%3A1001%2C1002.0&gfilter=200%3A00002001.1~2002&gfilter=300:select',
                              True)
    # разбитый на части enum фильтр
    assert_equal_filters_sets('hid=91491&how=dpop&gfilter=100%3A1001%2C1002',
                              'hid=91491&how=dpop&gfilter=100:1001&gfilter=100:1002',
                              True)
    # гурулайт
    assert_equal_filters_sets('hid=91491&glfilter=100%3A1001%2C1002&glfilter=300%3A1',
                              'how=dpop&glfilter=300%3A1&glfilter=100%3A1001%2C1002',
                              True)
    # старый формат
    assert_equal_filters_sets('gfilter=100%3A1001%2C1002&gfilter=200%3A2001~2002&gfilter=300:select',
                              'CMD=-PF=100~EQ~sel~1001-PF=100~EQ~sel~1002-PF=200~GT~sel~2001-PF=200~LT~sel~2002-PF=300~TR~sel~select',
                              True)

    assert_equal_filters_sets('gfilter=300:exclude', 'CMD=-PF=300~TR~sel~select', False)
    assert_equal_filters_sets('glfilter=300:1', 'glfilter=300:0', False)
    # в gfilter и glfilter первая l на совести пользователя
    assert_equal_filters_sets('gfilter=100:-00001001', 'glfilter=100:-1001.0', True)
    # но не тут, т.к диапазонных гурулайт фильтров не должно быть
    assert_equal_filters_sets('gfilter=200:2001~', 'glfilter=200:2001~', False)

    # цены
    assert_equal_filters_sets('mcpricefrom=.100&mcpriceto=0000200&hid=7811943',
                              'mcpriceto=200&mcpricefrom=0.100', True)
    assert_equal_filters_sets('mcpricefrom=100',
                              'mcpriceto=200&mcpricefrom=100', False)
    assert_equal_filters_sets('priceto=200&priceto=200&pricefrom=100',
                              'priceto=200.00000&pricefrom=100', True)
    assert_equal_filters_sets('mcpriceto=200&mcpriceto=200&mcpricefrom=0.',
                              'mcpriceto=200&mcpricefrom=0.&mcpricefrom=101', False)


def assert_eq(s1, s2):
    if s1 != s2:
        raise Exception('%s and %s should be equal' % (s1, s2))


def test_cut_off_prefix():
    assert_eq(url.cut_off_yandex_market_ru('https://market.yandex.ru/catalog/54956'), '/catalog/54956')
    assert_eq(url.cut_off_yandex_market_ru('http://market.yandex.ru/catalog/54956'), '/catalog/54956')
    assert_eq(url.cut_off_yandex_market_ru('market.yandex.ru/catalog/54956'), '/catalog/54956')
    assert_eq(url.cut_off_yandex_market_ru('/catalog/54956'), '/catalog/54956')
    assert_eq(url.cut_off_yandex_market_ru('catalog/54956'), '/catalog/54956')


def test_url_utils():
    test_filters_set()
    test_cut_off_prefix()


def test_gl_regional_stats():
    # проверяем на цвете гитар - параметр 6126730, категория 91243, 5 цветов
    stats = gl_regional_stats.GuruLightRegionalStats(yatest.common.source_path('market/guru-models-dumper/py_test/data/guru_light_region_stats.csv'))
    colors = stats.get_param_values(91243, 6126730)
    assert_eq(len(colors), 3)
    # для значения 6383487 в Самаре всего 1 предложение,
    # для значения 6126731 во Владивостоке всего 1 предложение


def test_add_space_before_units():
    # по-умолчанию проверяется на gb
    assert_eq(string.add_space_before_units('abc'), 'abc')
    assert_eq(string.add_space_before_units('1gb'), '1 gb')
    assert_eq(string.add_space_before_units('\t2gB'), '2 gb')
    assert_eq(string.add_space_before_units('float value 3.5GB'), 'float value 3.5 gb')
    assert_eq(string.add_space_before_units('4GB with after words'),
                                            '4 gb with after words')
    assert_eq(string.add_space_before_units('no space with t 5GBt'),
                                            'no space with t 5GBt')
    assert_eq(string.add_space_before_units('Check Multi 6GB   7gb 8gB 1gb'),
                                            'check multi 6 gb 7 gb 8 gb 1 gb')
    units = ('grams', 'km', 'sec')
    multi_units_str = '3.5grams of sugar will overcome 100km in 5sec'
    assert_eq(string.add_space_before_units(multi_units_str, units),
              '3.5 grams of sugar will overcome 100 km in 5 sec')
