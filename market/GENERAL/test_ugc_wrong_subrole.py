#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, UGCItem
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_wrong_subrole(cls):
        cls.settings.report_subrole = 'api'

        embs = [0.4] * 50

        cls.saas_ugc.on_request(embs=embs, search_size=100, top_size=10,).respond(
            items=[
                UGCItem(
                    relevance=988795807,
                    attributes={
                        'page_id': '56122',
                        'compiled_generation': '20200604_0600',
                        'pages_generation': '20200608_0800',
                        'title': 'Как сделать эко-люстру',
                        'type': 'knowledge',
                        'semantic_id': 'kak-sdelat-ehko-lyustru',
                        'image': '{}',
                    },
                ),
                UGCItem(
                    relevance=892847537,
                    attributes={
                        'page_id': '56124',
                        'title': 'Как выбрать люстру',
                        'subtitle': 'Как же её выбрать',
                        'type': 'expertise',
                        'semantic_id': 'kak-vybrat-lustru',
                        'author_name': 'Федюков Иван Станиславович',
                        'author_description': 'С люстрами на ты',
                        'image': '{}',
                    },
                    plain_attributes={
                        'RequestIdInMultiRequest': '0',
                    },
                ),
                UGCItem(
                    relevance=885100722,
                    attributes={
                        'page_id': '56125',
                        'title': '7 правил хорошего освещения',
                        'description': 'Семь, то есть три плюс четыре',
                        'type': 'blog',
                        'semantic_id': '7-pravil-horoshego-osveshhenija',
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'image': '{}',
                    },
                ),
            ]
        )

        cls.index.dssm.hard2_query_embedding.on(query='люстры').set(*embs)
        cls.index.dssm.hard2_query_embedding.on(query='канделябры').set(*embs)

        cls.index.offers += [
            Offer(title="люстры 1", ts=1, price=800, hid=1),
            Offer(title="люстры 2", ts=2, price=700, hid=1),
            Offer(title="люстры 3", ts=3, price=600, hid=1),
            Offer(title="люстры 4", ts=4, price=500, hid=1),
            Offer(title="люстры 5", ts=5, price=400, hid=1),
        ]

    def test_wrong_subrole(self):
        """
        Проверяем, что на subrole != market не работает статейная врезка
        """

        request = "place=prime&text=люстры&numdoc=8&viewtype=grid"
        flag_pos = "&rearr-factors=market_ugc_saas_position=%d"

        response = self.report.request_json(request + flag_pos % 4)
        self.assertFragmentNotIn(
            response,
            {
                "entity": "materialEntrypoints",
            },
        )


if __name__ == '__main__':
    main()
