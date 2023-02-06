import pytest

from test_utils import TestParser
from yandex_market_incut_parsers import MarketGeneralIncutParser


class TestYandexMarketSearchParser(TestParser):
    @staticmethod
    def _get_filter_by_id(filters: list, filter_id: str):
        for filter_item in filters:
            if filter_item.get("id") == filter_id:
                return filter_item

    @pytest.mark.parametrize("path, expected_count", [
        ("folga.html", 22),
        ("dremmel.html", 49),
        ("clarins_feedback.html", 13),
        ("apple_watch_bands.html", 9),
        ("xiaomi_mi_band_6_nfc.html", 25),
        ("xiaomi_mi_10.html", 49),
    ])
    def test_component_count(self, path, expected_count):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) == expected_count

    @pytest.mark.parametrize("path, component, cpa", [
        ("folga.html", 0, True),
        ("folga.html", 1, True),
        ("folga.html", 2, False),
    ])
    def test_component_cpa(self, path, component, cpa):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components)
        assert components[component].get("tags.isCpa") == cpa

    @pytest.mark.parametrize("path, component, document_type", [
        ("folga.html", 0, "model_card"),
        ("folga.html", 1, "model_card"),
        ("folga.html", 2, "offer"),
    ])
    def test_component_document_type(self, path, component, document_type):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components)
        assert components[component].get("text.market_document_type") == document_type

    @pytest.mark.parametrize("path, component, fast_delivery", [
        ("folga.html", 0, False),
        ("dremmel.html", 0, True),
    ])
    def test_component_fast_delivery(self, path, component, fast_delivery):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components)
        assert components[component].get("tags.fastDelivery") == fast_delivery

    def test_component_top_ad(self):
        parsed = self.parse_file("dremmel.html")
        components = parsed["components"]
        first = components[0]
        assert first['componentInfo'] == {'type': 3, 'alignment': 3}
        assert first["componentUrl"]["pageUrl"] == 'http://market.yandex.ru/offer/Cvm9tbAHmBzsKavrlClaiw?hid=15094413&hyperid=6151980&lr=213' \
                                                   '&modelid=6151980&nid=70955&text=%D0%B4%D1%80%D0%B5%D0%BC%D0%B5%D0%BB%D1%8C%204300'
        assert first["text.title"] == "Гравер Dremel 4000-1/45"
        assert first["text.snippet"] == "Производитель: Dremel<br/>Потребляемая мощность: 175 Вт"

    def test_component_incut_ad(self):
        parsed = self.parse_file("xiaomi_mi_band_6_nfc.html")
        components = parsed["components"]
        first = components[2]
        assert first['componentInfo'] == {'type': 3, 'alignment': 3}
        assert first["componentUrl"]["pageUrl"] == 'http://market.yandex.ru/product--umnyi-braslet-xiaomi-mi-smart-band-6/1658470702?nid=54440&context=search' \
                                                   '&text=xiaomi%20mi%20band%206%20nfc&sku=101577447838&do-waremd5=MX18mooTlYc7CYlRIC40fQ&sponsored=1'
        assert first["text.title"] == "Умный браслет Xiaomi Mi Smart Band 6"
        assert first["text.snippet"] == ""

    def test_component_incut_premium(self):
        parsed = self.parse_file("incut/inverter.html")
        components = parsed["components"]
        first = components[0]
        assert first['componentInfo'] == {'type': 3, 'alignment': 3, 'wizardType': 30}
        assert first['text.marketWizardType'] == "premium-offers-gallery"
        assert len(first['site-links']) == 10
        assert first["componentUrl"]["pageUrl"] is None

    def test_component_incut_ad_bids(self):
        parsed = self.parse_file("honor_9_case.html")
        components = parsed["components"]
        first = components[1]
        assert first['componentInfo'] == {'type': 3, 'alignment': 3}
        assert first["componentUrl"]["pageUrl"] == 'http://market.yandex.ru/product--chekhol-dlia-honor-9a-chekhol-na-khonor-9a-goluboi/927727673?nid=54440&context=search' \
                                                   '&text=%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D1%84%D0%B8%D1%80%D0%BC%D0%B5%D0%BD%D0%BD%D1%8B%D0%B9%20%D1%87%D0%B5%D1' \
                                                   '%85%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20honor%209&sku=101145232853&do-waremd5=3ePRNNpr0DvEPGAwyQu6yw&sponsored=1'
        assert first["text.title"] == "Чехол для Honor 9A / чехол на хонор 9а голубой"
        assert first["text.snippet"] == "цвет товара: голубой<br/>материал: силикон<br/>soft touch<br/>особенности: тонкий"

        assert first["long.vendorBid"] == 0
        assert first["long.vendorClickPrice"] == 0
        assert first["long.shopFee"] == 0
        assert first["long.shopBrokeredFee"] == 0

    def test_component_content(self):
        parsed = self.parse_file("dremmel.html")
        components = parsed["components"]
        first = components[1]
        assert first['componentInfo'] == {'type': 1, 'alignment': 3}
        assert first["componentUrl"]["pageUrl"] == 'http://market.yandex.ru/product--graver-dremel-4300-3-45ez/1732697683?nid=70955&context=search' \
                                                   '&text=%D0%B4%D1%80%D0%B5%D0%BC%D0%B5%D0%BB%D1%8C%204300&sku=1732697683&do-waremd5=CM9uzs-ogjSpNYTXnGC-gQ'
        assert first["text.title"] == "Гравер Dremel 4300-3/45EZ"
        assert first["text.snippet"] == "мощность 175 Вт<br/>частота вращения диска до 35000 об/мин<br/>Модификация F0134300JD<br/>вес: 0.66 кг"
        assert first["url.offerImageUrl"] == "https://avatars.mds.yandex.net/get-mpic/5217715/img_id402118417304413997.jpeg/x248_trim"

    @pytest.mark.parametrize("path, component, image_url", [
        ("men_sneakers.html", 0, "https://avatars.mds.yandex.net/get-mpic/5376959/img_id2209970639233087008.jpeg/5hq"),
        ("dremmel.html", 1, "https://avatars.mds.yandex.net/get-mpic/5217715/img_id402118417304413997.jpeg/x248_trim"),
    ])
    def test_image_url(self, path, component, image_url):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components)
        # TODO: fix test
        # assert components[component].get("url.offerImageUrl") == image_url

    def test_component_with_feedback_content(self):
        parsed = self.parse_file("clarins_feedback.html")
        components = parsed["components"]
        first = components[1]
        assert first['componentInfo'] == {'type': 1, 'alignment': 3}
        assert first["componentUrl"]["pageUrl"] == 'http://market.yandex.ru/product--syvorotka-seacare-instant-effect-eye-lifting-serum' \
                                                   '-podtiagivaiushchaia-mgnovennogo-deistviia-s-sesaflesh-izilians-i-argirelinom-dlia-kozhi' \
                                                   '-vokrug-glaz-20-ml/343048192?nid=17437160&context=search&cpa=1&' \
                                                   'text=clarins%20%D1%81%D1%8B%D0%B2%D0%BE%D1%80%D0%BE%D1%82%D0%BA%D0%B0%20%D0%B4%D0%BB%D1%8F%20%D0' \
                                                   '%B3%D0%BB%D0%B0%D0%B7%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&sku=343048192' \
                                                   '&do-waremd5=Z6VK34ULXYrKlEUCAAwouw'
        assert first["text.title"] == "Сыворотка SeaCare Instant Effect Eye Lifting Serum подтягивающая мгновенного действия с Сесафлеш Изильянс" \
                                      " и Аргирелином для кожи вокруг глаз, 20 мл"
        assert first["text.snippet"] == ""

    def test_filters(self):
        parsed = self.parse_file("dremmel.html")
        filters = parsed.get("filters")
        assert len(filters) == 22

        filter_item = self._get_filter_by_id(filters, "7893318")
        assert filter_item, "should exist"
        assert filter_item["name"] == "Производитель"
        assert filter_item["kind"] == "checkbox-list"
        options = filter_item["options"]

        assert len(options) == 1
        assert options[0]["name"] == "Dremel"

    def test_rich_snippet(self):
        parsed = self.parse_file("car_lamp.html")
        components = parsed["components"]
        first = components[0]
        assert first['componentInfo'] == {'type': 3, 'alignment': 3}


