from __future__ import absolute_import

from datetime import datetime
from mock import Mock
from typing import cast

from travel.avia.library.python.avia_data.models import AviaDirectionNational
from travel.avia.library.python.common.utils.date import MSK_TZ
from travel.avia.backend.repository.settlement import SettlementRepository, SettlementGeoIndex
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.tester.factories import create_settlement, get_model_factory
from travel.avia.library.python.tester.testcase import TestCase


create_avia_direction_national = get_model_factory(AviaDirectionNational)


class SettlementGeoIndexTest(TestCase):
    def setUp(self):
        self._environment = Mock()
        self._environment.now_aware = Mock(
            return_value=MSK_TZ.localize(datetime(2017, 9, 1))
        )
        self.settlement_repo = SettlementRepository(
            translated_title_repository=cast(TranslatedTitleRepository, Mock()),
            environment=self._environment
        )
        self.geo_index = SettlementGeoIndex(self.settlement_repo)

    def test_main_scenario(self):
        settlement = create_settlement(
            id=1,
            type_choices='plane',
            latitude=45.0,
            longitude=45.0,
        )
        near_settlement = create_settlement(
            id=2,
            type_choices='plane',
            latitude=45.1,
            longitude=45.1,
        )
        far_settlement = create_settlement(
            id=3,
            type_choices='plane',
            latitude=89.,
            longitude=89.,
        )
        create_avia_direction_national(
            departure_settlement=settlement,
            arrival_settlement=near_settlement,
            national_version='ru'
        )
        create_avia_direction_national(
            departure_settlement=settlement,
            arrival_settlement=far_settlement,
            national_version='ru'
        )
        self.settlement_repo.pre_cache()
        self.geo_index.pre_cache()
        actual_nearest = self.geo_index.get_nearest(settlement.id, 100)
        assert len(actual_nearest) == 1
        assert actual_nearest[0].id == near_settlement.id

    def test_main_scenario_including_current_settlement(self):
        settlement = create_settlement(
            id=1,
            type_choices='plane',
            latitude=45.0,
            longitude=45.0,
        )
        near_settlement = create_settlement(
            id=2,
            type_choices='plane',
            latitude=45.1,
            longitude=45.1,
        )
        far_settlement = create_settlement(
            id=3,
            type_choices='plane',
            latitude=89.,
            longitude=89.,
        )
        create_avia_direction_national(
            departure_settlement=settlement,
            arrival_settlement=near_settlement,
            national_version='ru'
        )
        create_avia_direction_national(
            departure_settlement=near_settlement,
            arrival_settlement=settlement,
            national_version='ru'
        )
        create_avia_direction_national(
            departure_settlement=settlement,
            arrival_settlement=far_settlement,
            national_version='ru'
        )
        self.settlement_repo.pre_cache()
        self.geo_index.pre_cache()
        actual_nearest = self.geo_index.get_nearest(settlement.id, 100, include_itself=True)
        assert len(actual_nearest) == 2
        assert actual_nearest[0].id == settlement.id
        assert actual_nearest[1].id == near_settlement.id

    def test_get_nonexistent_settlement(self):
        actual = self.geo_index.get_nearest(666, 100)
        assert len(actual) == 0
