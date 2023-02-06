# coding=utf-8

import os.path
import unicodedata
import urlparse
import HTMLParser
import requests
import logging
import re

logging = logging.getLogger("ylite")

import pytest
import pprint

from . import SerpDataHtml
from runtime.queries import SearchUrl


__author__ = 'aokhotin'


def read_vitallink_test_data(path):
    """
    Read data from vitallink URLs file.
    One record - one line. Record is line with 2 fields, <request> and
    <expected urls>, separated by '\t'.
    <expected urls> is a list of URLs separated by ','.
    Line starting with '#' is a comment.
    Example:
        vkontakte\thttp://vk.com/, http://vkontakte.ru/
    """
    with open(path) as f:
        lines = filter(lambda l: not l.startswith('#'), f.readlines() )

    get_query = lambda line: line.split('\t')[0].strip()
    get_expected = lambda line: [url.strip() for url in line.split('\t')[1].split(',')]

    return [(get_query(line), get_expected(line)) for line in lines]


def original_page_url(url):
    """
    Parse 'saved page copy' URL and extract original page URL from parameters.
    When given URL seems not to be a 'saved copy' URL just return the URL with no parsing at all.

    :param url: 'saved copy' page URL.
    :return: original page link when 'url' is a Yandex 'saved copy' URL.
    """

    # Some URLs can contain 'incorrect' urlencoded string which results into illegal characters after
    # URL decoding (unquoting). So, trying to decode such URL string as UTF-8 raises an UnicodeDecodeError.
    try:
        # Original 'saved copy' URL may contain HTML '&amp;' instead of '&' symbols, so replace them:
        html_replaced = HTMLParser.HTMLParser().unescape(url.decode('utf-8'))
    except UnicodeDecodeError:
        return url

    parsed_url = urlparse.urlparse(html_replaced)

    if parsed_url.netloc == 'hghltd.yandex.net' and parsed_url.path == '/yandbtm':
        original_unicode_url = urlparse.parse_qs(parsed_url.query)['url'][0]
        ascii_url = unicodedata.normalize('NFKD', original_unicode_url).encode('ascii', 'ignore')
        return ascii_url
    else:
        return url


def get_urls(beta, query):
    prod_seaarch_url = SearchUrl(url_string=beta)
    prod_seaarch_url.query += {"text": query}
    resp = requests.get(prod_seaarch_url.string, verify=False)
    resp.raise_for_status()
    urls = SerpDataHtml(resp.content).urls
    original_urls = map(original_page_url, urls)

    return original_urls

def is_passed(got, expected):
    for pattern in expected:
        found = False
    	for url in got:
            if re.search(pattern, url):
                found = True
                break
        if not found:
            return False
    return True

def test_vitallink(query, expected, beta, prod):
    beta_urls = get_urls(beta, query)
    beta_passed = is_passed(beta_urls, expected)
    prod_urls = get_urls(prod, query)
    prod_passed = is_passed(prod_urls, expected)
    message = '\nTest query: "{0}". \nExpected url: {1}. \nBeta urls: {2}. \nProd urls: {3}.'\
              .format(query,
                      expected,
                      pprint.pformat(beta_urls),
                      pprint.pformat(prod_urls)).decode('UTF-8')
    if not beta_passed and not prod_passed:
        logging.info(message)
        pytest.skip(message)
    assert beta_passed or not prod_passed, message


def pytest_generate_tests(metafunc):
    beta = metafunc.config.option.beta
    prod = metafunc.config.option.prod
    data_path = metafunc.config.option.test_data_path
    if "beta" in metafunc.fixturenames:
        metafunc.parametrize("beta", [beta])
    if "prod" in metafunc.fixturenames:
        metafunc.parametrize("prod", [prod])
    if set(["query", "expected"]).issubset(metafunc.fixturenames):
        data_file = os.path.join(data_path, "vitallink.test.data")
        metafunc.parametrize(
            "query, expected",
            read_vitallink_test_data(data_file),
            # ids=map(lambda x: unicode(x), read_vitallink_test_data(data_file))
        )
