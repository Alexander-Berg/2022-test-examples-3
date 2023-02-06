import pytest

from base_parsers import YandexJSONSerpParser
from test_utils import TestParser
from yandex_images_json_parser import ImageUtils

Alignments = YandexJSONSerpParser.MetricsMagicNumbers.Alignments
WizardTypes = YandexJSONSerpParser.MetricsMagicNumbers.WizardTypes
ComponentTypes = YandexJSONSerpParser.MetricsMagicNumbers.ComponentTypes

WIZARD = ComponentTypes.WIZARD

QUERY = {
    'text': "тест",
    'region': {'id': 1}
}

TOUCH_QUERY = {
    'text': "test",
    'region': {'id': 1},
    'device': 'ANDROID'
}


class TestYandexImagesJSONParser(TestParser):

    def test_prepare(self):
        assert self.prepare(query=QUERY)['uri'] == 'https://yandex.ru/images/search?text=%D1%82%D0%B5%D1%81%D1%82' \
                                                   '&lr=1&' \
                                                   'json_dump=searchdata.numdocs&' \
                                                   'json_dump=searchdata.images&' \
                                                   'json_dump=unanswer_data&' \
                                                   'json_dump=search_props&' \
                                                   'json_dump=searchdata.reask&' \
                                                   'json_dump=searchdata.reask.text&' \
                                                   'json_dump=wizplaces.related.0&' \
                                                   'json_dump=wizplaces.pdb.0&' \
                                                   'json_dump=eventlogs&no-tests=1'

    def test_prepare_touch(self):
        assert self.prepare(query=TOUCH_QUERY)['uri'] == 'https://yandex.ru/images/touch/search?text=test&' \
                                                         'lr=1&' \
                                                         'json_dump=searchdata.numdocs&' \
                                                         'json_dump=searchdata.images&' \
                                                         'json_dump=unanswer_data&' \
                                                         'json_dump=search_props&' \
                                                         'json_dump=searchdata.reask&' \
                                                         'json_dump=searchdata.reask.text&' \
                                                         'json_dump=wizplaces.related.0' \
                                                         '&json_dump=wizplaces.pdb.0' \
                                                         '&json_dump=eventlogs&no-tests=1'

    def test_clean_url(self):
        url = 'https://yandex.ru/images/touch/search?text=%D1%82%D0%B5%D1%81%D1%82&lr=1&json_dump=searchdata.images'
        clean_url = 'https://yandex.ru/images/touch/search?text=%D1%82%D0%B5%D1%81%D1%82&lr=1&noredirect=1'
        assert self.get_parser()._clean_url(url) == clean_url

    def test_parse(self):
        components = self.parse_file('lenovo.json')['components']
        assert len(components) == 62

        search_results = self._get_search_results(components)
        assert len(search_results) == 20

        component = search_results[0]
        assert component['url.imageBigThumbHref'] == 'http://im0-tub-ru.yandex.net/i?id=544eb238be2d07d287d765b75ffa517e-l&n=13'
        page_url = 'https://www.varle.lt/m/nesiojami-kompiuteriai/nesiojami-kompiuteriai/nesiojamas-kompiuteris-lenovo-thinkpad-t480s-140--8443533.html'
        assert component['componentUrl']['pageUrl'] == page_url

    def _get_search_results(self, components):
        return [c for c in components if c['componentInfo']['type'] == ComponentTypes.SEARCH_RESULT]

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("0_input.json", "0_expected_output.json")
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename)

    def test_blender_trigger(self):
        parsed = self.parse_file('blender.json')
        assert parsed['text.blender_trigger'] == 'IMAGESQUICK'

    def test_blender_intent(self):
        parsed = self.parse_file('blender.json')
        assert parsed['double.blender_intent'] > 0.01

    def test_numdoc(self):
        lenovo = self.parse_file('lenovo.json')
        assert lenovo['headers']['foundDocumentsCount'] == 11218

    def test_series(self):
        parsed = self.parse_file('seriesImages.json')
        components = parsed['components']
        search_results = self._get_search_results(components)
        assert len(search_results) == 34

    def test_related_iamges(self):
        parsed = self.parse_file('relatedImages.json')
        components = parsed['components']
        components[0]
        search_results = self._get_search_results(components)
        assert len(search_results) == 20

    def test_commercial_wizard(self):
        parsed = self.parse_file('commercial_wizard.json')
        assert len(parsed['components']) == 9

    def test_commercial_shopping_length(self):
        parsed = self.parse_file('shopping.json')
        assert len(parsed['components']) == 7

    def test_clothes_classifier(self):
        parsed = self.parse_file('clothes_classifier.json')
        assert isinstance(parsed['double.q_clothes_prob_v1'], float) and abs(parsed['double.q_clothes_prob_v1'] - 0.980768) < 0.001

    def test_commercial_shopping_price(self):
        parsed = self.parse_file('shopping.json')
        assert parsed['components'][0]['text.product_price_currency'] == 'RUR'

    def test_commercial_shopping_image_url(self):
        parsed = self.parse_file('shopping.json')
        assert parsed['components'][0]['imageadd']['url'] == 'https://avatars.mds.yandex.net/get-yabs_performance/3684337/2a00000174538a648c98591024f14f29576e/hugeX'

    def test_reask(self):
        base_url = "http://yandex.ru/images/search?text=%D0%B3%D0%BE%D1%80%D1%88%D0%BA%D0%BE%D0%B2+%D0%B8%D0%B3%D0%BE%D1%80%D1%8C+%D0%B2%D0%B0%D1%81%D0%B8%D0%BB" + \
                   "%D1%8C%D0%B5%D0%B2%D0%B8%D1%87+ujhjl+hzpfym&numdoc=1&nocookiesupport=yes&json_dump=searchdata" + \
                   ".images&json_dump=search_props&json_dump=unanswer_data&json_dump=searchdata.numdocs&json_dump=searchdata.images&json_dump=searchdata" + \
                   ".reask_wizard.text"
        reask_parsed = self.parse_file_with_base_url(base_url, 'reaskText.json')
        reask_wizard = reask_parsed['components'][0]
        assert reask_wizard['text.title'] == "горшков игорь васильевич город рязань"
        assert reask_wizard['componentInfo']['type'] == WIZARD
        assert reask_wizard['componentInfo']['wizardType'] == WizardTypes.WIZARD_MISPRINT
        assert reask_wizard['componentInfo']['alignment'] == Alignments.LEFT
        assert "noreask=1" in reask_wizard['componentUrl']['pageUrl']

    def test_related_images_nine_yards(self):
        whole_nine_yards = self.parse_file('whole_nine_yards.json')
        components = whole_nine_yards['components']
        assert components

        amanda_0 = components[0]
        assert amanda_0['text.title'] == "аманда пит девять ярдов"
        assert amanda_0['componentInfo']['type'] == WIZARD
        assert amanda_0['componentInfo']['wizardType'] == WizardTypes.METRICS_RELATED_QUERIES
        assert amanda_0['dimension.SCRAPER_IMAGE_DIMENSION']['w'] == 400
        assert amanda_0['dimension.SCRAPER_IMAGE_DIMENSION']['h'] == 200
        assert amanda_0['imageadd']['url'] == "http://im0-tub-ru.yandex.net/i?id=d262701d8f5b32140b3fb98bdbbd3d34&n=11"
        assert amanda_0['text.title'] == "аманда пит девять ярдов"
        assert amanda_0['type'] == "COMPONENT"

        amanda_1 = components[1]
        assert amanda_1['dimension.SCRAPER_IMAGE_DIMENSION']['w'] == 200
        assert amanda_1['dimension.SCRAPER_IMAGE_DIMENSION']['h'] == 200
        assert amanda_1['componentInfo']['wizardType'] == WizardTypes.METRICS_MOBILE_RELATED_QUERIES
        assert amanda_1['text.title'] == "аманда пит девять ярдов"

    def test_collections(self):
        whole_nine_yards = self.parse_file('whole_nine_yards.json')
        components = whole_nine_yards['components']
        wizard = components[12]
        compare_component_info(wizard, WIZARD, expected_wizard_type=WizardTypes.WIZARD_COLLECTIONS)

        compare_webadd(wizard, -1)

    def test_search_result(self):
        whole_nine_yards = self.parse_file('whole_nine_yards.json')
        components = whole_nine_yards['components']

        component = components[13]

        component_info = component['componentInfo']
        assert component_info['type'] == ComponentTypes.SEARCH_RESULT
        assert component_info['alignment'] == Alignments.LEFT
        assert component_info['rank'] == 1

        assert component['text.title'] == "Девять ярдов 2 \" Кино и сериалы онлайн"
        assert component['text.snippet'] == "У нашумевшего хита <b>Девять</b> <b>ярдов</b> - есть и не менее искрометное продолжение!"
        assert component['text.shard'] == "imgsidx-406-20200124-044306"

        assert component['componentUrl']['pageUrl'] == "https://kinoserial.tv/films/5102-devyat-yardov-2.html"
        assert component['componentUrl']['viewUrl'] == "kinoserial.tv"

        compare_webadd(component, 106716312.0)

        assert component['text.SERVER_DESCR'] == 'IMAGES'
        assert component['text.imageDocumentId'] == 'wgT26T7nx1YAAAAAAAAAAA=='
        assert component['long.mtime'] == 1465488542

        assert component['url.imageBigThumbHref'] == "http://im0-tub-ru.yandex.net/i?id=f0aed4863018e6375a4c7b6070810017-l&n=13"
        assert component['dimension.SCRAPER_IMAGE_DIMENSION'] == {'w': 869, 'h': 676}

        candidates = component['imageCandidates']
        assert len(candidates) == 6
        first_candidate = candidates[0]
        assert first_candidate['url'] == "https://kinoserial.tv/uploads/posts/2018-02/1519314405-614883786-1.jpg"
        assert first_candidate['crc'] == -2412973246486162019
        assert first_candidate['bigThumbHref'] == "http://im0-tub-ru.yandex.net/i?id=f0aed4863018e6375a4c7b6070810017-l&n=13"
        assert first_candidate['width'] == 869
        assert first_candidate['height'] == 676

        for candidate in candidates:
            assert candidate['bigThumbHref'] is not None

        assert component['imageadd']['candidates'] == [c['url'] for c in candidates]

    def test_market_wizard(self):
        market = self.parse_file('market.json')
        components = market['components']
        assert len(components) == 34

        wizard_indices = [1, 3, 6, 9]

        wizards = [c for i, c in enumerate(components) if i in wizard_indices]
        for w in wizards:
            compare_component_info(
                w,
                ComponentTypes.ADDV,
                Alignments.LEFT,
                WizardTypes.WIZARD_MARKET,
            )
        search_results = [c for i, c in enumerate(components) if i not in wizard_indices]
        for sr in search_results:
            compare_component_info(
                sr,
                ComponentTypes.SEARCH_RESULT,
                Alignments.LEFT,
            )

        market_wizard = wizards[0]
        compare_component_info(
            market_wizard,
            ComponentTypes.ADDV,
            Alignments.LEFT,
            WizardTypes.WIZARD_MARKET,
            expected_rank=1
        )

        compare_title(market_wizard, "Рюкзак OGIO Bandit 17, цвет черный/black (111074.03)")
        assert market_wizard['url.adv'] == "http://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjwp-NSB_UEZDamv" \
                                           "gZkwtvt8qUFG0yaL4qdGD-FNNK_8EXx7XougAigG6UkNjt1WRP1tNf6_4hAQJxTAKubs5cy4s4shb" \
                                           "hqQnW3bG5ok5R1FmNxpguRxZJ9gjcqUPrODpM_2FIli5xS_FLEV_Z6HbqzBQP9fasWzs9OPh-" \
                                           "93zvAu3NDUiPQAhg7xL8ymEPVA99PLgxhxdQidzmV4IbrH7tjqoPdMyjrY9vZIeW1O7v5jS06e3rQ" \
                                           "v4hPtpSD2jJY2yaDJU_IILVM0e72w5umlAWjT4sLCv2zQdmgRHG4glB5Iv1KyMs6j7eYB5WWaXLQf" \
                                           "Qfwqa8zWOGMkslftwB1L5neGg2YvfT4AG8n8RIK394bDORIOefJbTMTJO11Ua7s5BmAACJL7rGbt2Tr" \
                                           "3NXEYSJlXFjKI7ADfpNYPvrDoL21O9gyxjCoCbn0nVrKbcDoSGaMF2YvpBCffEV64u7GtrQDV4oHfvrX" \
                                           "dS__d1-iTuhjQCk7xMu8FI-ciGrCj9IqK4-knkQZvvkHtg7wTt9YTFa1qgnA_MJi85ukfy6iwqP08GuNw" \
                                           "97Bvy29EnKe-3SMdj8UNw1fO7P0mKYkNElPNYLi1rHuzYPm2Vyl0e0lJys-xWMPkRTT1aCXEbx77pYVCzcyQ-" \
                                           "Qfw4NRHRJFPJ-QjvzCaycvKYJrZJ_pS55hOFjBE0mxL-XzGpMfAXGmeCM7roYI7qKST94n0EyiHu0sYE20c6d" \
                                           "6yo-j50YKZ8XWQWngbktKzXvIWzdWWsAH-g87h4Oofx6A-OyaYJwo7inxDSZI_EwHF5OPdEXNfC6vlYHi30E-Y" \
                                           "kjmwAiy8vzePS0X3Ho6EV65IHujt-FF_8HSljpWKets4xxmdhLcrH5FxOkvd2VCWe0Mk8lFW1reKRe1h7Jcf" \
                                           "4nS4jeQYoIzWtpci6HSY2gyiatVuX2CUCeKK3_rLMwwRhiCfm-0,?data=QVyKqSPyGQwNvdoowNEPjfcz" \
                                           "L_7k97_rcBWurSlDAqLW-mLBhb2pku0FLgE_nWuNRkP5iTq3_LUYPgUD9eyw6Mv-XsZON6Z5pjQxPfHSv-" \
                                           "ZRwJfBr6FGxow5qPV7KA0GFvBZDY8owQLpqX-Jz4W0EfNTUVAdyemcF6aeM_AL2QR9AokTSPmnsgW1WJFv" \
                                           "9oVw9NbYY7FLZF3HxG0jM2TSYPn_TBuWf-69OPvpSr0oFNA9xm4weEI7BGiRObE8i0GcvUqAS5OaSzjZn2" \
                                           "sJ0aVcZ4gIWTLyLjQi_mzZMi83VtG59xAVcu0fJd2gNziz88kn0TLys1tMzldmTf6RTOourkQeqm0bOigcS98E6" \
                                           "NJDt_vtx24D61RIEFjpdMq7ZQUF&b64e=1&sign=6fb838c5441fb8e3423c3e22a4caa21e&keyno=1"

        assert market_wizard['componentUrl']['pageUrl'] == \
               "https://www.bymobile.ru/aksessuary-apple/chekhly-dlja-macbook/ryukzaki-dlya-noutbukov/rjukzak-dlja-noutbukov-do-17-ogio-bandit-17-11107403.htm"
        assert market_wizard['componentUrl']['viewUrl'] == "www.bymobile.ru"

        assert market_wizard.get('imageCandidates')
        first_candidate_url = "http://avatars.mds.yandex.net/get-marketpic/480326/market_gr2ruD0QWXMbFTUY_r1yDA/190x250"
        assert market_wizard['imageCandidates'][0]['url'] == first_candidate_url
        assert market_wizard.get('imageadd')
        assert market_wizard['imageadd']['candidates']
        assert market_wizard['imageadd']['candidates'][0] == first_candidate_url
        compare_webadd(market_wizard, -1.0)

    def test_wm_vs_mrq(self):
        base_url = "http://yandex.ru/images/touch/search?text=%D0%B4%D0%B5%D0%B4%D0%BF%D1%83%D0%BB+2&lr=10649&no-tests=1" \
                   "&numdoc=30&exp_flags=images_numdoc%3D30&json_dump=searchdata.numdocs&json_dump=searchdata.images" \
                   "&json_dump=unanswer_data&json_dump=search_props&json_dump=searchdata.reask&json_dump=searchdata.reask.text" \
                   "&json_dump=wizplaces.related.0&json_dump=wizplaces.pdb.0&json_dump=eventlogs" \
                   "&flag=restriction_profile%3Dweak_consistency__image__desktop__production" \
                   "&sbh=1&debug=show_eventlogs&debug=logsrcconfig&debug=dump_sources_answer_stat&srcskip=IMAGES_BS_PROXY" \
                   "&reqinfo=scrape-robot-imganalyst-robot-imganalyst-1582693244297&stype=image&nocache_completely=da" \
                   "&init_meta=use-src-tunneller&timeout=10000000&srcrwr=MISSPELL%3A%3A%3A1000" \
                   "&srcrwr=APP_HOST_MISSPELL%3A%3A%3A1000&srcrwr=APP_HOST_WEB_SETUP%3A%3A%3A1000" \
                   "&srcrwr=APP_HOST_ADV_MACHINE%3A%3A%3A1000&nocookiesupport=yes" \
                   "&waitall=da&pron=scrape-robot-imganalyst-robot-imganalyst-1582693244297" \
                   "&relev=porn_rnd_perm_weight%3D0&nocache=da"

        page_url = "http://yandex.ru/images/touch/search?app_host_params=need_debug_info=1&debug=show_eventlogs" \
                   "&debug=logsrcconfig&debug=dump_sources_answer_stat&debug=dump_sources_answer_stat" \
                   "&exp_flags=images_numdoc=30&flag=restriction_profile=weak_consistency__image__desktop__production" \
                   "&flag=scraper_mapper_req_id=117&init_meta=use-src-tunneller&init_meta=use-src-tunneller" \
                   "&init_meta=need_debug_info=1&init_meta=need_selected_with_address=1&init_meta=has_scraper_mapper=da" \
                   "&init_meta=has_scraper_mapper_web=da&init_meta=has_scraper_mapper_video=da" \
                   "&init_meta=has_scraper_mapper_videohosting=da&init_meta=has_scraper_mapper_videoquick=da" \
                   "&init_meta=has_scraper_mapper_images=da&init_meta=has_scraper_mapper_imagesquick=da" \
                   "&init_meta=has_scraper_mapper_imagesultra=da&init_meta=has_scraper_mapper_web=da" \
                   "&json_dump=searchdata.numdocs&json_dump=searchdata.images&json_dump=unanswer_data" \
                   "&json_dump=search_props&json_dump=searchdata.reask&json_dump=searchdata.reask.text" \
                   "&json_dump=wizplaces.related.0&json_dump=wizplaces.pdb.0&json_dump=eventlogs" \
                   "&lr=10649&no-tests=1&nocache=da&nocache_completely=da&nocookiesupport=yes" \
                   "&numdoc=30&pc=brotli5&pron=scrape-robot-imganalyst-robot-imganalyst-1582779802265&relev=porn_rnd_perm_weight=0" \
                   "&reqinfo=scrape-robot-imganalyst-robot-imganalyst-1582779802265&reqinfo=scraperoverytID=" \
                   "ec77f85b-9128e113-48a7db53-74515444&sbh=1&srcrwr=MISSPELL:::1000" \
                   "&srcrwr=APP_HOST_MISSPELL:::1000&srcrwr=APP_HOST_WEB_SETUP:::1000" \
                   "&srcrwr=APP_HOST_ADV_MACHINE:::1000" \
                   "&srcrwr=SCRAPER_MAPPER:[2a02:6b8:c1c:1fa2:0:4397:cdb1:0]:31008:10000" \
                   "&srcskip=IMAGES_BS_PROXY&stype=image&text=дедпул+2&timeout=10000000&waitall=da"

        deadpool = self.parse(
            self._read_file("deadpool.json"),
            {
                "url": base_url,
                "page_url": page_url,
            }
        )
        components = deadpool['components']

        for c in components[:40]:
            compare_component_info(c, expected_type=WIZARD)

            wizard_type = c['componentInfo']['wizardType']
            assert wizard_type == WizardTypes.METRICS_RELATED_QUERIES or WizardTypes.METRICS_MOBILE_RELATED_QUERIES

        wizard_misprint = components[40]
        compare_component_info(wizard_misprint, expected_type=WIZARD, expected_wizard_type=WizardTypes.WIZARD_MISPRINT)
        assert wizard_misprint['componentUrl']['pageUrl'] == page_url + "&noreask=1"

    @pytest.mark.parametrize("country, tld", [
        ("DE", "com"),
        ("RU", "ru"),
    ])
    def test_tld_mapping(self, country, tld):
        tlds = self.get_parser().get_tlds()
        assert tlds.get(country) == tld


