from travel.hotels.tools.region_pages_builder.common.declined import DeclinedByInflector


class TestDeclined:
    def test_declined_by_inflector(self):
        hotel_metropol = DeclinedByInflector("Гостиница Метрополь")

        assert hotel_metropol.nominative == "Гостиница Метрополь"
        assert hotel_metropol.genitive == "Гостиницы Метрополь"
        assert hotel_metropol.dative == "Гостинице Метрополь"
        assert hotel_metropol.accusative == "Гостиницу Метрополь"
        assert hotel_metropol.instrumental == "Гостиницей Метрополь"
        assert hotel_metropol.prepositional == "Гостинице Метрополь"

        assert hotel_metropol.ablative == "из Гостиницы Метрополь"
        assert hotel_metropol.directional == "в Гостиницу Метрополь"
        assert hotel_metropol.locative == "в Гостинице Метрополь"

        hotel_arcadia = DeclinedByInflector("Отель Аркадия")

        assert hotel_arcadia.nominative == "Отель Аркадия"
        assert hotel_arcadia.genitive == "Отеля Аркадия"
        assert hotel_arcadia.dative == "Отелю Аркадия"
        assert hotel_arcadia.accusative == "Отель Аркадия"
        assert hotel_arcadia.instrumental == "Отелем Аркадия"
        assert hotel_arcadia.prepositional == "Отеле Аркадия"

        assert hotel_arcadia.ablative == "из Отеля Аркадия"
        assert hotel_arcadia.directional == "в Отель Аркадия"
        assert hotel_arcadia.locative == "в Отеле Аркадия"
