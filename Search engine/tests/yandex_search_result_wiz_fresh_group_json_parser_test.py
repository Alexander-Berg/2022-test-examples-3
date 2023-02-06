from test_utils import TestParser

from yandex_fresh_group_news.yandex_search_result_wiz_fresh_group_json_parser import YandexSearchResultWizFreshGroupJSONParser


class TestYandexSearchResultWizFreshGroupJSONParser(TestParser):
    _parser_class = YandexSearchResultWizFreshGroupJSONParser

    def test_base(self):
        # tests for a new type of news block: type=fresh_group
        self._test_fresh_group()

        # tests for a old type of news block: type=news
        self._test_news()

    def _check_asserts(self, canon_data, parsed_data):
        assert canon_data['type'] == parsed_data['text.wiz_type'], 'Types not equal'
        assert canon_data['docs_amount'] == len(parsed_data['components']), 'Docs amount not equal'
        for ind, (canon_doc, parsed_doc) in enumerate(zip(canon_data['docs'], parsed_data['components'])):
            assert canon_doc['url'] == parsed_doc['componentUrl']['pageUrl'], 'Urls not equal at {}'.format(ind)
            assert canon_doc['host'] == parsed_doc['text.host'], 'Hosts not equal at {}'.format(ind)
            assert canon_doc['title'] == parsed_doc['text.title'], 'Titles not equal at {}'.format(ind)
            assert canon_doc.get('doc_text') == parsed_doc.get('text.doc_text'), 'Texts not equal at {}'.format(ind)

    def _test_fresh_group(self):
        example = self.parse_file('test_fresh_group.json')

        canon_data = {
            "type": "fresh_group",
            "docs_amount": 5,
            "docs": [
                {
                    "url": "https://www.kp.ru/online/news/4836244/",
                    "host": "kp.ru",
                    "title": "В США сообщили, что арабские шейхи высмеяли просьбу Байдена",
                    "doc_text": "Напомним, Байден объяснял необходимость визита в Саудовскую Аравию тем, что взаимодействие с королевством поможет"
                        " США противостоять России и конкурировать с Китаем. Ранее эксперт по проблемам Ближнего..."
                },
                {
                    "url": "https://www.gazeta.ru/business/news/2022/07/18/18150614.shtml",
                    "host": "gazeta.ru",
                    "title": "The Guardian: нефть выше $100 говорит о безрезультатной поездке Байдена в Саудовскую Аравию - Газета.Ru | Новости"
                },
                {
                    "url": "https://ria.ru/20220718/bayden-1803250120.html",
                    "host": "ria.ru",
                    "title": "В США рассказали о высмеянной в Саудовской Аравии просьбе Байдена"
                },
                {
                    "url": "https://www.ntv.ru/novosti/2715302/",
                    "host": "ntv.ru",
                    "title": "Итоги турне по Ближнему Востоку: Байден возвращается без гарантий поставок нефти и новых союзников // НТВ.Ru"
                },
                {
                    "url": "https://regnum.ru/news/3649441.html",
                    "host": "regnum.ru",
                    "title": "Спросу на нефть помог безрезультатный визит Байдена на Ближний Восток"
                }
            ]
        }

        self._check_asserts(canon_data, example)

    def _test_news(self):
        example = self.parse_file('test_news.json')

        canon_data = {
            "type": "news",
            "docs_amount": 5,
            "docs": [
                {
                    "url": "https://360tv.ru/news/mir/poezdku-bajdena-v-saudovskuju-araviju-nazvali-pozornoj/",
                    "host": "360tv.ru",
                    "title": "Поездку Байдена в Саудовскую Аравию назвали позорной",
                    "doc_text": "Визит президента США Джо Байдена в Саудовскую Аравию обернулся позором для американского лидера, поскольку он не достиг соглашения ни по одному из..."
                },
                {
                    "url": "https://www.bfm.ru/news/504630",
                    "host": "bfm.ru",
                    "title": "Добился ли Байден успеха в своем ближневосточном турне?",
                    "doc_text": "Свое ближневосточное турне Джо Байден начал с Израиля. Американская делегация провела переговоры с временным правительством страны."
                },
                {
                    "url": "https://lenta.ru/news/2022/07/19/biden_ukr/",
                    "host": "lenta.ru",
                    "title": "Американский полковник заявил о поражении Байдена в битве за Украину",
                    "doc_text": "Президент США Джо Байден, решив довести свою опосредованную войну с Россией до конца, проигрывает битву за Украину, заявил американский полковник Дуглас..."
                },
                {
                    "url": "https://www.gazeta.ru/business/news/2022/07/19/18156836.shtml",
                    "host": "gazeta.ru",
                    "title": "WSJ: неудачный визит Байдена в Саудовскую Аравию навредил США - Газета.Ru | Новости",
                    "doc_text": "Визит президента США Джо Байдена в Саудовскую Аравию обернулся позором для американцев. Об этом пишет американская журналистка Карен Эллиотт Хаус в "
                        "статье, опубликованной в американской газете The Wall Street Journal."
                },
                {
                    "url": "https://ria.ru/20220719/ssha-1803440980.html",
                    "host": "ria.ru",
                    "title": "\"Хуже, чем позор\": в США осудили поведение Байдена в Саудовской Аравии",
                    "doc_text": "Джо Байден опозорился во время поездки в Саудовскую Аравию, нанеся ущерб интересам безопасности США, заявила обозреватель "
                        "газеты The Wall Street Journal Карен... РИА Новости, 19.07.2022. 2022-07-19T13:29."
                }
            ]
        }

        self._check_asserts(canon_data, example)
