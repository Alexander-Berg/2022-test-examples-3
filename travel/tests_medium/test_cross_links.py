from unittest.case import TestCase

from travel.hotels.tools.region_pages_builder.data_miner.data_miner import cross_links


LINK_COUNT_LIMIT = 12


class TestCrossLinks(TestCase):
    def test_calculate_incoming_link_count(self):
        cities = []
        for i in range(100):
            cities.append(i)

        def popularity(geo_id: int) -> float:
            return float(geo_id)

        fixed_link_count = {0: 10, 1: 20, 2: 30}

        generator = cross_links.CrossLinkGenerator(
            link_count_limit=LINK_COUNT_LIMIT,
            custom_incoming_link_count=fixed_link_count,
            popularity_function=popularity,
        )

        result = generator._calculate_incoming_link_count(cities, fixed_link_count, popularity)

        self.assertEqual(10, result[0])
        self.assertEqual(20, result[1])
        self.assertEqual(30, result[2])
        self.assertEqual(22, result[99])

    def test_generate_cross_links(self):
        city_count = 100
        cities = []
        for i in range(city_count):
            cities.append(i)

        custom_incoming_link_count = {0: 10, 1: 20, 2: 30}
        fixed_links = {(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7), (7, 8)}

        def distance(c1: int, c2: int):
            return abs(c1 - c2)

        result = cross_links.CrossLinkGenerator(
            cost_function=distance,
            popularity_function=lambda x: x,
            link_count_limit=LINK_COUNT_LIMIT,
            fixed_links=fixed_links,
            custom_incoming_link_count=custom_incoming_link_count
        ).generate_links(cities)

        actual_link_count = {}
        for geo_id, links in result.items():
            for to_city in links:
                if to_city not in actual_link_count:
                    actual_link_count[to_city] = 0
                actual_link_count[to_city] += 1

        for geo_id, count in custom_incoming_link_count.items():
            self.assertEqual(count, actual_link_count[geo_id])

        for from_city, to_city in fixed_links:
            self.assertTrue(cities[to_city] in result[from_city])

        for from_city, to_city_list in result.items():
            self.assertTrue(cities[from_city] not in to_city_list)
