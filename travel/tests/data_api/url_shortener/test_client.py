# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.conf import settings
from six.moves.urllib_parse import quote

from common.data_api.url_shortener.client import ClckClient, ClckClientException


@pytest.mark.parametrize('url_type', [ClckClient.SHORT_TYPE, ClckClient.LONG_TYPE])
def test_shortened(httpretty, url_type):
    url_to_shorten = 'http://some.meaningless.url/?foo=baz'
    shortened_url = 'some.shortened.url'
    httpretty.register_uri(httpretty.GET,
                           '{}/--?type={}&url={}'.format(settings.SHORTENER_URL,
                                                         url_type,
                                                         quote(url_to_shorten)),
                           body=shortened_url)
    client = ClckClient(url_type=url_type)
    result = client.shorten(url_to_shorten)
    assert result == shortened_url
    query_string = httpretty.last_request.querystring
    assert query_string['url'][0] == url_to_shorten
    if url_type:
        assert query_string['type'][0] == url_type


def test_shortening_failed(httpretty):
    with pytest.raises(ClckClientException):
        httpretty.register_uri(httpretty.GET,
                               '{}/--?type=&url=foo'.format(settings.SHORTENER_URL),
                               status=500)
        ClckClient().shorten('foo')
