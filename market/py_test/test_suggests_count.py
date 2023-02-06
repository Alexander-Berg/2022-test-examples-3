# *- encoding: utf-8 -*


def test_all_suggests_file(suggest_storage):
    assert len(suggest_storage) == 48


def test_all_suggests_count(suggest_storage):
    # синие саджесты
    assert len(suggest_storage['suggests_category_blue.xml']) == 3, '\n'.join([obj.name for obj in suggest_storage['suggests_category_blue.xml']])
    assert len(suggest_storage['suggests_model_blue.xml']) == 9
    assert len(suggest_storage['suggests_category_nofarm_blue.xml']) == 1
    assert len(suggest_storage['suggests_model_nofarm_blue.xml']) == 6
    assert len(suggest_storage['suggests_categories_vendors_blue.xml']) == 2
    assert len(suggest_storage['suggests_categories_vendors_nofarm_blue.xml']) == 2
    assert len(suggest_storage['suggests_vendor_blue.xml']) == 2

    # белые саджесты
    assert len(suggest_storage['suggests_vendor.xml']) == 2
    assert len(suggest_storage['suggests_recipe.xml']) == 30
    assert len(suggest_storage['suggests_categories_vendors.xml']) == 81
    assert len(suggest_storage['suggests_shop.xml']) == 71
    assert len(suggest_storage['suggests_category.xml']) == 2028
    assert len(suggest_storage['suggests_model.xml']) == 625
    assert len(suggest_storage['suggests_model_reviews.xml']) == 5
    assert len(suggest_storage['suggests_model_opinions.xml']) == 7
    assert len(suggest_storage['suggests_collection.xml']) == 6
    assert len(suggest_storage['suggests_franchise.xml']) == 5
    assert len(suggest_storage['suggests_article.xml']) == 8

    # саджесты тача/мобилки
    assert len(suggest_storage['suggest_blue.xml']) == 16
    assert len(suggest_storage['suggest.xml']) == 2868
