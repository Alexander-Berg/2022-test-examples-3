# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import gzip
import os

from django.contrib.sitemaps import Sitemap
from django.contrib.sites.models import Site
from django.utils import six
from lxml import etree

from travel.rasp.library.python.sitemap.generate import generate_sitemaps


parser = etree.XMLParser(remove_blank_text=True, resolve_entities=False, load_dtd=False, no_network=True)


def compare_xml(expected_xml, xml_gz_filepath):
    assert os.path.exists(xml_gz_filepath)
    with gzip.open(xml_gz_filepath, 'rb') as f:
        result = etree.fromstring(f.read(), parser=parser)

    expected = etree.fromstring(expected_xml.encode('utf-8'), parser=parser)

    assert etree.tounicode(result) == etree.tounicode(expected)


class UrlsListSitemap(Sitemap):
    protocol = 'https'
    urls = None

    def items(self):
        return self.urls

    def location(self, url):
        return url


def test_no_pagination(tmpdir):
    class UrlsListSitemapNoPaginate(UrlsListSitemap):
        urls = ['/a', '/b']

    generate_sitemaps(
        six.text_type(tmpdir),
        Site(domain='domain.ru'),
        {
            'index': UrlsListSitemapNoPaginate
        }
    )

    compare_xml("""
    <?xml version="1.0" encoding="UTF-8"?>
    <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
        <sitemap>
            <loc>https://domain.ru/sitemaps/domain.ru/index.xml.gz</loc>
        </sitemap>
    </sitemapindex>
    """.strip(), six.text_type(tmpdir.join('sitemap.xml.gz')))

    compare_xml("""
    <?xml version="1.0" encoding="UTF-8"?>
    <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
        <url>
            <loc>https://domain.ru/a</loc>
        </url>
        <url>
            <loc>https://domain.ru/b</loc>
        </url>
    </urlset>
    """.strip(), six.text_type(tmpdir.join('index.xml.gz')))


def test_with_pagination(tmpdir):
    class UrlsListSitemapPaginate(UrlsListSitemap):
        limit = 1
        urls = ['/a', '/b']

    generate_sitemaps(
        six.text_type(tmpdir),
        Site(domain='domain.ru'),
        {
            'index': UrlsListSitemapPaginate
        }
    )

    compare_xml("""
    <?xml version="1.0" encoding="UTF-8"?>
    <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
        <sitemap>
            <loc>https://domain.ru/sitemaps/domain.ru/index_1.xml.gz</loc>
        </sitemap>
        <sitemap>
            <loc>https://domain.ru/sitemaps/domain.ru/index_2.xml.gz</loc>
        </sitemap>
    </sitemapindex>
    """.strip(), six.text_type(tmpdir.join('sitemap.xml.gz')))

    compare_xml("""
    <?xml version="1.0" encoding="UTF-8"?>
    <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
        <url>
            <loc>https://domain.ru/a</loc>
        </url>
    </urlset>
    """.strip(), six.text_type(tmpdir.join('index_1.xml.gz')))

    compare_xml("""
    <?xml version="1.0" encoding="UTF-8"?>
    <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
        <url>
            <loc>https://domain.ru/b</loc>
        </url>
    </urlset>
    """.strip(), six.text_type(tmpdir.join('index_2.xml.gz')))
