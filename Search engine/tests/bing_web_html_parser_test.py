from test_utils import TestParser


class TestBingWebParser(TestParser):
    MODULE_PATH = "bing.bing_web_html_parser"
    PARSER_CLASSNAME = "BingWebHtmlParser"

    def parse_file(self, *path_components, additional_parameters={"config": {"merge_knowledge_graph": False}}):
        return super().parse(self._read_file(*path_components), additional_parameters, module_name=self.MODULE_PATH, class_name=self.PARSER_CLASSNAME)

    def test_desktop_ugly_serp(self):
        parsed = self.parse_file("desktop", "ugly-serp.html")
        components = parsed["components"]
        first = components[0]
        assert first['componentInfo'] == {'type': 1, 'alignment': 3, "rank": 1}
        assert first["componentUrl"]["pageUrl"] == "https://obrazovaka.ru/books/chehov/vishnyovyj-sad"
        assert first["text.title"] == "Чехов «Вишнёвый сад» краткое содержание по действиям – чит…"

    def test_fast_api(self):
        parsed = self.parse_file("desktop", "fast_api.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 10

    def test_ads(self):
        parsed = self.parse_file("desktop", "suvorov.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 15

        components = parsed["components"]
        first_ad = components[0]
        assert first_ad['componentInfo'] == {'type': 1, 'alignment': 1, "rank": 1}
        assert first_ad["text.title"] == "Покупайте товары от Суворов выгодно – Яндекс.Маркет"
        assert first_ad["text.snippet"] == "Находите то, что нравится. Заказывайте туда, куда хочется на Яндекс.Маркете. · круглосуточно"

        second_ad = components[11]
        assert second_ad['componentInfo'] == {'type': 1, 'alignment': 2, "rank": 12}
        assert second_ad["text.title"] == "Виктор Суворов, День \"М\". Когда началась Вторая..."
        assert second_ad["text.snippet"] == "Более 1 000 000 книг в форматах FB2, EPUB, TXT, PDF, Аудиокниги. Выбирайте и читайте! · 98559 · круглосуточно"

        third_ad = components[12]
        assert third_ad['componentInfo'] == {'type': 1, 'alignment': 2, "rank": 13}
        assert third_ad["text.title"] == "Книжный магазин book24.ru - Бесплатная доставка книг"
        assert third_ad["text.snippet"] == "Дарим 150₽ на книги и кэшбэк 20% на первую покупку в книжном магазине Book24"
