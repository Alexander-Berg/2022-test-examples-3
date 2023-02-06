#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(title='excellent awesome model', aliases=['stupid alias']),
        ]

        cls.index.offers += [
            Offer(title='some cool offer', descr='description of this beautiful offer'),
        ]

    def test_body_on_parallel(self):
        '''Поиск по body на параллельном
        По умолчанию на параллельном включена Пантера, она игнорирует
        fuzzy и market_body_search и всегда ищет и по тайтлам и по описаниями и по алиасам

        При поиске по офферам market_body_search позволяет искать офферы,
        у которых часть слов из запроса находится в описании
        При поиске по моделям market_body_search не играет значения
        '''

        # офферный колдунщик формируется при при запросе по словам из title оффера (или по title целиком)
        response = self.report.request_bs('place=parallel&text=cool')
        self.assertFragmentIn(response, {"market_offers_wizard": []})

        # при запросе по словам из title и description (с флагом market_body_search=0 и без fuzzy=1) колдунщик формируется
        response = self.report.request_bs('place=parallel&text=cool+description&rearr-factors=market_body_search=0')
        self.assertFragmentIn(response, {"market_offers_wizard": []})
        response = self.report.request_bs(
            'place=parallel&text=some+cool+description+of+this+beautiful+offer&rearr-factors=market_body_search=0'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": []})

        # офферный колдунщик формируется при запросе по словам из title и description (market_body_search=1)
        response = self.report.request_bs('place=parallel&text=cool+description')
        self.assertFragmentIn(response, {"market_offers_wizard": []})
        response = self.report.request_bs('place=parallel&text=some+cool+description+of+this+beautiful+offer')
        self.assertFragmentIn(response, {"market_offers_wizard": []})

        # офферный колдунщик также формируется если включить fuzzy=1 (даже при выключенном market_body_search)
        response = self.report.request_bs(
            'place=parallel&text=cool+description&rearr-factors=market_body_search=0;fuzzy=1'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": []})
        response = self.report.request_bs(
            'place=parallel&text=some+cool+description+of+this+beautiful+offer&rearr-factors=market_body_search=0;fuzzy=1'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": []})

        # офферный колдунщик формируется если в запросе отсутствуют слова из title
        response = self.report.request_bs('place=parallel&text=description+of+beautiful')
        self.assertFragmentIn(response, {"market_offers_wizard": []})

        # офферный колдунщик формируется если в запросе отсутствуют слова из title (в том числе и c fuzzy=1)
        response = self.report.request_bs('place=parallel&text=description+of+beautiful&rearr-factors=fuzzy=1')
        self.assertFragmentIn(response, {"market_offers_wizard": []})

        # модельный колдунщик формируется только по словам из c title
        response = self.report.request_bs('place=parallel&text=awesome+model&rearr-factors=market_body_search=0')
        self.assertFragmentIn(response, {"market_model": []})

        # модельный колдунщик формируется если указаны какие-либо дополнительные слова из aliases
        response = self.report.request_bs('place=parallel&text=awesome+model+stupid')
        self.assertFragmentIn(response, {"market_model": []})

    def test_bodysearch_on_prime(self):
        '''На prime/visual по умолчанию market_body_search=1
        На prime по умолчанию fuzzy=1

        При поиске по офферам fuzzy=1 и market_body_search=1 почти что взаимозаменяемы
        (кроме того что fuzzy=1 позволяет добавлять еще и вообще не присутствующие слова)
        При поиске по моделям market_body_search не играет значения
        '''

        # Оффер ищется по словам из title и description а также с наличием лишних слов в запросе
        # (т.к. по умолчанию fuzzy=1)
        response = self.report.request_json('place=prime&text=cool+description')
        self.assertFragmentIn(response, {"entity": "offer"})
        response = self.report.request_json('place=prime&text=some+cool+description+of+this+beautiful+offer')
        self.assertFragmentIn(response, {"entity": "offer"})
        response = self.report.request_json('place=prime&text=cool+offer+description+bla')
        self.assertFragmentIn(response, {"entity": "offer"})

        # Оффер ищется по словам из title и description если установить fuzzy=0
        response = self.report.request_json('place=prime&text=cool+description&rearr-factors=fuzzy=0')
        self.assertFragmentIn(response, {"entity": "offer"})
        response = self.report.request_json(
            'place=prime&text=some+cool+description+of+this+beautiful+offer&rearr-factors=fuzzy=0'
        )
        self.assertFragmentIn(response, {"entity": "offer"})

        # При выключенном флаге market_body_search и fuzzy=0 офферы не найдутся если все слова не присутствуют в title
        response = self.report.request_json(
            'place=prime&text=cool+description&rearr-factors=market_body_search=0;fuzzy=0'
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})

        # Если при выключенном market_body_search присутствует fuzzy=1 - то оффер найдется
        response = self.report.request_json(
            'place=prime&text=cool+description&rearr-factors=market_body_search=0;fuzzy=1'
        )
        self.assertFragmentIn(response, {"entity": "offer"})

        # И даже если будут слова не из title или description
        response = self.report.request_json(
            'place=prime&text=some+cool+offer+bla&rearr-factors=market_body_search=0;fuzzy=1'
        )
        self.assertFragmentIn(response, {"entity": "offer"})

        # Модель ищется даже если есть лишние слова (за счет fuzzy=1)
        response = self.report.request_json('place=prime&text=excellent+model+bla')
        self.assertFragmentIn(response, {"entity": "product"})

        # Модель можно найти только по совпадению слов из title если fuzzy=0 (даже не смотря на market_body_search=1 по умолчанию)
        response = self.report.request_json('place=prime&text=excellent+model&rearr-factors=fuzzy=0')
        self.assertFragmentIn(response, {"entity": "product"})

        # Модель нельзя найти если некоторые слова не присутствуют в title если fuzzy=0 (даже если они совпадают с алиасами)
        response = self.report.request_json('place=prime&text=excellent+awesome+model+alias&rearr-factors=fuzzy=0')
        self.assertFragmentNotIn(response, {"entity": "product"})

    def test_description_highlight(self):
        '''
        MARKETOUT-12819
        Проверка подсветки описания
        '''

        # Задаем запросы в place prime, содержащий слова из описания оффера
        # со включенным флагом description_hilite
        # проверяем, что:
        # 1. Описание приходит в виде структуры, а не одной строки
        # 2. Слова из запроса помечены как подсвеченные
        # 3. Cлова, которых не было в запросе, не помечены для подсвечивания

        response = self.report.request_json('place=prime&text=cool+description' '&rearr-factors=description_hilite=1')
        self.assertFragmentIn(
            response,
            {
                "description": {
                    "raw": "description of this beautiful offer",
                    "highlighted": [
                        {"value": "description", "highlight": True},
                        {"value": " of this beautiful offer"},
                    ],
                }
            },
        )

        response = self.report.request_json(
            'place=prime&text=cool+offer+description+bla&' 'rearr-factors=description_hilite=1'
        )
        self.assertFragmentIn(
            response,
            {
                "description": {
                    "raw": "description of this beautiful offer",
                    "highlighted": [
                        {"value": "description", "highlight": True},
                        {"value": " of this beautiful "},
                        {"value": "offer", "highlight": True},
                    ],
                }
            },
        )


if __name__ == '__main__':
    main()
