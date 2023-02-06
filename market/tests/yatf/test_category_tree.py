# -*- coding: utf-8 -*-

import pytest

from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePb

from market.idx.pylibrary.mindexer_core.category_tree.category_tree import load_categories


@pytest.fixture(scope="module")
def categories_tree(tmpdir_factory):
    tovar_tree = TovarTreePb([
        MboCategory(hid=90401, tovar_id=0, parent_hid=-1,
                    unique_name="Все товары", name="Все товары",
                    output_type=MboCategory.GURULIGHT),
        MboCategory(hid=198119, tovar_id=1524, parent_hid=90401,
                    unique_name="Электроника", name="Электроника",
                    output_type=MboCategory.GURULIGHT),
        MboCategory(hid=90607, tovar_id=189, parent_hid=198119,
                    unique_name="Фото и видеокамеры", name="Фото и видеокамеры",
                    output_type=MboCategory.GURULIGHT),
        MboCategory(hid=91148, tovar_id=270, parent_hid=90607,
                    unique_name="Цифровые фотокамеры", name="Цифровые фотокамеры",
                    output_type=MboCategory.GURULIGHT),
        MboCategory(hid=90666, tovar_id=4, parent_hid=90401,
                    unique_name="Товары для дома и дачи", name="Дом и дача",
                    output_type=MboCategory.GURULIGHT),
        MboCategory(hid=91597, tovar_id=20, parent_hid=90666,
                    unique_name="Товары для строительства и ремонта", name="Строительство и ремонт",
                    output_type=MboCategory.GURULIGHT),
        MboCategory(hid=91704, tovar_id=857, parent_hid=91597,
                    unique_name="Отопление", name="Отопление",
                    output_type=MboCategory.GURULIGHT),
        MboCategory(hid=91708, tovar_id=1188, parent_hid=91704,
                    unique_name="Камины и печи", name="Камины и печи",
                    output_type=MboCategory.GURULIGHT)
    ])

    path = str(tmpdir_factory.getbasetemp().join(tovar_tree.filename))
    tovar_tree.dump(path)
    return load_categories(path)


def test_get_ids(categories_tree):
    CATEGORY_IDS = set([198119, 90666, 91148, 91597, 90607, 91704, 91708])
    CATEGORY_IDS_WITH_TOP = CATEGORY_IDS.union([90401])

    assert set(categories_tree.getIds()) == CATEGORY_IDS
    assert set(categories_tree.getIdsWithTop()) == CATEGORY_IDS_WITH_TOP


def test_cat_path(categories_tree):
    TEST_CASES = [
        (categories_tree.get(90401), [90401], [90401]),
        (categories_tree.get(198119), [198119], [198119, 90401]),
        (categories_tree.get(91148), [91148, 90607, 198119], [91148, 90607, 198119, 90401])
    ]

    for category, path, path_with_top in TEST_CASES:
        assert categories_tree.catPath(category) == path
        assert categories_tree.catPathWithTop(category) == path_with_top
