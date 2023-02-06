#!/usr/bin/env python
#  -*- coding: utf-8 -*-

import mock
import unittest

from search.geo.tools.goods_snippets.lib.extract_data import extract_goods, get_categories, get_merged_goods, get_goods_info


ITEM_WITH_NAME = {'name': 'name'}


def construct_category(name=None, item=None):
    category = {}
    if name:
        category.update({
            'category': name
        })
    if item is not None:
        category.update({
            'price_items': [item]
        })
    return category


def construct_categories(categories):
    return {
        'price_categories': categories
    }


class TestExtractGoods(unittest.TestCase):
    def test_given_empty_category_returns_empty(self):
        items = extract_goods({}, None)
        self.assertListEqual(items, [])

    def test_given_price_item_wo_name_returns_empty(self):
        category = construct_category(item={})
        items = extract_goods(category, None)
        self.assertListEqual(items, [])

    def test_given_price_item_with_name_returns_item(self):
        category = construct_category(item=ITEM_WITH_NAME)
        items = extract_goods(category, None)
        self.assertListEqual(items, [ITEM_WITH_NAME])

    def test_given_description_returns_item(self):
        item = {
            'name': 'name',
            'description': 'description'
        }
        category = construct_category(item=item)
        items = extract_goods(category, None)
        self.assertListEqual(items, [item])

    def test_given_volume_returns_item_with_unit(self):
        category = construct_category(item={
            'name': 'name',
            'volume': '1'
        })
        expected_items = [{
            'name': 'name',
            'unit': '1'
        }]
        items = extract_goods(category, None)
        self.assertListEqual(items, expected_items)

    def test_given_photo_link_returns_item_with_link(self):
        category = construct_category(item={
            'name': 'name',
            'photo_link': 'url'
        })
        expected_items = [{
            'name': 'name',
            'link': [{'uri': 'url'}]
        }]
        items = extract_goods(category, None)
        self.assertListEqual(items, expected_items)

    def test_given_source_url_returns_external_url(self):
        category = construct_category(item={
            'name': 'name',
            'source_url': 'url'
        })
        expected_items = [{
            'name': 'name',
            'external_url': 'url'
        }]
        items = extract_goods(category, None)
        self.assertListEqual(items, expected_items)

    def test_given_price_and_currency_returns_item_with_price(self):
        category = construct_category(item={
            'name': 'name',
            'price': '500'
        })
        expected_items = [{
            'name': 'name',
            'price': {
                    'value': 500.0,
                    'text': u'500 ₽',
                    'currency': 'RUB'
                }
        }]
        items = extract_goods(category, u'₽')
        self.assertListEqual(items, expected_items)

    def test_given_invalid_price_returns_items_wo_price(self):
        category = construct_category(item={
            'name': 'name',
            'price': 'not-a-number'
        })
        items = extract_goods(category, u'₽')
        self.assertListEqual(items, [ITEM_WITH_NAME])

    def test_given_price_wo_currency_returns_item_wo_price(self):
        category = construct_category(item={
            'name': 'name',
            'price': '500'
        })
        items = extract_goods(category, None)
        self.assertListEqual(items, [ITEM_WITH_NAME])

    def test_given_price_with_invalid_currency_returns_item_wo_price(self):
        category = construct_category(item={
            'name': 'name',
            'price': '500'
        })
        items = extract_goods(category, 'invalid')
        self.assertListEqual(items, [ITEM_WITH_NAME])

    def test_given_currency_wo_price_returns_item_wo_price(self):
        category = construct_category(item=ITEM_WITH_NAME)
        items = extract_goods(category, u'₽')
        self.assertListEqual(items, [ITEM_WITH_NAME])


@mock.patch('search.geo.tools.goods_snippets.lib.extract_data.extract_goods', autospec=True)
class TestGetCategories(unittest.TestCase):
    def test_when_goods_are_not_empty_returns_valid_categories(self, mock_extract_goods):
        mock_extract_goods.return_value = [ITEM_WITH_NAME]
        data = construct_categories([{}])
        expected_categories = [{
            'goods': [ITEM_WITH_NAME]
        }]
        categories = get_categories(data)
        self.assertListEqual(categories, expected_categories)

    def test_when_goods_are_empty_returns_wo_them(self, mock_extract_goods):
        mock_extract_goods.return_value = []
        data = construct_categories([construct_category(name='name1')])
        expected_categories = [{'name': 'name1'}]
        categories = get_categories(data)
        self.assertListEqual(categories, expected_categories)

    def test_given_no_goods_and_no_name_returns_empty(self, mock_extract_goods):
        mock_extract_goods.return_value = []
        data = construct_categories([{}])
        categories = get_categories(data)
        self.assertListEqual(categories, [])

    def test_given_empty_data_returns_empty(self, mock_extract_goods):
        categories = get_categories({})
        self.assertListEqual(categories, [])


@mock.patch('search.geo.tools.goods_snippets.lib.extract_data.get_categories', autospec=True)
class TestGetMergedGoods(unittest.TestCase):
    def test_given_empty_categories_returns_empty(self, mock_get_categories):
        mock_get_categories.return_value = []
        goods = get_merged_goods({})
        self.assertListEqual(goods, [])

    def test_given_categories_wo_goods_returns_empty(self, mock_get_categories):
        mock_get_categories.return_value = [
            {'name': 'name1'},
            {'name': 'name2'}
        ]
        goods = get_merged_goods({})
        self.assertListEqual(goods, [])

    def test_given_categories_with_goods_returns_merged_goods(self, mock_get_categories):
        mock_get_categories.return_value = [
            {'goods': [{'name': 'name1'}, {'name': 'name2'}]},
            {'goods': [{'name': 'name3'}]}
        ]
        expected_goods = [
            {'name': 'name1'},
            {'name': 'name2'},
            {'name': 'name3'}
        ]
        goods = get_merged_goods({})
        self.assertListEqual(goods, expected_goods)


class TestGetGoodsInfo(unittest.TestCase):
    def test_given_source_returns_source(self):
        snippet_w_source = {
            "source": {
                "catalog_type": "type",
                "disclaimer_text": "disclaimer",
                "id": "source_id",
                "name": "source_name)"
            }
        }
        goods_info = get_goods_info(snippet_w_source)
        self.assertDictEqual(goods_info, snippet_w_source)

    def test_given_relevance_returns_relevance(self):
        snippet_w_relevance = {
            "relevance": {
                "last_upload_time": "2022-05-23T15:06:44.997314Z",
                "last_update_time": "2022-05-23T14:06:44Z"
            }
        }
        goods_info = get_goods_info(snippet_w_relevance)
        self.assertDictEqual(goods_info, snippet_w_relevance)
