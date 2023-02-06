#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BookNowCtr
from core.testcase import TestCase, main
import test_book_now


class T(TestCase):
    @classmethod
    def prepare(cls):
        """Из интересного про данные:
        0. Все модели, категории и так далее берутся из теста test_book_now.py
        1. Тестируем все в рамках модели 308
        2. Для трех магазинов CTR задаем явно (т.е. по ним есть записи в БД ctr)
        3. Для магазина 107 не указываем CTR и для него должен взяться дефолтный (очень низкий, чтобы выделялся)
        """
        # Импортируем все даныне из теста test_book_now.py
        test_book_now.prepare_book_now_data(cls)
        cls.index.book_now_ctrs += [
            BookNowCtr(shop_id=1111, category_id=1111, model_id=111, clicks=1, shows=100000),
            BookNowCtr(shop_id=104, category_id=508, model_id=308, clicks=10, shows=100),
            BookNowCtr(shop_id=105, category_id=508, model_id=308, clicks=9, shows=100),
            BookNowCtr(shop_id=106, category_id=508, model_id=308, clicks=8, shows=100),
        ]

    def test_page1(self):
        # Запрашиваем превую страницу врезки. Проверяем rank, котоырй должен быть равен ctr*1000000
        response = self.report.request_json('place=book_now_incut&rids=6&hyperid=308&yandexuid=1&bsformat=2')
        self.assertFragmentIn(
            response,
            {
                "incut": {
                    "results": [
                        {
                            "shop": {"shopId": 104},
                            "rank": 100000,
                        },
                        {
                            "shop": {"shopId": 105},
                            "rank": 90000,
                        },
                        {
                            "shop": {"shopId": 106},
                            "rank": 80000,
                        },
                    ]
                }
            },
            preserve_order=False,
        )

    def test_page2(self):
        # Проверяем, что магазин 107 улетел на 2-ю страницу и имеет низкий ctr
        response = self.report.request_json(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=1&bsformat=2&book-now-incut-page=2'
        )
        self.assertFragmentIn(
            response,
            {
                "incut": {
                    "results": [
                        {
                            "shop": {"shopId": 107},
                            "rank": 279,
                        },
                    ]
                }
            },
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
