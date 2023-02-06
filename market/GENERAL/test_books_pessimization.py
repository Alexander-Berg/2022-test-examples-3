#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Book,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
)
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=10758, name='Химки'),
                ],
            ),
            Region(
                rid=10650,
                name='Брянская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=191, name='Брянск'),
                ],
            ),
        ]
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(fesh=2, priority_region=191, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225]),
            Shop(fesh=4, priority_region=213, regions=[225]),
            Shop(fesh=5, priority_region=213, regions=[225]),
            Shop(fesh=6, priority_region=213, regions=[225]),
            Shop(
                fesh=1000,
                datafeed_id=1,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
            ),
        ]

        local_delivery = [DeliveryOption(price=100, day_from=0, day_to=2, order_before=24)]
        delivery_on_russia = [RegionalDelivery(rid=225, options=[DeliveryOption(price=100)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=1, fesh=1, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=2, fesh=2, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=3, fesh=3, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=4, fesh=4, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=5, fesh=5, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=6, fesh=6, carriers=[99], regional_options=delivery_on_russia),
        ]

        cls.index.offers += [
            Offer(
                title='pessimized book offer',
                fesh=5,
                hid=90829,
                is_book=True,
                hyperid=302,
                delivery_buckets=[5],
                delivery_options=local_delivery,
                ts=101,
            ),
            Offer(
                title='not a book offer',
                fesh=1,
                hid=1,
                hyperid=301,
                delivery_buckets=[1],
                delivery_options=local_delivery,
                ts=102,
            ),
            Offer(
                title='not a book offer', fesh=3, hid=1, delivery_buckets=[3], delivery_options=local_delivery, ts=103
            ),
            Offer(
                title='pessimized book offer',
                fesh=4,
                hid=90829,
                is_book=True,
                hyperid=303,
                delivery_buckets=[4],
                delivery_options=local_delivery,
                ts=104,
            ),
            Offer(
                title='pessimized book offer',
                fesh=2,
                hid=90829,
                is_book=True,
                delivery_buckets=[2],
                delivery_options=local_delivery,
                ts=105,
            ),
            Offer(
                title='not a book offer', hid=1, ts=100, fesh=6, delivery_buckets=[6], delivery_options=local_delivery
            ),
            Offer(
                title='no isbn (978-5-699-12014-7)', fesh=1, hid=90829, delivery_options=local_delivery, is_book=True
            ),
            Offer(fesh=1, hid=90829, delivery_options=local_delivery, is_book=True),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.5)

        cls.index.models += [
            Model(title='not a book model', hyperid=301, hid=1, barcode='4660014181384'),
        ]

        cls.index.books += [
            Book(title='pessimized book model', hyperid=302, hid=90829, isbn='978-5-905463-15-0'),
            Book(title='pessimized book model', hyperid=303, hid=90829, isbn='978-5-699-12014-7'),
            Book(title='out-of-root book model', hyperid=333333, hid=666666, isbn='978-5-699-12014-8'),
            Book(
                title='look: other isbn but with similar isbn 978-5-699-12014-7 in title',
                hyperid=304,
                hid=90829,
                isbn='978-5-699-12015-7',
            ),
        ]

        cls.index.hypertree += [HyperCategory(hid=666666, uniq_name='Textbooks', process_as_book_category=True)]

        # запрос формирует дерево (9785699120147 | (isbn:9785699120147) | (barcode:9785699120147))
        cls.reqwizard.on_request('9785699120147').respond(
            qtree='cHicpZKxS8NAHIXfO1Maj6SEDqVkCpmK07WoTTqJU3FydNRaIYIoxM3Fzk6dOohTdxfB0d3Rv8C_pXeXBEy0k7fdce973_04eSI9VwSiLyIMhEIXIWLsYYSJ5yJAH_ocCketaesUZzhHxiXxTKyJN-KD0OuT-OI8fKfMZBFzS5yfjpODwzQdjtRwf6zZYhKVdN_SaemYwtCzl9X3q6j49WyjTCHhsdLqCOv34vp2QMVK_AEL4omedIxj13E7OXRZuKLMS-_dSFjv9uwiv7ybX9lpdOIa0tq3f8ymsi8mU0XXbOT-ekHPtJqSX55GbEl50xBzru9nt1utnK1WNvd_pdh3H4MF9XfZMSWNrWLC4t4GwoBf8w,,'  # noqa
        )

        # на запрос без пробела реквизард формирует только поисковый литерал (isbn:9785699120147)
        cls.reqwizard.on_request('isbn:9785699120147').respond(
            remove_query=False,
            qtree='cHic4yri4uFgEGCQ4FRg0GAyYBBiySxOypNiUGLQ4lOyNLcwNbO0NDQyMDQxN2Kw4uFgAapkAKpkMGBwYPBgCGCIYEhgyJgz7d5apgmMDLMYwboXMaJp3cTIsJeRAQhOMDJcYEwxYLBgdBID2QqyR4PRgBFqDmMVQwMjA9AgAH5LGms,',
        )
        # аналогично на реквизард формирует только поисковый литерал (barcode:4660014181384)
        cls.reqwizard.on_request('barcode:4660014181384').respond(
            remove_query=False,
            qtree='cHic46rg4uFgEGCQ4FRg0GAyYBBiT0osSs5PSZViUGLQ4lMyMTMzMDA0MbQwNLYwMWKw4uFgBypmACpmMGBwYPBgCGCIYEhgyJgz7d5apgmMDLMYYQYsYkTTvYmRYS8jAxCcYGS4wJhiwGDB6CQGshtklQajASPUKMYqhgZGBqBZAJDgHJY,',  # noqa
        )

        # запрос 978-5-699-12014-7 формирует (978 ^ 5 ^ 699 ^ 12014 ^ 7) | (isbn:9785699120147) | (barcode:9785699120147)
        cls.reqwizard.on_request('978-5-699-12014-7').respond(
            qtree='cHicpZM9SyRBEIarZnt323ZPhhE_GBBl7jhEWGjFcVwREaNFVNREQ_UU7kAU9qI7ThQ10sRADMREf4KBmbmBgYFgYir4CwyMrO752N2hXU6crLqr3nqm3i4xKQrcsq1Oqwd6LQkOuOBBHwzASIGDDZ1A5yBhPFvOzsIiLMFPPEI4RbhAuES4RqDvBuEOV92HvCiLuMz6H7kGYnso5iIxHrFlSsGwi8VEMKMFUQtCGbTg1b0VS6r0lKqEYZz4Rj8Mrrr1mmX0UdCLEuNf_As7CKTjbonpFAT6CgGLEQMaGB6Tv0LfBOBpAPSr7aX0je33UcynhzBUKtURmKZw-8STKVB-gynQbRWDAiPGIYqFFEa2f0D2D9aBZA0gZ6_PiR9hiYnlu2YJ72MaHRhp_omptCcBPbOGlrywxJKggSVBrSWBqbsXr0uGbj8QqS6kcIDdgnFmM6eDo9vKc23i4eto1_kfZ6wHigoqzMjZufqMqd3jakYrZYBtOc2cz-c5OpmZ2bXwlNk8OrVqTnPJaZxb0ArMYbylAmGoWtaEnGp0qEZ-gqISzbwp2uz8ynLlx-bqml7wFo8WyKfno10LtA35mnWPbQgXIy69wFSdyZl21VU1Mb6FIxTrKTD26_fKxrtU7F0qXfd5JO8L37Z3UPtOTVKhRP0MKO8N9GTLCQ,,'  # noqa
        )
        cls.reqwizard.on_default_request().respond()

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100).respond(0.001)
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.1)

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_book_model_dropped(self):
        """Запрос без указания категории - под флагом market_use_books_pessimization=1 модели книг удаляются из выдачи"""
        response = self.report.request_json('place=prime&text=book&rearr-factors=market_use_books_pessimization=1')
        self.assertFragmentNotIn(response, {'raw': 'Book Writer "pessimized book model"'})
        self.assertFragmentNotIn(response, {'raw': 'Book Writer "out-of-root book model"'})
        self.assertFragmentIn(response, {"raw": "not a book model"})

    def test_book_model_not_dropped(self):
        """Пессимизация на prime не работает если выполняется запрос в книжную категорию"""

        response = self.report.request_json('place=prime&text=book&hid=90829')
        self.assertFragmentIn(response, {'raw': 'Book Writer "pessimized book model"'})
        self.assertFragmentIn(response, {'raw': 'pessimized book offer'})

    def test_book_model_not_dropped_oor(self):
        response = self.report.request_json('place=prime&text=book&hid=666666')
        self.assertFragmentIn(response, {'raw': 'Book Writer "out-of-root book model"'})

    def test_book_offers_pessimized_prime(self):
        """Офферы из книжных категорий пессимизируются под флагом market_use_books_pessimization=1"""
        response = self.report.request_json(
            'place=prime&text=book+offer&rearr-factors=market_use_books_pessimization=1'
        )
        self.assertFragmentIn(
            response,
            {
                "isFuzzySearch": Absent(),
                "results": [
                    {"entity": "offer", "categories": [{"id": 1}]},
                    {"entity": "offer", "categories": [{"id": 1}]},
                    {"entity": "offer", "categories": [{"id": 90829}]},
                    {"entity": "offer", "categories": [{"id": 90829}]},
                ],
            },
            preserve_order=True,
        )

    def test_book_offers_pessimized_with_lof(self):
        '''Проверяем что сохраняется базовый порядок офферов
        IS_NOT_BOOK, COURIER_DELIVERY_WITHOUT_OPTIONS, DELIVERY_TYPE, ...
        '''

        # сначала идут не книги. Т.к. они все из другого региона - появляется regionalDelimiter
        response = self.report.request_json(
            'place=prime&text=offer&rids=191&local-offers-first=1' '&rearr-factors=market_use_books_pessimization=1'
        )
        self.assertFragmentIn(
            response,
            {
                "isFuzzySearch": Absent(),
                "results": [
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "not a book offer"}},
                    {"titles": {"raw": "not a book offer"}},
                    {"titles": {"raw": "not a book offer"}},
                    {
                        "titles": {"raw": "pessimized book offer"}
                    },  # этот оффер из локального региона, но он идет ниже т.к. пессимизирован по IS_NOT_BOOK
                    {"titles": {"raw": "pessimized book offer"}},
                    {"titles": {"raw": "pessimized book offer"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_book_offers_not_pessimized_with_lof(self):
        """Проверяем базовый порядок офферов без пессимизации книг
        https://st.yandex-team.ru/MARKETOUT-20737
        """
        for rearr in ['', '&rearr-factors=market_use_books_pessimization=0']:
            response = self.report.request_json('place=prime&text=offer&rids=191&local-offers-first=1' + rearr)
            self.assertFragmentIn(
                response,
                {
                    "isFuzzySearch": Absent(),
                    "results": [
                        {"titles": {"raw": "pessimized book offer"}},
                        {"entity": "regionalDelimiter"},
                        {"titles": {"raw": "not a book offer"}},
                        {"titles": {"raw": "pessimized book offer"}},
                        {"titles": {"raw": "not a book offer"}},
                        {"titles": {"raw": "pessimized book offer"}},
                        {"titles": {"raw": "not a book offer"}},
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_book_offers_pessimized_without_lof(self):
        '''Проверяем что сохраняется базовый порядок офферов
        IS_NOT_BOOK, COURIER_DELIVERY_WITHOUT_OPTIONS, DELIVERY_TYPE, ...
        Сначала идут не книги
        '''
        response = self.report.request_json(
            'place=prime&text=offer&rids=191&local-offers-first=0' '&rearr-factors=market_use_books_pessimization=1'
        )
        self.assertFragmentIn(
            response,
            {
                "isFuzzySearch": Absent(),
                "results": [
                    {"titles": {"raw": "not a book offer"}},
                    {"titles": {"raw": "not a book offer"}},
                    {"titles": {"raw": "not a book offer"}},
                    {"titles": {"raw": "pessimized book offer"}},
                    {"titles": {"raw": "pessimized book offer"}},
                    {"titles": {"raw": "pessimized book offer"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Запрос где все офферы в одинаково без опций доставки - книжные офферы пессимизируются
        response = self.report.request_json(
            'place=prime&text=offer&rids=225&local-offers-first=0' '&rearr-factors=market_use_books_pessimization=1'
        )
        self.assertFragmentIn(
            response,
            {
                "isFuzzySearch": Absent(),
                "results": [
                    {"titles": {"raw": "not a book offer"}},
                    {"titles": {"raw": "not a book offer"}},
                    {"titles": {"raw": "not a book offer"}},
                    {"titles": {"raw": "pessimized book offer"}},
                    {"titles": {"raw": "pessimized book offer"}},
                    {"titles": {"raw": "pessimized book offer"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_book_offers_not_pessimized_without_lof(self):
        """Проверяем что сохраняется базовый порядок офферов без пессимизации книг
        https://st.yandex-team.ru/MARKETOUT-20737
        """
        for rearr in ['', '&rearr-factors=market_use_books_pessimization=0']:
            response = self.report.request_json('place=prime&text=offer&rids=191&local-offers-first=0' + rearr)

            self.assertFragmentIn(
                response,
                {
                    "isFuzzySearch": Absent(),
                    "results": [
                        {"titles": {"raw": "pessimized book offer"}},
                        {"titles": {"raw": "not a book offer"}},
                        {"titles": {"raw": "pessimized book offer"}},
                        {"titles": {"raw": "not a book offer"}},
                        {"titles": {"raw": "pessimized book offer"}},
                        {"titles": {"raw": "not a book offer"}},
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

            response = self.report.request_json('place=prime&text=offer&rids=225&local-offers-first=0' + rearr)
            self.assertFragmentIn(
                response,
                {
                    "isFuzzySearch": Absent(),
                    "results": [
                        {"titles": {"raw": "pessimized book offer"}},
                        {"titles": {"raw": "not a book offer"}},
                        {"titles": {"raw": "pessimized book offer"}},
                        {"titles": {"raw": "not a book offer"}},
                        {"titles": {"raw": "pessimized book offer"}},
                        {"titles": {"raw": "not a book offer"}},
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_book_offers_not_pessimized_on_blue(cls):
        book_category = 90829

        cls.index.mskus += [
            MarketSku(sku=1, title='blue offer sku1 book', hid=book_category, blue_offers=[BlueOffer(price=10)]),
            MarketSku(sku=2, title='blue offer sku2 book', hid=book_category, blue_offers=[BlueOffer(price=20)]),
            MarketSku(sku=3, title='blue offer sku3 not a book', blue_offers=[BlueOffer(price=30)]),
            MarketSku(sku=4, title='blue offer sku4 not a book', blue_offers=[BlueOffer(price=40)]),
        ]

    def test_book_offers_not_pessimized_on_blue(self):
        """Проверяем базовый порядок офферов без пессимизации книг на синем маркете
        https://st.yandex-team.ru/MARKETOUT-20737
        """
        # Без флага market_use_blue_books_pessimization или с market_use_blue_books_pessimization=1
        # книги пессимизируются на синем маркете
        response = self.report.request_json(
            'place=prime&text=blue+offer&rgb=blue&allow-collapsing=0&how=aprice'
            '&rearr-factors=market_use_books_pessimization=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "blue offer sku3 not a book"}},
                    {"entity": "offer", "titles": {"raw": "blue offer sku4 not a book"}},
                    {"entity": "offer", "titles": {"raw": "blue offer sku1 book"}},
                    {"entity": "offer", "titles": {"raw": "blue offer sku2 book"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Флаг market_use_blue_books_pessimization=0 отключает пессимизацию книг на синем маркете
        for rearr in ['', '&rearr-factors=market_use_books_pessimization=0']:
            response = self.report.request_json(
                'place=prime&text=blue+offer&rgb=blue&allow-collapsing=0&how=aprice' + rearr
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "offer", "titles": {"raw": "blue offer sku1 book"}},
                        {"entity": "offer", "titles": {"raw": "blue offer sku2 book"}},
                        {"entity": "offer", "titles": {"raw": "blue offer sku3 not a book"}},
                        {"entity": "offer", "titles": {"raw": "blue offer sku4 not a book"}},
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

    def test_search_books_by_isbn(self):
        '''Поиск по isbn - пессимизация книг выключена - книжные модели не дропаются'''
        response = self.report.request_json('place=prime&isbn=9785699120147')
        self.assertFragmentIn(response, {'search': {'total': 1}})
        self.assertFragmentIn(response, {'raw': 'Book Writer "pessimized book model"'})

        response = self.report.request_json('place=prime&isbn=978-5-699-12014-7')
        self.assertFragmentIn(response, {'search': {'total': 1}})
        self.assertFragmentIn(response, {'raw': 'Book Writer "pessimized book model"'})

    def test_search_books_by_isbn_autoacceptence(self):
        '''Поиск по isbn - пессимизация книг выключена - книжные модели не дропаются
        и приходят от пантеры как autoaccepted
        '''
        response = self.report.request_json('place=prime&isbn=9785699120147&text=9785699120147&debug=da')
        self.assertFragmentIn(response, {'search': {'total': 1}})
        self.assertFragmentIn(response, {'raw': 'Book Writer "pessimized book model"'})
        self.assertFragmentIn(response, {'debug': {'docRel': "10005000"}})

    def test_search_books_by_only_isbn_literal(self):
        '''Поиск по тексту, который распознается реквизардом как один isbn-литерал
        Также есть ограничение по hid (книжная категория) - книжки не пессимизируются
        Пантера не работает, используется формула для бестекстовых запросов

        Проверяем что при отсутствии текста (есть только литерал) и наличии hid
        ищется нужная книжка (isbn литерал учитывается при поиске)
        '''
        response = self.report.request_json('place=prime&text=isbn:9785699120147&hid=90829&debug=da')
        self.assertFragmentIn(
            response, {'search': {'total': 1, 'results': [{'entity': 'product', 'id': 303}]}}, allow_different_len=False
        )

    def test_search_by_only_barcode_literal(self):
        '''Поиск по тексту, который распознается реквизардом как один barcode-литерал
        Пантера не работает, используется формула для бестекстовых запросов
        '''
        response = self.report.request_json('place=prime&text=barcode:4660014181384&debug=da')
        self.assertFragmentIn(response, 'tweakBasesearchBehavior(): Disabling Panther')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'id': 301,
                            'debug': {
                                'docRel': '0',
                                'rankedWith': 'base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4',
                            },
                        }
                    ],
                }
            },
        )

    def test_search_books_by_isbn_in_text(self):
        '''Поиск по isbn в тексте'''

        # оффер находится потому что у него есть isbn в тексте
        response = self.report.request_json('place=prime&text=978-5-699-12014-7')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {'titles': {'raw': 'no isbn (978-5-699-12014-7)'}},
                    ],
                }
            },
            allow_different_len=True,
            preserve_order=False,
        )

        # с указанием книжной категории - находится и оффер и модели
        # оффер и модель находятся потому что у них есть isbn в тексте, модель pessimized book model - потому что у нее есть литерал isbn
        response = self.report.request_json('place=prime&text=978-5-699-12014-7&hid=90829')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {'titles': {'raw': 'no isbn (978-5-699-12014-7)'}},
                        {'titles': {'raw': 'Book Writer "pessimized book model"'}},
                        {
                            'titles': {
                                'raw': 'Book Writer "look: other isbn but with similar isbn 978-5-699-12014-7 in title"'
                            }
                        },
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )

        # если указать текст isbn целиком - найдется только модель pessimized book model, т.к. у нее есть подходящий литерал
        response = self.report.request_json('place=prime&text=9785699120147&hid=90829')
        self.assertFragmentIn(
            response,
            {'search': {'total': 1, 'results': [{'titles': {'raw': 'Book Writer "pessimized book model"'}}]}},
            allow_different_len=False,
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
