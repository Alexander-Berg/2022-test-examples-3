# -*- coding: utf-8 -*-

from collections import namedtuple


Point = namedtuple('Point', ['lon', 'lat'])


def point_from_string(ll_str, delim=','):
    if not ll_str:
        return None
    lon, lat = [float(x) for x in ll_str.split(delim)]
    return Point(lon, lat)
