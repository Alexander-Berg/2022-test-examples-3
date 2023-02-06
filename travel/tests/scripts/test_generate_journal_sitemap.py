# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import gzip
from xml.etree import ElementTree

from travel.rasp.rasp_scripts.scripts.generate_journal_sitemap import generate_sitemap_file


def test_generate_sitemap_file(tmpdir):
    urls = [
        'https://travel.yandex.ru/url1',
        'https://travel.yandex.ru/url2',
    ]
    sitemap_file_path = tmpdir.mkdir('sitemaps').join('sitemap.xml.gz')
    generate_sitemap_file(sitemap_file_path.strpath, urls, False)

    assert sitemap_file_path.check()

    tree = ElementTree.parse(gzip.open(sitemap_file_path.strpath))
    root = tree.getroot()

    assert root.tag == r'{http://www.sitemaps.org/schemas/sitemap/0.9}urlset'

    namespaces = {'xmlns': 'http://www.sitemaps.org/schemas/sitemap/0.9'}
    for url_node in root.findall('xmlns:url', namespaces):
        loc_node = url_node.find('xmlns:loc', namespaces)
        assert loc_node.text in urls
