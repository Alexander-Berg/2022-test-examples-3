# -*- coding: utf-8 -*-

from market.proto.content.mbo import TovarTree_pb2
from market.pylibrary.mbostuff import tovartree


def generate_name(hid):
    return 'category_{}'.format(hid)


def generate_unique_name(hid):
    return 'unique_category_{}'.format(hid)


def generate_alias(hid, index, lang_id):
    return '{}_alias_{}_{}'.format(index, hid, lang_id)


def generate_category_pb(hid):
    category = TovarTree_pb2.TovarCategory()
    category.hid = hid
    category.tovar_id = hid * 100
    if hid > 100:
        category.no_search = hid > 200

    name = category.name.add()
    name.name = generate_name(hid)
    name.lang_id = 225
    unique_name = category.unique_name.add()
    unique_name.name = generate_unique_name(hid)
    unique_name.lang_id = 225

    for index in range(1, hid // 100):
        for lang_id in {225, 187}:
            alias_name = generate_alias(hid, index, lang_id)
            alias = category.alias.add()
            alias.name = alias_name
            alias.lang_id = lang_id

    return category


def test_tovartree():
    hids = (100, 200, 300)
    categories_pb = [generate_category_pb(hid) for hid in hids]
    tovar_categories = dict()
    for category in tovartree.tovar_tree_converter(categories_pb):
        tovar_categories[category.hid] = category

    assert len(tovar_categories) == 3
    for hid in hids:
        tovar_category = tovar_categories[hid]
        assert isinstance(tovar_category, tovartree.TovarCategory)
        assert tovar_category.hid == hid
        assert tovar_category.name == generate_name(hid)
        assert tovar_category.unique_name == generate_unique_name(hid)
        aliases_count = hid // 100 - 1
        assert len(tovar_category.aliases) == aliases_count * 2  # на двух языках
        if aliases_count:
            assert tovar_category.aliases[-2] == generate_alias(hid, aliases_count, 225)
            assert tovar_category.aliases[-1] == generate_alias(hid, aliases_count, 187)
        assert tovar_category.no_search == (hid > 200)
