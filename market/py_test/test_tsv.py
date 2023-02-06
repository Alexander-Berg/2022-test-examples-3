# *- encoding: utf-8 -*

from collections import defaultdict
import urlparse
import os
import pytest


@pytest.mark.skip(reason='MARKETOUT-39033 turned off generating tsv')
def test_popularity_from_yt(suggest_storage):
    """
    https://st.yandex-team.ru/MARKETOUT-34342
    Популярность саджестов рассчитывается отдельным регулярным процессом, результатом работы которого является табличка
    на yt, содержащая в себе ключи и их веса. Саджесту приравнивается вес ключа, если его текст совпадает с ключом.
    Если ни один из ключей не сматчился с саджестом, то саджест получает вес 1.
    """

    # локальная таблица популярности определена в py_test/conftest.py
    objs = suggest_storage.select_from_all_tsv().where(lambda obj: obj.popularity == 4321)

    assert len(objs) == 1, objs
    assert objs[0].name == '1 toy angry birds (т59159)'
    # проверяем, что это каноническое имя
    assert objs[0].canonic_name == objs[0].name

    objs = suggest_storage.select_from_all_tsv().where(lambda obj: obj.popularity == 1234)
    assert len(objs) == 1, objs
    assert objs[0].name == '"angry birds", 52х50 см'
    # проверяем, что это алиас, таким образом в отличие от xml выгрузки, в tsv алиас и его каноническое имя могут
    # иметь разную популярность
    assert objs[0].canonic_name == '1 toy angry birds (т59159)'

    # проверяем, что у оставшихся записей в tsv популярность равняется 1
    suggests_with_popularity = {
        u'1 toy angry birds (т59159)',
        u'"angry birds", 52х50 см',
        u'performa',
    }

    unexpected_suggests_with_popoularity = suggest_storage.select_from_all_tsv_except_search().where(
        lambda obj: obj.name not in suggests_with_popularity and obj.popularity != 1
    )
    assert unexpected_suggests_with_popoularity.expected_len(0), str(unexpected_suggests_with_popoularity)


@pytest.mark.skip(reason='MARKETOUT-39033 turned off generating tsv')
def test_aliases_count(suggest_storage):
    canonic_names = set(obj.canonic_name for obj in suggest_storage.select_from_all_tsv())
    assert sum(len(obj.aliases) for obj in suggest_storage.select_from_all_tsv().where(
        lambda obj: obj.name == obj.canonic_name and obj.name in canonic_names)) == 6770


@pytest.mark.skip(reason='MARKETOUT-39033 turned off generating tsv')
def test_groups(suggest_storage):
    """
    https://st.yandex-team.ru/MARKETOUT-34342
   %type%_groups.txt -- файл, описывающий группы саджестов типа type
   Каждый файл выглядит как набор строк, где через таб выписанны все ключи саджестов имеющие одинаковый урл
   Этот файл используется саджестером для того, чтобы в выдаче не было саджестов, ведущих на один урл.
   Вместо нескольки саджестов с одинаковым урлом, показывается тот у которого популярность ключа наибольшая.
   """
    for filename in suggest_storage.all_tsv_suggests_types:
        expected_groups_dict = defaultdict(set)
        for suggest in suggest_storage[filename]:
            expected_groups_dict[suggest.url].add(suggest.name)
        expected_groups = set()
        for group in expected_groups_dict.values():
            if len(group) > 1:
                expected_groups.add(tuple(sorted(group)))
        groups_file = os.path.join(suggest_storage.get_gendir(), filename.replace('_ready', '_groups'))
        actual_groups = set()
        with open(groups_file, 'r') as f:
            for line in f:
                actual_groups.add(tuple(sorted([unicode(group_part) for group_part in line.strip().split('\t')])))

        for expected_group in expected_groups:
            assert expected_group in actual_groups, 'expected group: "{}" not found @ {}'.format(
                '\t'.join(expected_group), groups_file)
        for actual_group in actual_groups:
            assert actual_group in expected_groups, 'unexpected group: "{}" found @ {}'.format('\t'.join(actual_group),
                                                                                               groups_file)


@pytest.mark.skip(reason='MARKETOUT-39033 turned off generating tsv')
def test_search_suggests(suggest_storage):
    """
    Поисковые саджесты -- отдельный тип саджестов, в который попадают все записи из таблицы популярности,
    которые не сматчились по имени с каким-нибудь из саджестов из оставшейся выгрузки.
    Они содержатся в файле search_ready.txt, который как и все другие ready файлы имеет формат tsv.
    """
    # сейчас в тестовой таблице поуплярности содержится только две записи, которые должны стать поисковыми саджестами

    expected_search_suggests = {
        u'поисковая подсказка': 100,
        u'я поисковая подсказка': 123,
        u'21-le fou': 121,  # матчится с синим алиасом, но это не учитывается при формировании белых поисковых подсказок
    }

    # если данная проверка упала, скорее всего таблица популярности в py_test/conftest.py была отредактирована,
    # либо была нарушена логика генерации поисковых подсказок/перевзвешивания (см. код в ../utils/popularity_storage.py)
    assert len(suggest_storage['search_ready.txt']) == len(expected_search_suggests)

    for suggest in suggest_storage['search_ready.txt']:
        assert suggest.name in expected_search_suggests, 'got unexpected search suggest'

        assert suggest.popularity == expected_search_suggests[suggest.name], 'got unexpected popularity for search suggest'

        # проверяем rich_props, там должно быть четыре поля
        # 1) text совпадающий с каноническим именем саджеста
        # 2) popularity -- популярность, взятая из таблицы популярности
        # 3) type -- поле, описывающее тип саджеста, в данном случае всегда равняется search
        # 4) link -- url, ведущий на поиск начинается с search.xml и содержит в себе следующие cgi-параметры:
        #    4.1 text, совпадающий с каноническим именем саджеста
        #    4.2 suggest=2, обозначающий, что поиск произошел из-за перехода по поисковому саджесту
        #    4.3 cvredirect=2, разрешающий редирект

        assert suggest.name == suggest.rich_props[0]['text']
        assert suggest.popularity == suggest.rich_props[0]['popularity']
        assert suggest.rich_props[0]['type'] == 'search'

        parsed = urlparse.urlparse(suggest.rich_props[0]['link'].encode('ascii'))
        assert parsed.path == '/search.xml'

        parsed_qs = urlparse.parse_qs(parsed.query)
        assert len(parsed_qs) == 3
        assert len(parsed_qs['text']) == 1 and parsed_qs['text'][0] == suggest.name
        assert len(parsed_qs['cvredirect']) and parsed_qs['cvredirect'][0] == '2'
        assert len(parsed_qs['suggest']) and parsed_qs['suggest'][0] == '2'

    # проверяем, что во всей выгрузке поисковые подсказки встречаются ровно один раз
    for suggest in expected_search_suggests:
        assert suggest_storage.select_from_all_tsv_except_search().where(lambda obj: obj.name == suggest).expected_len(0)