class TestYandexMarketSearchIntentParser(TestParser):
    @pytest.mark.parametrize("path, expected_count", [
        ("folga.html", 7),
    ])
    def test_component_count(self, path, expected_count):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) == expected_count


class TestYandexMarketSearchBasePremiumGalleryParser(TestParser):
    @pytest.mark.parametrize("path, expected_gallery_type", [
        ("smartphones.html", 'premium'),
        ('plywood.html', ''),
    ])
    def test_gallery_type(self, path, expected_gallery_type):
        parsed = self.parse_file(path)
        assert parsed['text.incutType'] == expected_gallery_type

    @pytest.mark.parametrize("path, expected_count", [
        ("smartphones.html", 10),
        ('plywood.html', 0),
    ])
    def test_component_count(self, path, expected_count):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) == expected_count

    def test_component_content(self):
        parsed = self.parse_file("smartphones.html")
        components = parsed["components"]
        third = components[3]
        assert third['componentInfo'] == {'type': 3, 'alignment': 3}
        assert third['text.title'] == 'Смартфон realme 8 6/128 ГБ RU, Cyber Black'

    def test_no_incut(self):
        parsed = self.parse_file('plywood.html')
        assert len(parsed['components']) == 0
        assert parsed['text.incutType'] in (None, '')


