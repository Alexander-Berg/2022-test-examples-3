# -*- coding: utf-8 -*-
from common.models.transport import TransportType
from travel.rasp.admin.scripts.export.export_suburban_cities import get_settlements

from tester.testcase import TestCase
from tester.factories import create_station, create_thread, create_settlement, create_suburban_zone


class TestGetSettlements(TestCase):
    def test_settlements_sort(self):
        zone = create_suburban_zone(settlement=create_settlement())

        settlement1 = create_settlement(suburban_zone=zone, title=u'Москва')
        settlement2 = create_settlement(suburban_zone=zone, title=u'Екатеринбург', title_en=u'Yekaterinburg')
        settlement3 = create_settlement(suburban_zone=zone, title=u'Санкт-Петербург')
        settlement4 = create_settlement(suburban_zone=zone, title=u'Новосибирск')
        settlement5 = create_settlement(suburban_zone=zone, title=u'Екатеринбург', title_en=u'Ekaterinburg')

        create_thread(
            t_type=TransportType.SUBURBAN_ID,
            schedule_v1=[
                [None, 0, create_station(settlement=settlement1)],
                [10, 11, create_station(settlement=settlement2)],
                [12, 13, create_station(settlement=settlement3)],
                [13, 14, create_station(settlement=settlement4)],
                [15, None, create_station(settlement=settlement5)],
            ]
        )

        expected = [settlement5, settlement2, settlement1, settlement4, settlement3]
        assert get_settlements() == expected
