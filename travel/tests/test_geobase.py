# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.utils_db.geobase import geobase, _Geobase6Lookup, _GeoRegion


class TestGeobase(object):
    def setup_class(self):
        self.geobase6 = geobase
        assert isinstance(self.geobase6, _Geobase6Lookup)

    @pytest.mark.parametrize('attr', [
        'id', 'name', 'ename', 'timezone', 'chief_region'
    ])
    def test_region_by_id_attrs(self, attr):
        go6 = self.geobase6.region_by_id(977)
        assert isinstance(go6, _GeoRegion)
