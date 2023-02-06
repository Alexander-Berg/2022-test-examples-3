#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, DynamicSkuOffer, MarketSku, MinBidsModel, Model, Offer, Shop, Tax, Vat
from core.testcase import TestCase, main
from core.bigb import BigBKeyword, WeightedValue
from core.dj import DjModel
from core.matcher import Absent, NotEmpty


import random

DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]

DEFAULT_PROFILE_WITH_UID = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]


def get_omm_vertical_model_ids(base, count):
    model_ids = [base + i for i in range(1, count + 1)]
    random.seed(0)
    random.shuffle(model_ids)
    return model_ids


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.settings.report_subrole = 'api'
        cls.settings.set_default_reqid = False

    @classmethod
    def prepare_blue_attractive_models(cls):
        cls.index.shops += [
            Shop(
                fesh=1886710,
                datafeed_id=188671001,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
            ),
            Shop(fesh=1886711, priority_region=213),
        ]

        cls.index.shops += [
            Shop(
                fesh=12345,
                datafeed_id=12345,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                disable_auto_warehouse_id=True,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1886701,
                sku=188670101,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
            MarketSku(
                hyperid=1886702,
                sku=188670201,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
            MarketSku(
                hyperid=1886703,
                sku=188670301,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
            MarketSku(
                hyperid=1886704,
                sku=188670401,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=1886711, hyperid=1886701),
            Offer(fesh=1886711, hyperid=1886702),
            Offer(fesh=1886711, hyperid=1886703),
            Offer(fesh=1886711, hyperid=1886704),
            Offer(fesh=1886711, hyperid=1886705),
        ]

        cls.dj.on_request(exp='blue_attractive_models', yandexuid='001').respond(
            models=[
                DjModel(id=1886702, title='model2'),
                DjModel(id=1886701, title='model1'),
                DjModel(id=1886703, title='model3'),
                DjModel(id=1886704, title='model4'),
            ]
        )

    def test_blue_attractive_models(self):
        """Проверяем, что на выдаче place=blue_attractive_models с rgb=blue
        находятся модели с синими офферами и выдача "закольцована".
        При этом в ДО в выдаче есть идентификатор marketSku
        """
        response = self.report.request_json('place=blue_attractive_models&rgb=blue&yandexuid=001&numdoc=5&debug=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 1886702, "offers": {"items": [{"marketSku": "188670201"}]}},
                    {"entity": "product", "id": 1886701, "offers": {"items": [{"marketSku": "188670101"}]}},
                    {"entity": "product", "id": 1886703, "offers": {"items": [{"marketSku": "188670301"}]}},
                    {"entity": "product", "id": 1886704, "offers": {"items": [{"marketSku": "188670401"}]}},
                    {"entity": "product", "id": 1886702, "offers": {"items": [{"marketSku": "188670201"}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_omm_vertical(cls):

        hid = 2001000
        model_ids = get_omm_vertical_model_ids(base=hid, count=256)

        cls.fast_dj.on_request(
            exp='inter_vertical_unknown',
            yandexuid='001',
        ).respond(models=[DjModel(id=hyperid, title='model{}'.format(hyperid)) for hyperid in model_ids])

        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid in model_ids]

        cls.index.offers += [Offer(title='title N{}'.format(hyperid), hyperid=hyperid) for hyperid in model_ids]

    @classmethod
    def prepare_omm_findings(cls):
        cls.fast_dj.on_request(exp='white_omm_findings', yandexuid='001').respond(
            models=[
                DjModel(id=1898805, title='model5'),
                DjModel(id=1898808, title='model8'),
                DjModel(id=1898812, title='model12'),
                DjModel(id=1898809, title='model9'),
                DjModel(id=1898804, title='model4'),
                DjModel(id=1898814, title='model14'),
                DjModel(id=1898815, title='model15'),
                DjModel(id=1898802, title='model2'),
                DjModel(id=1898803, title='model3'),
                DjModel(id=1898801, title='model1'),
            ]
        )

        for seq in range(1, 16):
            cls.index.models += [
                Model(hyperid=1898800 + seq, hid=1898800),
            ]

            cls.index.offers += [
                Offer(title='title N{}'.format(seq), hyperid=1898800 + seq),
            ]

    def test_omm_findings(self):
        """Проверяем, что на выдаче place=omm_findings находятся модели,
        рекомендуемые OMM.
        """
        response = self.report.request_json('place=omm_findings&numdoc=3&yandexuid=001&debug=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 1898805},
                    {"entity": "product", "id": 1898808},
                    {"entity": "product", "id": 1898812},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_omm_findings_encryptedmodel(cls):
        cls.fast_dj.on_request(exp='white_omm_findings', yandexuid='003').respond(
            models=[
                DjModel(id=1905901, title='model1'),
                DjModel(id=1905902, title='model2'),
                DjModel(id=1905903, title='model3'),
                DjModel(id=1905904, title='model4'),
            ]
        )

        cls.index.models += [
            Model(hyperid=1905901, hid=1905900),
            Model(hyperid=1905902, hid=1905900),
            Model(hyperid=1905903, hid=1905900),
            Model(hyperid=1905904, hid=1905900),
        ]

        cls.index.offers += [
            Offer(title='title N1', bid=10, hyperid=1905901, price=10000),
            Offer(title='title N1', bid=100, hyperid=1905902, price=10000),
            Offer(title='title N1', bid=100, hyperid=1905903, price=10000),
            Offer(title='title N1', bid=1000, hyperid=1905904, price=10000),
        ]

        cls.index.min_bids_model_stats += [
            MinBidsModel(
                model_id=1905901,
                geo_group_id=0,
                drr=0.01,
                search_clicks=1,
                search_orders=3,
                card_clicks=144,
                card_orders=9,
                full_card_orders=0,
                full_card_clicks=0,
            ),
            MinBidsModel(
                model_id=1905902,
                geo_group_id=0,
                drr=0.02,
                search_clicks=1,
                search_orders=3,
                card_clicks=144,
                card_orders=9,
                full_card_orders=0,
                full_card_clicks=0,
            ),
            MinBidsModel(
                model_id=1905903,
                geo_group_id=0,
                drr=0.03,
                search_clicks=1,
                search_orders=3,
                card_clicks=144,
                card_orders=9,
                full_card_orders=0,
                full_card_clicks=0,
            ),
            MinBidsModel(
                model_id=1905904,
                geo_group_id=0,
                drr=0.04,
                search_clicks=1,
                search_orders=3,
                card_clicks=144,
                card_orders=9,
                full_card_orders=0,
                full_card_clicks=0,
            ),
        ]

    def test_omm_findings_encryptedmodel(self):
        """Проверяем, что на выдаче place=omm_findings находятся модели,
        рекомендуемые OMM, при этом в ДО присутствует шифрованная ссылка
        Проверяем, что модели не переранжируются по ставкам, а
        click_price в ссылках соответствует min_bid
        """
        response = self.report.request_json(
            'place=omm_findings&numdoc=4&show-urls=encryptedmodel,phone&yandexuid=003&debug=1&pp=279'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1905901, "offers": {"items": [{"urls": {"encrypted": NotEmpty(), "callPhone": Absent()}}]}},
                    {"id": 1905902, "offers": {"items": [{"urls": {"encrypted": NotEmpty(), "callPhone": Absent()}}]}},
                    {"id": 1905903, "offers": {"items": [{"urls": {"encrypted": NotEmpty(), "callPhone": Absent()}}]}},
                    {"id": 1905904, "offers": {"items": [{"urls": {"encrypted": NotEmpty(), "callPhone": Absent()}}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log.expect(hyper_id=1905901, click_price=18, bid=18, pp=279).once()
        self.show_log.expect(hyper_id=1905902, click_price=35, bid=100, pp=279).once()
        self.show_log.expect(hyper_id=1905903, click_price=52, bid=100, pp=279).once()
        self.show_log.expect(hyper_id=1905904, click_price=69, bid=1000, pp=279).once()
        self.click_log.expect(hyper_id=1905901, cp=18, cb=18, min_bid=18, pp=279).once()
        self.click_log.expect(hyper_id=1905902, cp=35, cb=100, min_bid=35, pp=279).once()
        self.click_log.expect(hyper_id=1905903, cp=52, cb=100, min_bid=52, pp=279).once()
        self.click_log.expect(hyper_id=1905904, cp=69, cb=1000, min_bid=69, pp=279).once()

    @classmethod
    def prepare_blue_attractive_models_green_filtering(cls):
        """Создаем 6 моделей: 4 синих и 2 зеленых
        Используем существующий магазин
        """
        cls.dj.on_request(exp='blue_attractive_models', yandexuid='003').respond(
            models=[
                DjModel(id=1902404, title='model4'),
                DjModel(id=1902402, title='model2'),
                DjModel(id=1902401, title='model1'),
                DjModel(id=1902403, title='model3'),
                DjModel(id=1902405, title='model5'),
                DjModel(id=1902406, title='model6'),
            ]
        )

        cls.index.mskus += [
            MarketSku(
                hyperid=1902401,
                sku=190240101,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
            MarketSku(
                hyperid=1902402,
                sku=190240201,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
            MarketSku(
                hyperid=1902405,
                sku=190240501,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
            MarketSku(
                hyperid=1902406,
                sku=190240601,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
        ]
        for seq in range(1, 7):
            cls.index.models += [
                Model(hyperid=1902400 + seq, hid=1902400),
            ]

            cls.index.offers += [
                Offer(title='title N{}'.format(seq), hyperid=1902400 + seq),
            ]

    def test_blue_attractive_models_green_filtering(self):
        """Проверяем, что на выдаче place=blue_attractive_models с rgb=blue
        находятся модели с синими офферами, а зеленые (не с синим ДО)
        отфильтровываются. При этом numdoc работает
        """
        response = self.report.request_json('place=blue_attractive_models&rgb=blue&yandexuid=003&numdoc=3&debug=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 1902402, "offers": {"items": [{"marketSku": "190240201"}]}},
                    {"entity": "product", "id": 1902401, "offers": {"items": [{"marketSku": "190240101"}]}},
                    {"entity": "product", "id": 1902405, "offers": {"items": [{"marketSku": "190240501"}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=blue_attractive_models&rgb=blue&yandexuid=003&numdoc=6&debug=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 1902402, "offers": {"items": [{"marketSku": "190240201"}]}},
                    {"entity": "product", "id": 1902401, "offers": {"items": [{"marketSku": "190240101"}]}},
                    {"entity": "product", "id": 1902405, "offers": {"items": [{"marketSku": "190240501"}]}},
                    {"entity": "product", "id": 1902406, "offers": {"items": [{"marketSku": "190240601"}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_omm_productoffers_requests(cls):
        cls.fast_dj.on_request(exp='inter_vertical_unknown', yandexuid='005',).respond(
            models=[
                DjModel(id=2128304, title='model4'),
            ]
        )

        cls.index.models += [
            Model(hyperid=2128301, hid=2128300),
            Model(hyperid=2128302, hid=2128300),
        ]

        cls.index.offers += [
            Offer(title='title N1', hyperid=2128301),
        ]

    def test_omm_productoffers_requests(self):
        """Проверяем, что плейс place=productoffers не делает холостой запрос
        в ОММ при наличии ДО в результатах и не делает при отсутствии ДО
        Отсутствие ошибок в логах означает, что настоящий запрос в OMМ
        соответствует заданному в prepare
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=default&yandexuid=001&hyperid=2128301&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "title N1"},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=productoffers&offers-set=default&yandexuid=001&hyperid=2128302&debug=1'
        )
        self.assertFragmentIn(response, {"results": []}, preserve_order=True, allow_different_len=False)

    @classmethod
    def prepare_omm_blue_findings(cls):
        _ = {
            'keywords': DEFAULT_PROFILE,
            'models': [],
            'history_models': [],
        }

        adv_machine_models_15 = []
        for seq in range(15):
            adv_machine_models_15 += [
                DjModel(
                    id=2158001 + seq,
                    title='attractive_model' + str(seq + 1),
                    pic_url='//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/orig'.format(
                        seq, seq
                    ),
                ),
            ]

        adv_machine_models_25 = []
        for seq in range(25):
            adv_machine_models_25 += [
                DjModel(
                    id=2158021 + seq,
                    title='attractive_model' + str(seq + 1),
                    pic_url='//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/orig'.format(
                        seq, seq
                    ),
                ),
            ]

        cls.dj.on_request(yandexuid='005', exp='blue_omm_findings').respond(models=adv_machine_models_25)
        cls.dj.on_request(yandexuid='006', exp='blue_omm_findings').respond(models=adv_machine_models_15)

        for seq in range(15):
            cls.index.models += [
                Model(hyperid=2158001 + seq, hid=2158000),
            ]

            cls.index.offers += [
                Offer(title='title N{}'.format(seq), hyperid=2158001 + seq),
            ]

            cls.index.mskus += [
                MarketSku(
                    hyperid=2158001 + seq,
                    sku=(2158001 + seq) * 100 + 1,
                    blue_offers=[
                        BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                    ],
                ),
            ]

        for seq in range(25):
            cls.index.models += [
                Model(hyperid=2158021 + seq, hid=2158000),
            ]

        for seq in range(9):
            cls.index.offers += [
                Offer(title='title N2{}'.format(seq), hyperid=2158021 + seq),
            ]

            cls.index.mskus += [
                MarketSku(
                    hyperid=2158021 + seq,
                    sku=(2158021 + seq) * 100 + 1,
                    blue_offers=[
                        BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                    ],
                ),
            ]

        for seq in range(10, 25):
            cls.index.offers += [
                Offer(title='title N2{}'.format(seq), hyperid=2158021 + seq),
            ]

            cls.index.mskus += [
                MarketSku(
                    hyperid=2158021 + seq,
                    sku=(2158021 + seq) * 100 + 1,
                    blue_offers=[
                        BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                    ],
                ),
            ]

    def test_blue_omm_findings(self):
        """Проверяем, что на выдаче place=blue_omm_finding с rgb=blue
        находятся модели из выдачи ОММ без какого-либо вырезания моделей из начала выдачи
        """
        response = self.report.request_json('place=blue_omm_findings&rgb=blue&yandexuid=005&numdoc=5')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 2158021, "offers": {"items": [{"marketSku": "215802101"}]}},
                    {"entity": "product", "id": 2158022, "offers": {"items": [{"marketSku": "215802201"}]}},
                    {"entity": "product", "id": 2158023, "offers": {"items": [{"marketSku": "215802301"}]}},
                    {"entity": "product", "id": 2158024, "offers": {"items": [{"marketSku": "215802401"}]}},
                    {"entity": "product", "id": 2158025, "offers": {"items": [{"marketSku": "215802501"}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=blue_omm_findings&rgb=blue&yandexuid=005&numdoc=8')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 2158021, "offers": {"items": [{"marketSku": "215802101"}]}},
                    {"entity": "product", "id": 2158022, "offers": {"items": [{"marketSku": "215802201"}]}},
                    {"entity": "product", "id": 2158023, "offers": {"items": [{"marketSku": "215802301"}]}},
                    {"entity": "product", "id": 2158024, "offers": {"items": [{"marketSku": "215802401"}]}},
                    {"entity": "product", "id": 2158025, "offers": {"items": [{"marketSku": "215802501"}]}},
                    {"entity": "product", "id": 2158026, "offers": {"items": [{"marketSku": "215802601"}]}},
                    {"entity": "product", "id": 2158027, "offers": {"items": [{"marketSku": "215802701"}]}},
                    {"entity": "product", "id": 2158028, "offers": {"items": [{"marketSku": "215802801"}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=blue_omm_findings&rgb=blue&yandexuid=006&numdoc=5')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 2158001, "offers": {"items": [{"marketSku": "215800101"}]}},
                    {"entity": "product", "id": 2158002, "offers": {"items": [{"marketSku": "215800201"}]}},
                    {"entity": "product", "id": 2158003, "offers": {"items": [{"marketSku": "215800301"}]}},
                    {"entity": "product", "id": 2158004, "offers": {"items": [{"marketSku": "215800401"}]}},
                    {"entity": "product", "id": 2158005, "offers": {"items": [{"marketSku": "215800501"}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_omm_parallel_places_with_do(cls):
        """Создаем категории, модели, ответы BigB, OMM для синих моделей"""
        adv_machine_models = []
        for seq in range(1, 16):
            picture_link = '//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/orig'.format(
                seq, seq
            )

            cls.index.models += [
                Model(
                    hyperid=2435700 + seq, title='Синяя модель для ПП ' + str(seq), hid=2435700, picinfo=picture_link
                ),
            ]

            cls.index.offers += [
                Offer(hyperid=2435700 + seq, price=100 + seq * 10),
                Offer(hyperid=2435700 + seq, price=2000 + seq * 10),
            ]

            cls.index.mskus += [
                MarketSku(
                    hyperid=2435700 + seq,
                    sku=243570000 + seq,
                    blue_offers=[
                        BlueOffer(
                            price=500,
                            waremd5='WARE_{:02d}______________g'.format(seq),
                            vat=Vat.NO_VAT,
                            feedid=12345,
                            offerid=243570000 + seq,
                            randx=1,
                        ),
                    ],
                ),
                MarketSku(
                    hyperid=2435700 + seq,
                    sku=243570100 + seq,
                    blue_offers=[
                        BlueOffer(
                            price=2000,
                            waremd5='WARX_{:02d}______________g'.format(seq),
                            vat=Vat.NO_VAT,
                            feedid=12345,
                            offerid=243570100 + seq,
                            randx=2,
                        ),
                    ],
                ),
                MarketSku(
                    hyperid=2435700 + seq,
                    sku=243570200 + seq,
                    blue_offers=[],
                ),
            ]

            adv_machine_models += [
                DjModel(id=2435700 + seq, title='Синяя модель для ПП ' + str(seq), pic_url=picture_link)
            ]

        cls.bigb.on_request(yandexuid='24357001', client='merch-machine').respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            exp='inter_vertical_unknown',
            yandexuid='24357001',
        ).respond(models=adv_machine_models)

    def get_blue_yandexapp_vertical_model(self, index, clid=None, has_waremd5=False):
        seq = index + 1
        model_id = 2435700 + seq
        sku_id = 243570100 + seq

        parts = []
        if clid:
            parts.append('clid={}'.format(clid))
        if has_waremd5:
            parts.append('do-waremd5={}'.format('WARX_{:02d}______________g'.format(seq)))
        parts.append('wprid=')

        slug_str = 'product--siniaia-model-dlia-pp-{}'.format(seq)
        return {
            "entity": "product",
            "titles": {"raw": "Синяя модель для ПП " + str(seq)},
            "urls": {"direct": "//m.market.yandex.ru/{}/{}?{}".format(slug_str, model_id, '&'.join(parts))},
            "id": model_id,
            "marketSku": str(sku_id),
            "prices": {"min": str(2000), "max": str(2000), "currency": "RUR", "avg": str(2000)},
        }

    def test_blue_omm_yandexapp_vertical_api(self):
        """Проверяем, что на выдаче place=omm_parallel&omm_place=yandexapp_vertical находятся синие модели,
        рекомендуемые OMM (со статистиками из ДО)
        """
        for place_id in [
            "place=omm_parallel&omm_place=yandexapp_vertical&rearr-factors=market_yandexapp_blue_vertical=1"
        ]:
            # initial query
            response = self.report.request_json(
                '{place_id}&yandexuid=24357001&rgb=blue&numdoc=3&pof=905'.format(place_id=place_id)
            )
            pages = response.root["search"]["pages"]
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_blue_yandexapp_vertical_model(0, 905),
                            self.get_blue_yandexapp_vertical_model(1, 905),
                            self.get_blue_yandexapp_vertical_model(2, 905),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            self.dynamic.disabled_sku_offers += [
                DynamicSkuOffer(shop_id=12345, sku='243570001'),
                DynamicSkuOffer(shop_id=12345, sku='243570101'),
                DynamicSkuOffer(shop_id=12345, sku='243570201'),
            ]

            response = self.report.request_json(
                '{place_id}&yandexuid=24357001&rgb=blue&numdoc=3&pof=905'.format(place_id=place_id)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_blue_yandexapp_vertical_model(1, 905),
                            self.get_blue_yandexapp_vertical_model(2, 905),
                        ],
                        "pages": pages,
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
