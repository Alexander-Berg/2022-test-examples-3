# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, contains_inanyorder

from common.models.geo import StationMajority
from common.models.transport import TransportType
from common.tester.factories import create_station, create_settlement, create_country
from travel.rasp.train_api.tariffs.train.base.utils import get_point_express_code, get_possible_numbers, fix_train_number


@pytest.mark.parametrize('number, variants', [
    ('001Ц', ['001Ц', '002Ц']),
    ('ФФ001Ц', ['ФФ001Ц']),
    ('50Ф', ['050Ф', '049Ф']),
    ('', ['']),
    ('100', ['099', '100']),
    ('strange', ['strange'])
])
def test_get_possible_numbers(number, variants):
    assert_that(get_possible_numbers(number), contains_inanyorder(*variants))


@pytest.mark.parametrize('raw_number, expected_number', [
    ('001Ц', '001Ц'),
    ('50Ф', '050Ф'),
    ('00050Ф', '050Ф'),
    ('50*Ф', '050Ф'),
    ('', ''),
    ('100', '100'),
    ('strange', 'strange')
])
def test_fix_train_number(raw_number, expected_number):
    assert fix_train_number(raw_number) == expected_number


@pytest.mark.dbuser
class TestGetPointExpressCode(object):
    def test_by_station(self):
        station = create_station(__={'codes': {'express': '1000'}})
        assert get_point_express_code(station) == '1000'

        station_no_code = create_station()
        assert get_point_express_code(station_no_code) is None

    def test_by_country(self):
        assert get_point_express_code(create_country()) is None

    def test_by_settlement_has_main(self):
        settlement = create_settlement()
        create_station(settlement=settlement, t_type=TransportType.TRAIN_ID,
                       __={'codes': {'express': '1000'}}, majority=StationMajority.MAIN_IN_CITY_ID)
        create_station(settlement=settlement, t_type=TransportType.TRAIN_ID,
                       __={'codes': {'express': '2000'}}, majority=StationMajority.NOT_IN_TABLO_ID)
        create_station(settlement=settlement, t_type=TransportType.TRAIN_ID,
                       majority=StationMajority.MAIN_IN_CITY_ID)
        assert get_point_express_code(settlement) == '1000'

    def test_by_settlement_has_express_fake(self):
        settlement = create_settlement()
        create_station(settlement=settlement, t_type=TransportType.TRAIN_ID,
                       __={'codes': {'express': '1000'}}, majority=StationMajority.MAIN_IN_CITY_ID)
        create_station(settlement=settlement, t_type=TransportType.TRAIN_ID,
                       __={'codes': {'express': 'fake_1000'}}, majority=StationMajority.EXPRESS_FAKE_ID)
        assert get_point_express_code(settlement) == 'fake_1000'

    def test_by_settlement_has_no(self):
        assert get_point_express_code(create_settlement()) is None
