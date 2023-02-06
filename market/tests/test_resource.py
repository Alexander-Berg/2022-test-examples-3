# -*- coding: utf-8 -*-

import json
import market.pylibrary.yatestwrap.yatestwrap as yatestwrap

from market.idx.promos.blue_promo_landing.lib.cms.resource import Resource

PATH_TO_SAMPLE_JSON = yatestwrap.source_path('market/idx/promos/blue_promo_landing/lib/cms/tests/fixtures/sample.json')

with open(PATH_TO_SAMPLE_JSON, 'r') as f:
    fixture = json.load(f)


class TestResource:
    def test_filter_links(self):
        instance = Resource(fixture)

        prev_values_len = len(instance.get_values())
        prev_nodes_len = len(instance.get_nodes())

        instance.filter_tag_links()

        actual_values_len = len(instance.get_values())
        actual_nodes_len = len(instance.get_nodes())

        assert prev_values_len - actual_values_len == 2
        assert prev_nodes_len - actual_nodes_len == 1

    def test_filter_links_should_update_page_links_in_values(self):
        instance = Resource(fixture)

        links_value = instance.get_links_value()

        items_count_before = len(links_value['collections']['items'])

        instance.filter_tag_links()

        items_count_after = len(links_value['collections']['items'])

        assert items_count_before - items_count_after == 1

    def test_add_new_promo_link(self):
        instance = Resource(fixture)

        instance.filter_tag_links()

        prev_values_len = len(instance.get_values())
        prev_nodes_len = len(instance.get_nodes())

        node_id = instance.add_new_promo_link(1, 'promo1')

        assert node_id == -1

        actual_values_len = len(instance.get_values())
        actual_nodes_len = len(instance.get_nodes())

        assert actual_values_len - prev_values_len == 2
        assert actual_nodes_len - prev_nodes_len == 1

        new_node = instance.get_nodes()[node_id]

        assert len(new_node['values']) == 2

        new_values = instance.get_values()

        assert new_values.get(-1) is not None
        assert new_values.get(-2) is not None
