#!/usr/bin/env python
# -*- coding: utf-8 -*-

from search.geo.tools.iznanka.lib.bbox_tools import bbox, is_small_bbox
from search.geo.tools.iznanka.lib.url_tools import get_host, normalize_url
from search.geo.tools.iznanka.lib.permalink_owners import from_one_owner


def testNormalizeUrl():
    url2normalized = [
        ('http://ya.ru/maps?text=cafe', 'ya.ru/maps?text=cafe'),
        ('www.host.ru:1234/path/', 'host.ru:1234/path'),
        ('google.com', 'google.com'),
        ('http://www.m.ya.рф/maps?text=cafe', 'ya.рф/maps?text=cafe'),
        ('http://m.ya.рф/maps?text=cafe', 'ya.рф/maps?text=cafe'),
    ]
    for u, n in url2normalized:
        assert(normalize_url(u) == n)


def testGetHost():
    url2host = [
        ('http://ya.ru/maps?text=cafe', 'ya.ru'),
        ('www.host.ru:1234/path/', 'host.ru:1234'),
        ('google.com', 'google.com'),
        ('http://www.m.ya.рф/maps?text=cafe', 'ya.рф'),
        ('http://m.ya.рф/maps?text=cafe', 'ya.рф'),
    ]
    for u, h in url2host:
        assert(get_host(u) == h)


def testFromOneOwner():
    permalink2chain = {1: 10, 2: 10, 3: 20}
    assert(from_one_owner([1], permalink2chain))
    assert(from_one_owner([1, 2], permalink2chain))
    assert(from_one_owner([4], permalink2chain))
    assert(not from_one_owner([1, 3], permalink2chain))
    assert(not from_one_owner([1, 4], permalink2chain))
    assert(not from_one_owner([4, 5], permalink2chain))


def testBbox():
    assert(bbox([[65.520132, 57.161828], [65.52032586, 57.161694792], [65.52032586, 57.161694792], [65.521042, 57.160839], [65.521042, 57.160839]]) == [[65.520132, 57.160839], [65.521042, 57.161828]])


def testIsSmallBbox():
    assert(is_small_bbox([[65.520132, 57.160839], [65.521042, 57.161828]]))
    assert(not is_small_bbox([[65.520132, 57.160839], [65.530272, 57.160659]]))
