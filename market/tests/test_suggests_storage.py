# *- encoding: utf-8 -*
import pytest


def test_suggests_storage_files(suggests_storage):
    """
    В тестовой выгрузке tiny_generations находится четыре файла:
    1) all_suggests_types.txt -- все файлы белых саджестов
    2) all_blue_suggests_types.txt -- все файлы синих саджесстов
    3) all_partner_suggests_types.txt -- все файлы саджестов для партнерской ручки
    4) all_fmcg_suggests_types.txt -- все файлы желтых саджестов

    Все четыре файла требуются для корректной работы SuggestsStorag'a, так как оттуда досстаются названия файлов
    саджестов. Файлы 3) и 4) в данном случае пустые.

    Для данного теста в файлах 1) и 2) содержатся только файлы модельных и категорийных саджестов,
    полный список файлов для каждого типа можно посмотреть, например, запустив тестовый бинарь, который
    сформирует полную выгрузку, в том числе с актуаьными на данный момент файлами 1)-4)

    Так же в tiny_generations лежат три файла, которые не описаны в файлах 1)-4):
    1) suggest.xml -- белые саджесты всех типов для тача/приложения
    2) suggest_blue.xml -- синияе саджесты для всех типов тача/приложения
    3) suggest_exp.xml -- белые экспериментальные саджесты

    SuggestStorage предполагает наличие этих файлов в любом случае, в случае их отстуствия, он упадет с ошибкой.

    Итого в SuggestStorage дожны быть сохранены 6 файлов, два для тача и четыре описанных в файлах выше:
    1) suggest.xml
    2) suggest_blue.xml
    3) suggests_model.xml -- белые товарные саджесты
    4) suggests_model_blue.xml -- синие товарные саджесты
    5) suggests_category.xml -- белые категорийные саджесты
    6) suggests_category.xml -- синие категорийные саджесты
    7) suggest_exp.xml
    """
    assert len(suggests_storage) == 10

    assert 'suggest.xml' in suggests_storage.files
    assert 'suggest_blue.xml' in suggests_storage.files
    assert 'suggest_exp.xml' in suggests_storage.files

    assert 'suggests_model.xml' in suggests_storage.files
    assert 'suggests_model_blue.xml' in suggests_storage.files

    assert 'suggests_category.xml' in suggests_storage.files
    assert 'suggests_category_blue.xml' in suggests_storage.files


def test_select_from(suggests_storage):
    """
    Метод select_from принимает имя файла из которого нужно достать все саджесты и возвращает их список.

    В метод можно передавать имена нескольких файлов, тогда он вернет саджесты, содержащиеся во всех переданых файлах.
    """

    assert len(suggests_storage.select_from('suggest.xml')) == 7

    # проверяем мультифайловый селект
    assert len(suggests_storage.select_from('suggest.xml', 'suggest_blue.xml')) == 14


def test_select_from_where(suggests_storage):
    """
    При необходимости после метода select_from можно позвать метод where, который принимает функцию, возвращающую
    True, либо False и в зависимости от ее результата, либо добавляет объект в выдачу, либо нет. Можно вызывать
    несколько where подряд, вызвать select_from после where невозможно.
    У where есть два флага, один из которых используется для того, чтобы исключить (exclude_fixed) из результатов вайтлистовые
    саджесты, другой для того, чтобы оставить только их (only_fixed). Эти два флага не могут быть одновременно True,
    вместе с ними можно как и раньше передавать любую функцию, которая по объекту возвращает True или False,
    в результате останутся только объекты, которые удовлетворяют обоим условиям.
    """

    # выбираем все товарные саджесты у которых производитель -- Пакет
    assert len(suggests_storage.select_from('suggests_model.xml').where(lambda obj: obj.vendor == u'Пакет')) == 3

    # проверяем, что where не меняет содержимое листа, а возвращет новы
    low_popularity = suggests_storage.select_from('suggest_blue.xml')

    assert len(low_popularity) == 7
    assert len(low_popularity.where(lambda obj: obj.vendor == u'Пакет')) == 3
    assert len(low_popularity) == 7

    # проверяем несколько where подряд
    french_soldier = suggests_storage.select_from('suggest_blue.xml')\
        .where(lambda obj: obj.vendor == u'Пакет')\
        .where(lambda obj: obj.name.startswith(u'Французский'))

    assert len(french_soldier) == 1

    # вызов where одновременно с only_fixed=True и exclude_fixed=True приводит к исключению, нельзя исключить
    # из результатов записи из вайтлиста и одновременно оставить только их
    with pytest.raises(AssertionError):
        suggests_storage.select_from('suggest.xml').where(only_fixed=True, exclude_fixed=True)

    # в выгрузке для тестов is_fixed есть только в файле suggest.xml
    names_of_fixed = {u'Украшения для организации праздников', u'Садовые столы', u'Скоростной чех'}
    only_fixed = suggests_storage.select_from('suggest.xml').where(only_fixed=True)

    assert len(only_fixed) == 3
    for obj in only_fixed:
        assert obj.name in names_of_fixed

    exclude_fixed = suggests_storage.select_from('suggest.xml').where(exclude_fixed=True)
    assert len(exclude_fixed) == 4
    for obj in exclude_fixed:
        assert obj.name not in names_of_fixed

    mixed_condition_exclude = suggests_storage.select_from('suggest.xml').where(lambda obj: obj.type == 'category', exclude_fixed=True)
    assert len(mixed_condition_exclude) == 2
    for obj in mixed_condition_exclude:
        assert obj.type == 'category'
        assert not obj.rich_props[0]['is_fixed']

    mixed_condition_only = suggests_storage.select_from('suggest.xml').where(lambda obj: obj.type == 'model', only_fixed=True)
    assert len(mixed_condition_only) == 1
    assert mixed_condition_only[0].type == 'model'
    assert mixed_condition_only[0].rich_props[0]['is_fixed']


