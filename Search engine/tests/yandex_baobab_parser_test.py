import pytest
import responses

from test_utils import TestParser
try:
    from base_parsers import JSONSerpParser
    from yandex_baobab.yandex_baobab_parser import YandexBaobabParser
    from yabs_redirect_handler import resolve_yabs_redirect
    from yabs_redirect_handler import extract_redirect_url_from_content
except ImportError:
    from parsers.base_parsers import JSONSerpParser
    from parsers.yandex_baobab.yandex_baobab_parser import YandexBaobabParser
    from parsers.yabs_redirect_handler import resolve_yabs_redirect
    from parsers.yabs_redirect_handler import extract_redirect_url_from_content


REDIRECT_ASSETS_DIRECTORY = "redirect"

ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes
WizardTypes = JSONSerpParser.MetricsMagicNumbers.WizardTypes
Alignments = JSONSerpParser.MetricsMagicNumbers.Alignments


class TestYandexBaobabParser(TestParser[YandexBaobabParser]):
    _parser_class = YandexBaobabParser

    def test_robot_dater_fresh_age(self):
        components = self.parse_file("robot_dater_fresh_age.json")['components']
        assert 12 == len(components)

        component0 = components[0]
        assert component0["componentUrl"]["pageUrl"] == "https://www.youtube.com/"
        assert component0["text.shard"] == "primus-PlatinumTier0-0-158-1566080784"
        assert component0["long.documentAge"] == 446887042000

        component1 = components[1]
        assert component1["componentUrl"]["pageUrl"] == "http://youtu.be/"
        assert component1["text.shard"] == "primus-PlatinumTier0-0-91-1566080784"
        assert component1["long.documentAge"] == 1566295170000

    def test_component_count(self):
        components = self.parse_file("test.html")['components']
        assert 12 == len(components)

    def test_title(self):
        components = self.parse_file("title_and_snippet.json")['components']
        assert components[1].get("text.title") == u"Тестовый заголовок специально для теста"

    def test_snippet(self):
        components = self.parse_file("title_and_snippet.json")['components']
        assert components[1].get("text.snippet") == u"Тестовый сниппет специально для теста"

    def test_data_cid(self):
        component = self.parse_file("test.html")['components'][0]
        assert component["text.dataCid"] == 0

    def test_component_selector(self):
        component = self.parse_file("test.html")['components'][0]
        assert component["text.cssSelector"] == '[data-log-node="7xsw"],[data-log-node="7xsw"] .Favicon'

    def test_protocol(self):
        components = self.parse_file("protocol.html")['components']
        for component in components:
            url = component["componentUrl"]['pageUrl']
            if url:
                check_protocol(url)

    # def compare_preparer_output(self, input_filename, expected_output_filename):
    #     basket_query = self._read_json_file("preparer", input_filename)
    #     prepared = self._get_parser_class()().prepare(0, basket_query, "yandex.ru")
    #     expected = self._read_json_file("preparer", expected_output_filename)
    #     assert prepared == expected

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("0_input.json", "0_expected_output.json"),
        ("1_input.json", "1_expected_output.json"),
        ("2_input.json", "2_expected_output.json"),
        ("3_input.json", "3_expected_output.json"),
        ("4_input.json", "4_expected_output.json")  # DE -> yandex.eu
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename)

    def test_preparer_url(self):
        input_0 = self._read_json_file("preparer", "0_input.json")
        preparer = self.get_parser()
        assert preparer._prepare_url(input_0, "yandex.com") == "https://yandex.ru/search/?text=89086479109&lr=63&exp_flags=baobab%3Dexport&sbh=1&no-tests=1"
        assert preparer._prepare_url(input_0, "yandex.com", {'cgi': "nocache=da"}) == "https://yandex.ru/search/?nocache=da&text=89086479109&lr=63&exp_flags=baobab%3Dexport&sbh=1&no-tests=1"
        assert preparer._prepare_url(input_0, "yandex.com", {'numdoc': 10}) == "https://yandex.ru/search/?text=89086479109&lr=63&exp_flags=baobab%3Dexport&sbh=1&numdoc=10&no-tests=1"
        assert preparer._prepare_url(input_0, "yandex.com", {'cgi': "test-id=1"}) == "https://yandex.ru/search/?test-id=1&text=89086479109&lr=63&exp_flags=baobab%3Dexport&sbh=1"
        assert preparer._prepare_url(input_0, "yandex.com", {'allow-experiments': True}) == "https://yandex.ru/search/?text=89086479109&lr=63&exp_flags=baobab%3Dexport&sbh=1"

    @pytest.mark.parametrize("country, tld", [
        ("DE", "eu"),
        ("RU", "ru"),
    ])
    def test_tld_mapping(self, country, tld):
        parser = self.get_parser()
        tlds = parser.get_tlds()
        assert tlds.get(country) == tld

    def test_wizard_transport(self):
        component = self.parse_file("wizard_transport.html")['components'][0]
        assert component["componentInfo"]["type"] == ComponentTypes.WIZARD
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_TRANSPORT

    def test_wizard_transport_2(self):
        component = self.parse_file('wizard_transport_2.html')['components'][2]
        assert component["componentInfo"]["type"] == ComponentTypes.WIZARD
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_TRANSPORT

    def test_wizard_panorama(self):
        component = self.parse_file("wizard_panorama.html")['components'][1]
        assert component["componentInfo"]["type"] == ComponentTypes.WIZARD
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_PANORAMA

    def test_wizard_maps(self):
        component = self.parse_file("wizard_maps.html")['components'][0]
        assert component["componentInfo"]["type"] == ComponentTypes.WIZARD
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_MAPS

    def test_wizard_route(self):
        component = self.parse_file("wizard_route.html")['components'][0]
        assert component["componentInfo"]["type"] == ComponentTypes.WIZARD
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_ROUTE

    def test_wizard_orgmn(self):
        component = self.parse_file("wizard_orgmn.html")['components'][0]
        assert component["componentInfo"]["type"] == ComponentTypes.WIZARD
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_ORGMN

    def test_wizard_adresa(self):
        component = self.parse_file("wizard_adresa.html")['components'][2]
        assert component["componentInfo"]["type"] == ComponentTypes.WIZARD
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_ADRESA

    def test_wizard_bna(self):
        component = self.parse_file("wizard_bna.html")['components'][0]
        assert component["componentInfo"]["type"] == ComponentTypes.SEARCH_RESULT
        assert component["componentInfo"]["wizardType"] == WizardTypes.WIZARD_BNA

    def test_sbh_component_data(self):
        component = self.parse_file("sbh_component_data.html")['components'][0]
        assert component["componentUrl"]["pageUrl"] == "https://www.speedtest.net/ru"
        assert component["url.originalUrl"] == "https://www.speedtest.net/ru"
        assert component["text.baseType"] == "rus"
        assert component["text.shard"] == "primus-PlatinumTier0-0-82-1562981063"
        assert component["webadd"]["relevance"] == 104064672
        assert component["text.dataCid"] == 0

    def test_clean_wizard_url(self):
        component = self.parse_file('wizard_cleanurl.html')['components'][6]
        assert component['componentInfo']['type'] == ComponentTypes.WIZARD
        assert component['componentUrl']['pageUrl'] == "https://yandex.ru/video/search?text=" \
                                                       "%22%D1%80%D0%B5%D0%B3%D0%B8%D0%BE%D0%BD%D0%B0%D0%BB%D1%8C" \
                                                       "%D0%BD%D1%8B%D0%B9%20%D1%82%D1%80%D0%B5%D0%BA%3A%20" \
                                                       "%D1%81%D0%B4%D0%B5%D0%BB%D0%B0%D0%BD%D0%BE%20%D0%B2%20%D1%80%D0%BE" \
                                                       "%D1%81%D1%81%D0%B8%D0%B8%22%20%D1%81%D0%BF%D0%B1"

    def test_knowledge_graph(self):
        components = self.parse_file('wizard_wiki.html')['components']
        assert components[0]['componentInfo']['wizardType'] == WizardTypes.WIZARD_KNOWLEDGE_GRAPH
        assert components[1]['componentInfo']['wizardType'] == WizardTypes.WIZARD_KNOWLEDGE_GRAPH
        assert components[3]['componentInfo']['wizardType'] == WizardTypes.WIZARD_KNOWLEDGE_GRAPH

    def test_wizard_news(self):
        component = self.parse_file('wizard_news.html')['components'][0]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_NEWS
        assert component['componentUrl']['pageUrl'] == 'https://m.yandex.ru/news/rubric/index?from=newswizard&wizard=rubric&serp_referer_reask=1'

    def test_wizard_auto(self):
        component = self.parse_file('wizard_auto.html')['components'][0]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_AUTO

    def test_knowledge_graph_right(self):
        component = self.parse_file('wizard_kg_right.html')['components'][12]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_KNOWLEDGE_GRAPH
        assert component['componentInfo']['alignment'] == Alignments.RIGHT

    def test_wizard_colors(self):
        component = self.parse_file('wizard_colors.html')['components'][0]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_COLORS

    def test_wizard_esu(self):
        component = self.parse_file('wizard_esu.html')['components'][0]
        assert component['componentInfo']['wizardType'] == WizardTypes.WIZARD_ENTITY_SEARCH_UPPER
        assert component['text.ento'] == '0oCglydXcxMTA0MjQSN2xzdC5zbG1DaUlLQkhSbGVIUVNHbk5mZEdsMGJHVTZLTkdFMExEUXZkR0MwTDdRdk5DdzBZRXAYAkIQ0YTQsNC90YLQvtC80LDRgQhNxwQ'

    def test_top_addv(self):
        components = self.parse_file('top_addv.html')['components']
        component = components[0]
        assert component['componentInfo']['alignment'] == Alignments.TOP
        assert component['componentInfo']['type'] == ComponentTypes.ADDV
        last_component = components[-1]
        assert last_component['componentInfo']['alignment'] == Alignments.BOTTOM
        assert last_component['componentInfo']['type'] == ComponentTypes.ADDV

    def test_new_turbo(self):
        component = self.parse_file('new_turbo.html')['components']
        assert component[1]['tags.isTurbo'] is True
        assert component[2]['tags.isTurbo'] is True
        assert component[3]['tags.isTurbo'] is None

    def test_baobab_features_1(self):
        component = self.parse_file('wizard_esu.html')['components'][0]
        assert "texts.baobabFeatures" in component
        assert component["texts.baobabFeatures"] == ["entity_search"]

    def test_baobab_features_2(self):
        component = self.parse_file('wizard_wiki.html')['components'][0]
        assert component["texts.baobabFeatures"] == ['entity_search']

    def test_baobab_features_3(self):
        component = self.parse_file('title_and_snippet.json')['components'][0]
        assert component["texts.baobabFeatures"] == ['sport/livescore']

    def test_baobab_children_paths_1(self):
        component = self.parse_file('wizard_esu.html')['components'][0]
        assert "texts.baobabChildrenPaths" in component
        assert len(component["texts.baobabChildrenPaths"]) == 9
        assert component["texts.baobabChildrenPaths"][3] == 'carousel/showcase'

    def test_baobab_children_paths_2(self):
        component = self.parse_file('wizard_wiki.html')['components'][0]
        assert len(component["texts.baobabChildrenPaths"]) == 19
        assert component["texts.baobabChildrenPaths"][15] == 'scroller'

    def test_baobab_children_paths_3(self):
        component = self.parse_file('title_and_snippet.json')['components'][0]
        assert len(component["texts.baobabChildrenPaths"]) == 6
        assert component["texts.baobabChildrenPaths"][5] == 'team/p1'

    @pytest.mark.parametrize("url,expected", [
        ("https://someurl", "https://someurl"),
        ("//noprotocol", "https://noprotocol"),
        ("/safety/?url=https://silent.az/", "https://yandex.ru/safety/?url=https://silent.az/")
    ])
    def test_fix_url(self, url, expected):
        assert self.get_parser()._fix_url(url) == expected

    @pytest.mark.parametrize("url,base_url,expected", [
        ("https://someurl", "https://yandex.ru/", "https://someurl"),
        ("//noprotocol", "https://yandex.ru/", "https://noprotocol"),
        ("/safety/?url=https://silent.az/", "https://yandex.ru/", "https://yandex.ru/safety/?url=https://silent.az/"),
        ("/safety/?url=https://silent.az/", "https://yandex.ru/", "https://yandex.ru/safety/?url=https://silent.az/"),
        ("/safety/?url=https://silent.az/", "https://zzzandex.ru", "https://zzzandex.ru/safety/?url=https://silent.az/"),
        ("/safety/?url=https://silent.az/", "https://man.yandex.ru", "https://yandex.ru/safety/?url=https://silent.az/")
    ])
    def test_fix_url_with_base_url(self, url, base_url, expected):
        assert self.get_parser()._fix_url(url, base_url) == expected

    def test_parsing_with_base_url(self):
        c = self.parse_file_with_base_url("https://zzzandex.ru", "relative_links.html")['components']
        assert c[0]['componentUrl']['pageUrl'] == "https://zzzandex.ru/ru"

    def test_relative_link_with_missing_base_link(self):
        c = self.parse_file_with_base_url(None, "relative_links.html")['components']
        assert c[0]['componentUrl']['pageUrl'] == "https://yandex.ru/ru"

    @pytest.mark.parametrize("input_url,expected_url", [
        ("https://fml-models-acceptance.hamster.yandex.ua/turbo?text=https%3A%2F%2Fru.wikipedia.org",
         "https://yandex.ua/turbo?text=https%3A%2F%2Fru.wikipedia.org"),
        ("https://hamster.yandex.ru/", "https://hamster.yandex.ru/"),
        ("https://myyandex.yandex.ru/turbo?", "https://yandex.ru/turbo?"),
        ("https://a.b.yandex.c.yandex.d.e.f.g/turbo?abc", "https://yandex.d.e.f.g/turbo?abc"),
        ("https://a.b.yandex.c.yandex.d.e.f.g/turbo?://abc.yandex.ru/turbo?test",
         "https://yandex.d.e.f.g/turbo?://abc.yandex.ru/turbo?test"),
        ("https://www.fml-models-acceptance.hamster.yandex.ua/turbo?text=https%3A%2F%2Fru.wikipedia.org",
         "https://www.yandex.ua/turbo?text=https%3A%2F%2Fru.wikipedia.org"),
        ("https://frozen-saas.hamster.yandex.az/safety/?url=https://www.tatli.biz/extra/android/indir/",
         "https://yandex.az/safety/?url=https://www.tatli.biz/extra/android/indir/")

    ])
    def test_turbo_and_safety(self, input_url, expected_url):
        parser = self.get_parser()
        assert parser._replace_turbo_or_safety(input_url) == expected_url

    @pytest.mark.parametrize("input_url, expected_url", [
        ("https://hamster.yandex.ru/search/?test-id=411529&whocares", "https://yandex.ru/search/?test-id=411529&whocares"),
        ("https://hamster.yandex.ru/?test-id=411529&whocares", "https://hamster.yandex.ru/?test-id=411529&whocares"),
        ("https://some.hamster.yandex.ru/search/?test-id=411529&whocares", "https://yandex.ru/search/?test-id=411529&whocares")
    ])
    def test_hamster_wizard_url(self, input_url, expected_url):
        parser = self.get_parser()
        assert parser._replace_orgmn_hamster(input_url) == expected_url

    @pytest.mark.parametrize("url, device, clean_url", [
        ("https://ya.ru/search/touch?q=test", "ANDROID", "https://ya.ru/search/touch?q=test&noredirect=1"),
        ("https://ya.ru/search/touch?q=test", "DESKTOP", "https://ya.ru/search/touch?q=test")
    ])
    def test_clean_url(self, url, device, clean_url):
        assert self.parse("{}", additional_parameters={'url': url, 'userdata': {'device': device}})['urls.cleanUrl'] == clean_url

    @responses.activate
    def test_resolve_yabs_redirect_with_status_code(self):
        original_url = self._read_file(REDIRECT_ASSETS_DIRECTORY, "url_fiesta_original.txt", strip=True)
        redirect_url = self._read_file(REDIRECT_ASSETS_DIRECTORY, "url_fiesta_redirect.txt", strip=True)
        responses.add(
            responses.GET,
            original_url,
            headers={"Location": redirect_url},
            status=302,
        )
        responses.add(
            responses.GET,
            redirect_url,
            status=200,
        )

        assert resolve_yabs_redirect(original_url) == redirect_url

    @responses.activate
    def test_resolve_yabs_redirect_with_content(self):
        original_url = self._read_file(REDIRECT_ASSETS_DIRECTORY, "url_fiesta_original.txt", strip=True)
        redirect_url = self._read_file(REDIRECT_ASSETS_DIRECTORY, "url_bestmebelshop.txt", strip=True)
        content = self._read_file(REDIRECT_ASSETS_DIRECTORY, "redirect.html")
        responses.add(
            responses.GET,
            original_url,
            status=200,
            body=content
        )

        assert resolve_yabs_redirect(original_url) == redirect_url

    def test_extract_redirect_url_from_content(self):
        content = self._read_file(REDIRECT_ASSETS_DIRECTORY, "redirect.html")
        redirect_url = self._read_file(REDIRECT_ASSETS_DIRECTORY, "url_bestmebelshop.txt", strip=True)
        assert extract_redirect_url_from_content(content) == redirect_url

    @pytest.mark.parametrize(
        "original_url, clean_url",
        [
            (
                (
                    "https://yandex.by/turbo/ilive.com.ua/s/food/poleznye-chai-pri-gastrite-zelenyy-chernyy-s-molokom-medom-i-limonom_130364i15882.html?"
                    "sign=7646c9e61b69047aa7c0df3222e1c48f9e7838e56c463a71bfb7c0e256b73864:1614933841"
                ),
                "https://yandex.by/turbo/ilive.com.ua/s/food/poleznye-chai-pri-gastrite-zelenyy-chernyy-s-molokom-medom-i-limonom_130364i15882.html",
            ),
            (
                (
                    "https://lenta-ru.turbopages.org/lenta.ru/s/news/2021/03/05/8marta_msk/?"
                    "turbo_uid=AACOORTqO78OWpM25cBIqnix185k5DNYtww4AkE_rWLf2wWoxtEvw3yaq3Vl8zHXjSiZRt5LoBYCLidDHg5UGP0J8urGN4TtpWmV&"
                    "turbo_ic=AAC-0BuDpPS8TzD6Gz7R4hde-sFnzQCXCItYB15_hBqJ3PCOGBNWgCj2jg3gTUlYKBKWsWc5T7ONGoATuI6ut7i_6cZPAe5ybgM2"
                ),
                "https://lenta-ru.turbopages.org/lenta.ru/s/news/2021/03/05/8marta_msk/",
            ),
            (
                ("https://belaruspartisan-by.turbopages.org/belaruspartisan.by/s/economic/512666/?"
                 "parent-reqid=1615544294928984-1389711425986217895200115-hamster-app-host-sas-web-yp-54&"
                 "trbsrc=wb&"
                 "event-id=km65d0oqb8"),
                "https://belaruspartisan-by.turbopages.org/belaruspartisan.by/s/economic/512666/"
            ),
        ],
    )
    def test_clean_turbo_url(self, original_url, clean_url):
        assert self.get_parser()._clean_turbo_url(original_url) == clean_url

    def test_videowiz_sitelinks(self):
        component = self.parse_file('wizard_videowiz.html')['components'][1]
        site_links = component['site-links']
        main_url = (
            "https://yandex.ru/video/touch/preview/?text=%D1%8E%D1%82%D1%83%D0%B1&path=wizard&parent-reqid=163557717992"
            "6168-7209531001053399942-sas3-0893-4f3-sas-l7-balancer-8080-BAL-2529&wiz_type=vital"
        )
        urls = [
            f"{main_url}&filmId=10142038794646750652",
            f"{main_url}&filmId=4112440142591713576",
            f"{main_url}&filmId=11300414517439269701",
            f"{main_url}&filmId=862211005613617497",
            f"{main_url}&filmId=1299877064511192697",
            f"{main_url}&filmId=2888897335902458995",
        ]
        assert len(urls) == len(site_links)
        for url, site_link in zip(urls, site_links):
            assert url == site_link["sitelinks"]["url"]

    def test_videowiz_sitelinks_2(self):
        component = self.parse_file('wizard_videowiz_2.html')['components'][5]
        site_links = component['site-links']
        main_url = (
            "https://yandex.ru/video/touch/preview/""?text=%D0%B4%D0%B8%D1%81%D0%BA%20%D0%BF%D0%BE%20%D0%BC%D0%B5%D1%82"
            "%D0%B0%D0%BB%D0%BB%D1%83%20%D0%B4%D0%BB%D1%8F%20%D1%86%D0%B8%D1%80%D0%BA%D1%83%D0%BB%D1%8F%D1%80%D0%BD%D0%"
            "BE%D0%B9%20%D0%BF%D0%B8%D0%BB%D1%8B&path=wizard&parent-reqid=1635406503905505-5212383257128010940-vla1-278"
            "6-vla-l7-balancer-8080-BAL-8755&wiz_type=xl"
        )
        urls = [
            f"{main_url}&filmId=7168526531047964096",
            f"{main_url}&filmId=18414470953781311271",
            f"{main_url}&filmId=10365496413261341593",
            f"{main_url}&filmId=15915361778531885947",
            f"{main_url}&filmId=7063650753571201297",
            f"{main_url}&filmId=17764107449767668782",
        ]
        assert len(urls) == len(site_links)
        for url, site_link in zip(urls, site_links):
            assert url == site_link["sitelinks"]["url"]

    def test_related_discovery(self):
        component = self.parse_file('related_discovery.html')['components'][1]
        site_links = component['site-links']
        urls = [
            "https://top-ogorod.ru/sovety-po-uhodu/podgotovka-semyan-k-posevu.html",
            "https://oazisvdome.ru/4-jeffektivnyh-sposoba-obezzarazhivanija-pochvy-dlja-rassady/",
            "https://vk.com/topic-72068157_37060639",
            "https://www.youtube.com/watch?v=mYkpfh7djiE",
            "https://fresh-mania.com/kak-prorastit-semena-v-domashnih-usloviyah/",
            "https://vasha-teplitsa.ru/virashivanie/pochemu-nelzya-sazhat-v-polnolunie.html",
        ]
        assert len(urls) == len(site_links)
        for url, site_link in zip(urls, site_links):
            assert url == site_link["sitelinks"]["url"]


def check_protocol(url):
    assert url.startswith("http://") or url.startswith("https://")
