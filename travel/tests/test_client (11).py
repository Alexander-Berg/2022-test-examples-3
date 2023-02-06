# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty
from hamcrest import assert_that, has_entries, has_items

from travel.rasp.library.python.api_clients.market_cms import MarketCmsClient


@httpretty.activate
def test_market_cms_client():
    url = 'http://url.yandex'
    content = """
    {
        "__info__": {
            "version": "2021.10.27",
            "hostname": "vla1-5920-vla-market-prod-templator-9145.gencfg-c.yandex.net",
            "collection": "cms-context-relations",
            "caller": "GetContextPageSaas",
            "servant": "templator",
            "uptime": "315625.757525s"
        },
        "result": [
            {"type": "travel_article", "semanticPrefix": "prefix0", "semanticId": "id0", "pageTitle": "title0"},
            {"type": "travel_article", "semanticPrefix": "prefix1", "semanticId": "id1", "pageTitle": "title1"}
        ]
    }"""
    httpretty.register_uri(httpretty.GET, '{}/tarantino/getcontextpage'.format(url), status=200, body=content)

    client = MarketCmsClient(url)
    pages_data = client.get_context_page()

    assert_that(httpretty.last_request().querystring, has_entries({
        'device': ['desktop'],
        'format': ['json'],
        'type': ['travel_article,travel_tag,travel_journal'],
        'zoom': ['entrypoints'],
    }))

    assert_that(pages_data, has_items(
        has_entries({
            'type': 'travel_article',
            'semanticPrefix': 'prefix0',
            'semanticId': 'id0',
            'pageTitle': 'title0'
        }),
        has_entries({
            'type': 'travel_article',
            'semanticPrefix': 'prefix1',
            'semanticId': 'id1',
            'pageTitle': 'title1'
        }),
    ))
