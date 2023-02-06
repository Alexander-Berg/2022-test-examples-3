#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DeliveryBucket, GLParam, GLType, Offer, Outlet, PickupBucket, PickupOption, MarketSku, BlueOffer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.gltypes += [
            GLType(param_id=201, hid=2, gltype=GLType.BOOL),
            GLType(param_id=202, hid=2, gltype=GLType.BOOL),
        ]

        cls.index.outlets += [
            Outlet(fesh=214, region=214, point_type=Outlet.FOR_PICKUP, point_id=10001),
            Outlet(fesh=215, region=215, point_type=Outlet.FOR_STORE, point_id=10002),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5014,
                fesh=214,
                carriers=[99],
                options=[PickupOption(outlet_id=10001)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5015,
                fesh=215,
                carriers=[99],
                options=[PickupOption(outlet_id=10002)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=2,
                title="matreshka",
                fesh=214,
                glparams=[GLParam(param_id=201, value=1), GLParam(param_id=202, value=1)],
                pickup_buckets=[5014],
            ),
            Offer(
                hid=2,
                title="balalaika",
                fesh=215,
                glparams=[GLParam(param_id=201, value=1), GLParam(param_id=202, value=1)],
                pickup_buckets=[5015],
            ),
            Offer(
                hid=2,
                title="matreshka",
                fesh=215,
                glparams=[GLParam(param_id=201, value=1), GLParam(param_id=202, value=0)],
                pickup_buckets=[5015],
            ),
            Offer(
                hid=2,
                title="balalaika",
                fesh=214,
                glparams=[GLParam(param_id=201, value=1), GLParam(param_id=202, value=0)],
                pickup_buckets=[5014],
            ),
            Offer(hid=2, title="matreshka", fesh=214, glparams=[GLParam(param_id=201, value=1)], pickup_buckets=[5014]),
            Offer(hid=2, title="balalaika", fesh=215, glparams=[GLParam(param_id=201, value=1)], pickup_buckets=[5015]),
            Offer(hid=2, title="matreshka", fesh=214, pickup_buckets=[5014]),
            Offer(hid=2, title="vodka", fesh=214, adult=True, pickup_buckets=[5014]),
            Offer(
                hid=2,
                title="vodka",
                fesh=214,
                glparams=[GLParam(param_id=201, value=1), GLParam(param_id=202, value=1)],
                adult=True,
                pickup_buckets=[5014],
            ),
        ]

    # MARKETOUT_8606
    # test found and initialFound values for boolean gl-filters
    def test_disabling_bool_filters(self):

        r = '&rearr-factors=market_early_gl_filtering_textless=1;market_early_pre_early_gl_filtering=1;market_join_gl_filters=0'

        def fragment(total, f201, f202):
            # total - количество найденных офферов
            # f201 = {0:(initialFound, found), 1:(initialFound, found)}
            return {
                "search": {"total": total},
                "filters": [
                    {
                        "id": "201",
                        "values": [
                            {"initialFound": f201[0][0], "found": f201[0][1], "value": "0"},
                            {"initialFound": f201[1][0], "found": f201[1][1], "value": "1"},
                        ],
                    },
                    {
                        "id": "202",
                        "values": [
                            {"initialFound": f202[0][0], "found": f202[0][1], "value": "0"},
                            {"initialFound": f202[1][0], "found": f202[1][1], "value": "1"},
                        ],
                    },
                ],
            }

        # без фильтрации: для значения value = 0  found включает в себя офферы со значением 0 или офферы без значения
        response = self.report.request_json('place=prime&hid=2' + r)
        self.assertFragmentIn(response, fragment(total=7, f201={0: (1, 1), 1: (6, 6)}, f202={0: (5, 5), 1: (2, 2)}))

        # если разрешить товары для взрослых, добавятся еще 2 оффера
        response = self.report.request_json('place=prime&hid=2&adult=1' + r)
        self.assertFragmentIn(response, fragment(total=9, f201={0: (2, 2), 1: (7, 7)}, f202={0: (6, 6), 1: (3, 3)}))

        # с фильтрацией (по параметру 201):
        # для параметров учасвующих в фильтрации (201) :
        #     для значения value = 1  found включает в себя офферы со значением 1 среди офферов отфильтрованных другими gl-фильтрам
        #     для значения value = 0  found включает в себя офферы со значением 0 или офферы без значения среди всех офферов без фильтрации по gl-фильтрам
        # для параметров не участвующих в фильтрации (202) :
        #     для значения value = 1  found включает в себя офферы со значением 1 среди офферов отфильтрованных другими gl-фильтрами
        #     для значения value = 0  found включает в себя офферы со значением 0 или офферы без значения среди офферов отфильтрованных другими gl-фильтрами
        response = self.report.request_json('place=prime&hid=2&glfilter=201:1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=6,
                # found = initialFound = офферы до фильтрации (т.к. фильтр выбран)
                f201={0: (1, 1), 1: (6, 6)},
                f202={0: (5, 4), 1: (2, 2)},
            ),
        )

        response = self.report.request_json('place=prime&hid=2&glfilter=201:0' + r)
        self.assertFragmentIn(response, fragment(total=1, f201={0: (1, 1), 1: (6, 6)}, f202={0: (5, 1), 1: (2, 0)}))

        # с товарами для взрослых и с фильтрацией
        response = self.report.request_json('place=prime&hid=2&glfilter=201:1&adult=1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=7,
                # found = intitialFound = 2 + 7 = 9 офферы до фильтрации (с товарами
                # для взрослых)
                f201={0: (2, 2), 1: (7, 7)},
                f202={0: (6, 4), 1: (3, 3)},
            ),
        )

        response = self.report.request_json('place=prime&hid=2&glfilter=201:0&adult=1' + r)
        self.assertFragmentIn(response, fragment(total=2, f201={0: (2, 2), 1: (7, 7)}, f202={0: (6, 2), 1: (3, 0)}))

        # текстовая фильтрация
        response = self.report.request_json('place=prime&hid=2&text=matreshka' + r)
        self.assertFragmentIn(response, fragment(total=4, f201={0: (1, 1), 1: (3, 3)}, f202={0: (3, 3), 1: (1, 1)}))

        # текстовая фильтрация и фильтрация по параметру 201
        response = self.report.request_json('place=prime&hid=2&text=matreshka&glfilter=201:1' + r)
        self.assertFragmentIn(response, fragment(total=3, f201={0: (1, 1), 1: (3, 3)}, f202={0: (3, 2), 1: (1, 1)}))

        # текстовая фильтрация и фильтрация по параметрам 201 и 202
        # (оба параметра выбраны, поэтому found для значения value = 0 для обоих рассчитывается как по нефильтрованной выдаче)
        # однако для значения value=1 found рассчитывается по выдаче фильтрованной другими фильтрами
        response = self.report.request_json('place=prime&hid=2&text=matreshka&glfilter=201:1&glfilter=202:1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=1,
                # found: 1 для value=1: рассчитывается по выдаче, фильтованной другим
                # gl-фильтром (в данном случае эта выдача состоит из 1 оффера)
                f201={0: (1, 1), 1: (3, 1)},
                f202={0: (3, 3), 1: (1, 1)},
            ),
        )

        # фильтрация по региону
        response = self.report.request_json('place=prime&hid=2&rids=214&adult=1' + r)
        self.assertFragmentIn(response, fragment(total=6, f201={0: (2, 2), 1: (4, 4)}, f202={0: (4, 4), 1: (2, 2)}))

        response = self.report.request_json('place=prime&hid=2&rids=215' + r)
        self.assertFragmentIn(response, fragment(total=3, f201={0: (0, 0), 1: (3, 3)}, f202={0: (2, 2), 1: (1, 1)}))

        # фильтрация по тексту и региону (***) - запрос без фильтрации по gl-фильтрам
        response = self.report.request_json('place=prime&hid=2&rids=214&text=matreshka' + r)
        self.assertFragmentIn(response, fragment(total=3, f201={0: (1, 1), 1: (2, 2)}, f202={0: (2, 2), 1: (1, 1)}))

        # фильтрация по тексту региону и параметру 201:1
        response = self.report.request_json('place=prime&hid=2&rids=214&text=matreshka&glfilter=201:1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=2,
                # found для 0 как в (*** - без фильтрации по gl-фильтрам) т.к. по этому
                # параметру идет фильтрация
                f201={0: (1, 1), 1: (2, 2)},
                f202={0: (2, 1), 1: (1, 1)},
            ),
        )

        # фильтрация по тексту региону и параметру 202:0
        response = self.report.request_json('place=prime&hid=2&rids=214&text=matreshka&glfilter=202:0' + r)
        self.assertFragmentIn(response, fragment(total=2, f201={0: (1, 1), 1: (2, 1)}, f202={0: (2, 2), 1: (1, 1)}))

        # фильтрация по тексту региону и параметрам 201:1 и 202:0
        response = self.report.request_json(
            'place=prime&hid=2&rids=214&text=matreshka&glfilter=201:1&glfilter=202:0' + r
        )
        self.assertFragmentIn(
            response,
            fragment(
                total=1,
                f201={
                    0: (1, 1),  # found для 0 как в (***) т.к. по этому параметру идет фильтрация
                    1: (2, 1),
                },  # found для 1 как при фильтрации по 202:0  (т.е. без учета данного фильтра)
                f202={0: (2, 2), 1: (1, 1)},  # found для 0 как в (***) т.к. по этому параметру идет фильтрация
            ),
        )

    # MARKETOUT_8606
    # test found and initialFound values for boolean gl-filters
    def test_disabling_bool_filters_join_gl_filters(self):

        r = '&rearr-factors=market_early_gl_filtering_textless=1;market_early_pre_early_gl_filtering=1;market_join_gl_filters=1'

        def fragment(total, f201, f202):
            # total - количество найденных офферов
            # f201 = {0:(initialFound, found), 1:(initialFound, found)}
            return {
                "search": {"total": total},
                "filters": [
                    {
                        "id": "201",
                        "values": [
                            {"initialFound": f201[0][0], "found": f201[0][1], "value": "0"},
                            {"initialFound": f201[1][0], "found": f201[1][1], "value": "1"},
                        ],
                    },
                    {
                        "id": "202",
                        "values": [
                            {"initialFound": f202[0][0], "found": f202[0][1], "value": "0"},
                            {"initialFound": f202[1][0], "found": f202[1][1], "value": "1"},
                        ],
                    },
                ],
            }

        # без фильтрации: для значения value = 0  found включает в себя офферы со значением 0 или офферы без значения
        # дозапроса за фильтрами нет - статистика честная
        response = self.report.request_json('place=prime&hid=2' + r)
        self.assertFragmentIn(response, fragment(total=7, f201={0: (1, 1), 1: (6, 6)}, f202={0: (5, 5), 1: (2, 2)}))

        # если разрешить товары для взрослых, добавятся еще 2 оффера
        # дозапроса за фильтрами нет - статистика честная
        response = self.report.request_json('place=prime&hid=2&adult=1' + r)
        self.assertFragmentIn(response, fragment(total=9, f201={0: (2, 2), 1: (7, 7)}, f202={0: (6, 6), 1: (3, 3)}))

        # с фильтрацией (по параметру 201):
        # для параметров учасвующих в фильтрации (201) :
        #     для значения value = 1  found включает в себя офферы со значением 1 среди офферов отфильтрованных другими gl-фильтрам
        #     для значения value = 0  found включает в себя офферы со значением 0 или офферы без значения среди всех офферов без фильтрации по gl-фильтрам
        # для параметров не участвующих в фильтрации (202) :
        #     для значения value = 1  found включает в себя офферы со значением 1 среди офферов отфильтрованных другими gl-фильтрами
        #     для значения value = 0  found включает в себя офферы со значением 0 или офферы без значения среди офферов отфильтрованных другими gl-фильтрами
        # поскольку статистика из дозапроса за фильтрами суммируется со статистикой когда применена ранняя фильтрация то некоторые офферы удовлетворяющие фильтру учитываются дважды
        response = self.report.request_json('place=prime&hid=2&glfilter=201:1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=6,
                # found = initialFound = офферы до фильтрации (т.к. фильтр выбран)
                # 6 офферов со значением 201:1 продублировались дважды из них 4 оффера с 202:0 и 2 оффера с 202:1
                f201={0: (1, 1), 1: (12, 12)},  # {0: (1, 1), 1: (6, 6)},
                f202={0: (9, 8), 1: (4, 4)},
            ),
        )  # {0: (5, 4), 1: (2, 2)}

        response = self.report.request_json('place=prime&hid=2&glfilter=201:0' + r)
        # 1 оффер со значением 201:0 202:0 продублировался дважды
        self.assertFragmentIn(
            response,
            fragment(total=1, f201={0: (2, 2), 1: (6, 6)}, f202={0: (6, 2), 1: (2, 0)}),  # {0: (1, 1), 1: (6, 6)
        )  # {0: (5, 1), 1: (2, 0)}

        # с товарами для взрослых и с фильтрацией
        response = self.report.request_json('place=prime&hid=2&glfilter=201:1&adult=1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=7,
                # found = intitialFound = 2 + 7 = 9 офферы до фильтрации (с товарами для взрослых)
                # 7 офферов со значением 201:1 из которых 4 со значением 202:0 и 3 со значением 202:1 дублируются дважды
                f201={0: (2, 2), 1: (14, 14)},  # {0: (2, 2), 1: (7, 7)}
                f202={0: (10, 8), 1: (6, 6)},
            ),
        )  # {0: (6, 4), 1: (3, 3)}

        response = self.report.request_json('place=prime&hid=2&glfilter=201:0&adult=1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=2,
                # 2 оффера со значением 201:0 из которых оба имеют значение 202:0 дублируются дважды
                f201={0: (4, 4), 1: (7, 7)},  # {0: (2, 2), 1: (7, 7)}
                f202={0: (8, 4), 1: (3, 0)},
            ),
        )  # {0: (6, 2), 1: (3, 0)}

        # текстовая фильтрация
        # без фильтров - настоящее количество
        response = self.report.request_json('place=prime&hid=2&text=matreshka' + r)
        self.assertFragmentIn(response, fragment(total=4, f201={0: (1, 1), 1: (3, 3)}, f202={0: (3, 3), 1: (1, 1)}))

        # текстовая фильтрация и фильтрация по параметру 201
        response = self.report.request_json('place=prime&hid=2&text=matreshka&glfilter=201:1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=3,
                # 3 оффера matreshka из которых 2 оффера с 202:0 и 1 оффер с 202:1 продублированы
                f201={0: (1, 1), 1: (6, 6)},  # {0: (1, 1), 1: (3, 3)}
                f202={0: (5, 4), 1: (2, 2)},
            ),
        )  # {0: (3, 2), 1: (1, 1)}

        # текстовая фильтрация и фильтрация по параметрам 201 и 202
        # (оба параметра выбраны, поэтому found для значения value = 0 для обоих рассчитывается как по нефильтрованной выдаче)
        # однако для значения value=1 found рассчитывается по выдаче фильтрованной другими фильтрами
        response = self.report.request_json('place=prime&hid=2&text=matreshka&glfilter=201:1&glfilter=202:1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=1,
                # found: 1 для value=1: рассчитывается по выдаче, фильтованной другим
                # gl-фильтром (в данном случае эта выдача состоит из 1 оффера)
                # найденный оффер со значениями 201:1 202:1 будет учтен 2 раза
                f201={0: (1, 1), 1: (4, 2)},  # {0: (1, 1), 1: (3, 1)
                f202={0: (3, 3), 1: (2, 2)},
            ),
        )  # {0: (3, 3), 1: (1, 1)}

        # фильтрация по региону - настоящие значения фильтров
        response = self.report.request_json('place=prime&hid=2&rids=214&adult=1' + r)
        self.assertFragmentIn(response, fragment(total=6, f201={0: (2, 2), 1: (4, 4)}, f202={0: (4, 4), 1: (2, 2)}))

        response = self.report.request_json('place=prime&hid=2&rids=215' + r)
        self.assertFragmentIn(response, fragment(total=3, f201={0: (0, 0), 1: (3, 3)}, f202={0: (2, 2), 1: (1, 1)}))

        # фильтрация по тексту и региону (***) - запрос без фильтрации по gl-фильтрам
        response = self.report.request_json('place=prime&hid=2&rids=214&text=matreshka' + r)
        self.assertFragmentIn(response, fragment(total=3, f201={0: (1, 1), 1: (2, 2)}, f202={0: (2, 2), 1: (1, 1)}))

        # фильтрация по тексту региону и параметру 201:1
        response = self.report.request_json('place=prime&hid=2&rids=214&text=matreshka&glfilter=201:1' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=2,
                # found = initialFound как в (*** - без фильтрации по gl-фильтрам) т.к. по этому параметру идет фильтрация
                # 2 оффера matreshka c glfilter=201:1 из которых 1 оффер с 202:1 а другой с 202:0 учитываются дважды
                f201={0: (1, 1), 1: (4, 4)},  # {0: (1, 1), 1: (2, 2)}
                f202={0: (3, 2), 1: (2, 2)},
            ),
        )  # {0: (2, 1), 1: (1, 1)}

        # фильтрация по тексту региону и параметру 202:0
        response = self.report.request_json('place=prime&hid=2&rids=214&text=matreshka&glfilter=202:0' + r)
        self.assertFragmentIn(
            response,
            fragment(
                total=2,
                # 2 оффера matreshka c glfilter=202:0 из которых 1 оффер с 201:1 а другой с 201:0 учитываются дважды
                f201={0: (2, 2), 1: (3, 2)},  # {0: (1, 1), 1: (2, 1)}
                f202={0: (4, 4), 1: (1, 1)},
            ),
        )  # {0: (2, 2), 1: (1, 1)}

        # фильтрация по тексту региону и параметрам 201:1 и 202:0
        response = self.report.request_json(
            'place=prime&hid=2&rids=214&text=matreshka&glfilter=201:1&glfilter=202:0' + r
        )
        self.assertFragmentIn(
            response,
            fragment(
                total=1,
                # 1 оффер matreshka удовлетворяющий glfilter=201:1&glfilter=202:0 учитывается дважды
                f201={
                    0: (1, 1),  # (1, 1) found для 0 как в (***) т.к. по этому параметру идет фильтрация
                    1: (3, 2),
                },  # (2, 1) found для 1 как при фильтрации по 202:0  (т.е. без учета данного фильтра)
                f202={0: (3, 3), 1: (1, 1)},  # (2, 2)found для 0 как в (***) т.к. по этому параметру идет фильтрация
            ),
        )  # (1, 1)

    @classmethod
    def prepare_special_vendor_filtration(cls):

        cls.index.gltypes += [
            GLType(param_id=7893318, hid=18946, gltype=GLType.ENUM, values=[1001, 1002, 1003, 1004, 1005], vendor=True),
        ]

        # делаем отдельными for-ами чтобы в кишках docid шли последовательно (сначала один вендор, потом другой) - имитируем плохой тесткейс
        for i in range(15):
            cls.index.offers += [
                # более релевантный документ с точки зрения tfidf
                Offer(
                    title="каракулька каракулька синенькая",
                    vendor_id=1001,
                    hid=18946,
                    glparams=[GLParam(param_id=7893318, value=1001)],
                )
            ]

        for i in range(15):
            cls.index.offers += [
                # чуть менее релевантный документ с точки зрения tfidf
                Offer(
                    title="каракулька каракулька красненькая",
                    vendor_id=1002,
                    hid=18946,
                    glparams=[GLParam(param_id=7893318, value=1002)],
                )
            ]

        for i in range(15):
            cls.index.offers += [
                # еще менее релевантный документ с точки зрения tfidf
                Offer(
                    title="барабулька синенькая",
                    vendor_id=1003,
                    hid=18946,
                    glparams=[GLParam(param_id=7893318, value=1003)],
                ),
            ]

    def test_special_vendor_filtration(self):
        """Решаем проблему с тем что не все вендора находятся по широким запросам даже если нажать Показать все в фильтре по вендору
        Стараемся из пантеры нагребать документы так чтобы по несколько самых релевантных документов от каждого вендора попало.
        Ускоряем обработку тем что если вендор уже найден то другие документы не обрабатываем
        Благодаря этому можем раскрутить пантерный топ

        Особая программа обработки включается по условию &show-filter=vendor&nosearchresults=1&showVendors=all
        под флагом market_panther_coef_for_special_filter_subrequest
        """

        # проверяем что "каракулька каракулька синенькая" более релевантна с точки зрения пантерной релевантности
        # чем "каракулька красненькая" и самая последняя "барабулька синенькая"
        response = self.report.request_json('place=prime&text=каракулька синенькая&hid=18946&numdoc=100&debug=da')
        self.assertFragmentIn(
            response,
            {'entity': 'offer', 'titles': {'raw': 'каракулька каракулька синенькая'}, 'debug': {'docRel': '30125'}},
        )
        self.assertFragmentIn(
            response,
            {'entity': 'offer', 'titles': {'raw': 'каракулька каракулька красненькая'}, 'debug': {'docRel': '17616'}},
        )
        self.assertFragmentIn(
            response, {'entity': 'offer', 'titles': {'raw': 'барабулька синенькая'}, 'debug': {'docRel': '12509'}}
        )
        # в фильтре присутствуют все 3 производителя
        self.assertFragmentIn(
            response,
            {'id': '7893318', 'values': [{'id': '1001'}, {'id': '1002'}, {'id': '1003'}]},
            allow_different_len=False,
        )

        # при пантерном топе 15 пантерой нагребутся только 15 документов с текстом "каракулька каракулька синенькая" как наиболее релевантные
        # соответственно производители 1002 и 1003 вообще не попадают в фильтр

        response = self.report.request_json(
            'place=prime&text=каракулька синенькая&hid=18946&numdoc=100&debug=da' '&rearr-factors=panther_offer_tpsz=15'
        )
        self.assertFragmentIn(response, {'titles': {'raw': 'каракулька каракулька синенькая'}})
        self.assertFragmentNotIn(response, {'titles': {'raw': 'каракулька каракулька красненькая'}})
        self.assertFragmentNotIn(response, {'titles': {'raw': 'барабулька синенькая'}})

        # в фильтре присутствуют только 1 производитель
        self.assertFragmentIn(response, {'id': '7893318', 'values': [{'id': '1001'}]}, allow_different_len=False)

        # добавляем параметры &show-filter=vendor&nosearchresults=1&no-intents=1&showVendors=all
        # как при запросе при нажатии на кнопку "Показать все" в фильтре по производителю

        # с флагом market_panther_coef_for_special_filter_subrequest=1.0 производитель как и на выдаче - один

        response = self.report.request_json(
            'place=prime&text=каракулька синенькая&hid=18946&numdoc=100&debug=da'
            '&show-filter=vendor&nosearchresults=1&no-intents=1&showVendors=all'
            '&rearr-factors=panther_offer_tpsz=15;market_panther_coef_for_special_filter_subrequest=1.0'
        )
        self.assertFragmentNotIn(response, 'collect_maximum_vendor_values: true')
        self.assertFragmentIn(response, {'id': '7893318', 'values': [{'id': '1001'}]}, allow_different_len=False)

        # с флагом даже без увеличения пантерного топа производителей будет 3
        # и от каждого найдется по 5 документов (1 эшелон)
        response = self.report.request_json(
            'place=prime&text=каракулька синенькая&hid=18946&numdoc=100&debug=da&debug-doc-count=100'
            '&show-filter=vendor&nosearchresults=1&no-intents=1&showVendors=all'
            '&rearr-factors=panther_offer_tpsz=15;market_panther_coef_for_special_filter_subrequest=1.001'
        )
        self.assertFragmentIn(
            response,
            {
                'id': '7893318',
                'values': [
                    {'id': '1001', 'found': 1},  # по одному документу, т.к все остальные отфильтровываются через
                    {'id': '1002', 'found': 1},
                    {'id': '1003', 'found': 1},
                ],
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'search': {},
                'debug': {
                    'brief': {
                        'filters': {'VENDOR_FILTER_ALREADY_ACCEPTED': 12}
                    },  # хз почему он иногда 2 раза учитывает
                    'report': {
                        'context': {'collections': {'SHOP': {'pron': ['panther_top_size_=15']}}}
                    },  # пантерный топ - 15
                },
            },
        )
        self.assertFragmentIn(response, 'collect_maximum_vendor_values: true')

        # флагом market_panther_coef_for_special_filter_subrequest=2.0 (зеначение по умолчанию: 4.0) можно увеличить пантерный топ
        response = self.report.request_json(
            'place=prime&text=каракулька синенькая&hid=18946&numdoc=100&debug=da'
            '&show-filter=vendor&nosearchresults=1&no-intents=1&showVendors=all'
            '&rearr-factors=panther_offer_tpsz=15;market_panther_coef_for_special_filter_subrequest=2.0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {},
                'debug': {
                    'brief': {'filters': {'VENDOR_FILTER_ALREADY_ACCEPTED': 27}},
                    'report': {'context': {'collections': {'SHOP': {'pron': ['panther_top_size_=30']}}}},
                },
            },
        )
        self.assertFragmentIn(response, 'collect_maximum_vendor_values: true')

        # зеначение флага по умолчанию market_panther_coef_for_special_filter_subrequest=4.0
        response = self.report.request_json(
            'place=prime&text=каракулька синенькая&hid=18946&numdoc=100&debug=da'
            '&show-filter=vendor&nosearchresults=1&no-intents=1&showVendors=all'
            '&rearr-factors=panther_offer_tpsz=15'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {},
                'debug': {
                    'brief': {'filters': {'VENDOR_FILTER_ALREADY_ACCEPTED': 42}},
                    'report': {'context': {'collections': {'SHOP': {'pron': ['panther_top_size_=60']}}}},
                },
            },
        )
        self.assertFragmentIn(response, 'collect_maximum_vendor_values: true')

    def test_special_vendor_filtration_by_glfilter_id(self):
        """Аналогично test_special_vendor_filtration но по show-filter=7893318 (синоним show-filter=vendor)"""

        response = self.report.request_json(
            'place=prime&text=каракулька синенькая&hid=18946&numdoc=100&debug=da'
            '&show-filter=7893318&nosearchresults=1&no-intents=1&showVendors=all'
            '&rearr-factors=panther_offer_tpsz=15;market_panther_coef_for_special_filter_subrequest=2.0'
        )
        self.assertFragmentIn(response, 'collect_maximum_vendor_values: true')
        self.assertFragmentIn(
            response,
            {
                'id': '7893318',
                'allowExtended': True,  # означает что фильтр поддерживает дозапрос за всеми значениями за кнопкой Показать все
                'values': [
                    {
                        'id': '1001',
                        'found': 1,
                    },  # по одному документу, т.к все остальные отфильтровываются через VENDOR_FILTER_ALREADY_ACCEPTED
                    {'id': '1002', 'found': 1},
                    {'id': '1003', 'found': 1},
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                'search': {},
                'debug': {
                    'brief': {'filters': {'VENDOR_FILTER_ALREADY_ACCEPTED': 27}},
                    'report': {'context': {'collections': {'SHOP': {'pron': ['panther_top_size_=30']}}}},
                },
            },
        )

    def test_special_vendor_filtration_on_textless(self):
        """Проверяем что происходит когда включается специальный запрос за вендорами при нажатии [Показать все] на бестексте
        Должен увеличиться pruncount, документы с одинаковым вендором должны отфильтровываться по VENDOR_FILTER_ALREADY_ACCEPTED"""

        prun_rearr = '&rearr-factors=market_prime_prun_count_for_special_filter_subrequest=100;market_prun_for_leaf_not_guru_categories_without_text=5'
        metadoc_rearr = (
            '&rearr-factors=market_metadoc_force_on_textless_everywhere=1;'
            'market_metadoc_total_pruncount_textless_not_app=10;market_metadoc_total_pruncount_textless_app=10;'
            'market_metadoc_effective_pruncount_for_prime_filters_subrequest=1;market_metadoc_total_pruncount_for_special_filter_subrequest=100'
        )

        response = self.report.request_json("place=prime&hid=18946&debug=da" + prun_rearr)
        self.assertFragmentNotIn(response, 'collect_maximum_vendor_values: true')
        self.assertFragmentIn(response, 'GetPrimePrunCount(): Pruning: 5')
        self.assertFragmentIn(response, {'id': '7893318', 'values': [{'id': '1001'}]}, allow_different_len=False)

        response = self.report.request_json("place=prime&hid=18946&debug=da" + prun_rearr + metadoc_rearr)
        self.assertFragmentNotIn(response, 'collect_maximum_vendor_values: true')
        self.assertFragmentIn(response, 'GetPrimePrunCount(): Pruning: 5')
        self.assertFragmentIn(response, 'metadoc_total_pruncount: 10')
        self.assertFragmentIn(
            response, {'id': '7893318', 'allowExtended': True, 'values': [{'id': '1001'}]}, allow_different_len=False
        )

        self.assertFragmentIn(
            response,
            {
                'search': {},
                'debug': {
                    'brief': {'counters': {'TOTAL_DOCUMENTS_PROCESSED': 12, 'TOTAL_DOCUMENTS_ACCEPTED': 12}},
                },
            },
        )

        # включаем специальный запрос за вендором
        special_vendor = '&showVendors=all&no-intents=1&nosearchresults=1&show-filter=vendor'

        expected_all_filters = {
            'id': '7893318',
            'allowExtended': True,  # означает что фильтр поддерживает дозапрос за всеми значениями за кнопкой Показать все
            'values': [
                {
                    'id': '1001',
                    'found': 1,
                },  # по одному документу, т.к все остальные отфильтровываются через VENDOR_FILTER_ALREADY_ACCEPTED
                {'id': '1002', 'found': 1},
                {'id': '1003', 'found': 1},
            ],
        }

        expected_filtered_by = {
            'search': {},
            'debug': {
                'brief': {
                    'filters': {'VENDOR_FILTER_ALREADY_ACCEPTED': 42},
                    'counters': {'TOTAL_DOCUMENTS_PROCESSED': 45, 'TOTAL_DOCUMENTS_ACCEPTED': 3},
                },
            },
        }

        response = self.report.request_json("place=prime&hid=18946&debug=da" + prun_rearr + special_vendor)
        self.assertFragmentIn(response, 'collect_maximum_vendor_values: true')
        self.assertFragmentIn(response, 'GetPrimePrunCount(): Pruning: 100')
        self.assertFragmentIn(response, expected_all_filters, allow_different_len=False)
        self.assertFragmentIn(response, expected_filtered_by)

        response = self.report.request_json(
            "place=prime&hid=18946&debug=da" + prun_rearr + metadoc_rearr + special_vendor
        )
        self.assertFragmentIn(response, 'collect_maximum_vendor_values: true')
        self.assertFragmentIn(response, 'GetPrimePrunCount(): Pruning: 100')
        self.assertFragmentIn(response, 'metadoc_total_pruncount: 100')
        self.assertFragmentIn(response, expected_all_filters, allow_different_len=False)
        self.assertFragmentIn(response, expected_filtered_by)

    @classmethod
    def prepare_metadocs(cls):
        cls.index.gltypes += [
            GLType(param_id=301, hid=3, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=302, hid=3, gltype=GLType.ENUM, values=[3, 4, 5]),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=123,
                hid=3,
                title='кулебяка 1',
                blue_offers=[BlueOffer(title='кулебяка 1')],
                glparams=[
                    GLParam(param_id=301, value=1),
                    GLParam(param_id=302, value=4),
                ],
            ),
            MarketSku(
                sku=456,
                hid=3,
                title='кулебяка 2',
                blue_offers=[BlueOffer(title='кулебяка 2')],
                glparams=[
                    GLParam(param_id=301, value=1),
                    GLParam(param_id=302, value=5),
                ],
            ),
            MarketSku(
                sku=789,
                hid=3,
                title='кулебяка 3',
                blue_offers=[BlueOffer(title='кулебяка 3')],
                glparams=[
                    GLParam(param_id=301, value=2),
                    GLParam(param_id=302, value=3),
                ],
            ),
        ]

    def test_metadocs(self):
        """
        Проверяем, что засеривание работает в поиске по ску
        """

        name = '&rearr-factors=market_metadoc_search='

        for metadoc_flag in ('', name + 'no', name + 'offers', name + 'skus'):
            response = self.report.request_json('place=prime&text=кулебяка&glfilter=301:1{}'.format(metadoc_flag))

            self.assertFragmentIn(
                response, {'filters': [{'id': '302', 'values': [{'id': '3', 'found': 0, 'initialFound': 1}]}]}
            )


if __name__ == '__main__':
    main()
