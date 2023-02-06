import json
import textwrap
from datetime import datetime

import pytest
import responses

from google_web_parser import GoogleWebParser
from test_utils import TestParser, ComponentTypes, WizardTypes, Alignments


class TestGoogleWebParser(TestParser):
    PITER_ID = 2
    RIGA_ID = 11474
    STAMBUL_ID = 11508
    KIEV_ID = 143
    BILBAO_ID = 109265
    MOSCOW_ID = 213
    UNKNOWN_ID = 99999
    GEOBASE_URL_TEMPLATE = "http://geobase.qloud.yandex.ru/v1/region_by_id?id={}"

    # don't merge kg in tests by deafult
    def parse_file(self, *path_components, additional_parameters={"config": {"merge_knowledge_graph": False}}):
        return super().parse(self._read_file(*path_components), additional_parameters)

    @pytest.mark.parametrize("filename, expected_count", [
        ("python.html", 12),
        ("perforator.html", 17),
        ("js_push.html", 12),
        ("ooo_volna.html", 11),
        ("playdom_casino.html", 31),
        ("covid_online.html", 16),
        ("altai_covid.html", 39),
        ("ikea.html", 11),
    ])
    def test_parser_desktop_component_count(self, filename, expected_count):
        parsed = self.parse_file("desktop", filename)
        components = parsed["components"]
        assert len(components) == expected_count

        # selectors are unique
        selectors = []
        for component in components:
            assert 'text.cssSelector' in component
            if selector := component.get('text.cssSelector'):
                selectors.append(selector)

        assert len(selectors) == len(set(selectors))

    @pytest.mark.parametrize("filename, expected_count", [
        ("python.html", 10),
        ("perforator.html", 9),
        ("stroymateriali.html", 9),
        ("js_push.html", 9),
        ("ooo_volna.html", 10),
        ("playdom_casino.html", 30),
        ("covid_online.html", 10),
        ("altai_covid.html", 30),
        ("corona_desktop.html", 10),
    ])
    def test_parser_desktop_search_results_count(self, filename, expected_count):
        parsed = self.parse_file("desktop", filename)
        components = parsed["components"]
        search_results = self._get_search_results(components)
        assert len(search_results) == expected_count

    def test_desktop_python(self):
        parsed = self.parse_file("desktop", "python.html")
        components = parsed["components"]
        first = components[0]
        assert first['componentInfo'] == {'type': 1, 'alignment': 3}
        assert first["componentUrl"]["pageUrl"] == 'https://www.python.org/'
        assert first["componentUrl"]['viewUrl'] == 'www.python.org'
        assert first["text.title"] == "Welcome to Python.org"
        assert first["text.snippet"] == "The official home of the Python Programming Language."
        assert first['text.cssSelector'] == "[data-hveid='CAYQAA']"

    def test_desktop_perforator(self):
        parsed = self.parse_file("desktop", "perforator.html")
        components = parsed["components"]
        first = components[0]
        assert first['componentInfo'] == {'type': 3, 'alignment': 1}
        assert first["componentUrl"]["pageUrl"] == 'https://pokupki.market.yandex.ru/catalog/perforatory/56419/list'
        assert first["componentUrl"]['viewUrl'] == 'pokupki.market.yandex.ru/'
        assert first["text.title"] == "Купить Перфораторы по низким ценам в интернет-магазинах"
        assert first["text.snippet"] == "Покупайте товары на Яндекс.Маркете по низким ценам. Скидки и акции Каждый День!"
        assert first['text.cssSelector'] == "[data-hveid='CAcQAA']"

    @pytest.mark.parametrize("filename, expected_count", [
        ("birthday_cards.html", 23),
        ("iphone.html", 16),
        ("mangalib.html", 9),
        ("radiohead_clips.html", 12),
        ("theorems.html", 10),
        ("python_touch.html", 16),
        ("kvn_bububu.html", 8),
        ("the_book.html", 13),
        ("tom_hardy_films.html", 13),
        ("ooo_volna.html", 15),
        ("bad_nerd.html", 15),
        ("integrals_calc.html", 13),
        ("pogoda.html", 13),
        ("mi_band_4.html", 15),
        ("kan_zaman.html", 14),
        ("mass.html", 14),
        ("morgenstern.html", 34),
        ("nezabudka.html", 14),
        ("biography.html", 13),
        ("random.html", 12),
        ("boy_with_cross.html", 12),
        ("daughter_song.html", 13),
        ("lsp_song.html", 13),
        ("hurricane.html", 15),
        ("ios_app_multiline_wizard.html", 16),
    ])
    def test_parser_touch_component_count(self, filename, expected_count):
        parsed = self.parse_file("touch", filename)
        components = parsed["components"]
        import logging
        logging.warning(f"{filename=} {len(components)=} {expected_count=}")
        assert len(components) == expected_count

        # selectors are unique
        selectors = []
        for component in components:
            assert 'text.cssSelector' in component
            if selector := component.get('text.cssSelector'):
                selectors.append(selector)

        assert len(selectors) == len(set(selectors))

    @pytest.mark.parametrize("filename, expected_count", [
        ("birthday_cards.html", 18),
        ("iphone.html", 9),
        ("kvn_bububu.html", 5),
        ("mangalib.html", 6),
        ("python_touch.html", 10),
        ("radiohead_clips.html", 9),
        ("radiohead.html", 9),  # The Java version gives 7
        ("smeshariki.html", 8),  # The Java version gives 6
        ("the_book.html", 9),
        ("theorems.html", 10),
        ("turkey.html", 8),
        ("umnyasha.html", 10),
        ("tom_hardy_films.html", 9),
        ("ooo_volna.html", 10),
        ("bad_nerd.html", 9),
        ("integrals_calc.html", 10),
        ("pogoda.html", 9),
        ("cinema_star_armenia.html", 9),
        ("mi_band_4.html", 9),
        ("kan_zaman.html", 9),
        ("morgenstern.html", 29),
        ("reviews.html", 30),
        ("tsoys_songs.html", 8),
        ("types_of_snakes.html", 9),
        ("coronavirus_in_tatarstan.html", 10),
        ("coronavirus_in_belarus.html", 9),
        ("vesna_na_zarechnoy.html", 8),
        ("boy_with_cross.html", 9),
        ("daughter_song.html", 8),
        ("ios_app_multiline_wizard.html", 8),
        ("akita.html", 8),
        ("corso.html", 7),
        ("skyscraper.html", 9),
    ])
    def test_parser_touch_search_results_count(self, filename, expected_count):
        parsed = self.parse_file("touch", filename)
        components = parsed["components"]
        search_results = self._get_search_results(components)
        assert len(search_results) == expected_count

    def test_touch_theorems(self):
        parsed = self.parse_file("touch", "theorems.html")
        components = parsed["components"]
        first = components[0]
        assert first['componentInfo'] == {'type': 1, 'alignment': 3}
        assert first["componentUrl"]["pageUrl"] == 'https://people.mpi-sws.org/~dreyer/tor/papers/wadler.pdf'
        assert first["componentUrl"]['viewUrl'] == 'people.mpi-sws.org › wadler'
        assert first["text.title"] == "Theorems for free! - People at MPI-SWS"
        assert first["text.snippet"] == "From the type of a polymorphic function we can de- " \
                                        "rive a theorem that it satisfies. Every function of the " \
                                        "same type satisfies the same theorem. This provides a free " \
                                        "source of useful theorems, courtesy of Reynolds' abstraction " \
                                        "theorem for the polymorphic lambda calcu- lus."
        assert first['text.cssSelector'] == "[data-hveid='CAIQAA']"

    @pytest.mark.parametrize("path, css_selector", [
        ("desktop/ooo_volna.html", "[data-hveid='CB4QAQ']"),
        ("touch/ooo_volna.html", "[data-hveid='CBsQAA'],[data-hveid='CBgQAA']"),
        ("touch/semizvetik.html", "[data-hveid='CBYQAA'],[data-hveid='CBUQAA']")
    ])
    def test_organization_card(self, path, css_selector):
        parsed = self.parse_file(path)
        components = parsed["components"]
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_ORGANIZATION_CARD)[0]
        assert wizard
        assert wizard['text.cssSelector'] == css_selector

    @pytest.mark.parametrize("path, css_selectors", [
        ("oo/avengers_endgame.html", [
            "[data-hveid='CD8QAA']"
        ]),
        ("oo/keira.html", [
            "[data-hveid='CB8QAA']"
        ]),
        ("oo/sony.html", [
            "[data-hveid='CDoQAA']"
        ]),
        ("python.html", [
            "[data-hveid='CBkQAA']"
        ]),
        ("covid_online.html", [
            "[data-hveid='CBMQAA']",
            "[data-hveid='CBEQAA']",
            "[data-hveid='CC8QAA']",
            "[data-hveid='CCMQAA']",
            "[data-hveid='CCcQAA']",
        ]),
        ("altai_covid.html", [
            "[data-hveid='CBEQAA']",
            "[data-hveid='CAkQAA']",
            "[data-hveid='CAcQAA']",
            "[data-hveid='CBIQAA']",
            "[data-hveid='CF8QAA']",
            "[data-hveid='CCAQAA']",
            "[data-hveid='CBsQAA']",
            "[data-hveid='CBoQAA']",
        ]),
    ])
    def test_desktop_knowledge_graph(self, path, css_selectors):
        parsed = self.parse_file("desktop", path)
        components = parsed["components"]
        wizards = self._get_wizard_components(components, WizardTypes.WIZARD_KNOWLEDGE_GRAPH)
        assert wizards
        assert len(wizards) == len(css_selectors)
        for wizard, css_elector in zip(wizards, css_selectors):
            assert wizard['text.cssSelector'] == css_elector

    @pytest.mark.parametrize("path, css_selectors", [
        ("radiohead.html", [
            "[data-hveid='CCUQAA'],[data-hveid='CCMQAA'],[data-hveid='CD8QAA'],[data-hveid='CCIQAA'],[data-hveid='CDoQAA']",
            "[data-hveid='CEcQAA']",
            "[data-hveid='CD4QAA']"
        ]),
        ("the_book.html", [
            "[data-hveid='CEUQAA'],[data-hveid='CCIQAA'],[data-hveid='CDgQAA']",
            "[data-hveid='CEEQAA']",
            "[data-hveid='CD0QAA']"
        ]),
        ("bad_nerd.html", [
            "[data-hveid='CAgQAA'],[data-hveid='CAUQAA']",
            "[data-hveid='CB0QAA']",
            "[data-hveid='CC4QAA']"
        ]),
        ("integrals_calc.html", [
            "[data-hveid='CCgQAA']",
            "[data-hveid='CCAQAA']",
        ]),
        ("kinopoisk.html", [
            "[data-hveid='CBAQAA'],[data-hveid='CBoQAA'],[data-hveid='CA0QAA'],[data-hveid='CAwQAA']",
        ]),
        ("kan_zaman.html", [
            "[data-hveid='CDkQAA']",
            "[data-hveid='CCYQAA']",
            "[data-hveid='CEIQAA']",
            "[data-hveid='CEQQAA']",
        ]),
        ("big_tasty.html", [
            "[data-hveid='CA8QAA']",
            "[data-hveid='CAoQAA']",
            "[data-hveid='CDcQAA']",
            "[data-hveid='CCkQAA']",
        ]),
        ("rumba.html", [
            "[data-hveid='CDUQAA']",
            "[data-hveid='CDsQAA']",
            "[data-hveid='CE4QAw']",
            "[data-hveid='CCwQAA']",
            "[data-hveid='CFIQAA']",
        ])
    ])
    def test_touch_knowledge_graph(self, path, css_selectors):
        parsed = self.parse_file("touch", path)
        components = parsed["components"]
        wizards = self._get_wizard_components(components, WizardTypes.WIZARD_KNOWLEDGE_GRAPH)
        assert wizards
        assert len(wizards) == len(css_selectors)
        for wizard, css_elector in zip(wizards, css_selectors):
            assert wizard['text.cssSelector'] == css_elector

    def test_touch_merge_knowledge_graph(self):
        parsed = self.parse_file("touch", "radiohead.html", additional_parameters={})
        assert len(parsed["components"]) == 13

    @pytest.mark.parametrize("path, css_elector", [
        ("desktop/perforator.html", "[data-hveid='CBQQAA']"),
    ])
    def test_maps(self, path, css_elector):
        parsed = self.parse_file(path)
        components = parsed["components"]
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_MAPS)[0]
        assert wizard
        assert wizard['text.cssSelector'] == css_elector

    @pytest.mark.parametrize("path, css_elector", [
        ("desktop/300x150.html", "[jsaction='mouseover:Q6qFEf;mouseout:lpHYw']"),
    ])
    def test_calculator(self, path, css_elector):
        parsed = self.parse_file(path)
        components = parsed["components"]
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_CALCULATOR)[0]
        assert wizard
        assert wizard['text.cssSelector'] == css_elector

    def test_desktop_python_knowledge_graph(self):
        parsed = self.parse_file("desktop", "python.html")
        components = parsed["components"]
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_KNOWLEDGE_GRAPH)[0]
        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': 4, 'wizardType': WizardTypes.WIZARD_KNOWLEDGE_GRAPH}
        assert wizard["text.title"] == "Python"
        assert wizard['text.cssSelector'] == "[data-hveid='CBkQAA']"

    @pytest.mark.parametrize("file_name", [
        "touch/birthday_cards.html",
        "touch/boy_with_cross.html",
        "desktop/perforator.html",
    ])
    def test_touch_images(self, file_name):
        parsed = self.parse_file(file_name)
        components = parsed["components"]
        assert self._wizard_is_present(components, WizardTypes.WIZARD_IMAGE)

    def test_touch_organic_carousel(self):
        parsed = self.parse_file("touch/radiohead_clips.html")
        components = parsed["components"]
        component = components[1]
        assert component['componentInfo'] == {
            'type': ComponentTypes.SEARCH_RESULT,
            'alignment': Alignments.LEFT,
        }
        assert component["componentUrl"]["pageUrl"] == 'https://m.youtube.com/playlist?list=PLukmsaXDPJie7L7Ihn63HJhA6YMp7tUUr'
        assert component["componentUrl"]["viewUrl"] == 'm.youtube.com › playlist'
        assert component["text.cssSelector"] == "[data-hveid='CBwQAA']"
        assert component["text.title"] == "Radiohead Official Videos (HD) - YouTube"

    def test_touch_birthday_cards_image(self):
        parsed = self.parse_file("touch/birthday_cards.html")
        components = parsed["components"]
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_IMAGE)[0]
        assert wizard['componentInfo'] == {
            'type': ComponentTypes.WIZARD,
            'alignment': Alignments.LEFT,
            'wizardType': WizardTypes.WIZARD_IMAGE
        }
        assert wizard["componentUrl"]["pageUrl"] == 'https://google.ru/search?source=univ&tbm=isch&q=%D0%BE%D1%82%D0%BA%D1%80%D1%8B%D1%82%D0%BA%D0' \
                                                    '%B8+%D1%81+%D0%B4%D0%BD%D0%B5%D0%BC+%D1%80%D0%BE%D0%B6%D0%B4%D0%B5%D0%BD%D0%B8%D1%8F&hl=ru&' \
                                                    'fir=DOQzqvoASdeNmM%252CveiKOtco8JURbM%252C_%253BaUutgnpAPoSBDM%252CYKMYBGDPhe2chM%252C_' \
                                                    '%253BcXZrU9uCQ8iDbM%252CYp8aTy9OevosZM%252C_%253B66Q6CUOCLy28AM%252ChfiBLmKv04a0_M%252C_' \
                                                    '%253B68wCDiq0mERaMM%252CGLiRKa0LAtgnQM%252C_%253BYaGHPHw-GyuAiM%252C8Eqc9DJ7YcU-AM%252C_' \
                                                    '%253B9REiYXye54dNaM%252CKQRymOQtuAYGtM%252C_%253BeqCmUD8JX7-9RM%252COzTkSTqT34M7bM%252C_' \
                                                    '%253BRyuv5mPBR0zNQM%252Cw6ABfwXgYKSmmM%252C_%253BhWyaOI4vV6_H1M%252CveiKOtco8JURbM%252C_' \
                                                    '%253Bjea1uJ8FyBpuQM%252C2kSvyqyrvVFldM%252C_%253BWS7rarKxfjt_zM%252CfQ6ggO4WI7sRIM%252C_'
        assert wizard['text.cssSelector'] == "[data-hveid='CBAQAA']"

    @pytest.mark.parametrize("file_name", [
        "kvn_bububu.html",
        "radiohead_clips.html",
        "radiohead.html",
        "tom_hardy_films.html",
        "bad_nerd.html",
        "daughter_song.html"
    ])
    def test_touch_video(self, file_name):
        parsed = self.parse_file("touch", file_name)
        components = parsed["components"]
        assert self._wizard_is_present(components, WizardTypes.WIZARD_VIDEO)

    @pytest.mark.parametrize("file_name, css_selectors", [
        ("touch/python_touch.html", ["[data-hveid='CDgQAA']", "[data-hveid='CCoQAA']"]),
        ("touch/birthday_cards.html", ["[data-hveid='CCUQAA']", "[data-hveid='CCQQAA']"]),
        ("touch/radiohead_clips.html", ["[data-hveid='CBcQAA']", "[data-hveid='CBAQAA']"]),
        ("touch/tom_hardy_films.html", ["[data-hveid='CBwQAA']"]),
        ("touch/iphone.html", ["[data-hveid='CCMQAA']"]),
    ])
    def test_touch_related_queries(self, file_name, css_selectors):
        parsed = self.parse_file(file_name)
        components = parsed["components"]
        wizards = self._get_wizard_components(components, WizardTypes.METRICS_RELATED_QUERIES)
        assert wizards
        assert len(wizards) == len(css_selectors)
        for wizard, css_elector in zip(wizards, css_selectors):
            assert wizard['text.cssSelector'] == css_elector

    # def _check_wizard_suggest_fact(
    #     self,
    #     parsed,
    #     component_index,
    #     expected_title,
    #     expected_page_url,
    #     expected_view_url,
    # ):
    #     components = parsed["components"]
    #     assert len(components) > component_index
    #     wsf = components[component_index]
    #     assert self._check_wizard_type(wsf, WizardTypes.WIZARD_SUGGEST_FACT)
    #     assert wsf["text.title"] == expected_title
    #     assert wsf["componentUrl"]["pageUrl"] == expected_page_url
    #     assert wsf["componentUrl"]["viewUrl"] == expected_view_url

    @pytest.mark.parametrize("filename, component_index, expected_title, expected_page_url, expected_view_url", [
        ("js_push.html", 0, "Pop, Push, Shift and Unshift Array Methods in JavaScript",
         "https://alligator.io/js/push-pop-shift-unshift-array-methods/",
         "alligator.io › js › push-pop-shift-unshift-array-methods"
         ),
        ("rigid.html", 0, "How to create a rigidbody - Unity Answers",
         "https://answers.unity.com/questions/329407/how-to-create-a-rigidbody.html",
         "answers.unity.com › questions › how-to-create-a-rigidbody",
         ),
        ("flow.html", 1, "Что такое «жить в потоке», или как применить принципы ...",
         "https://shkolazhizni.ru/psychology/articles/1671/",
         "shkolazhizni.ru › Психология › Статьи"
         ),
        ("long_night.html", 0, "Какая ночь будет самой длинной в 2020 году ...",
         "https://rostov.aif.ru/society/details/kakaya_noch_budet_samoy_dlinnoy_v_2020_godu",
         "rostov.aif.ru › society › details › kakaya_noch_budet_sa...",
         ),
    ])
    def test_parser_desktop_wizard_suggest_fact(
        self,
        filename,
        component_index,
        expected_title,
        expected_page_url,
        expected_view_url,
    ):
        parsed = self.parse_file("desktop", filename)
        components = parsed["components"]
        assert len(components) > component_index
        wsf = components[component_index]
        assert self._check_wizard_type(wsf, WizardTypes.WIZARD_SUGGEST_FACT)
        assert wsf["text.title"] == expected_title
        assert wsf["componentUrl"]["pageUrl"] == expected_page_url
        assert wsf["componentUrl"]["viewUrl"] == expected_view_url

    @pytest.mark.parametrize("filename, component_index, expected_title, expected_page_url, expected_view_url", [
        ("220.html", 1, "Формула нахождения напряжения: основные понятия ... - 220v.guru",
         "https://220v.guru/fizicheskie-ponyatiya-i-pribory/napryazhenie/formula-rascheta-napryazheniya-cherez-silu-toka-i-soprotivlenie.html",
         "220v.guru › ... › Напряжение"
         ),
    ])
    def test_parser_touch_wizard_suggest_fact(
        self,
        filename,
        component_index,
        expected_title,
        expected_page_url,
        expected_view_url,
    ):
        parsed = self.parse_file("touch", filename)
        components = parsed["components"]
        assert len(components) > component_index
        wsf = components[component_index]
        assert (wsf["componentInfo"]["type"] == ComponentTypes.WIZARD and
                wsf["componentInfo"]["wizardType"] == WizardTypes.WIZARD_SUGGEST_FACT)
        assert wsf["text.title"] == expected_title
        assert wsf["componentUrl"]["pageUrl"] == expected_page_url
        assert wsf["componentUrl"]["viewUrl"] == expected_view_url

    @pytest.mark.parametrize("filename, component_index, expected_title, expected_selector", [
        ["crimson_ringing.html", 0, "Николай Гнатюк - Малиновый звон - YouTube", "[data-hveid='CAsQAA']"],
    ])
    def test_parser_desktop_wizard_unknown_video_fact(self, filename, component_index, expected_title, expected_selector):
        parsed = self.parse_file("desktop", filename)
        components = parsed["components"]
        assert len(components) > component_index
        first = components[component_index]
        assert self._check_wizard_type(first, WizardTypes.WIZARD_UNKNOWN)
        assert first["text.title"] == expected_title
        assert first["text.cssSelector"] == expected_selector

    def test_parser_desktop_rhs_ads(self):
        parsed = self.parse_file("desktop", "perforator.html")
        components = parsed["components"]
        assert components
        ads = components[-1]

        assert ads['componentInfo'] == {'type': ComponentTypes.ADDV, 'alignment': Alignments.RIGHT, "wizardType": WizardTypes.WIZARD_SHOPPING}
        assert ads["componentUrl"]["pageUrl"] == 'https://www.google.ru/search?q=%D0%BF%D0%B5%D1%80%D1%84%D0%BE%D1%80%D0%B0%D1%82%D0%BE%D1%80&' \
                                                 'sxsrf=ALeKk03Z4-bz9mwcCyogdJkUYZPqQuK-NQ:1605007503983&source=univ&tbm=shop&tbo=u'
        assert ads["componentUrl"]["viewUrl"] == 'Другие результаты в Google'
        assert ads['text.cssSelector'] == "[data-hveid='CAIQUQ']"
        site_links = ads['site-links']
        assert len(site_links) == 6

        site_link = site_links[0]
        assert site_link['type'] == "SITELINK"
        assert site_link['text.cssSelector'] == "[data-hveid='CAIQUw']"
        site_link_inner = site_link['sitelinks']

        assert site_link_inner['url'] == "https://pokupki.market.yandex.ru/product/perforator-setevoi-bosch-pbh-2100-re-1-7-dzh/7874204" \
                                         "?utm_term=91651%7C7874204&lrfake=213"

    @pytest.mark.parametrize("filename, expected_url, expected_sitelink_count, expected_sitelink_url, "
                             "expected_sitelink_title, expected_price, expected_currency", [
                                 [
                                     "korella.html",
                                     "https://google.ru/search?q=%D0%BA%D0%BE%D1%80%D0%B5%D0%BB%D0%BB%D0%B0+%D0%B2"
                                     "%D0%B8%D1%81%D1%82%D0%B0+%D0%BA%D0%BE%D0%BB%D1%8F%D1%81%D0%BA%D0%B0&source=univ&tbm=shop&tbo=u",
                                     14,
                                     "https://www.akusherstvo.ru/catalog/493501-progulochnaya-kolyaska-carello-vista/",
                                     "Прогулочная коляска Carrello Vista",
                                     12600,
                                     "RUB"
                                 ],
                                 [
                                     "flow.html",
                                     "https://www.ozon.ru/context/detail/id/136458574/",  # no url porbably better, currently return java url
                                     3,
                                     "https://www.ozon.ru/context/detail/id/136458574/",
                                     "(16+) Поток. Психология оптимального переживания",
                                     384,
                                     "RUB"
                                 ],
                                 [
                                     "buy_pump.html",
                                     "https://google.ru/search?q=%D1%86%D0%B5%D0%BD%D0%B0+%D0%BD%D0%B0%D1%81%D0%BE%D1%81%D0%B0+%D1%81%D1%82%D0%B8%D1%80%D0%B0%D0%BB%D1%8C"
                                     "%D0%BD%D0%BE%D0%B9+%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D1%8B+%D1%8D%D0%BB%D0%B5%D0%BA%D1%82%D1%80%D0%BE%D0%BB%D1%8E%D0%BA%D1%81&hl=ru"
                                     "&source=univ&tbm=shop&tbo=u",
                                     8,
                                     "https://master-parts.by/132",
                                     "Сливной насос Askoll 25W (3 защелки, фишки назад), стиральной машины",
                                     37,
                                     "BYN"
                                 ],
                             ])
    def test_parser_desktop_market_ads_top(self, filename, expected_url, expected_sitelink_count,
                                           expected_sitelink_url, expected_sitelink_title, expected_price,
                                           expected_currency):
        parsed = self.parse_file("desktop", filename)
        components = parsed["components"]
        ads = components[0]
        self._check_ads(ads, Alignments.TOP, expected_url, expected_sitelink_count, expected_sitelink_url,
                        expected_sitelink_title, expected_price, expected_currency)

    @pytest.mark.parametrize("filename, expected_url, expected_sitelink_count, expected_sitelink_url, "
                             "expected_sitelink_title, expected_price, expected_currency", [
                                 [
                                     "corsair_crystal_680x_rgb.html",
                                     "https://www.xcom-shop.ru/corsair_crystal_series_680x_rgb_727993.html",
                                     5,
                                     "https://www.xcom-shop.ru/corsair_crystal_series_680x_rgb_727993.html",
                                     "Корпус Corsair Crystal Series 680X RGB",
                                     20646,
                                     "RUB"
                                 ],
                                 [
                                     "best_books_to_read.html",
                                     "https://google.ru/search?q=best+books+to+read&hl=ru&source=univ&tbm=shop&tbo=u",
                                     2,
                                     "https://www.syssoft.ru/FlipBuilder/Android-Book-App-Maker/918435/",
                                     "(18+) Android Book App Maker 100 Licenses Арт. FLPB19007334",
                                     374306.40,
                                     "RUB"
                                 ],
                                 [
                                     "snow_maiden_outfit.html",
                                     "https://google.ru/search?q=%D0%BA%D0%BE%D1%81%D1%82%D1%8E%D0%BC+%D1%81%D0%BD%D0%B5%D0%B3%D1%83%D1%80%D0"
                                     "%BE%D1%87%D0%BA%D0%B8+%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C&hl=uk&source=univ&tbm=shop&tbo=u",
                                     9,
                                     "https://google.ru/aclk?sa=l&ai=DChcSEwiCtpOFwM3xAhVOAYsKHUVMDY0YABAHGgJlZg&sig"
                                     "=AOD64_0SeM0FafomwMjEW2oMesU4Uzz1DQ&ctype=5&rct=j&q=&ved=2ahUKEwjFtYqFwM3xAhVysYsKHR0JBKEQ5bgDegQIAxBf"
                                     "&adurl=",
                                     "Карнавальный костюм Адам и Ева",
                                     969,
                                     "UAH"
                                 ]
                             ])
    def test_parser_desktop_market_ads_right(self, filename, expected_url, expected_sitelink_count,
                                             expected_sitelink_url, expected_sitelink_title, expected_price,
                                             expected_currency):
        parsed = self.parse_file("desktop", filename)
        components = parsed["components"]
        ads_components = [component for component in components
                          if component["componentInfo"].get("wizardType") == WizardTypes.WIZARD_SHOPPING]
        assert len(ads_components) == 1
        ads = ads_components[0]
        self._check_ads(ads, Alignments.RIGHT, expected_url, expected_sitelink_count, expected_sitelink_url,
                        expected_sitelink_title, expected_price, expected_currency)

    def test_parser_touch_market_ads_top(self):
        parsed = self.parse_file("touch", "buy_iphone_spb.html")
        components = parsed["components"]
        ads = components[0]
        self._check_ads(ads,
                        Alignments.TOP,
                        "https://google.ru/search?q=iphone+11+pro+64gb+%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C+%D0%B2"
                        "+%D1%81%D0%BF%D0%B1&hl=ru&prmd=sniv&source=univ&tbm=shop",
                        5,
                        "https://google.ru/aclk?sa=l&ai=DChcSEwi4wPnPxs3xAhWK13cKHT_ICGkYABAEGgJlZg&ae=2&sig"
                        "=AOD64_2RUKpp-tVi_18XZ-C8FgUQOqStxQ&ctype=5&q=&ved"
                        "=2ahUKEwi0j_HPxs3xAhWKlIsKHSaSCVAQwg96BAgBEBs&adurl=",
                        "Смартфон Apple iPhone 11 Pro 64Gb Серебристый",
                        77990,
                        "RUB")

    def test_parser_touch_market_ads_bottom_old_price(self):
        parsed = self.parse_file("touch", "glasses_bottom.html")
        components = parsed["components"]
        ads_components = [component for component in components
                          if component["componentInfo"].get("wizardType") == WizardTypes.WIZARD_SHOPPING]
        assert len(ads_components) == 1
        ads = ads_components[0]
        self._check_ads(ads,
                        Alignments.BOTTOM,
                        "https://google.ru/search?q=%D0%BE%D1%87%D0%BA%D0%B8+%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C&hl"
                        "=ru&prmd=smivn&source=univ&tbm=shop",
                        5,
                        "https://google.ru/aclk?sa=l&ai=DChcSEwiEoLyfldrxAhWKxLIKHSwjB6oYABAFGgJscg&ae=2&sig"
                        "=AOD64_2edCy6SU8AFYuDge1AVMLd8OnKiw&ctype=5&q=&ved"
                        "=2ahUKEwiFk7WfldrxAhVwsYsKHTy4BNIQ8w56BAgCEBM&adurl=",
                        "Versace VE4361 GB1/87 [VE4361 GB1/87]",
                        14136,
                        "RUB")
        assert ads["site-links"][1]["json.prices"][0].get("isOld")
        assert ads["site-links"][1]["json.prices"][0].get("price") == 1999

    def _check_ads(self, ads, alignment, expected_url, expected_sitelink_count, expected_sitelink_url,
                   expected_sitelink_title, expected_price, expected_currency):
        assert ads["componentInfo"] == {
            "type": ComponentTypes.ADDV,
            "alignment": alignment,
            "wizardType": WizardTypes.WIZARD_SHOPPING
        }
        assert ads["componentUrl"]["pageUrl"] == expected_url
        site_links = ads["site-links"]
        assert len(site_links) == expected_sitelink_count
        site_link = site_links[0]
        assert site_link["sitelinks"].get("url") == expected_sitelink_url
        assert site_link["sitelinks"].get("title") == expected_sitelink_title
        assert site_link["json.prices"][0].get("price") == expected_price
        assert site_link["json.prices"][0].get("currency") == expected_currency

    def test_parser_desktop_moldova_coronavirus(self):
        parsed = self.parse_file("desktop", "moldova_coronavirus.html")
        components = parsed["components"]
        assert components
        search_results = self._get_search_results(components)
        assert search_results
        assert len(search_results) == 10
        first_sr = search_results[0]
        assert first_sr["componentUrl"]["pageUrl"] == "https://www.kp.md/online/news/4080261/"

    def test_parser_desktop_uno_coronavirus(self):
        parsed = self.parse_file("desktop", "covid_uno.html")
        components = parsed["components"]
        news_wizard_info = {"type": ComponentTypes.WIZARD,
                            "alignment": Alignments.LEFT,
                            "wizardType": WizardTypes.WIZARD_NEWS}
        assert components[0]["componentInfo"] == news_wizard_info
        assert components[2]["componentInfo"] == news_wizard_info

    def test_parser_desktop_main_news_wiz(self):
        parsed = self.parse_file("desktop", "ulianovsk_news.html")
        components = parsed["components"]
        assert components
        assert len(components) == 11
        news = components[0]
        assert news['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT, "wizardType": WizardTypes.WIZARD_NEWS}
        assert news["componentUrl"] == {}

        assert news["text.title"] == 'Главные новости'
        assert news['text.cssSelector'] == "[data-hveid='CAUQAA']"
        site_links = news['site-links']
        assert len(site_links) == 3

        site_link = site_links[0]
        assert site_link['type'] == "SITELINK"
        assert site_link['text.cssSelector'] == "[data-hveid='CBMQAA']"
        site_link_inner = site_link['sitelinks']

        assert site_link_inner['url'] == "https://73online.ru/r/v_ulyanovske_umer_rukovoditel_centra_mediciny_katastrof_konstantin_vdovin-84596"
        assert site_link_inner['title'] == "В Ульяновске умер руководитель центра медицины катастроф Константин Вдовин"

    def test_parser_desktop_playdom_casino(self):
        parsed = self.parse_file("desktop", "playdom_casino.html")
        components = parsed["components"]
        assert len(components) == 31
        first_component = components[0]
        assert first_component["componentUrl"]["pageUrl"] == "https://casino.ru/casino-playdom/"

    def test_parser_touch_top_cards_ads(self):
        parsed = self.parse_file("touch", "umnyasha.html")
        components = parsed["components"]
        assert components
        ads = components[0]

        assert ads['componentInfo'] == {'type': ComponentTypes.ADDV, 'alignment': Alignments.TOP, "wizardType": WizardTypes.WIZARD_SHOPPING}
        assert ads["componentUrl"]["pageUrl"] == 'https://google.ru/search?q=%D1%83%D0%BC%D0%BD%D1%8F%D1%88%D0%B0+%D0%B4%D0%B5%D1%82%D1%81%D0%BA%D0' \
                                                 '%B8%D0%B9+%D1%80%D0%B0%D0%B7%D0%B2%D0%B8%D0%B2%D0%B0%D1%8E%D1%89%D0%B8%D0%B9&hl=ru&prmd=ivsn' \
                                                 '&source=univ&tbm=shop'

        assert ads["text.title"] == 'Реклама·Результаты по запросу "умняша детский развивающий"'
        assert ads['text.cssSelector'] == "[data-hveid='CBEQCg']"
        site_links = ads['site-links']
        assert len(site_links) == 3

        site_link = site_links[0]
        assert site_link['type'] == "SITELINK"
        assert site_link['text.cssSelector'] == "[data-hveid='CBEQDg']"
        site_link_inner = site_link['sitelinks']

        assert site_link_inner['url'] == "https://google.ru/aclk?sa=l&ai=DChcSEwib8-Smy-PsAhW6IK0GHRFOCzoYABADGgJwdg&ae=2&sig=" \
                                         "AOD64_3iVuSTTf067E8-Bx3OGa-IM3hjZA&ctype=5&q=&ved=2ahUKEwj3tt-my-PsAhVCjp4KHTlDAe8Qwg96BAgREA8&adurl="
        assert site_link_inner['title'] == "Игровой смартфончик Пчёлка Умняша"

    def test_parser_touch_bottom_cards_ads(self):
        parsed = self.parse_file("touch", "birthday_cards.html")
        components = parsed["components"]
        assert components
        ads = components[-2]

        assert ads['componentInfo'] == {'type': ComponentTypes.ADDV, 'alignment': Alignments.BOTTOM, "wizardType": WizardTypes.WIZARD_SHOPPING}
        assert ads["componentUrl"]["pageUrl"] == 'https://google.ru/search?q=%D0%BE%D1%82%D0%BA%D1%80%D1%8B%D1%82%D0%BA%D0%B8+%D1%81+' \
                                                 '%D0%B4%D0%BD%D0%B5%D0%BC+%D1%80%D0%BE%D0%B6%D0%B4%D0%B5%D0%BD%D0%B8%D1%8F&hl=ru&prmd=inv' \
                                                 '&source=univ&tbm=shop'

        assert ads["text.title"] == 'Реклама·Результаты по запросу "открытки с днем рождения"'
        assert ads['text.cssSelector'] == "[data-hveid='CCMQDQ']"
        site_links = ads['site-links']
        assert len(site_links) == 3

        site_link = site_links[0]
        assert site_link['type'] == "SITELINK"
        assert site_link['text.cssSelector'] == "[data-hveid='CCMQEQ']"
        site_link_inner = site_link['sitelinks']

        assert site_link_inner['url'] == 'https://google.ru/aclk?sa=l&ai=DChcSEwj0vanLqubsAhUjPa0GHQD3APkYABADGgJwdg&ae=1&' \
                                         'sig=AOD64_1DB-ANZUf6FN4SGwVSShZOyqP5uQ&ctype=5&q=&ved=2ahUKEwigz6LLqubsAhUAHzQIHS' \
                                         '_-D_gQ8w56BAgjEBI&adurl=https://roots-store.ru/item/bouquet_16/' \
                                         '%3Fyagla%3D%26gclid%3DEAIaIQobChMI9L2py6rm7AIVIz2tBh0A9wD5EAsYASABEgJd2vD_BwE'
        assert site_link_inner['title'] == 'bouquet 16'

    def test_parser_touch_wiz_orgmn(self):
        parsed = self.parse_file("touch", "buy_rope_spb.html")
        components = parsed["components"]
        assert components
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_ORGMN)[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT, "wizardType": WizardTypes.WIZARD_ORGMN}
        assert wizard["componentUrl"]["pageUrl"] == 'https://google.ru/search?hl=ru&tbs=lf:1,lf_ui:10&q=%D0%B2%D0%B5%D1%80%D0%B5%D0%B2%D0%BA%D0%B0' \
                                                    '+%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C+%D1%81%D0%BF%D0%B1&ibp=gwp;0,6&rflfq=1'

        assert wizard["text.title"] == 'Веревка Купить'
        assert wizard['text.cssSelector'] == "[data-hveid='CCsQAA']"

    def test_parser_touch_wiz_translate(self):
        parsed = self.parse_file("touch", "snake_in_english.html")
        components = parsed["components"]
        assert components
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_TRANSLATE)[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT, "wizardType": WizardTypes.WIZARD_TRANSLATE}
        assert wizard['text.cssSelector'] == "[data-hveid='CAEQAQ']"

    def test_parser_touch_wiz_track(self):
        parsed = self.parse_file("touch", "track.html")
        components = parsed["components"]
        assert components
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_UNKNOWN)[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT, "wizardType": WizardTypes.WIZARD_UNKNOWN}
        assert wizard["componentUrl"] == {}

        assert wizard["text.title"] == 'Отслеживайте посылку'
        assert wizard['text.cssSelector'] == "[data-hveid='CAMQAA']"

    def test_parser_touch_entity_carousel(self):
        parsed = self.parse_file("touch", "muzei_v_minske.html")
        components = parsed["components"]
        assert components
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_KNOWLEDGE_GRAPH)[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.TOP, "wizardType": WizardTypes.WIZARD_KNOWLEDGE_GRAPH}
        assert wizard["componentUrl"]["pageUrl"] == 'https://google.ru/search?q=%D0%9C%D1%83%D0%B7%D0%B5%D0%B8&npsic=0'

        assert wizard["text.title"] == 'Музеи/Минск'
        assert wizard['text.cssSelector'] == "[data-hveid='CAQQKQ']"

    def test_parser_touch_entity_carousel_with_map(self):
        parsed = self.parse_file("touch", "san_francisco.html")
        components = parsed["components"]
        assert components
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_KNOWLEDGE_GRAPH)[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT, "wizardType": WizardTypes.WIZARD_KNOWLEDGE_GRAPH}
        assert wizard["componentUrl"]["pageUrl"] == 'https://google.ru/travel/things-to-do/see-all?g2lb=2502548,4258168,4306835,4317915,4328159,' \
                                                    '4371334,4401769,4419364,4429192,4431137,4433754,4463263,4463666,4464463,4466981,4270859,' \
                                                    '4284970,4455785&hl=ru-RU&gl=ru&un=1&dest_mid=/m/0d6lp&dest_state_type=sattd&dest_src=ts&' \
                                                    'rf=EhAKDE1PU1RfVklTSVRFRCgB'

        assert wizard['text.title'] == "Сан-Франциско: достопримечательности"
        assert wizard['text.cssSelector'] == "[data-hveid='CAQQCA']"

    def test_parser_touch_entity_list(self):
        parsed = self.parse_file("touch", "fight_night.html")
        components = parsed["components"]
        assert components
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_KNOWLEDGE_GRAPH)[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.TOP,
                                           "wizardType": WizardTypes.WIZARD_KNOWLEDGE_GRAPH}
        assert wizard["componentUrl"]["pageUrl"] == 'https://google.ru/search?q=Fight+Night+Champion'

        assert wizard['text.title'] == "Fight Night"
        assert wizard['text.cssSelector'] == "[jscontroller='kHf6sf']"

    def test_parser_touch_exchange(self):
        parsed = self.parse_file("touch", "dollor_kurs.html")
        components = parsed["components"]
        assert components
        wizard = components[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT,
                                           "wizardType": WizardTypes.WIZARD_CONVERTER}
        assert wizard['text.cssSelector'] == "[data-hveid='CAIQAA']"

    def test_parser_desktop_exchange(self):
        parsed = self.parse_file("desktop", "120_dollor.html")
        components = parsed["components"]
        assert components
        wizard = components[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT,
                                           "wizardType": WizardTypes.WIZARD_CONVERTER}

    def test_parser_desktop_stocks(self):
        parsed = self.parse_file("desktop", "uber_share.html")
        components = parsed["components"]
        assert components
        wizard = components[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT,
                                           "wizardType": WizardTypes.WIZARD_STOCKS}
        assert wizard['text.cssSelector'] == "[data-hveid='CBAQAA']"

    def test_parser_touch_stocks(self):
        parsed = self.parse_file("touch", "sber_share.html")
        components = parsed["components"]
        assert components
        wizard = components[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT,
                                           "wizardType": WizardTypes.WIZARD_STOCKS}
        assert wizard['text.cssSelector'] == "[jscontroller='ij8bP']"

    def test_parser_touch_wsf(self):
        parsed = self.parse_file("touch", "mass.html")
        components = parsed["components"]
        assert len(components) > 1
        wizard = components[1]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT, "wizardType": WizardTypes.WIZARD_UNKNOWN}
        assert wizard["componentUrl"] == {}

        assert wizard['text.cssSelector'] == "[jscontroller='OClNZ']"

    def test_parser_touch_wsf_like(self):
        parsed = self.parse_file("touch", "grape_beauty.html")
        components = parsed["components"]
        assert components
        component = components[0]

        assert component['componentInfo'] == {'type': ComponentTypes.SEARCH_RESULT, 'alignment': Alignments.LEFT}
        assert component["componentUrl"]["pageUrl"] == "https://vinedresser.info/sorta/436-krasotka/amp"
        assert component["componentUrl"]["viewUrl"] == "vinedresser.info › 436-krasotka"

        assert component['text.title'] == "Виноград Красотка (Павловского Е.Г.) - фото, видео, описание ..."
        assert component['text.cssSelector'] == "[data-hveid='CAoQAA']"

    @pytest.mark.parametrize("path, css_selector", [
        ("touch/pogoda.html", "[data-hveid='CAEQAA']"),
    ])
    def test_touch_weather_pogoda(self, path, css_selector):
        parsed = self.parse_file(path)
        components = parsed["components"]
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_WEATHER)[0]
        assert wizard
        assert wizard['text.cssSelector'] == css_selector

    @pytest.mark.parametrize("path, css_selector", [
        ("touch/pogoda.html", "[data-hveid='CBQQAA']"),
    ])
    def test_touch_news_pogoda(self, path, css_selector):
        parsed = self.parse_file(path)
        components = parsed["components"]
        wizard = self._get_wizard_components(components, WizardTypes.WIZARD_NEWS)[0]
        assert wizard
        assert wizard['text.cssSelector'] == css_selector

    def test_parser_touch_wiz_dict(self):
        parsed = self.parse_file("touch", "society_is.html")
        components = parsed["components"]
        assert components
        wizard = components[0]

        assert wizard['componentInfo'] == {'type': ComponentTypes.WIZARD, 'alignment': Alignments.LEFT, "wizardType": WizardTypes.WIZARD_UNKNOWN}
        assert wizard["componentUrl"]["pageUrl"] == 'https://languages.oup.com/google-dictionary-ru'

        assert wizard["text.title"] == 'ÓБЩЕСТВО'
        assert wizard['text.cssSelector'] == "[jscontroller='Fk55qd']"

    def test_parser_touch_wiz_missprint(self):
        parsed = self.parse_file("touch", "scarlet_witch.html")
        assert parsed["components"][0]["componentInfo"]["wizardType"] == WizardTypes.WIZARD_MISPRINT

    def test_parser_desktop_wesu(self):
        parsed = self.parse_file("desktop", "wesu.html")
        assert parsed["components"][0]["componentInfo"]["wizardType"] == WizardTypes.WIZARD_ENTITY_SEARCH_UPPER

    @pytest.mark.parametrize("text, expected_numdoc", [
        ("About 3,930,000,000 results (0.73 seconds)", 3_930_000_000),
        ("Yaklaşık 55.400.000 sonuç bulundu (0,61 saniye)", 55_400_000),
        ("Приблизна кількість результатів: 4 780 000 (0,56 сек.)", 4_780_000),
        ("About 531,000 results (0.39 seconds)", 531_000),
        # these have non-breaking spaces:
        ("Результатов: примерно 3 020 000 000 (0,71 сек.)", 3_020_000_000),
        ("Приблизна кількість результатів: 4 480 000 (0,46 сек.)", 4_480_000),

    ])
    def test_extract_numdoc(self, text, expected_numdoc):
        assert self.get_parser()._extract_numdoc(text) == expected_numdoc

    @pytest.mark.parametrize("filename, expected_numdoc", [
        ("cat.html", 3_020_000_000),
        ("cat_ua.html", 4_480_000),
        ("cat_tr.html", 64200000),
        ("python.html", 334_000_000),
        ("js_push.html", 187_000_000),  # this one has linebreaks
    ])
    def test_numdoc(self, filename, expected_numdoc):
        parsed = self.parse_file("desktop", filename)
        assert "long.docsFound" in parsed, f"The 'long.docsFound' key is missing for {filename}."
        numdoc = parsed["long.docsFound"]
        assert numdoc == expected_numdoc

    def test_document_age_desktop(self):
        parsed = self.parse_file("desktop", "biden.html")
        search_results = self._get_search_results(parsed["components"])
        assert search_results[0]["webadd"]["isFastRobotSrc"] is False
        assert search_results[1]["long.documentAge"] == 19 * 60 * 60 * 1000
        assert search_results[1]["webadd"]["isFastRobotSrc"] is True

    def test_document_age_touch(self):
        parsed = self.parse_file("touch", "palace.html")
        search_results = self._get_search_results(parsed["components"])
        assert search_results[0]["long.documentAge"] == 17 * 60 * 60 * 1000
        assert search_results[0]["webadd"]["isFastRobotSrc"] is True

    @pytest.mark.parametrize("directory, filename, start, end, duration", [
        ("desktop", "shoelaces_desktop.html", "0:08", "2:02", "Рекомендуемый клип · 46 сек."),
        ("touch", "shoelaces_android.html", "0:40", "2:02", "Рекомендуемый клип · 40 сек."),
    ])
    def test_video_with_dedicated_time(self, directory, filename, start, end, duration):
        parsed = self.parse_file(directory, filename)
        wizard = parsed["components"][0]
        assert self._check_wizard_type(wizard, WizardTypes.WIZARD_SUGGEST_FACT)
        video_with_dedicated_time_key = "json.video_with_dedicated_time"
        assert video_with_dedicated_time_key in wizard
        dedicated_time = wizard[video_with_dedicated_time_key]
        assert dedicated_time["start_time"] == start
        assert dedicated_time["end"] == end
        assert dedicated_time["duration"] == duration

    @pytest.mark.parametrize("filename, url_prefix, title", [
        ("twitter_musk.html", "https://twitter.com/elonmusk", "Elon Musk (@elonmusk) · Твиттер"),
        ("twitter_rogozin.html", "https://twitter.com/Rogozin", "РОГОЗИН (@Rogozin) · Твиттер"),
    ])
    def test_parser_twitter(self, filename, url_prefix, title):
        parsed = self.parse_file("desktop", filename)
        components = parsed["components"]
        search_results = self._get_search_results(components)
        first_sr = search_results[0]
        assert first_sr["componentUrl"]["pageUrl"].startswith(url_prefix)
        assert first_sr["text.title"] == title

    def test_dont_starve(self):
        parsed = self.parse_file("touch", "dont_starve.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 8

    def test_kpop_new(self):
        parsed = self.parse_file("desktop", "kpop_new.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 10

    def test_kpop_old(self):
        parsed = self.parse_file("desktop", "kpop_old.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 10

    def test_star_wars_new(self):
        parsed = self.parse_file("desktop", "star_wars_new.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 9

    def test_star_wars_old(self):
        parsed = self.parse_file("desktop", "star_wars_old.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 9

    def test_shined_new(self):
        parsed = self.parse_file("touch", "shined_new.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 8

    def test_shined_old(self):
        parsed = self.parse_file("touch", "shined_old.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 9

    def test_peace_new(self):
        parsed = self.parse_file("touch", "peace_new.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 8

    def test_peace_old(self):
        parsed = self.parse_file("touch", "peace_old.html")
        search_results = self._get_search_results(parsed["components"])
        assert len(search_results) == 9

    ########################
    # Preparer tests below #
    ########################

    def _prepare_responses(self):
        def request_callback(request):
            REGION_COORDINATES = {
                self.PITER_ID: {
                    "latitude": 59.938951,
                    "longitude": 30.315635
                },
                self.RIGA_ID: {
                    "latitude": 56.94684,
                    "longitude": 24.106075
                },
                self.STAMBUL_ID: {
                    "latitude": 41.008857,
                    "longitude": 28.96747
                },
                self.KIEV_ID: {
                    "latitude": 50.450458,
                    "longitude": 30.52346
                },
                self.BILBAO_ID: {
                    "latitude": 43.263659,
                    "longitude": -2.938075
                },
                self.MOSCOW_ID: {
                    "latitude": 55.753215,
                    "longitude": 37.622504
                }
            }
            for region_id in REGION_COORDINATES:
                if request.url == self.GEOBASE_URL_TEMPLATE.format(region_id):
                    return 200, {}, json.dumps(REGION_COORDINATES[region_id])
            error_payload = {"error": f"(std::runtime_error) Impl::RegIdxById({region_id}) - unknown id", "code": 400}
            return 400, {}, json.dumps(error_payload)

        for region_id in [self.PITER_ID, self.RIGA_ID, self.STAMBUL_ID, self.KIEV_ID, self.MOSCOW_ID, self.UNKNOWN_ID]:
            responses.add_callback(
                responses.GET,
                self.GEOBASE_URL_TEMPLATE.format(region_id),
                callback=request_callback,
                content_type="application/json"
            )

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("cat_piter_input.json", "cat_piter_output.json"),
        ("cat_kiev_input.json", "cat_kiev_output.json"),
        ("cat_riga_input.json", "cat_riga_output.json"),
        ("cat_stambul_input.json", "cat_stambul_output.json"),
        ("cat_bilbao_input.json", "cat_bilbao_output.json"),
        ("cat_bilbao_android_input.json", "cat_bilbao_android_output.json"),
    ])
    @pytest.mark.freeze_time('2020-12-10')
    @responses.activate
    def test_preparer(self, input_filename, expected_output_filename):
        self._prepare_responses()
        self.compare_preparer_output(input_filename, expected_output_filename, host="google.com")

    @pytest.mark.parametrize("input_filename, expected_accepted_language", [
        ("cat_piter_input.json", ["ru,en;q=0.9"]),
        ("cat_kiev_input.json", ["ru,en;q=0.9"]),
        ("cat_riga_input.json", ["ru,en;q=0.9"]),
        ("cat_stambul_input.json", ["tr"]),
        ("cat_bilbao_input.json", ["es"]),  # This one is not based on country
    ])
    def test_prepare_accept_language(self, input_filename, expected_accepted_language):
        bq = self.get_basket_query(input_filename)
        assert self.get_parser()._prepare_accept_language(bq) == expected_accepted_language

    @pytest.mark.parametrize("input_filename, expected_hl", [
        ("cat_piter_input.json", "ru"),
        ("cat_kiev_input.json", "uk"),
        ("cat_riga_input.json", "ru"),
        ("cat_stambul_input.json", "tr"),
        ("cat_bilbao_input.json", "es"),
    ])
    def test_get_hl(self, input_filename, expected_hl):
        bq = self.get_basket_query(input_filename)
        assert self.get_parser()._get_hl(bq) == expected_hl

    @pytest.mark.parametrize("geobase_coordinate, e7_coodinate", [
        (53.902496, 539024960),
        (-53.902496, -539024960),
        (53.902, 539020000),
    ])
    def test_format_coordinate(self, geobase_coordinate, e7_coodinate):
        assert self.get_parser()._format_coordinate(geobase_coordinate) == e7_coodinate

    @pytest.mark.parametrize("device,radius", [
        ("DESKTOP", GoogleWebParser.COORDINATE_DEFAULT_BIG_RADIUS),
        ("ANDROID", GoogleWebParser.COORDINATE_DEFAULT_SMALL_RADIUS)
    ])
    def test_prepare_cookie_payload(self, device, radius):

        now = datetime(2020, 12, 11, 7, 4, 30, 892000)
        ts_now = str(int(now.timestamp() * 1000)) + "000"
        lat, lon = 539024960, 275614810
        assert self.get_parser()._prepare_cookie_payload(
            lat=lat,
            lon=lon,
            device=device,
            now=now,
        ) == textwrap.dedent(f"""\
            role:1
            producer:12
            provenance:6
            timestamp:{ts_now}
            latlng{{
            latitude_e7:{lat}
            longitude_e7:{lon}
            }}
            radius:{radius}
            """)

    @responses.activate
    def test_unknown_region_has_0_0_coordinates(self):
        self._prepare_responses()
        assert self.get_parser().get_region_latitude_longitude(self.UNKNOWN_ID) == (0, 0)

    @pytest.mark.parametrize("results_per_page, must_be_set", [
        (20, True),
        (30, True),
        (40, True),
        (50, True),
        (100, True),
        (10, False),
        (14, False),
        (3, False),
        (99, False),
        (23, False),
    ])
    @responses.activate
    # test for this https://stackoverflow.com/questions/17660910/getting-more-search-results-per-page-via-url
    def test_prepare_results_per_page(self, results_per_page, must_be_set):
        self._prepare_responses()
        bq = self.get_basket_query("results_per_page_input.json")
        prepared_query = self.prepare(
            query=bq,
            host="google.ru",
            additional_parameters={'numdoc': results_per_page}
        )
        assert (f"num={results_per_page}" in prepared_query.get("uri")) == must_be_set

    ###############################
    # Local functions tests below #
    ###############################

    def test_google_redirect_url(self):
        redirect_urls_tests = {
            "https://google.ru/url?q=https://alcorehab.ru/articles/skolko-vyvodyatsya-narkotiki-iz-organizma/":
                "https://alcorehab.ru/articles/skolko-vyvodyatsya-narkotiki-iz-organizma/",
            "https://google.ru/url?q=https://narcorehab.com/articles/skolko-narkotiki-vuvodyatsya-iz-organizma/amp/":
                "https://narcorehab.com/articles/skolko-narkotiki-vuvodyatsya-iz-organizma/amp/",
            "https://google.ru/url?q=https://ugodie.ru/vajno-znat/narkomania/skolko-narkotik-derjitsya-v-krovi/":
                "https://ugodie.ru/vajno-znat/narkomania/skolko-narkotik-derjitsya-v-krovi/",
            "https://google.ru/url?q=https://helix.ru/kb/item/19-010":
                "https://helix.ru/kb/item/19-010",
            "https://google.ru/url?q=https://bykovo-media.ru/chitat-vsyu-lentu/narkotiki-v-krovi-skolko-derzhatsya-kak-opredelyayut-ikh-nalichie":
                "https://bykovo-media.ru/chitat-vsyu-lentu/narkotiki-v-krovi-skolko-derzhatsya-kak-opredelyayut-ikh-nalichie",
            "https://google.ru/url?q=https://invitromed.am/analizes/for-doctors/868/16657/":
                "https://invitromed.am/analizes/for-doctors/868/16657/"
        }

        for google_redirect_url, redirect_url in redirect_urls_tests.items():
            assert GoogleWebParser.get_url_without_redirect_new_mobile(google_redirect_url) == redirect_url
