from test_utils import TestParser


class TestYandexUslugiSaasLight(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")
        url0 = "https://uslugi.yandex.ru/search?worker_id=4b192452-1be1-44e5-b4ef-662a9d9a1902"
        url2 = "https://uslugi.yandex.ru/search?worker_id=2729ba79-ae4d-47f1-85a3-d1522eed9a35"
        cnt = 759
        assert url0 == example["components"][0]["componentUrl"]["pageUrl"]
        assert url2 == example["components"][2]["componentUrl"]["pageUrl"]
        assert cnt == example["long.docsFound"]