class TestYandexMarketSearchVendorGalleryParser(TestParser):
    @pytest.mark.parametrize("path, expected_gallery_type", [
        ("fridges.html", 'vendor'),
        ('plywood.html', ''),
    ])
    def test_gallery_type(self, path, expected_gallery_type):
        parsed = self.parse_file(path)
        assert parsed['text.incutType'] == expected_gallery_type

    @pytest.mark.parametrize("path, expected_count", [
        ("fridges.html", 8),
        ('plywood.html', 0),
    ])
    def test_component_count(self, path, expected_count):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) == expected_count

    def test_component_content(self):
        parsed = self.parse_file("fridges.html")
        components = parsed["components"]
        third = components[3]
        assert third['componentInfo'] == {'type': 3, 'alignment': 3}
        assert third['text.title'] == 'MAUNFELD MFF177NFSB'

    def test_no_incut(self):
        parsed = self.parse_file('plywood.html')
        assert len(parsed['components']) == 0
        assert parsed['text.incutType'] in (None, '')


class TestYandexMarketSearchPremiumGalleryParser(TestParser):
    @pytest.mark.parametrize("path, expected_incut_type", [
        ("iphone_12_rich.html", 'rich-snippet'),
        ('plywood.html', ''),
    ])
    def test_gallery_type(self, path, expected_incut_type):
        parsed = self.parse_file(path)
        assert parsed['text.incutType'] == expected_incut_type

    @pytest.mark.parametrize("path, expected_count", [
        ("iphone_12_rich.html", 1),
        ('plywood.html', 0),
    ])
    def test_component_count(self, path, expected_count):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) == expected_count

    def test_component_content(self):
        parsed = self.parse_file("iphone_12_rich.html")
        components = parsed["components"]
        first = components[0]
        assert first['componentInfo'] == {'type': 3, 'alignment': 3}
        assert first['text.title'] == 'Смартфон Apple iPhone 12 64 ГБ RU, черный'


class TestMarketGeneralIncutParser(TestParser):

    _parser_class = MarketGeneralIncutParser

    @pytest.mark.parametrize("path, expected_incut_type, expected_incut_place", [
        ('new_front_search_rich-snippet-card.html', 'rich-snippet-card', 'search'),
        ('new_front_search_premium-offers-gallery.html', 'premium-offers-gallery', 'search'),
        ('new_front_top_incutCarouselWithLogo.html', 'incutCarouselWithLogo', 'top'),
        ('new_front_top_vendorPromoScrollBox.html', 'vendorPromoScrollBox', 'top'),
    ])
    def test_incut_type_and_place(self, path, expected_incut_type, expected_incut_place):
        parsed = self.parse_file(path)
        has_correct_place_and_vt = False
        for comp in parsed["components"]:
            if comp['text.marketWizardType'] == expected_incut_type and comp['text.place'] == expected_incut_place:
                has_correct_place_and_vt = True
        assert has_correct_place_and_vt
