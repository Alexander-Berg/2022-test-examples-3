import pytest

from test_utils import TestParser


class TestYandexNewsTopDocumentJSONParser(TestParser):
    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("unprepared_query.json", "prepared_query.json")
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename)

    def test_parser(self):
        def components_len_asserts(components, correct_len):
            assert len(components) == correct_len
            assert len(list(filter(lambda x: x["componentInfo"]["type"] == 1, components))) == correct_len
            assert len([c["type"] for c in components if c["type"] == "COMPONENT"]) == correct_len

        def components_data_asserts(components, **kwargs):
            def get_field_from_list(components, field_name):
                return [component[field_name] for component in components]

            urls = kwargs["urls"]
            assert [component["componentUrl"]["pageUrl"] for component in components][:len(urls)] == urls

            document_ages = kwargs["document_ages"]
            assert get_field_from_list(components, "long.documentAge")[:len(document_ages)] == document_ages

            titles = kwargs["titles"]
            assert get_field_from_list(components, "text.title")[:len(titles)] == titles

            snippets = kwargs["snippets"]
            assert get_field_from_list(components, "text.snippet")[:len(snippets)] == snippets

        parsed = self.parse_file("search_answer.json", additional_parameters={"userdata": {"soy-curl": {"uri": "http://yandex.ru/news/search?pron=1612270018"}}})
        components = parsed["components"]

        components_len_asserts(components, 16)

        urls = [
            "https://360tv.ru/news/transport/jandeks-raskryl-summu-sdelki-pokupki-vezet/",
            "https://www.cnews.ru/news/line/2021-02-02_yandeks_nazval_samye_populyarnye",
            "https://trashbox.ru/link/2021-02-01-yandex-plus-free-delivery",
            "https://53news.ru/novosti/64630-yandeks-i-pravitelstvo-novgorodskoj-"
            "oblasti-rasskazali-o-sovmestnykh-proektakh-i-planakh.html",
            "https://retail-loyalty.org/news/v-yandeks-lavke-poyavilas-detskaya-gotovaya-eda/",
            "https://davydov.in/povestka-messendzhery/o-kvotax-na-socreklamu-v-google-i-yandex/",
            "https://probalakovo.ru/2021/02/02/v-balakovo-taksisty-yandeksa-bastuyut-"
            "iz-za-raboty-po-14-16-chasov-i-nizkoj-pribyli/"
        ]
        document_ages = [
            6538000,
            5532000,
            85098000,
            61138000,
            100412000,
            82935000,
            22809000
        ]
        titles = [
            "«\u0007[Яндекс\u0007]» раскрыл сумму сделки покупки «Везет»",
            "«\u0007[Яндекс\u0007]» назвал самые популярные темы для спама в России",
            "\u0007[Яндекс\u0007] сделал доставку товаров с Маркета бесплатной для подписчиков \u0007[Яндекс\u0007].Плюс",
            "Как «\u0007[Яндекс\u0007]» изменил жизнь в одном отдельно взятом городе",
            "В \u0007[Яндекс\u0007].Лавке появилась детская готовая еда"
        ]
        snippets = [
            "Компания «\u0007[Яндекс\u0007].Такси» объявила о покупке колл-центров и бизнеса по "
            "заказу грузоперевозок группы компаний «Везет». Сумма сделки составит 178 миллионов долларов.",
            "Команда «\u0007[Яндекс\u0007].Почты 360» выяснила, какие темы использовали спамеры "
            "в 2020 г. Возглавили список рассылки о разного рода выплатах, в основном связанных с коронавирусом.",
            "Пользователи, которые оформили подписку \u0007[Яндекс\u0007].Плюс, теперь могут заказывать "
            "товары с \u0007[Яндекс\u0007].Маркета с бесплатной доставкой. Услуга на текущий момент доступна в более чем 100 городах России..."
        ]
        components_data_asserts(components, urls=urls, document_ages=document_ages, titles=titles, snippets=snippets)

        parsed = self.parse_file(
            "search_answer.json",
            additional_parameters={"userdata": {"soy-curl": {"uri": "http://yandex.ru/news/search?pron=1612270018"}}, "config": {"ungroup_cluster_docs": True}}
        )
        components = parsed["components"]

        components_len_asserts(components, 19)

        urls = [
            "https://360tv.ru/news/transport/jandeks-raskryl-summu-sdelki-pokupki-vezet/",
            "https://bcs-express.ru/novosti-i-analitika/iandeks-pokupaet-chast-biznesa-gruppy-vezet",
            "https://www.nakanune.ru/news/2021/02/02/22594067/",
            "https://www.cnews.ru/news/line/2021-02-02_yandeks_nazval_samye_populyarnye",
            "https://trashbox.ru/link/2021-02-01-yandex-plus-free-delivery",
            "https://53news.ru/novosti/64630-yandeks-i-pravitelstvo-novgorodskoj-oblasti-rasskazali-o-sovmestnykh-proektakh-i-planakh.html",
            "https://retail-loyalty.org/news/v-yandeks-lavke-poyavilas-detskaya-gotovaya-eda/",
            "https://davydov.in/povestka-messendzhery/o-kvotax-na-socreklamu-v-google-i-yandex/",
            "https://tech.sm.news/v-rossii-yandeks-i-google-obyazhut-razmeshhat-na-sajtax-socialnuyu-reklamu-13271/"
        ]
        document_ages = [6538000, 7078000, 6118000, 5532000, 85098000]
        titles = [
            "«\u0007[Яндекс\u0007]» раскрыл сумму сделки покупки «Везет»",
            "\u0007[Яндекс\u0007] покупает часть бизнеса группы «Везет»",
            "«\u0007[Яндекс\u0007].Такси» купит call-центры и грузовой бизнес группы «Везет» за 178 миллионов долларов",
            "«\u0007[Яндекс\u0007]» назвал самые популярные темы для спама в России"
        ]
        snippets = [
            "Компания «\u0007[Яндекс\u0007].Такси» объявила о покупке колл-центров и бизнеса по "
            "заказу грузоперевозок группы компаний «Везет». Сумма сделки составит 178 миллионов долларов.",
            "Группа компаний \u0007[Яндекс\u0007].Такси покупает колл-центры и бизнес по заказу грузоперевозок "
            "группы компаний «Везет», говорится в пресс-релизе \u0007[Яндекса\u0007]. Сделка позволит \u0007[Яндекс\u0007].Такси улучшить клиентский...",
        ]
        components_data_asserts(components, urls=urls, document_ages=document_ages, titles=titles, snippets=snippets)
