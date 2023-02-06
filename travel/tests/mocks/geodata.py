# -*- encoding: utf-8 -*-
from http_geobase.models import GeobaseRegion


class MockGeoDataClient(object):
    def __init__(self):
        self.params = {}

    def save(self, **kw):
        self.params.update(kw)

    def country_by_geo_id(self, geo_id):
        return GeobaseRegion(self.params)

    def country_by_iso_name(self, iso_name):
        return GeobaseRegion(self.params)
