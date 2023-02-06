import logging
import json

from library.python import resource

from travel.hotels.feeders.partners.bronevik.lib.bronevik import BronevikRowMapper

from travel.hotels.feeders.partners.bronevik.lib import rubrics_mapping, hotel_type_mapping

LOG = logging.getLogger(__name__)

hotel_info_items = json.loads(resource.find('/bronevik-hotel-info.json'))
rubric_map = rubrics_mapping.rubric_map
hotel_type_map = hotel_type_mapping.hotel_type_map


def test_mapping():
    hotel = BronevikRowMapper()\
        .bronevik_map(hotel_info_items[0], {'lang': 'ru'}, rubric_map, hotel_type_map)\
        .to_dict(partner_name='test')
    LOG.info(hotel.keys())
    assert hotel['country'] == 'RU'
    assert hotel['originalId'] == '7'
    assert hotel['address'] == [{'one_line': 'Россия, Новосибирск, ул. Дениса Давыдова, 1/3'}]
    assert hotel['lon'] == 82.9619249
    assert hotel['lat'] == 55.070011
    assert hotel['zipIndex'] == '630084'
    assert hotel['rubric'] == [{'value': '184106414'}]
    assert hotel['name'] == [{'value': '55 Широта'}]
    assert hotel['country'] == 'RU'
    LOG.info(hotel["feature"])
