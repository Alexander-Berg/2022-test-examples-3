from test_utils import TestParser


class TestYandexUslugiSaas(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")
        component = example["components"][0]
        url = "https://yandex.ru/uslugi/search?worker_id=4b192452-1be1-44e5-b4ef-662a9d9a1902"
        cnt = 759
        assert url == component["componentUrl"]["pageUrl"]
        assert cnt == example["long.docsFound"]

    def test_direct(self):
        components = self.read_components("test_direct.json")
        assert components[0]["tags.isFromDirect"]
        assert components[0]["tags.hasYabsAttributes"]
        assert components[0]["text.YabsUrl"] is not None
        assert components[1]["tags.isFromDirect"]
        assert components[1]["tags.hasYabsAttributes"]
        assert components[1]["text.YabsUrl"] is not None
        assert not components[2]["tags.isFromDirect"]
        assert not components[2]["tags.hasYabsAttributes"]
        assert components[2]["text.YabsUrl"] is None

    def test_verified_rating(self):
        components = self.read_components("test_verified_rating.json")
        assert components[0]["double.verified_rating"] == 2.0
        assert components[1]["double.verified_rating"] == 5.0
        assert components[2]["double.verified_rating"] == 4.0
        assert components[0]["long.verified_ratings_count"] == 1
        assert components[1]["long.verified_ratings_count"] == 6
        assert components[2]["long.verified_ratings_count"] == 0

    def test_distance(self):
        components = self.read_components("test_distance.json")
        assert components[0]["double.distance"] == 0.0
        assert components[1]["double.distance"] == 0.0
        assert components[2]["double.distance"] == 0.0
        assert components[3]["double.distance"] == 0.0

        assert components[7]["double.distance"] == 3500.0
        assert components[8]["double.distance"] == 22800.0
        assert components[9]["double.distance"] == 3100.0

        assert components[0]["long.has_known_distance"] == 0
        assert components[1]["long.has_known_distance"] == 0
        assert components[2]["long.has_known_distance"] == 0
        assert components[3]["long.has_known_distance"] == 0

        assert components[7]["long.has_known_distance"] == 1
        assert components[8]["long.has_known_distance"] == 1
        assert components[9]["long.has_known_distance"] == 1

    def test_activity(self):
        components = self.read_components("test_activity.json")

        assert components[0]["double.activity"] == 0.0 and components[0]["text.activity_group"] == ""
        assert components[1]["double.activity"] == 0.0 and components[1]["text.activity_group"] == ""
        assert components[2]["double.activity"] == 0.0 and components[2]["text.activity_group"] == ""
        assert components[3]["double.activity"] == 0.0 and components[3]["text.activity_group"] == ""
        assert components[4]["double.activity"] == 0.0 and components[4]["text.activity_group"] == ""
        assert components[5]["double.activity"] == 0.0 and components[5]["text.activity_group"] == ""
        assert components[7]["double.activity"] == 0.327483 and components[7]["text.activity_group"] == "normal"
