# *- encoding: utf-8 -*


def test_blue_shops_filtered_out(suggest_storage):
    """
    Синие магазины не должны прорастать в белую магазинную выгрузку, а должны быть
    отфильтрованы.
    """

    # магазин с данным shop_id имеет в shops.dat поле blue_status=REAL, что и означает, что он синий
    supplier_id = '10224210'
    assert suggest_storage.select_from('suggests_shop.xml').where(lambda obj: supplier_id in obj.url).empty()

    # магазин с данным shop_id имеет в shops.dat поле blue_status=NO, что означает, что он НЕ должен быть отфильтрован как синий
    not_supplier_but_shop_id = '10203657'
    assert suggest_storage.select_from('suggests_shop.xml').where(lambda obj: not_supplier_but_shop_id in obj.url).expected_len(1)
