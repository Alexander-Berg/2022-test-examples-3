#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, Model, NavCategory, Offer, RegionalModel, Shop
from core.testcase import main
from core.matcher import ElementCount
from simple_testcase import SimpleTestCase

# dfyz inspired


class T(SimpleTestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.offers += [Offer(title='vanished title', hyperid=123, discount=50)]

        cls.index.navtree += [
            NavCategory(nid=100, hid=100, name="Test cat"),
        ]

        cls.index.models += [Model(hyperid=123, title='vanished model title', hid=100)]

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=400, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=500, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=600, output_type=HyperCategoryType.GURU),
        ]

        # data for test_paging
        cls.index.navtree += [
            NavCategory(nid=200, hid=200, name="Test cat"),
            NavCategory(nid=300, hid=300, name="Test cat 2"),
        ]

        cls.index.models += [
            Model(hyperid=1, title='title 1', hid=200),
            Model(hyperid=2, title='title 2', hid=200),
            Model(hyperid=3, title='title 3', hid=200),
            Model(hyperid=4, title='title 4', hid=200),
            Model(hyperid=5, title='title 5', hid=200),
            Model(hyperid=6, title='title 6', hid=200),
            Model(hyperid=7, title='title 7', hid=200),
            Model(hyperid=8, title='title 8', hid=200),
            Model(hyperid=9, title='title 9', hid=200),
            Model(hyperid=10, title='title 10', hid=200),
            Model(hyperid=11, title='title 11', hid=200),
            Model(hyperid=12, title='title 12', hid=200),
        ]

        cls.index.models += [
            Model(hyperid=21, title='title 21', hid=300),
            Model(hyperid=22, title='title 22', hid=300),
            Model(hyperid=23, title='title 23', hid=300),
            Model(hyperid=24, title='title 24', hid=300),
            Model(hyperid=25, title='title 25', hid=300),
            Model(hyperid=26, title='title 26', hid=300),
            Model(hyperid=27, title='title 27', hid=300),
            Model(hyperid=28, title='title 28', hid=300),
            Model(hyperid=29, title='title 29', hid=300),
            Model(hyperid=30, title='title 30', hid=300),
            Model(hyperid=31, title='title 31', hid=300),
            Model(hyperid=32, title='title 32', hid=300),
            Model(hyperid=33, title='title 33', hid=300),
            Model(hyperid=34, title='title 34', hid=300),
            Model(hyperid=35, title='title 35', hid=300),
            Model(hyperid=36, title='title 36', hid=300),
            Model(hyperid=37, title='title 37', hid=300),
            Model(hyperid=38, title='title 38', hid=300),
            Model(hyperid=39, title='title 39', hid=300),
            Model(hyperid=40, title='title 40', hid=300),
            Model(hyperid=41, title='title 41', hid=300),
            Model(hyperid=42, title='title 42', hid=300),
            Model(hyperid=43, title='title 43', hid=300),
            Model(hyperid=44, title='title 44', hid=300),
            Model(hyperid=45, title='title 45', hid=300),
            Model(hyperid=46, title='title 46', hid=300),
            Model(hyperid=47, title='title 47', hid=300),
            Model(hyperid=48, title='title 48', hid=300),
            Model(hyperid=49, title='title 49', hid=300),
            Model(hyperid=50, title='title 50', hid=300),
            Model(hyperid=51, title='title 51', hid=300),
        ]

        cls.index.offers += [
            Offer(title='title N1', hyperid=1, discount=29),
            Offer(title='title N2', hyperid=2, discount=30),
            Offer(title='title N3', hyperid=3, discount=31),
            Offer(title='title N4', hyperid=4, discount=32),
            Offer(title='title N5', hyperid=5, discount=33),
            Offer(title='title N6', hyperid=6, discount=34),
            Offer(title='title N7', hyperid=7, discount=35),
            Offer(title='title N8', hyperid=8, discount=36),
            Offer(title='title N9', hyperid=9, discount=37),
            Offer(title='title N10', hyperid=10, discount=38),
            Offer(title='title N11', hyperid=11, discount=39),
            Offer(title='title N12', hyperid=12, discount=89),
        ]

        cls.index.offers += [
            Offer(title='title N21', hyperid=21, discount=28),
            Offer(title='title N22', hyperid=22, discount=29),
            Offer(title='title N23', hyperid=23, discount=30),
            Offer(title='title N24', hyperid=24, discount=31),
            Offer(title='title N25', hyperid=25, discount=32),
            Offer(title='title N26', hyperid=26, discount=33),
            Offer(title='title N27', hyperid=27, discount=34),
            Offer(title='title N28', hyperid=28, discount=35),
            Offer(title='title N29', hyperid=29, discount=36),
            Offer(title='title N30', hyperid=30, discount=37),
            Offer(title='title N31', hyperid=31, discount=39),
            Offer(title='title N32', hyperid=32, discount=89),
            Offer(title='title N33', hyperid=33, discount=90),
            Offer(title='title N34', hyperid=34, discount=90),
            Offer(title='title N35', hyperid=35, discount=90),
            Offer(title='title N36', hyperid=36, discount=90),
            Offer(title='title N37', hyperid=37, discount=90),
            Offer(title='title N38', hyperid=38, discount=90),
            Offer(title='title N39', hyperid=39, discount=90),
            Offer(title='title N40', hyperid=40, discount=90),
            Offer(title='title N41', hyperid=41, discount=90),
            Offer(title='title N42', hyperid=42, discount=90),
            Offer(title='title N43', hyperid=43, discount=90),
            Offer(title='title N44', hyperid=44, discount=90),
            Offer(title='title N45', hyperid=45, discount=90),
            Offer(title='title N46', hyperid=46, discount=90),
            Offer(title='title N47', hyperid=47, discount=90),
            Offer(title='title N48', hyperid=48, discount=90),
            Offer(title='title N49', hyperid=49, discount=90),
            Offer(title='title N50', hyperid=50, discount=90),
            Offer(title='title N51', hyperid=51, discount=99),
        ]

        # data for test filtering by vendor
        cls.index.models += [
            Model(hyperid=80, hid=400, vendor_id=1),
            Model(hyperid=81, hid=400, vendor_id=2),
            Model(hyperid=82, hid=400, vendor_id=3),
            Model(hyperid=83, hid=500, vendor_id=1),
            Model(hyperid=84, hid=500, vendor_id=2),
            Model(hyperid=85, hid=500, vendor_id=3),
            Model(hyperid=86, hid=600, vendor_id=1),
        ]

        cls.index.offers += [
            Offer(hyperid=80, discount=29),
            Offer(hyperid=81, discount=30),
            Offer(hyperid=82, discount=31),
            Offer(hyperid=83, discount=32),
            Offer(hyperid=84, discount=33),
            Offer(hyperid=85, discount=34),
            Offer(hyperid=86, discount=35),
        ]

        # data for *allowable_price_diff tests
        cls.index.regional_models += [
            RegionalModel(hyperid=210, rids=[225], offers=2, price_min=999.2, price_max=1400),
            RegionalModel(hyperid=220, rids=[225], offers=2, price_min=999, price_max=1400),
        ]

        cls.index.shops += [
            Shop(fesh=100, priority_region=225),
            Shop(fesh=101, priority_region=225),
        ]

        cls.index.models += [
            Model(hyperid=210, hid=101),
            Model(hyperid=220, hid=102),
        ]

        cls.index.offers += [
            Offer(fesh=100, hyperid=210, price=1400),
            Offer(fesh=101, hyperid=210, price=1000, price_old=1500),
            Offer(fesh=100, hyperid=220, price=1400),
            Offer(fesh=101, hyperid=220, price=1000, price_old=1500),
        ]

        cls.recommender.on_request_models_of_interest(user_id="yandexuid:", item_count=1000).respond(
            {"models": ['123']}
        )

    def test_vanished_model_title(self):
        response = self.report.request_xml('place=bestdeals&hid=100')
        self.assertFragmentIn(response, '<name>vanished model title</name>', preserve_order=True)
        self.error_log.ignore('Personal category config is not available')

    @classmethod
    def prepare_filter_by_vendor(cls):
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:1888", item_count=1000).respond(
            {"models": ['80', '83', '86']}
        )

    def test_filter_by_vendor(self):
        self.error_log.ignore('Personal category config is not available')

        # test request by vendor
        response = self.report.request_xml('place=bestdeals&yandexuid=1888&vendor_id=1')
        self.assertFragmentIn(
            response,
            '''
            <offers>
                <model id="80" />
                <model id="83" />
                <model id="86" />
            </offers>
            ''',
            preserve_order=True,
            allow_different_len=False,
        )

        # test requests by multiple vendors
        response = self.report.request_xml('place=bestdeals&yandexuid=1888&vendor_id=1&vendor_id=2')
        self.assertFragmentIn(
            response,
            '''
            <offers>
                <model id="80" />
                <model id="83" />
                <model id="86" />
                <model id="81" />
                <model id="84" />
            </offers>
            ''',
            preserve_order=True,
            allow_different_len=False,
        )

        # test request by vendor and hid
        response = self.report.request_xml('place=bestdeals&yandexuid=1888&vendor_id=1&hid=500')
        self.assertFragmentIn(
            response,
            '''
            <offers>
                <model id="83" />
            </offers>
            ''',
            preserve_order=True,
            allow_different_len=False,
        )

        # test request by multiple vendors and multiple hids
        response = self.report.request_xml('place=bestdeals&yandexuid=1888&vendor_id=1&vendor_id=2&hid=400&hid=500')
        self.assertFragmentIn(
            response,
            '''
            <offers>
                <model id="80" />
                <model id="83" />
                <model id="81" />
                <model id="84" />
            </offers>
            ''',
            preserve_order=True,
            allow_different_len=False,
        )

    # best deal exists when price diff (|offer_price - mrs_min_price|) < 1RUR
    def test_best_deal_with_allowable_price_diff(self):
        response = self.report.request_json('place=prime&hid=101&rids=225')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {"id": 101},
                "prices": {"currency": "RUR", "discount": {"oldMin": "1500", "percent": 33, "isBestDeal": True}},
                "manufacturer": {"entity": "manufacturer", "warranty": True},
            },
        )

    def test_no_best_deal_with_unallowable_price_diff(self):
        response = self.report.request_json('place=prime&hid=102&rids=225')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {"id": 101},
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "shop": {"id": 101},
                "prices": {"currency": "RUR", "discount": {"oldMin": "1500", "percent": 33, "isBestDeal": True}},
                "manufacturer": {"entity": "manufacturer", "warranty": True},
            },
        )

    def test_missing_pp(self):
        response = self.report.request_xml(
            'place=bestdeals&vendor_id=1&hid=500&ip=127.0.0.1', strict=False, add_defaults=False
        )
        self.error_log.ignore('Personal category config is not available')
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    @classmethod
    def prepare_pictureless_models(cls):
        """
        Скидки с картинками и без
        """
        cls.index.models += [
            Model(hyperid=197, title="model with picture #197", hid=333, no_picture=False),
            Model(hyperid=198, title="model with picture #198", hid=333, no_picture=False),
            Model(hyperid=199, title="model without picture", hid=333, no_picture=True, no_add_picture=True),
        ]
        cls.index.offers += [
            Offer(fesh=100, hyperid=197, discount=35),
            Offer(fesh=100, hyperid=198, discount=35),
            Offer(fesh=100, hyperid=199, discount=35),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=333, output_type=HyperCategoryType.GURU),
        ]
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:1333", item_count=1000).respond(
            {"models": ['197']}
        )

    def test_pictureless_models(self):
        """
        Проверяем, что модель бкз картинок (199) не попадают в выдачу
        Проверяем также, что две другие модели, отличающиеся от 199 только тем,
        что имеют каартинки, попадают в выдачу
        """
        response = self.report.request_json("place=bestdeals&yandexuid=1333&hid=333")
        self.error_log.ignore('Personal category config is not available')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"type": "model", "id": 197},
                        {"type": "model", "id": 198},
                    ],
                }
            },
            preserve_order=False,
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, {"search": {"results": [{"type": "model", "id": 199}]}})

    @classmethod
    def prepare_realtime_dicount(cls):
        """
        Модели и офферы со скидками и без
        """
        cls.index.hypertree += [
            HyperCategory(hid=339, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=201, hid=339),
            Model(hyperid=202, hid=339),
            Model(hyperid=203, hid=339),
        ]
        cls.index.offers += [
            Offer(fesh=100, hyperid=201, discount=None),
            Offer(fesh=100, hyperid=202, discount=35),
        ]
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:1339", item_count=1000).respond(
            {"models": ['201']}
        )

    def test_realtime_dicount(self):
        """
        Проверяем, что все модели выдачи имеют скидочные офферы
        """
        self.error_log.ignore("Personal category config is not available")
        self.assertOnlyModelsInResponse(
            query="place=bestdeals&yandexuid=1339&hid=339", ids=[202], all_ids=[201, 202, 203]
        )

    def test_show_log(self):
        """
        Проверка поля url_hash в show log
        """
        self.error_log.ignore("Personal category config is not available")
        self.report.request_json("place=bestdeals&yandexuid=1002&hid=111")
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    @classmethod
    def prepare_new_personal_categories(cls):
        cls.index.hypertree += [
            HyperCategory(hid=111, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(hyperid=1111, hid=111, vendor_id=1111111),
        ]
        cls.index.offers += [
            Offer(hyperid=1111, discount=35),
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1002', with_timestamps=False).respond(
            {'models': ['1111']}
        )

    def test_new_personal_categories(self):
        response = self.report.request_json(
            'place=bestdeals&yandexuid=1002&rearr-factors=market_use_recommender=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            {'search': {'total': 1, 'results': [{'entity': 'product', 'id': 1111}]}},
            preserve_order=True,
            allow_different_len=False,
        )
        self.error_log.ignore("Personal category config is not available")


if __name__ == '__main__':
    main()
