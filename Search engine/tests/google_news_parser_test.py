import json
import pytest
import responses

from test_utils import TestParser


class TestGoogleNewsParser(TestParser):
    GEOBASE_URL_TEMPLATE = "http://geobase.qloud.yandex.ru/v1/region_by_id?id={}"
    MOSCOW_ID = 213

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("unprepared_query.json", "prepared_query.json")
    ])
    @pytest.mark.freeze_time('2020-06-25')
    @responses.activate
    def test_preparer(self, input_filename, expected_output_filename):
        def request_callback(request):
            REGION_COORDINATES = {
                f"{self.MOSCOW_ID}": {
                    "latitude": 55.753215,
                    "longitude": 37.622504
                }
            }
            if (request.headers.get("Authorization") != "news") or (not request.url.endswith(f"id={self.MOSCOW_ID}")):
                return 404, {}, []
            return 200, {}, json.dumps(REGION_COORDINATES[f"{self.MOSCOW_ID}"])

        responses.add_callback(
            responses.GET,
            self.GEOBASE_URL_TEMPLATE.format(self.MOSCOW_ID),
            callback=request_callback,
            content_type="application/json"
        )

        self.compare_preparer_output(input_filename, expected_output_filename, host="google.ru")
        assert len(responses.calls) == 1

    @staticmethod
    def common_test_parser_asserts(components):
        assert len(components) == 10
        assert len([x for x in components if x["componentInfo"]["type"] == 1]) == 10
        assert len([c["type"] for c in components if c["type"] == "COMPONENT"]) == 10

    @staticmethod
    def compare_page_urls(components, urls):
        assert [x["componentUrl"]["pageUrl"] for x in components][:len(urls)] == urls

    def test_parser(self):
        parsed = self.parse_file("search_answer.json")
        components = parsed["components"]

        TestGoogleNewsParser.common_test_parser_asserts(components)

        urls = [
            "https://news.sportbox.ru/Vidy_sporta/Futbol/Evropejskie_chempionaty/"
            "Italiya/spbnews_NI1204510_Mauricio_Sarri_Vse_govorat_o_padenii_Juventusa"
            "_a_my_pervyje_Ja_ustal_slyshat_chrezmernuju_kritiku",
            "https://3dnews.ru/1014044",
            "https://www.championat.com/boxing/news-4064133-blejds-ja-ustal"
            "-volkova-bylo-gorazdo-tjazhelee-poborot-chem-ja-ozhidal.html",
            "http://www.19rus.info/index.php/vlast-i-politika/item/"
            "129714-ya-ustal-ya-ukhozhu-ya-otdokhnul-ya-prikhozhu",
            "https://74.ru/text/politics/2020/06/25/69334909/comments/",
            "https://naviny.by/article/20200603/"
            "1591179532-vladimir-mackevich-chto-delat-ya-ustal-ya-uhozhu"
        ]

        TestGoogleNewsParser.compare_page_urls(components, urls)

        assert components[0]["text.title"] == "Маурицио Сарри: «Все говорят о падении «Ювентуса», а ..."

        second_snippet = "Я устал, но не ухожу: Нила Дракманна измотали беспочвенные обвинения в адрес "\
                         "разработчиков The Last of Us Part II. 23.06.2020 [14:57], Дмитрий\xa0..."

        assert components[1]["text.snippet"] == second_snippet

        third_sitelinks_urls = [
            "https://www.gazeta.ru/sport/2020/06/21/a_13124977.shtml",
            "https://www.sports.ru/tribuna/blogs/mmardoboi/2794928.html"
        ]
        assert [sitelink["sitelinks"]["url"] for sitelink in components[2]["site-links"]] == third_sitelinks_urls

    def test_parser_touch(self):
        parsed = self.parse_file("search_answer_touch.txt")
        components = parsed["components"]

        TestGoogleNewsParser.common_test_parser_asserts(components)

        urls = [
            "https://russian.rt.com/russia/news/759651-putin-prikaz-prizyv/amp",
            "https://amp.rbc.ru/rbcnews/society/29/06/2020/5efa14509a79473e67d889dd",
            "https://m.gazeta.ru/amp/army/2020/06/29/13135639.shtml",
            "https://echo.msk.ru/news/2668165-echo.html",
            "https://ria.ru/amp/20200629/1573647987.html",
            "https://amp.vesti.ru/doc.html?id=3277170",
            "https://m.lenta.ru/news/2020/06/29/meeting/amp/",
            "https://amp.ura.news/news/1052438521",
            "https://tass.ru/armiya-i-opk/8841927/amp",
            "https://meduza.io/amp/news/2020/06/28/putin-rasskazal-chto-"
            "sdaet-testy-na-koronavirus-raz-v-tri-chetyre-dnya"
        ]

        TestGoogleNewsParser.compare_page_urls(components, urls)

        assert len([c for c in components if c.get("tags.isAmp", False)]) == 9

        original_urls = [
            "https://russian.rt.com/russia/news/759651-putin-prikaz-prizyv",
            "https://www.rbc.ru/society/29/06/2020/5efa14509a79473e67d889dd",
            "https://www.gazeta.ru/army/2020/06/29/13135639.shtml",
            "",
            "https://ria.ru/20200629/1573647987.html",
            "https://www.vesti.ru/doc.html?id=3277170",
            "https://lenta.ru/news/2020/06/29/meeting",
            "https://ura.news/news/1052438521",
            "https://tass.ru/armiya-i-opk/8841927",
            "https://meduza.io/news/2020/06/28/putin-rasskazal-chto-sdaet-testy-"
            "na-koronavirus-raz-v-tri-chetyre-dnya"
        ]
        assert [x.get("url.originalUrl", "") for x in components] == original_urls
        assert [x.get("url.ampOriginalUrl", "") for x in components] == original_urls

        titles = [
            "Путин подписал указ о призыве запасников на военные сборы",
            "Путин посмертно наградил волонтера за борьбу с вирусом орденом Пирогова",
            "Путин поручил призвать запасников на военные сборы",
            "Президент Путин подписал указ о призыве на военные сборы россиян, \nнаходящихся в запасе",
            "Путин наградил волонтера Светлану Анурьеву орденом Пирогова посмертно"
        ]
        assert [x["text.title"] for x in components][:len(titles)] == titles
