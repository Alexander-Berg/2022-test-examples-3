# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from mock import call, Mock

from common.tester.factories import create_region, create_settlement
from common.tester.testcase import TestCase
from common.utils.geobase import geobase
from travel.rasp.wizards.wizard_lib.geobase_region_type import GeobaseRegionType
from travel.rasp.wizards.wizard_lib.region_capital_provider import RegionCapitalProvider


class TestRegionCapitalProvider(TestCase):
    def setUp(self):
        self._fake_geobase = Mock()
        self._fake_logger = Mock()

        self._provider = RegionCapitalProvider(
            geobase=self._fake_geobase,
            logger=self._fake_logger,
        )

    def test_geo_id_is_none(self):
        self._provider.build_cache()
        result = self._provider.find_region_capital(
            geo_id=None,
        )

        assert result is None
        self._fake_logger.debug.assert_called_with("Can not find the region capital, because geo id is None")
        self._fake_geobase.region_by_id.call_count = 0

    def test_unknown_geo_id(self):
        self._provider.build_cache()
        result = self._provider.find_region_capital(
            geo_id=213,)
        assert result is None
        self._fake_logger.debug.assert_called_with(
            "Can not find the region capital, because geobase does not know about it. geo_id: %d", 213
        )
        self._fake_geobase.region_by_id.assert_has_calls([call(213)])

    def test_unsuported_geo_object_type(self):
        forbiden_types = [
            GeobaseRegionType.DELETED,
            GeobaseRegionType.OTHER,
            GeobaseRegionType.CONTINENT,
            GeobaseRegionType.SUBCONTINENT,
            GeobaseRegionType.COUNTRY,
            GeobaseRegionType.FEDERAL_DISTRICT,
            GeobaseRegionType.REGION,
            # GeobaseRegionType.CITY,
            # GeobaseRegionType.VILLAGE,
            GeobaseRegionType.CITY_DISTRICT,
            GeobaseRegionType.METRO_STATION,
            GeobaseRegionType.DISTRICT,
            GeobaseRegionType.AIRPORT,
            GeobaseRegionType.FOREIGN_TERRITORY,
            GeobaseRegionType.SECOND_DISTRICT,
            GeobaseRegionType.MONORAIL_STATION,
            GeobaseRegionType.RURAL_SETTLEMENT,
            GeobaseRegionType.UNIVERSAL,
        ]
        for forbiden_type in forbiden_types:
            self.setUp()
            fake_moscow = Mock()
            fake_moscow.type = forbiden_type
            fake_moscow.id = 213
            self._fake_geobase.region_by_id = Mock(return_value=fake_moscow)

            self._provider.build_cache()
            result = self._provider.find_region_capital(
                geo_id=213,
            )

            assert result is None
            self._fake_logger.debug.assert_called_with(
                'Can not find the region capital, because geo object has wrong type. geo_id: %d geo_object_type: %s',
                213, forbiden_type
            )
            self._fake_geobase.region_by_id.call_count = 1
            self._fake_geobase.region_by_id.assert_has_calls([call(213)])

    def test_can_not_find_parent_because_parent_is_not_region(self):
        forbiden_parent_types = [
            GeobaseRegionType.DELETED,
            GeobaseRegionType.OTHER,
            GeobaseRegionType.CONTINENT,
            GeobaseRegionType.SUBCONTINENT,
            GeobaseRegionType.COUNTRY,
            GeobaseRegionType.FEDERAL_DISTRICT,
            # GeobaseRegionType.REGION,
            # GeobaseRegionType.CITY,
            GeobaseRegionType.VILLAGE,
            GeobaseRegionType.CITY_DISTRICT,
            GeobaseRegionType.METRO_STATION,
            GeobaseRegionType.DISTRICT,
            GeobaseRegionType.AIRPORT,
            GeobaseRegionType.FOREIGN_TERRITORY,
            GeobaseRegionType.SECOND_DISTRICT,
            GeobaseRegionType.MONORAIL_STATION,
            GeobaseRegionType.RURAL_SETTLEMENT,
            GeobaseRegionType.UNIVERSAL,
        ]
        for forbiden_type in forbiden_parent_types:
            self.setUp()
            fake_moscow_grandfather = Mock()
            fake_moscow_grandfather.type = forbiden_type
            fake_moscow_grandfather.id = 1
            fake_moscow_grandfather.parent_id = 0

            fake_moscow_father = Mock()
            fake_moscow_father.type = forbiden_type
            fake_moscow_father.id = 2
            fake_moscow_father.parent_id = fake_moscow_grandfather.id

            fake_moscow = Mock()
            fake_moscow.type = GeobaseRegionType.VILLAGE
            fake_moscow.id = 3
            fake_moscow.parent_id = fake_moscow_father.id

            self._fake_geobase.region_by_id = Mock(
                side_effect=[
                    fake_moscow,
                    fake_moscow_father,
                    fake_moscow_grandfather,
                    fake_moscow_father,
                    fake_moscow_grandfather,
                ]
            )

            self._provider.build_cache()
            result = self._provider.find_region_capital(
                geo_id=fake_moscow.id,
            )

            assert result is None
            self._fake_logger.debug.assert_called_with(
                'Can not find the region capital, because geobase does not geo object region. geo_id: %d', 3
            )
            self._fake_geobase.region_by_id.assert_has_calls([call(3), call(2), call(1), call(2), call(1)])

    def test_can_not_find_relative_between_region_and_settlement_in_base(self):
        fake_moscow_grandfather = Mock()
        fake_moscow_grandfather.type = GeobaseRegionType.REGION
        fake_moscow_grandfather.id = 1
        fake_moscow_grandfather.parent_id = 0

        fake_moscow_father = Mock()
        fake_moscow_father.type = GeobaseRegionType.DISTRICT
        fake_moscow_father.id = 2
        fake_moscow_father.parent_id = fake_moscow_grandfather.id

        fake_moscow = Mock()
        fake_moscow.type = GeobaseRegionType.VILLAGE
        fake_moscow.id = 3
        fake_moscow.parent_id = fake_moscow_father.id

        self._fake_geobase.region_by_id = Mock(
            side_effect=[
                fake_moscow,
                fake_moscow_father,
                fake_moscow_grandfather,
                fake_moscow_father,
                fake_moscow_grandfather,
            ]
        )

        self._provider.build_cache()
        result = self._provider.find_region_capital(
            geo_id=fake_moscow.id,
        )

        assert result is None
        self._fake_logger.debug.assert_called_with(
            'Can not find the region capital, because rasp base does not know it or'
            'settlement is hidden. geo_id: %d region_id: %d',
            3, 1
        )
        self._fake_geobase.region_by_id.assert_has_calls([call(3), call(2), call(1)])

    def test_can_find_region_capital(self):
        msk_region = create_region(id=1)
        msk = create_settlement(
            id=10,
            region_id=msk_region.id,
            hidden=False
        )

        fake_moscow_grandfather = Mock()
        fake_moscow_grandfather.type = GeobaseRegionType.REGION
        fake_moscow_grandfather.id = 1
        fake_moscow_grandfather.parent_id = 0

        fake_moscow_father = Mock()
        fake_moscow_father.type = GeobaseRegionType.DISTRICT
        fake_moscow_father.id = 2
        fake_moscow_father.parent_id = fake_moscow_grandfather.id

        fake_moscow = Mock()
        fake_moscow.type = GeobaseRegionType.VILLAGE
        fake_moscow.id = 3
        fake_moscow.parent_id = fake_moscow_father.id

        self._fake_geobase.region_by_id = Mock(
            side_effect=[
                fake_moscow,
                fake_moscow_father,
                fake_moscow_grandfather,
                fake_moscow_father,
                fake_moscow_grandfather,
            ]
        )

        self._provider.build_cache()
        result = self._provider.find_region_capital(
            geo_id=fake_moscow.id,
        )

        assert result == msk
        assert self._fake_logger.debug.call_count == 0
        self._fake_geobase.region_by_id.assert_has_calls([call(3), call(2), call(1)])

    def test_can_find_fake_region_capital(self):
        fake_moscow_grandfather = Mock()
        fake_moscow_grandfather.type = GeobaseRegionType.REGION
        fake_moscow_grandfather.id = 1
        fake_moscow_grandfather.parent_id = 0

        fake_region_capital = Mock()
        fake_region_capital.type = GeobaseRegionType.CITY
        fake_region_capital.id = 2
        fake_region_capital.parent_id = fake_moscow_grandfather.id

        fake_moscow_father = Mock()
        fake_moscow_father.type = GeobaseRegionType.DISTRICT
        fake_moscow_father.id = 3
        fake_moscow_father.parent_id = fake_region_capital.id

        fake_moscow = Mock()
        fake_moscow.type = GeobaseRegionType.VILLAGE
        fake_moscow.id = 4
        fake_moscow.parent_id = fake_moscow_father.id

        msk = create_settlement(
            id=10,
            _geo_id=fake_region_capital.id,
            hidden=False
        )

        self._fake_geobase.region_by_id = Mock(
            side_effect=[
                fake_moscow,
                fake_moscow_father,
                fake_region_capital,
            ]
        )

        self._provider.build_cache()
        result = self._provider.find_region_capital(
            geo_id=fake_moscow.id,
        )

        assert result == msk
        assert self._fake_logger.debug.call_count == 0
        self._fake_geobase.region_by_id.assert_has_calls([call(4), call(3), call(2)])


class TestIntegrationRegionCapitalProvider(TestCase):
    def setUp(self):
        self._fake_logger = Mock()

        self._provider = RegionCapitalProvider(
            geobase=geobase,
            logger=self._fake_logger,
        )

    def test_real_region_capital(self):
        krasnodar_krai = create_region(id=10995)  # Краснодарский край
        krasnodar = create_settlement(
            id=10,
            region_id=krasnodar_krai.id,
            hidden=False
        )  # Сочи

        self._provider.build_cache()
        settlement = self._provider.find_region_capital(
            geo_id=10994,  # Красная поляна
        )

        assert settlement == krasnodar
        assert self._fake_logger.debug.call_count == 0

    def test_real_fake_region_capital(self):
        sochi = create_settlement(
            id=10,
            _geo_id=239,
            hidden=False
        )  # Сочи

        self._provider.build_cache()
        settlement = self._provider.find_region_capital(
            geo_id=20777,  # Адлер
        )

        assert settlement == sochi
        assert self._fake_logger.debug.call_count == 0
