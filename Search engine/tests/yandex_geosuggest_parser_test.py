# -*- encoding: utf-8 -*-

from test_utils import TestParser


def isclose(a, b, rel_tol):
    return abs(a - b) <= rel_tol * max(abs(a), abs(b))


class TestYandexGeoSuggestParser(TestParser):

    def test_parse(self):
        parsed_data = self.parse_file("geosuggest_response.json")
        assert parsed_data

        EXPECTED_COUNT = 4
        components = parsed_data.get("components", [])
        assert len(components) == EXPECTED_COUNT

        EXPECTED_TITLES = [
            "Lva Tolstogo Street, 16",
            "Lva Tolstogo Street, 5/1",
            "ulitsa Lva Tolstogo",
            "State Kremlin Palace",
        ]

        EXPECTED_SNIPPETS = [
            "Russia, Moscow, Lva Tolstogo Street, 16",
            "Russia, Moscow, Lva Tolstogo Street, 5/1",
            "Russia, Moscow Region, Lyubertsy, ulitsa Lva Tolstogo",
            "State Kremlin Palace",
        ]

        for index, component in enumerate(components):
            assert component["title"] == EXPECTED_TITLES[index]
            assert component["snippet"] == EXPECTED_SNIPPETS[index]

            assert component["raw-data"], "Raw data is empty"
            assert component["rubrics"] == ""
            assert component["type"] == "SEARCH_RESULT"
            assert component["alignment"] == "LEFT"
            assert component["media-links"] == []
            assert component["site-links"] == []
            assert not component["json.geoSelectedGta"]["is_from_osm"]

        EXPECTED_POSITIONS = [
            [37.588150, 55.733849],
            [37.588764, 55.732155],
            [37.896286, 55.701599],
            [37.924000, 55.697500],
        ]
        for index, component in enumerate(components):
            assert isinstance(component["longitude"], float)
            assert isinstance(component["latitude"], float)
            assert isclose(component["longitude"], EXPECTED_POSITIONS[index][0], rel_tol=0.0000001)
            assert isclose(component["latitude"], EXPECTED_POSITIONS[index][1], rel_tol=0.0000001)

            if component.get("map-position"):
                assert isinstance(component["map-position"]["bottomLeft"]["longitude"], float)
                assert isinstance(component["map-position"]["bottomLeft"]["latitude"], float)
                assert isinstance(component["map-position"]["topRight"]["longitude"], float)
                assert isinstance(component["map-position"]["topRight"]["latitude"], float)

        EXPECTED_OIDS = [
            None,
            None,
            None,
            "1037077702",
        ]
        for index, component in enumerate(components):
            if EXPECTED_OIDS[index]:
                assert component["organization-id"] == EXPECTED_OIDS[index]

        EXPECTED_PAGE_URLS = [
            "http://maps.yandex.ru/?ll=37.588150%2C55.733849&spn=0.001500%2C0.001000",
            "http://maps.yandex.ru/?ll=37.588764%2C55.732155&spn=0.001600%2C0.001100",
            "http://maps.yandex.ru/?ll=37.896286%2C55.701599&spn=0.006447%2C0.007111",
            "http://maps.yandex.ru/?ol=biz&oid=1037077702",
        ]
        for index, component in enumerate(components):
            assert component["page-url"] == EXPECTED_PAGE_URLS[index]

    def test_parse_rubrics(self):
        parsed_data = self.parse_file("geosuggest_response_with_rubrics.json")

        components = parsed_data.get("components", [])

        expected = [
            ("Яндекс", "IT-компания"),
            ("Яндекс.Еда", "Кадровое агентство"),
            ("Яндекс", "IT-компания"),
            ("Центр для водителей Яндекс.Такси", "Центр Яндекс.Такси"),
            ("Яндекс", "Бизнес-школа"),
            ("Яндекс", "IT-компания"),
            ("Яндекс", "IT-компания")
        ]

        assert len(components) == len(expected)

        for i, component in enumerate(components):
            assert component["title"] == expected[i][0]
            assert component["rubrics"] == expected[i][1]

    def test_parse_osm(self):
        parsed_data = self.parse_file("geosuggest_response_with_osm.json")

        components = parsed_data.get("components", [])

        expected = [
            ("Bonanjo", "", "https://www.openstreetmap.org/#map=14/4.043024/9.686496"),
            ("Cca Bank Bonanjo", "Bank", "https://www.openstreetmap.org/#map=19/4.044548/9.686957"),
            ("Tourne dos Bonanjo", "Restaurant", "https://www.openstreetmap.org/#map=19/4.040931/9.686182"),
            ("Polyclinique Bonanjo", "Clinique", "https://www.openstreetmap.org/#map=19/4.039066/9.687569"),
            ("Banque Atlantique agence de Bonanjo", "Banque", "https://www.openstreetmap.org/#map=19/4.043206/9.689376"),
            ("La Falaise Bonanjo", "Hôtel", "https://www.openstreetmap.org/#map=19/4.046445/9.687948"),
            ("Bonanjo", "Pub, bar", "https://www.openstreetmap.org/#map=19/3.847282/11.486223")
        ]

        assert len(components) == len(expected)

        for i, component in enumerate(components):
            ex = expected[i]
            assert component["title"] == ex[0]
            assert component["rubrics"] == ex[1]
            assert component["page-url"] == ex[2]
            assert component["json.geoSelectedGta"]["is_from_osm"]
