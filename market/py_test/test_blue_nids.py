# *- encoding: utf-8 -*


def test_blue_nid_filtration(suggest_storage):
    """ Проверяем, что фильтруются nid'ы, для которых не проставлен флажок is_blue="1"
    """
    blue_nid = suggest_storage.select_from('suggests_category_blue.xml').where(lambda obj: obj.name == 'Blue Раскладушки')
    assert len(blue_nid) == 0

    blue_nid = suggest_storage.select_from('suggests_category_blue.xml').where(lambda obj: obj.name == 'Blue World of Tanks')
    assert len(blue_nid) == 1
