# coding: utf-8

from datetime import datetime

import pytest

from common.utils.date import MSK_TZ
from travel.rasp.morda_backend.morda_backend.tariffs.static.partners import PARTNER_MODULES


@pytest.mark.parametrize('partner_code, now, order_data, expected', (
    ('allticketsfor-me', datetime(1999, 1, 1), {}, False),  # слишком рано
    ('allticketsfor-me', datetime(2000, 1, 1), {}, False),  # слишком поздно
    ('allticketsfor-me', datetime(1999, 12, 1), {}, True),
    ('avperm', datetime(2000, 1, 1, 10), {}, False),               # нет day_sale
    ('avperm', datetime(1999, 12, 30), {'day_sale': 1}, False),    # слишком рано
    ('avperm', datetime(2000, 1, 1, 11), {'day_sale': 1}, False),  # слишком поздно
    ('avperm', datetime(2000, 1, 1, 9), {'day_sale': 1}, True),
    ('Donavto', datetime(1999, 1, 1), {}, False),     # слишком рано
    ('Donavto', datetime(2000, 1, 1, 8), {}, False),  # слишком поздно
    ('Donavto', datetime(2000, 1, 1, 7), {}, True),
    ('e-traffic', datetime(2000, 1, 1), {}, False),  # нет etraffic_races
    ('e-traffic', datetime(2000, 1, 1), {'etraffic_races': {}}, False),  # нет рейса
    ('e-traffic', datetime(2000, 1, 1), {'etraffic_races': {'2000-01-01 12:00:00': False}}, False),  # нет продажи
    ('e-traffic', datetime(2000, 1, 1), {'etraffic_races': {'2000-01-01 12:00:00': True}}, True),
    ('infobus', datetime(1999, 1, 1), {}, False),  # слишком рано
    ('infobus', datetime(2000, 1, 1), {}, False),  # слишком поздно
    ('infobus', datetime(1999, 12, 30), {}, True),
    ('kvc_tula', datetime(1999, 1, 1), {}, False),  # слишком рано
    ('kvc_tula', datetime(2000, 1, 1), {}, False),  # слишком поздно
    ('kvc_tula', datetime(1999, 12, 30), {}, True),
    ('mrtrans', datetime(1999, 1, 1), {}, False),      # слишком рано
    ('mrtrans', datetime(2000, 1, 1, 11), {}, False),  # слишком поздно
    ('mrtrans', datetime(2000, 1, 1, 8), {}, True),
    ('NeoplanTransTur', datetime(1999, 1, 1), {}, False),      # слишком рано
    ('NeoplanTransTur', datetime(2000, 1, 1, 11), {}, False),  # слишком поздно
    ('NeoplanTransTur', datetime(2000, 1, 1, 8), {}, True),
    ('Pavlodar-AV', datetime(1999, 1, 1), {}, False),          # слишком рано
    ('Pavlodar-AV', datetime(2000, 1, 1, 11, 50), {}, False),  # слишком поздно
    ('Pavlodar-AV', datetime(2000, 1, 1, 11), {}, True),
    ('takebus', datetime(1999, 1, 1), {}, False),          # слишком рано
    ('takebus', datetime(2000, 1, 1, 11, 30), {}, False),  # слишком поздно
    ('takebus', datetime(2000, 1, 1, 10, 30), {}, True),
    ('udmbus', datetime(2000, 1, 1), {}, False),  # пустая order_data
    ('udmbus', datetime(2000, 1, 1), {'station_from_udm_code': None, 'station_to_udm_code': None}, True),
    ('unistation-common-xml', datetime(1999, 1, 1), {'group_code': 'chelyabinsk'}, False),  # Рано для Челябинска
    ('unistation-common-xml', datetime(1999, 1, 1), {'group_code': 'kursk'}, True),         # но для Курска не рано
    ('unistation-common-xml', datetime(2000, 1, 1, 11, 30), {'group_code': 'chelyabinsk'}, False),  # слишком поздно
    ('unistation-common-xml', datetime(2000, 1, 1, 10, 30), {'group_code': 'chelyabinsk'}, True),
    ('unitiki', datetime(1999, 1, 1), {}, False),          # слишком рано
    ('unitiki', datetime(2000, 1, 1, 11, 56), {}, False),  # слишком поздно
    ('unitiki', datetime(2000, 1, 1, 11, 50), {}, True),
    ('utisauto', datetime(1999, 1, 1), {}, False),        # слишком рано
    ('utisauto', datetime(2000, 1, 1, 9, 5), {}, False),  # слишком поздно
    ('utisauto', datetime(2000, 1, 1, 8), {}, True),
))
def test_can_buy_from(partner_code, now, order_data, expected):
    module = PARTNER_MODULES[partner_code]
    now_aware = MSK_TZ.localize(now)
    departure = MSK_TZ.localize(datetime(2000, 1, 1, 12))
    assert bool(module.can_buy_from(departure, now_aware, order_data)) == expected
