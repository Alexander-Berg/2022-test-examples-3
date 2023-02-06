import unittest
from search.geo.tools.task_manager.generators.common.offline_geocoder import HouseFinder


class HouseFinderTests(unittest.TestCase):
    def test_point_in_polygon(self):
        square_wkt = 'POLYGON ((0 1, 1 1, 1 0, 0 0, 0 1))'
        expected_geoid = 1
        house_description = {'address': 'test addr', 'geoid': expected_geoid, 'lat': 0.5, 'lon': 0.5, 'name': '1', 'wkt': square_wkt}
        finder = HouseFinder([house_description])

        lon_from_square = 0.75
        lat_from_square = 0.75
        house = finder.find(lon_from_square, lat_from_square)

        self.assertEqual(house.geoid, expected_geoid)

    def test_point_outside_polygon(self):
        square_wkt = 'POLYGON ((0 1, 1 1, 1 0, 0 0, 0 1))'
        house_description = {'address': 'test addr', 'geoid': 1, 'lat': 0.5, 'lon': 0.5, 'name': '1', 'wkt': square_wkt}
        finder = HouseFinder([house_description])

        lon_outside_square = 1.75
        lat_outside_square = 1.75
        house = finder.find(lon_outside_square, lat_outside_square)

        self.assertFalse(house)

    def test_find_by_wkt(self):
        first_square_wkt = 'POLYGON ((0 1, 1 1, 1 0, 0 0, 0 1))'
        second_square_wkt = 'POLYGON ((0 3, 1 3, 1 2, 0 2, 0 3))'
        wrong_house_with_close_lon_lat = {'address': 'test addr', 'geoid': 1, 'lat': 2.6, 'lon': 0.6, 'name': '1', 'wkt': first_square_wkt}
        expected_geoid = 2
        expected_house_with_bad_lon_lat = {'address': 'test addr', 'geoid': expected_geoid, 'lat': 3.0, 'lon': 0.5, 'name': '1', 'wkt': second_square_wkt}
        finder = HouseFinder([wrong_house_with_close_lon_lat, expected_house_with_bad_lon_lat])

        lon = 0.5
        lat = 2.5
        house = finder.find(lon, lat)

        self.assertEqual(house.geoid, expected_geoid)

    def test_max_neighbors(self):
        first_square_wkt = 'POLYGON ((0 1, 1 1, 1 0, 0 0, 0 1))'
        second_square_wkt = 'POLYGON ((0 3, 1 3, 1 2, 0 2, 0 3))'
        third_square_wkt = 'POLYGON ((2 1, 2 2, 1 2, 1 1, 2 1))'
        first_house = {'address': 'test addr', 'geoid': 1, 'lat': 1.0, 'lon': 1.0, 'name': '1', 'wkt': first_square_wkt}
        second_house = {'address': 'test addr', 'geoid': 2, 'lat': 2.0, 'lon': 0.5, 'name': '1', 'wkt': second_square_wkt}
        third_house = {'address': 'test addr', 'geoid': 3, 'lat': 0.5, 'lon': 0.5, 'name': '1', 'wkt': third_square_wkt}

        finder = HouseFinder([first_house, second_house, third_house])

        lon = 1.5
        lat = 1.5
        max_neighbors = 2
        house = finder.find(lon, lat, max_neighbors)

        self.assertFalse(house)

    def test_empty_wkt(self):
        first_square_wkt = None
        first_house = {'address': 'test addr', 'geoid': 1, 'lat': 1.0, 'lon': 1.0, 'name': '1', 'wkt': first_square_wkt}

        finder = HouseFinder([first_house])

        lon = 1.2
        lat = 1.2
        house = finder.find(lon, lat)

        self.assertFalse(house)
