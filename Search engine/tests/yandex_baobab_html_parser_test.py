import re

import pytest

import json

import responses

from base_parsers import JSONSerpParser
from test_utils import TestParser, compare_urls

UNO_URL_PARAMETERS = {"config": {"uno_url": True}}

Alignments = JSONSerpParser.MetricsMagicNumbers.Alignments
WizardTypes = JSONSerpParser.MetricsMagicNumbers.WizardTypes
ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes

from yandex_baobab.yandex_baobab_html_parser import YandexBaobabHTMLParser


class TestYandexBaobabHTMLParser(TestParser):
    _parser_class = YandexBaobabHTMLParser

    def test_component_count(self):
        components = self.parse_file("keira_with_baobab.html")['components']
        assert 14 == len(components)

    def test_extract_baobab(self):
        html = self._read_file("keira_with_baobab.html")
        extracted_baobab_string = self._get_parser_class().extract_baobab(html)
        expected_baobab_string = self._read_file("keira_baobab.json")
        assert extracted_baobab_string == expected_baobab_string

    def test_extract_baobab_exception(self):
        with pytest.raises(ValueError) as e:
            self._get_parser_class().extract_baobab("nothing here")
        assert self._get_parser_class().MISSING_BAOBAB_ERROR_MESSAGE == str(e.value)

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("0_input.json", "0_expected_output.json")
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename)

    def test_url_preparer_with_ssr(self):
        basket_query = self._read_json_file("preparer", "0_input.json")
        ssr = self._read_json_file("preparer", "merged_request.json")
        expected_url = self._read_file("preparer", "0_expected_url_with_ssr.txt", strip=True)
        preparer = self._get_parser_class()()
        compare_urls(preparer._prepare_url(basket_query, "yandex.ru", {'ssr': ssr}), expected_url)

    def test_reqid(self):
        source = self._read_file("keira_with_baobab.html")
        parser = self._get_parser_class()()
        expected_reqid = "1556530260209569-841159735975802286500035-vla1-3312"
        assert parser.extract_reqid(source) == expected_reqid

    def test_wizard_auto(self):
        component = self.parse_file('wizard_auto.html')['components'][0]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_AUTO

    def test_baobab_with_quote(self):
        html = self._read_file("dude_instagram_with_quote.html")
        extracted_baobab_string = self._get_parser_class().extract_baobab(html)
        expected_baobab_string = self._read_file("dude_instagram_with_quote.json")
        assert extracted_baobab_string == expected_baobab_string

    def test_baobab_instagram(self):
        extracted_baobab_string = self.parse_file("dude_instagram_with_quote.html")
        assert extracted_baobab_string["components"][0]["text.snippet"]

    def test_baobab_some_meta_info(self):
        extracted_baobab_string = self.parse_file("some_meta_info.html")
        assert extracted_baobab_string

    def test_wizard_entity_search_upper(self):
        component = self.parse_file('wizard_esu.html')['components'][0]
        assert component['componentInfo']['type'] == ComponentTypes.WIZARD
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_ENTITY_SEARCH_UPPER

    def test_baobab_slices(self):
        extracted_baobab_string = self.parse_file("slices_baobab.html")
        a = [value["json.slices"] for value in extracted_baobab_string["components"] if value.get("json.slices")]
        assert a == [["WIZIMAGES"], ["VIDEOWIZ"], ["VHS_WIZARD"], ["ENTITY_SEARCH"]]

    def test_baobab_slices_filtration(self):
        assert self.get_parser().parse_slices('VHS_WIZARD:20.0|*:smth|WEB_NAV:1') == ['VHS_WIZARD']

    def test_bna(self):
        component = self.parse_file('bno.html')['components'][0]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_BNA
        assert len(component['site-links']) == 4

    def test_baobab_proxima_predict(self):
        extracted_baobab_string = self.parse_file("slices_baobab.html")
        assert extracted_baobab_string["components"][1].get("text.ProximaPredict", "") == "0.7427"

    def test_baobab_search_runtime_info(self):
        extracted_baobab_string = self.parse_file("web_serp_with_markers_20200924.html")
        assert extracted_baobab_string["components"][4].get("text.ProximaPredict", "") == "1.429"
        data_example = extracted_baobab_string["components"][4].get("json.SearchRuntimeInfo", {})["MetaFeatures"][0]
        assert "Name" in data_example
        data_example["Name"] = "removed"
        assert json.dumps(
            data_example
        ) == json.dumps({
            "Id": 522,
            "Name": "removed",
            "Value": 0.6261005998
        })

    def test_baobab_search_runtime_info2(self):
        extracted_baobab_string = self.parse_file("web_serp_with_signal_dumper_markers_20201204.html")
        data_example = extracted_baobab_string["components"][4].get("json.SearchRuntimeInfo", {})["MetaRearrFeatures"][0]
        assert "Name" in data_example
        data_example["Name"] = "removed"
        assert json.dumps(
            data_example
        ) == json.dumps({
            "Id": 9,
            "Name": "removed",
            "Value": 0.4572696984
        })

    def test_web_kuka_parse(self):
        extracted_baobab_string = self.parse_file(
            "web_serp_with_markers_20200924.html",
            additional_parameters={"config": {"ParseWebKuka": True}}
        )
        assert extracted_baobab_string["json.WebKukaInfo"].get("KukaParseSuccess", False)
        assert not extracted_baobab_string["json.WebKukaInfo"].get("IsCachedAtMiddleSearch", True)
        assert extracted_baobab_string["json.WebKukaInfo"].get("UnanswersRatio", None) == 0
        assert extracted_baobab_string["json.WebKukaInfo"].get("DegradationRatio", None) == 0

    def test_web_kuka_parse_documents_stats(self):
        extracted_baobab_string = self.parse_file(
            "test_BUKI-3173.html",
            additional_parameters={"config": {"ParseWebKuka": True}}
        )
        assert extracted_baobab_string["json.WebKukaInfo"].get("KukaParseSuccess", False)
        assert extracted_baobab_string["json.WebKukaInfo"].get("DocumentsStats", {}).get("Stages", {})

    def test_web_sitelinks_problem(self):
        extracted_baobab_string = self.parse_file(
            "sitelinks_problem.html",
            additional_parameters={"config": {"ParseWebKuka": True}}
        )
        assert extracted_baobab_string["json.WebKukaInfo"].get("KukaParseSuccess", False)

    def test_market_url(self):
        url = self.parse_file('market_url.html')['components'][2]['componentUrl']['pageUrl']
        assert url == 'https://market.yandex.kz/product--telefon-philips-xenium-x1560/10686989'

    def test_baobab_query_proxima_predict(self):
        extracted_baobab_string = self.parse_file("slices_baobab.html")
        assert extracted_baobab_string["json.queryParams"]["ProximaPredict"] == \
            {
                'Dcg10Global': '2.12225',
                'Dcg10Grouping': '2.10049',
                'Dcg5Global': '1.69413',
                'UsedModel': 'fml-mn-547520',
                'Dcg5Grouping': '1.6927'
            }

    def test_baobab_query_fresh_detector(self):
        extracted_baobab_string = self.parse_file("slices_baobab.html")
        assert extracted_baobab_string["json.queryParams"]["FreshDetector"] == \
            {
                'PrsSourceRatioFromQuickRt': '0',
                'PrsSourceRatioFromQuick': '0',
                'PrsSourceRatioFromCallisto': '0',

                'PrsAgeRatio_300': '0',
                'PrsAgeRatio_600': '0',
                'PrsAgeRatio_3600': '0',
                'PrsAgeRatio_7200': '0',
                'PrsAgeRatio_14400': '0',
                'PrsAgeRatio_28800': '0',
                'PrsAgeRatio_86400': '0',
                'PrsAgeRatio_259200': '0',

                'PrsSize': '152',
            }

    def test_calories_url(self):
        assert self.parse_file('calories_url.html')['components'][0]['componentUrl']['pageUrl'] == \
            'http://health-diet.ru/base_of_food/sostav/81.php'

    def test_distance_url(self):
        assert self.parse_file('distance.html')['components'][0]['componentUrl']['pageUrl'] == \
            'https://maps.yandex.kz/?rtext=43.238293,76.945465~26.913249,75.805207&ll=76.945465,43.238293&z=10'

    def test_lyrics_url(self):
        assert self.parse_file('lyrics.html')['components'][0]['componentUrl']['pageUrl'] == \
            'http://teksty-pesenok.ru/rus-aleksej-makarov/tekst-pesni-ya-vernus/1731104'

    def test_music_url(self):
        assert self.parse_file('music.html')['components'][0]['componentUrl']['pageUrl'] == \
            'https://music.yandex.by/album/3663896/track/30262414?from=serp'

    def test_baobab_music_wizard_link(self):
        extracted_baobab_string = self.parse_file('music_wizard_link.html')
        assert extracted_baobab_string['components'][11]['componentUrl']['pageUrl'] == \
            'http://music.yandex.ru/album/88436/track/15672'

    def test_ento(self):
        additional = {'userdata': {'text': 'фильмы смотреть'}}
        component = self.parse_file('ento.html', additional_parameters=additional)['components'][0]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_ENTITY_SEARCH_UPPER
        assert component['componentInfo']['alignment'] == Alignments.TOP
        assert component['text.ento'] == '0oCgpydXc3MTA3NDY4Egtsc3QucmVjZmlsbRgCQh3RhNC40LvRjNC80Ysg0YHQvNC-0YLRgNC10YLRjEZ0LY0'
        assert component['componentUrl']['pageUrl'] == \
            'https://yandex.ru/search/entity/touch/?no-tests=1&noredirect=1&lr=213&' \
            'text=%D1%84%D0%B8%D0%BB%D1%8C%D0%BC%D1%8B+%D1%81%D0%BC%D0%BE%D1%82%D1%80%D0%B5%D1%82%D1%8C&' \
            'ento=0oCgpydXc3MTA3NDY4Egtsc3QucmVjZmlsbRgCQh3RhNC40LvRjNC80Ysg0YHQvNC-0YLRgNC10YLRjEZ0LY0' \
            '&content-handler=entity&show-only-content=1'

    def test_entref(self):
        additional = {'userdata': {'text': 'фильмы смотреть'}}
        components = self.parse_file('entref.html', additional_parameters=additional)['components']
        component = components[14]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_COMPILATIONS
        assert component['text.ento'] == '0oGAJCEdGE0LjQu9GM0LzRiyAyMDE4zWjCbQ'
        assert component['componentUrl']['pageUrl'] == \
            'https://yandex.ru/search/entity/touch/?no-tests=1&noredirect=1&lr=213&' \
            'text=%D1%84%D0%B8%D0%BB%D1%8C%D0%BC%D1%8B+%D1%81%D0%BC%D0%BE%D1%82%D1%80%D0%B5%D1%82%D1%8C&' \
            'ento=0oGAJCEdGE0LjQu9GM0LzRiyAyMDE4zWjCbQ' \
            '&content-handler=entity&show-only-content=1'

        assert components[0]['componentInfo']['wizardType'] == WizardTypes.WIZARD_ENTITY_SEARCH_UPPER

    def test_wkg_entref(self):
        additional = {'userdata': {'text': 'он дракон мира и арман'}}
        component = self.parse_file('wkg_ento.html', additional_parameters=additional)['components'][0]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_KNOWLEDGE_GRAPH
        assert component['text.ento'] == '0oCgpydXc1ODAyMDkwGAJCKNC-0L0g0LTRgNCw0LrQvtC9INC80LjRgNCwINC4INCw0YDQvNCw0L2kARRE'
        assert component['componentUrl']['pageUrl'] == \
            'https://yandex.ru/search/entity/touch/?no-tests=1&noredirect=1&lr=213&' \
            'text=%D0%BE%D0%BD+%D0%B4%D1%80%D0%B0%D0%BA%D0%BE%D0%BD+%D0%BC%D0%B8%D1%80%D0%B0+%D0%B8+%D0%B0%D1%80%D0%BC%D0%B0%D0%BD&' \
            'ento=0oCgpydXc1ODAyMDkwGAJCKNC-0L0g0LTRgNCw0LrQvtC9INC80LjRgNCwINC4INCw0YDQvNCw0L2kARRE' \
            '&content-handler=entity&show-only-content=1'

    def test_wizard_adresa_url(self):
        component = self.parse_file("wizard_adresa_url.html")["components"][0]
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_ADRESA
        assert component["componentUrl"]["pageUrl"] == 'https://maps.yandex.by/?' \
            'text=%D0%BD%D0%BE%D1%80%D0%B4%D0%B8%D0%BD&source=wizbiz_new_map_single&z=14&ll=27.587934%2C53.927350&ol=biz&oid=1028189546'

    def test_wizard_adresa_other_url(self):
        component = self.parse_file("wizard_adresa_other_url.html")["components"][5]
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_ADRESA
        assert component["componentUrl"]["pageUrl"] == \
            "https://yandex.ru/maps/org/bekitser/28992281469/?source=wizbiz_new_text_single"

    def test_wizard_adresa_ya_url(self):
        component = self.parse_file("wizard_adresa_ya_url.html")["components"][2]
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_ADRESA
        assert component["componentUrl"]["pageUrl"] == \
               "https://maps.yandex.ru/?text=" \
               "%D0%BC%D0%B8%D1%80%D0%BE%D0%B2%D0%BE%D0%B9%20%D1%81%D1%83" \
               "%D0%B4%20%D0%B2%D0%BE%D1%80%D0%BE%D1%82%D1%8B%D0" \
               "%BD%D1%81%D0%BA%D0%BE%D0%B3%D0%BE%20%D1%80%D0%B0%D0%B9%D0%" \
               "BE%D0%BD%D0%B0&source=wizbiz_new_map_single&z=14" \
               "&ll=45.858290%2C56.056084&ol=biz&oid=1067810378"

    def test_wizard_adresa_ya_url_2(self):
        component = self.parse_file("wizard_adresa_ya_url_2.html")["components"][0]
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_ADRESA
        assert component["componentUrl"]["pageUrl"] == "https://yandex.ru/support/sprav/manage/verified-owner.html"

    def test_adv_url(self):
        component = self.parse_file("windows.html")["components"][0]
        assert component["componentInfo"]["type"] == ComponentTypes.ADDV
        assert component["url.noRedirectUrl"] == "http://vseoknatyt.ru"
        assert component["componentUrl"]["pageUrl"] == "http://yabs.yandex.ru/count/" \
            "WYaejI_zOCi1_H00D1mWHggTvekdwmK0om8GWZWntQ-XNW00000u109mYkhAW0A00U2-nORee8Byg0680OFXwUesa07SXlk47PW1Zggt" \
            "a1wu0QB4oQWJm05Ss06Ifg8Eu06oY8qEw04Ue0Birj4Em08Bs082y0B1rDRY2vW3aOWT_07e1Cx60_W4jCT3Y0MqnqEG1P67UA05cCuK" \
            "g0M_bX6m1R-M4RW5yy8Um0Nm-eK1o0MmXoBqb96E1Q069AW69BW6gWF91jrVgZ-Bu3PkM6Lw9EOhixuTi0U0W9WCk0Uq1F47j3jxxPt0" \
            "USA4We21m8AswwW7W0e1c0hQZmR9gWiGcjMJ36Lw000YMC6bzgC50DaBw0kqnqFm2mM83Agjthu1gGm00000miPbl-WC8-0DWu20G8aE" \
            "Wr59dO0cxjs_gVspaRYRRg0Em8Gzs0u1eG-2eTiCa12QqktXnidWgZl840pG4DMMhr_u40E04PWHlc5wyxbtXaq_iH9Se1F5f7NbF-0J" \
            "yy8Ue1JpmXwe5967UC2DWl05w1G8o1M9aFol1T0Lm8s2y0NO5S6AzkoZZxpyO_2O5j2acVG5eCaMq1QghTw-0O4Nc1U9lBeFg1S17G0q" \
            "v90VmIg3osLxRFxCvNy-In6MDdPqI2ovESaT5sGjlGH0MuElzhKeOcS98FgFnB7HirgAk2tl14OXD1aX2FTHRSo2UxkrCqQb1sYofy_6" \
            "e_XLngFL2346~1?from=www.yandex.ru%3Bsearch%26%23x2F%3B%3Bweb%3B%3B0%3B&q=%D0%BE%D0%BA%D0%BD%D0%B0&etext=" \
            "2202._rW_CufiIsQjmKotQaRjw2Flb2d0aGV2aGZ1ZmF3dGY.aefae4e429fc48ad2ba5c8010b9cf9c7f00428ed"

    def test_big_answer(self):
        component = self.parse_file("wizard_big_answer.html")["components"][0]
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_BIG_ANSWER
        assert component["componentUrl"]["pageUrl"] == "https://kinopoisk.ru/film/844878/watch?from=qa_serp&source=serp&vars=block%3Dbig-answer"

    def test_compilations(self):
        component = self.parse_file("wizard_compilations.html")["components"][11]
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_COMPILATIONS

    def test_misspell(self):
        misspell = self.parse_file("misspell_sug10k.html")["json.MisspellAnswer"]
        misspell_mobile = self.parse_file("misspell_mobile_sug10k.html")["json.MisspellAnswer"]
        expected = {"text.QueryBefore": "путтин", "text.QueryAfter": "путин", "text.Confidence": "sug10000"}
        assert misspell == misspell_mobile == expected

        misspell = self.parse_file("misspell_keyboard.html")["json.MisspellAnswer"]
        misspell_mobile = self.parse_file("misspell_mobile_keyboard.html")["json.MisspellAnswer"]
        expected = {"text.QueryBefore": "genby", "text.QueryAfter": "путин", "text.Confidence": "sug10000"}
        assert misspell == misspell_mobile == expected

        misspell = self.parse_file("misspell_sug8k.html")["json.MisspellAnswer"]
        misspell_mobile = self.parse_file("misspell_mobile_sug8k.html")["json.MisspellAnswer"]
        expected = {"text.QueryBefore": "молокоа", "text.QueryAfter": "молока", "text.Confidence": "sug8000"}
        assert misspell == misspell_mobile == expected

        misspell = self.parse_file("misspell_sug100.html")["json.MisspellAnswer"]
        misspell_mobile = self.parse_file("misspell_mobile_sug100.html")["json.MisspellAnswer"]
        expected = {"text.QueryBefore": "jethro tull –скщыы", "text.QueryAfter": "jethro tull -cross", "text.Confidence": "sug100"}
        assert misspell == misspell_mobile == expected

        misspell_ua = self.parse_file("misspell_sug8k_ua.html")["json.MisspellAnswer"]
        expected = {"text.QueryBefore": "tcnm kb ctvmz e 'ylibgtkz", "text.QueryAfter": "есть ли семья у эндыипеля",
                    "text.Confidence": "sug8000"}
        assert misspell_ua == expected

        misspell_kz = self.parse_file("misspell_mobile_sug8k_kz.html")["json.MisspellAnswer"]
        expected = {"text.QueryBefore": "несмешно сатпаев", "text.QueryAfter": "не смешно сатпаев",
                    "text.Confidence": "sug8000"}
        assert misspell_kz == expected

        assert "json.MisspellAnswer" not in self.parse_file("misspell_nosug.html")
        assert "json.MisspellAnswer" not in self.parse_file("misspell_mobile_nosug.html")

        # should not raise exceptions
        assert self.parse_file("misspell_dnevnik_out_of_range.html")

    def test_remove_sign_parameter_for_turbo(self):
        components = self.parse_file('turbo_sign.html')['components']
        search_results = self._get_search_results(components)
        turbo_result = search_results[1]
        url_wo_sign = "https://yandex.by/turbo/ilive.com.ua/s/food/poleznye-chai-pri-gastrite-zelenyy-chernyy-s-molokom-medom-i-limonom_130364i15882.html"
        assert self._get_page_url(turbo_result) == url_wo_sign

    @responses.activate
    def test_market_shopping(self):
        url = "https://mock.url/"
        responses.add(responses.GET, re.compile("https?://yabs.*"),
                      status=302, headers={"Location": url})
        responses.add(responses.GET, url, status=200)
        component = self.parse_file("buy_samsungA51.html",
                                    additional_parameters={"config": {"need_advertised_urls": True}})["components"][0]
        assert component["componentInfo"].get("wizardType") == WizardTypes.WIZARD_SHOPPING
        assert len(component["site-links"]) == 6
        sitelink = component["site-links"][0]
        assert sitelink["json.prices"][0].get("price") == 17390
        assert sitelink["sitelinks"].get("title") == "Смартфон SAMSUNG Galaxy A51 64Gb, SM-A515F, черный"
        assert sitelink["sitelinks"].get("url") == url

    def test_wizard_market_document_url(self):
        component = self.parse_file("vault.html")["components"][6]
        assert component["componentInfo"].get("wizardType") == WizardTypes.WIZARD_MARKET
        assert component["componentUrl"].get("pageUrl") == "https://market.yandex.ru/search?text=%D1%81%D0%B5%D0%B9%D1%84&lr=2&cpa=1&clid=545"

    def test_wizard_adresa_uno_url(self):
        component = self.parse_file("touch_sm.html", additional_parameters=UNO_URL_PARAMETERS)["components"][1]
        assert component["componentUrl"]["pageUrl"] == "https://www.sportmaster.ru/"

    def test_wkg_uno_url(self):
        component = self.parse_file("uno_site.html", additional_parameters=UNO_URL_PARAMETERS)["components"][1]
        assert component["componentUrl"]["pageUrl"] == "http://www.ovt.com"

    def test_wkg_uno_340(self):
        additional_parameters = {
            "url": "https://hamster.yandex.ru",
            "config": {"uno_url": True},
            "userdata": {"text": "кэндис пату"}
        }
        component = self.parse_file("uno_wkg_hamster.html", additional_parameters=additional_parameters)["components"][1]
        assert component["componentUrl"]["pageUrl"] == "https://kinopoisk.ru/name/274329"

    def test_wzd_fct_desktop(self):

        component = self.parse_file("wizard_fact_desktop.html")["components"][0]
        assert component["json.wizardInfo"]["wizardFactAnswer"] == "Известные люди, связанные с Алтайским краем. " \
                                                                   "Актёры, режиссёры, поэты и деятели искусств : " \
                                                                   "Алексей Булдаков (1951), Алексей Ванин (1925), " \
                                                                   "Михаил Евдокимов (1957—2005), Валерий Золотухин (1941), " \
                                                                   "Владимир Кашпур (1926—2009), Ирина Мирошниченко (1942), " \
                                                                   "Александр Панкратов-Чёрный (1949), Иван Пырьев (1901—1968), " \
                                                                   "Роберт Рождественский (1932—1994), " \
                                                                   "Екатерина Савинова (1926—1970), Нина Усатова (1951), " \
                                                                   "Владимир Хотиненко."
        assert component["json.wizardInfo"].get("wizardFactDetail") is None

    def test_wzd_fct_android(self):

        component = self.parse_file("wizard_fact_android.html")["components"][0]
        assert component["json.wizardInfo"]["wizardFactAnswer"] == "Мозгобойня"
        assert component["json.wizardInfo"]["wizardFactAnswerDetail"] == "«Мозгобо́йня» — паб-квиз в 276 городах 17 " \
                                                                         "стран мира. Проект был основан в Минске 25 " \
                                                                         "апреля 2012 года Екатериной Максимовой и " \
                                                                         "Александром Ханиным. В 2020 и 2021 году " \
                                                                         "«Мозгобойня» занимает 28 место в рейтинге " \
                                                                         "«30 самых выгодных франшиз 2020» среди " \
                                                                         "российских компаний по версии Forbes.ru. " \
                                                                         "Основателями «Мозгобойни» являются Екатерина " \
                                                                         "Максимова и Александр Ханин, которые привезли " \
                                                                         "концепт паб-квиза из Вильнюса. Первая игра " \
                                                                         "состоялась в 2012 году в минском кафе, собрав " \
                                                                         "всего 45 человек."

    def test_wzd_no_fct(self):

        component = self.parse_file("wizard_no_wizard.html")["components"][0]
        assert component.get("json.wizardInfo") is None

    def test_wzd_metadoc(self):

        components = self.parse_file("wizard_metadoc.html")["components"]
        assert components[1]["json.wizardInfo"]["metadocInfo"] == [{"title": "Какая страна 77?",
                                                                    "url": "https://phonenum.info/phone/77"},
                                                                   {"title": "Какая страна 63?",
                                                                    "url": "https://ru.m.wikipedia.org/wiki/%D0%A2%D0%B5%D0%BB%D0%B5%D1%84%D0%BE%D0%BD%D0%BD%D1%8B%D0%B5_"
                                                                           "%D0%BA%D0%BE%D0%B4%D1%8B_%D1%81%D1%82%D1%80%D0%B0%D0%BD"},
                                                                   {"title": "Какая страна +7?",
                                                                    "url": "http://www.bolshoyvopros.ru/questions/1533126-kakim-stranam-prinadlezhit-telefonnyj-kod-7.html"},
                                                                   {"title": "Какая страна +6?",
                                                                    "url": "https://AndroidLime.ru/phone-numbers-code-6"},
                                                                   {"title": "Какая страна +82?",
                                                                    "url": "https://ru.m.wikipedia.org/wiki/%D0%A2%D0%B5%D0%BB%D0%B5%D1%84%D0%BE%D0%BD%D0%BD%D1%8B%D0%B5_"
                                                                           "%D0%BA%D0%BE%D0%B4%D1%8B_%D1%81%D1%82%D1%80%D0%B0%D0%BD"},
                                                                   {"title": "Какая страна +91?",
                                                                    'url': 'https://translate.yandex.ru/translate?lang=en-ru&url=https%3A%2F%2Fwww.quora.com%2FWhy-is-India-country-code-91&view=c'}
                                                                   ]

    def test_wzd_uno_err_url(self):
        additional_parameters = {
            "url": "https://hamster.yandex.ru",
            "config": {"uno_url": True},
            "userdata": {"text": "кэндис пату"}
        }
        component = self.parse_file("uno_youtube_url.html", additional_parameters=additional_parameters)["components"][16]
        assert component["text.baobabWizardName"] == "entity_search"
        assert component["componentUrl"]["pageUrl"] == "https://www.youtube.com/c/freddieismyqueen"

    def test_component_source(self):
        component = self.parse_file("component_source.html")["components"][0]
        assert component["text.componentSource"] == "offline_fs_fact"

    def test_wzd_uno_err_wo_url(self):
        additional_parameters = {
            "url": "https://hamster.yandex.ru",
            "config": {"uno_url": True},
            "userdata": {"text": "кэндис пату"}
        }
        component = self.parse_file("uno_oo_wizard_wo_url.html", additional_parameters=additional_parameters)["components"][0]
        assert component["text.baobabWizardName"] == "entity_search"
        assert component["componentUrl"]["pageUrl"] is None

    def test_wzd_uno_rich_fact_url(self):
        components = self.parse_file("wizard_rich_fact_url.html")["components"]
        assert components[0]["componentUrl"]["pageUrl"] == "https://PythonRu.com/osnovy/kak-udalit-element-iz-spiska-python"