def compare_title(component, expected_title):
    assert component['text.title'] == expected_title


def compare_component_info(
        component,
        expected_type=None,
        expected_alignment=None,
        expected_wizard_type=None,
        expected_rank=None
):
    component_info = component['componentInfo']
    if expected_type:
        assert component_info['type'] == expected_type
    if expected_alignment:
        assert component_info['alignment'] == expected_alignment
    if expected_wizard_type:
        assert component_info['wizardType'] == expected_wizard_type
    if expected_rank:
        assert component_info['rank'] == expected_rank


def compare_webadd(
        component,
        relevance,
        is_found_by_link=False,
        is_fast_robot_src=False,
        has_video_player=False,
):
    webadd = component.get('webadd')
    assert webadd, "component's webadd is missing"
    assert webadd['relevance'] == pytest.approx(relevance), "component's relevance doesn't match the expected value"
    assert webadd['isFoundByLink'] == is_found_by_link
    assert webadd['isFastRobotSrc'] == is_fast_robot_src
    assert webadd['hasVideoPlayer'] == has_video_player


def test_crc_parsing():
    assert ImageUtils._parse_crc(3192029553092072432) == 3192029553092072432
    for number in [
        3192029553092072400,
        15992226059614706448,
    ]:
        assert -9223372036854775808 <= ImageUtils._parse_crc(number) <= 9223372036854775807


@pytest.mark.parametrize("big_thumb_href, expected_big_thumb_href", [
    ("", ""),
    ("http://im0-tub-by.yandex.net/i?id=47a0e72bacbcd6b963b380ded26e51e2-l", "http://im0-tub-by.yandex.net/i?id=47a0e72bacbcd6b963b380ded26e51e2-l&n=13"),
])
def test_normalize_big_thumb_href(big_thumb_href, expected_big_thumb_href):
    assert ImageUtils._normalize_big_thumb_href(big_thumb_href) == expected_big_thumb_href
