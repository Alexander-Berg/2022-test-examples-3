from test_utils import TestParser


class TestYandexSearchResultWizUnisearchEducationJSONParser(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")
        url_0 = "https://vse-kursy.com/onlain/6118-onlain-izuchenie-programmirovaniya-dlya-detei-ot-5-do-17-let.html"
        title_0 = "Онлайн изучение программирования для детей от 5 до 17 лет"
        # price_0 = 3500
        avatar_1 = "https://248006.selcdn.ru/LandGen/phone_9a9e89565b50fecf829979e99c4e773a8b693e54.png"
        organisation_1 = "Skillbox"
        # price_per_month_1 = 8727
        # price_discounted_1 = 5236
        url_2 = "https://skillbox.ru/course/hyper-casual/"
        title_2 = "Геймдев для начинающих: Hyper Casual"
        length_months_2 = 4

        assert url_0 == example["components"][0]["componentUrl"]["pageUrl"]
        assert title_0 == example["components"][0]["text.title"]
        # assert price_0 == example["components"][0]["long.price"]
        assert avatar_1 == example["components"][1]["url.avatar"]
        # assert price_per_month_1 == example["components"][1]["long.price_per_month"]
        # assert price_discounted_1 == example["components"][1]["long.price_discounted"]
        assert avatar_1 == example["components"][1]["url.avatar"]
        assert organisation_1 == example["components"][1]["text.organization"]
        assert url_2 == example["components"][2]["componentUrl"]["pageUrl"]
        assert title_2 == example["components"][2]["text.title"]
        assert length_months_2 == example["components"][2]["long.length_months"]
