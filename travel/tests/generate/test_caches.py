# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from collections import defaultdict

import mock
import pytest
from django.contrib.contenttypes.models import ContentType

from common.models.geo import CodeSystem, PointSynonym, StationCode, SettlementCode, Station, Settlement

from travel.rasp.suggests_tasks.suggests.generate.caches import (
    Precache, CodePrecache, StationCodePrecache, SettlementCodePrecache, SynonymsPrecache
)

from common.tester.factories import create_settlement, create_station, create_station_code
from common.tester.testcase import TestCase


class TestPrecache(TestCase):
    def test_init(self):
        precache = Precache()
        assert precache.is_precached is False

    def test_precache(self):
        precache = Precache()
        precache.precache()
        assert precache.is_precached is True

    def test_get_if_not_precached(self):
        precache = Precache()
        with pytest.raises(NotImplementedError):
            precache.get_if_not_precached('obj')

    def test_get_if_precached(self):
        precache = Precache()
        with pytest.raises(NotImplementedError):
            precache.get_if_precached('obj')

    def test_get(self):
        with mock.patch.object(Precache, 'get_if_precached', return_value='precached') as m_if_cache, \
                mock.patch.object(Precache, 'get_if_not_precached', return_value='not_precached') as m_if_not_cache:
            obj = 'object'
            precache = Precache()
            assert precache.get(obj) == 'not_precached'
            m_if_not_cache.assert_called_once_with(obj)
            precache.precache()
            assert precache.get(obj) == 'precached'
            m_if_cache.assert_called_once_with(obj)


class TestCodePrecache(TestCase):
    code_systems = list(CodeSystem.objects.all()[:3])

    def test_init(self):
        precache = CodePrecache([system.code for system in self.code_systems])

        assert precache.model is None
        assert precache.is_precached is False
        assert precache.verbose is False
        assert precache.allowed_systems == self.code_systems
        assert precache.codes == defaultdict(dict)

    def test_precache(self):
        stations = [create_station() for i in range(2)]
        codes = [create_station_code(code='code{}'.format(i), system=self.code_systems[i], station=st)
                 for i, st in enumerate(stations)]
        df = defaultdict(dict)
        for i, code in enumerate(codes):
            df[stations[i]][self.code_systems[i].code] = code

        with mock.patch.object(Precache, 'precache') as m_precache:
            precache = CodePrecache([system.code for system in self.code_systems])
            precache.model = StationCode
            precache.precache()
            m_precache.assert_called_once_with()
            assert precache.codes == df

    def test_get_if_precached(self):
        precache = CodePrecache([system.code for system in self.code_systems])
        precache.codes['obj']['sys_code'] = 'code'
        assert precache.get_if_precached('not exist') == {}
        assert precache.get_if_precached('obj') == {'sys_code': 'code'}


class TestStationCodePrecache(TestCase):
    def test_get_if_not_precached(self):
        code_systems = list(CodeSystem.objects.all()[:3])
        stations = [create_station() for i in range(2)]
        codes = [create_station_code(code='code{}'.format(i), system=code_systems[i], station=st)
                 for i, st in enumerate(stations)]

        precache = StationCodePrecache([system.code for system in code_systems])

        assert precache.model == StationCode
        for i, st in enumerate(stations):
            assert precache.get_if_not_precached(st) == {code_systems[i].code: codes[i]}


class TestSettlementCodePrecache(TestCase):
    def test_get_if_not_precached(self):
        code_systems = list(CodeSystem.objects.all()[:3])
        settlements = [create_settlement() for i in range(2)]
        codes = [SettlementCode.objects.create(code='code{}'.format(i), system=code_systems[i], settlement=sett)
                 for i, sett in enumerate(settlements)]

        precache = SettlementCodePrecache([system.code for system in code_systems])

        assert precache.model == SettlementCode
        for i, st in enumerate(settlements):
            assert precache.get_if_not_precached(st) == {code_systems[i].code: codes[i]}


class TestSynonymsPrecache(TestCase):
    models = [Station, Settlement]

    def gen_point_syns(self, objs):
        content_models = [ContentType.objects.get_for_model(obj.__class__) for obj in objs]
        args = [{'title': u'syn_ {}'.format(i), 'content_type': content_models[i], 'object_id': obj.id}
                  for i, obj in enumerate(objs)]
        return [PointSynonym.objects.create(**arg) for arg in args]

    def test_init(self):
        precache = SynonymsPrecache(self.models)

        assert precache.models == self.models
        assert precache.is_precached is False
        assert precache.cache == defaultdict(lambda: defaultdict(list))

    def test_precache(self):
        objs = [create_station(), create_settlement()]
        point_syns = self.gen_point_syns(objs)

        df = defaultdict(lambda: defaultdict(list))
        for i, model in enumerate(self.models):
            df[model][objs[i].id] = [point_syns[i]]

        with mock.patch.object(Precache, 'precache') as m_precache:
            precache = SynonymsPrecache(self.models)
            precache.precache()
            m_precache.assert_called_once_with()
            assert precache.cache == df

    def test_get_if_precached(self):
        precache = SynonymsPrecache(self.models)
        station1 = create_station()
        station2 = create_station()
        precache.cache[Station][station1.id] = ['syn1']
        assert precache.get_if_precached(station2) == []
        assert precache.get_if_precached(station1) == ['syn1']

    def test_get_if_not_precached(self):
        precache = SynonymsPrecache(self.models)
        objs = [create_station(), create_station(), create_settlement()]
        point_syns = self.gen_point_syns(objs)

        for i, obj in enumerate(objs):
            assert precache.get_if_not_precached(obj) == [point_syns[i]]
