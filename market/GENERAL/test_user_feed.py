#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop
from core.bigb import WeightedValue, BigBKeyword
from core.dj import DjModel
from core.testcase import TestCase, main
from core.types.chat_info import ChatInfo


DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.models += [
            Model(hyperid=hyperid, hid=102, title='model{}'.format(hyperid)) for hyperid in range(11, 22)
        ]

        cls.index.shops += [
            Shop(
                fesh=555555,
                priority_region=213,
                name='shop with chat',
                chat_info=ChatInfo(chat_id="8390a14a-6749-407c-bcef-42d97f555555"),
            )
        ]

        cls.bigb.on_request(yandexuid='0002', client='merch-machine').respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            yandexuid='0002',
            exp='white_attractive_models_user_feed',
        ).respond(models=[DjModel(id=hyperid, title='model{}'.format(hyperid)) for hyperid in range(11, 22)])

        cls.index.offers += [
            Offer(hyperid=hyperid, fesh=555555, title='offer{}'.format(hyperid)) for hyperid in range(11, 22)
        ]

    def test_numdoc(self):
        """Проверяем параметр numdoc:
        -делаем запрос без numdoc, получаем 11 офферов(больше нет)
        -делаем запрос с numdoc=1, получаем 1 оффер
        -делаем запрос с numdoc=5, получаем 5 оффер
        -делаем запрос с numdoc=100, получаем 11 оффер(больше нет)
        """
        response = self.report.request_json('place=user_feed&yandexuid=0002&debug=da')
        self.assertFragmentIn(
            response,
            {"total": 11, "results": [{"titles": {"raw": "offer{}".format(hyperid)}} for hyperid in range(11, 22)]},
        )

        # Проверяем, что не делается запрос в saas чатов
        self.assertNotIn("Making a request to chats saas service", str(response))

        response = self.report.request_json('place=user_feed&yandexuid=0002&numdoc=1&debug=da')
        self.assertFragmentIn(
            response, {"total": 11, "results": [{"titles": {"raw": "offer11"}}]}, allow_different_len=False
        )

        # Проверяем, что не делается запрос в saas чатов
        self.assertNotIn("Making a request to chats saas service", str(response))

        response = self.report.request_json('place=user_feed&yandexuid=0002&numdoc=5&debug=da')
        self.assertFragmentIn(
            response,
            {"total": 11, "results": [{"titles": {"raw": "offer{}".format(hyperid)}} for hyperid in range(11, 16)]},
            allow_different_len=False,
        )

        # Проверяем, что не делается запрос в saas чатов
        self.assertNotIn("Making a request to chats saas service", str(response))

        response = self.report.request_json('place=user_feed&yandexuid=0002&numdoc=100&debug=da')
        self.assertFragmentIn(
            response,
            {"total": 11, "results": [{"titles": {"raw": "offer{}".format(hyperid)}} for hyperid in range(11, 22)]},
            allow_different_len=False,
        )

        # Проверяем, что не делается запрос в saas чатов
        self.assertNotIn("Making a request to chats saas service", str(response))

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='11')

    @classmethod
    def prepare_dj(cls):
        cls.dj.on_request(yandexuid='0003', exp='user_feed_exp_name').respond(
            [
                DjModel(id='13', title='model{13}'),
                DjModel(id='11', title='model{11}'),
                DjModel(id='12', title='model{12}'),
            ]
        )

    def test_dj(self):
        """
        Проверяем, что под флагом market_dj_exp_for_user_feed плейс user_feed ходит в dj за моделями
        и сохраняет их порядок на выдаче
        """
        response = self.report.request_json(
            'place=user_feed&yandexuid=0003&rearr-factors=market_dj_exp_for_user_feed=user_feed_exp_name'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 3,
                "results": [
                    {"entity": "offer", "model": {"id": 13}},
                    {"entity": "offer", "model": {"id": 11}},
                    {"entity": "offer", "model": {"id": 12}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