def test_types(suggests_storage):
    for suggest in suggests_storage.select_from('suggests_model.xml', 'suggests_model_blue.xml'):
        assert suggest.type == 'model'

    for suggest in suggests_storage.select_from('suggests_category.xml', 'suggests_category_blue.xml'):
        assert suggest.type == 'category'


def test_suggests_fields(suggests_storage):
    """
    select_from возвращает объект, описывающий саджест, который содержит следующие поля:
    1) type -- тип саджеста (model, category, shop etc.)
    2) vendor -- производитель товара, которому соответсвует саджеста
    3) name -- каноническое имя саджеста
    4) aliases -- список алиасов, ассоциированных с саджестом
    5) popularity -- вещественное число большее, либо равное 1
    TODO: в тестовой выгрузке нашел объект с популярностью 0, нужно проверить все ли идет хорошо
    6) url
    7) rich_props -- json, описывающий объект, должен быть заключен в тег <![CDATA[{json}]]]>

    У самого объекта в выгрузке больше полей, некоторые из них не используются, про некоторые мне неизвестно,
    что они используются, добавил самые необходимые на мой взгляд.

    Поля 1), 3), 4) и 5) -- обязательные, если SuggestsStorage не сможет их найти, то упадет с сответствующей ошибкой
    на этапе создания, их значения отличны от None, в противном случае, если значение поля не найдено, то оно считается
    равным None.
    """

    mandatory_fields = ['popularity', 'type', 'name', 'url']
    other_fields = ['vendor', 'aliases', 'rich_props']

    # проверяем, что все перечисленные поля есть у каждого объекта выгрузки, при этом значение обязательных полей
    # отлично от None, для этого явно обратимся к каждому полю
    for filename in suggests_storage.files:
        for suggest in suggests_storage.select_from(filename):
            for mandatory_field in mandatory_fields:
                try:
                    getattr(suggest, mandatory_field)
                except AttributeError:
                    pytest.fail('Unexpected attribute error with {} property'.format(mandatory_field))

                assert getattr(suggest, mandatory_field) is not None

            for other_field in other_fields:
                try:
                    getattr(suggest, other_field)
                except AttributeError:
                    pytest.fail('Unexpected attribute error with {} property'.format(other_field))

    french_solder_models = suggests_storage.select_from('suggest_blue.xml').where(lambda obj: obj.name == u'Французский солдат')

    # проверяем, что нашелся ровно один саджест (однако это не значит, что все name уникальны в общем случае)
    assert len(french_solder_models) == 1

    # проверяем содержимое этого саджеста, а заодно, что обращение к элементу через квадратные скобки работает
    french_soldier = french_solder_models[0]

    # <object type="model">
    assert french_soldier.type == 'model'

    assert french_soldier.vendor == u'Пакет'
    assert french_soldier.name == u'Французский солдат'
    assert french_soldier.aliases == [u'M4A1 REVALORISE', u'M4A1 REVALORISE (Пакет "Французский солдат")', u'M4A1 REVALORISE (Французский солдат)']
    assert french_soldier.popularity == 1.0

    # в урле из выгрузки встречается &amp; вместо &, но xml парсер их проглатывает
    assert french_soldier.url == '/product--paket-frantsuzskii-soldat/13740402' \
                                '?hid=13491631&rt=4&' \
                                'suggest_text=%D0%9F%D0%B0%D0%BA%D0%B5%D1%82%20%D0%A4%D1%80%D0%B0%D0%BD%D1%86%D1%83%D0%B7%D1' \
                                '%81%D0%BA%D0%B8%D0%B9%20%D1%81%D0%BE%D0%BB%D0%B4%D0%B0%D1%82&' \
                                'suggest=1&suggest_type=model'

    # конкретно у этого объекта rich_props в выгрузке не совсем настоящие, но основанные на реальных событиях
    french_soldier_rich_props = [{
        "prices":
            [
                {"region": 172},
                {
                    "max": 62936.0,
                    "region": 213,
                    "avg": 32399.0,
                    "min": 14590.0
                },
                {"region": 240}
            ],
        "text": u"Пакет Французский солдат",
        "type": "model",
        "img": "image_link",
        "link": "suggest_link"
    }]

    assert french_soldier.rich_props == french_soldier_rich_props

    # данный категорийный саджест не содержит rich_props в выгрузке suggest_blue.xml
    fast_czech_models = suggests_storage.select_from('suggest_blue.xml').where(lambda obj: obj.name == u'Скоростной чех')

    # проверяем, что Скоростной чех уникален
    assert len(fast_czech_models) == 1

    fast_czech = fast_czech_models[0]

    assert fast_czech.rich_props is None

    # проверяем, что оставшиеся необязательные поля не None
    assert fast_czech.vendor is not None

    # проверяем, что алиас всего один
    assert fast_czech.aliases == [u'SKODA T 40 (Пакет "Скоростной чех")']
