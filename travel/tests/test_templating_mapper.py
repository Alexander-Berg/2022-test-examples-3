import json
from typing import Optional
from unittest.case import TestCase

from yaml import load as load_yaml, Loader

from library.python import resource

from travel.hotels.proto.region_pages.region_pages_pb2 import EPageType
from travel.hotels.tools.region_pages_builder.common.tanker_data import ConfigRegionFilter, TankerDataStorage
from travel.hotels.tools.region_pages_builder.renderer.renderer.templater import Hotel, Region, RegionData, Station, Templater
from travel.hotels.tools.region_pages_builder.renderer.renderer.templating_mapper import TemplatingMapper

from google.protobuf.json_format import MessageToJson, MessageToDict


class TestTemplatingMapper(TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        raw_tanker_dict = load_yaml(resource.find('tanker-dict.yaml').decode('utf-8'), Loader=Loader)
        tanker_data = TankerDataStorage(raw_tanker_dict)

        raw_tanker_config = load_yaml(resource.find('tanker-config.yaml').decode('utf-8'), Loader=Loader)
        config = TankerDataStorage(raw_tanker_config)

        cls.templating_mapper = TemplatingMapper(Templater(), tanker_data, config, {})

    @staticmethod
    def get_city_data(filter_config: Optional[ConfigRegionFilter] = None) -> RegionData:
        city = Region(json.loads(resource.find('city-row.json')), None, 'ru', None)
        hotel = Hotel(json.loads(resource.find('hotel-row.json')))
        station = Station(json.loads(resource.find('station-row.json')))
        return RegionData(
            region=city,
            all_hotels={hotel.permalink: hotel},
            stations={station.id: station},
            links=[city, city],
            filter_config=filter_config,
        )

    def test_render_page_to_proto(self):
        proto_page = self.templating_mapper.render_page(self.get_city_data(), EPageType.PT_City)
        rendered_page = MessageToJson(proto_page) + "\n"

        self.assertEqual(
            resource.find("moscow-rendered.json").decode("utf-8"),
            rendered_page,
        )

    def test_render_with_filter(self):
        filter_config = ConfigRegionFilter(
            name='test_name',
            filterSlug='test_slug',
            category='test_category',
            yqlCondition='test_yql_condition',
            regions=list(),
            geoSearchFilters=['test_filter_1'],
        )
        city_data = self.get_city_data(filter_config)
        proto_page = self.templating_mapper.render_page(city_data, EPageType.PT_City)
        rendered_page = MessageToDict(proto_page)

        block_1 = rendered_page['Content'][1]['HotelListBlock']['GeoSearchRequestData']
        self.assertEqual(['test_filter_1'], block_1['Filters'])

        block_2 = rendered_page['Content'][2]['HotelListBlock']['GeoSearchRequestData']
        expected = ['hotel_city_center_dist_category:hotel_city_center_dist_category_less_3', 'test_filter_1']
        self.assertEqual(expected, block_2['Filters'])

        block_3 = rendered_page['Content'][3]['HotelListBlock']['GeoSearchRequestData']
        expected = ['hotel_pansion_with_offerdata:hotel_pansion_breakfast_included', 'test_filter_1']
        self.assertEqual(expected, block_3['Filters'])
