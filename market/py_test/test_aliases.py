# *- encoding: utf-8 -*


def test_aliases_count(suggest_storage):
    """
    Тест для отлавливания ситуаций подобных https://st.yandex-team.ru/MARKETOUT-30584
    если суммарное количество алиасов сильно и без особых причин поменялось, то
    возможно, что что-то пошло не так
    """
    assert sum(len(obj.aliases) for obj in suggest_storage.select_from_all_except_tsv()) == 29405


def test_unicode_aliases_at_category_suggest(suggest_storage):
    """
    Проверяем, что корректно обрабатывается ситуации, когда вместо списка алиасов
    передается unicode строка из алиасов, разделенных переносом строки
    https://st.yandex-team.ru/MARKETOUT-30584
    """
    obj = suggest_storage.select_from('suggests_category.xml').where(lambda obj: obj.name == u'Электровеники')[0]
    assert len(obj.aliases) == 2
    assert obj.aliases[0] == u'Электрические веники'


def test_vendor_line_in_aliases(suggest_storage):
    """
    Проверяем, что линейки. которые так же есть в таблице популярности, попадают в алиасы моельных саджестов.
    https://st.yandex-team.ru/MARKETOUT-37802
    """

    # синие саджесты
    # В выгрузке это sku с id 100521757240, name 'биосовместимый многоцелевой', линейкой у него является '21-le Fou'
    objs = suggest_storage.select_from('suggests_model_blue.xml').where(lambda obj: obj.name == u'биосовместимый многоцелевой')
    assert len(objs) == 1
    obj = objs[0]

    assert '21-le Fou' in obj.aliases

    # белые саджесты

    # проверяем, что линейка Performa добавилась в алиасы, так как содержится в таблице популярности
    objs = suggest_storage.select_from('suggests_model.xml').where(lambda obj: 'Performa' in obj.aliases)
    assert objs.expected_len(2), str(objs)

    expected_suggests_names = {
        u'контрольный раствор Performa',
        u'тест-полоски Performa'
    }

    for obj in objs:
        assert obj.name in expected_suggests_names

    # проверяем, что какая-нибудь другая линейка, например, UltraEasy не добавилась в алиасы какого-либо саджеста
    assert suggest_storage.select_from('suggests_model.xml').where(lambda obj: 'UltraEasy' in obj.aliases).expected_len(0)
