# *- encoding: utf-8 -*
from utils import url


def test_show_logo_in_blue_vendor_suggest(suggest_storage):
    """
    На синем лого добавляется в вендорный саджест по флагу из МБО
    https://st.yandex-team.ru/MARKETKGB-1248
    """
    show_logo_in_vendor_suggest = {'1 TOY': True}

    vendors = suggest_storage.select_from('suggests_vendor_blue.xml')
    vendors.extend(suggest_storage.select_from('suggest_blue.xml').where(lambda obj: obj.type == "vendor"))

    for obj in vendors:
        logo_in_rich_props = 'img' in obj.rich_props[0]

        if show_logo_in_vendor_suggest.get(obj.name, False):
            assert logo_in_rich_props, "no logo for %s" % obj.name
        else:
            assert not logo_in_rich_props, "unexpected logo for %s" % obj.name


def test_rich_props_has_text_in_touch(suggest_storage):
    """
    Белая мобильная выгрузка должна содержать rich_props и поле text в нем, чтобы фронт мог брать текст подсказки
    оттуда, а не из выдачи саджестера.
    https://st.yandex-team.ru/MARKETOUT-30268
    https://st.yandex-team.ru/MARKETOUT-29469
    """

    # белосписочные саджесты содержат в поле text немного другое (см. test_fixed_objects.py), поэтому выбрасываем их из
    # рассмотрения
    suggests_white = suggest_storage.select_from('suggest.xml').where(exclude_fixed=True)
    for obj in suggests_white:
        assert obj.rich_props is not None, '%s @ suggest.xml with type=%s has no rich_props' % (obj.name, obj.type)
        assert 'text' in obj.rich_props[0], '%s @ suggest.xml with type=%s has no "text"' % (obj.name, obj.type)
        if obj.type == 'model':
            assert obj.rich_props[0]['text'].strip() == obj.vendor + ' ' + obj.name if obj.vendor is not None else obj.name,\
                '%s @ suggest.xml, type=model, unexpected text=%s' % (obj.name, obj.rich_props[0]['text'])
        else:
            assert obj.rich_props[0]['text'].strip() == obj.name


def test_model_id(suggest_storage):
    """
    В ричпропсах у белых моделей должен быть параметр model_id
    """
    # Проверяем, что model_id встречается только у моделей на белом
    for obj in suggest_storage.select_from_white():
        if obj.type == 'model':
            assert obj.rich_props is not None, 'empty rich_props fot model with name=%s' % obj.name
            assert 'model_id' in obj.rich_props[0], 'model with name=%s has no model_id' % obj.name

            # в большинстве случаев верный model_id можно распарсить из урла текущего объекта нашей выгрузки
            expected_model_id = url.get_product_id(obj.url)

            # в случае укороченного имени объекта проствляется ссылкаа на поиск, из которой нельзя восстановить
            # изначальный model_id; проверим вручную для нескольких model_id, что им соответствует правильное имя
            if expected_model_id is None:
                if obj.rich_props[0]['model_id'] == 1759295080:
                    assert obj.name == 'Холодное сердце Анна, Кристофф, Олаф и', 'unexpected name %s for model_id=%d' % (obj.name, obj.rich_props[0]['model_is'])
                if obj.rich_props[0]['model_id'] == 1759295086:
                    assert obj.name == 'Холодное сердце Анна, Кристофф, Олаф и', 'unexpected name %s for model_id=%d' % (obj.name, obj.rich_props[0]['model_is'])
            else:
                # иначе проверяем, что model_id совпадает с тем, что в урле
                assert obj.rich_props[0]['model_id'] == expected_model_id, 'model_id mismatch name="%s" ' % obj.name
        else:
            assert obj.rich_props is None or 'model_id' not in obj.rich_props[0], '%s %s has model_id' % (obj.type, obj.name)

    # Проверяем, что на синем не проставляется model_id
    for obj in suggest_storage.select_from_blue():
        assert obj.rich_props is None or 'model_id' not in obj.rich_props[0]
