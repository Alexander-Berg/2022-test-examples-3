#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop, YamarecPlace, YamarecSettingPartition
from core.testcase import main
from core.types.autogen import Const
from simple_testcase import SimpleTestCase


class T(SimpleTestCase):
    """
    Набор тестов для конфигурируемой фильтрации по цене
    метода офферных аксессуаров
    """

    @classmethod
    def prepare(cls):
        """
        Данные оффера и его аксессуаров, собранных из трёх источников:
        из фидов (REC), модельной формулы и офферной формулы
        """

        # Глааная модель и оффлайн-аксессуары
        cls.index.models += [
            Model(hyperid=1, hid=101, title="model #1", accessories=[6, 7, 8]),
        ]

        cls.index.offers += [
            # главный оффер
            Offer(
                hyperid=1,
                hid=101,
                price=1000,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                waremd5="EpnWVxDQxj4wg7vVI1ElnA",
                rec=["TTnVlqbztMi95ithBNMa3g", "BH8EPLtKmdLQhLUasgaOnA"],
            ),
            # офферная формула
            Offer(hyperid=2, hid=102, price=700, cpa=Offer.CPA_REAL, fesh=1010, waremd5="pCl2on9YL4fCV8poq57hRg"),
            Offer(hyperid=3, hid=102, price=1100, cpa=Offer.CPA_REAL, fesh=1010, waremd5="bpQ3a9LXZAl_Kz34vaOpSg"),
            # фиды
            Offer(hyperid=4, hid=102, price=600, cpa=Offer.CPA_REAL, fesh=1010, waremd5="TTnVlqbztMi95ithBNMa3g"),
            Offer(hyperid=5, hid=102, price=1110, cpa=Offer.CPA_REAL, fesh=1010, waremd5="BH8EPLtKmdLQhLUasgaOnA"),
            # модельная формула
            Offer(hyperid=6, hid=102, price=650, cpa=Offer.CPA_REAL, fesh=1010),
            Offer(hyperid=7, hid=102, price=900, cpa=Offer.CPA_REAL, fesh=1010),
            Offer(hyperid=8, hid=102, price=1157, cpa=Offer.CPA_REAL, fesh=1010),
        ]

        cls.index.shops += [
            Shop(fesh=1010, name='The Shop', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name="offer-accessory",
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # Ограничения по цене не заданы
                    YamarecSettingPartition(
                        params={"version": "1", "use-external": "1", "use-product": "1", "use-shop": "1"},
                        splits=[{"split": "1"}],
                    ),
                    # Ограничения по цене явно отключены
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "0",
                            "filter-by-basket-offer-price": "0",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "2"}],
                    ),
                    # filter-by-price. Не задан price-to
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                            "filter-by-basket-offer-price": "0",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "3"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                            "filter-by-basket-offer-price": "0",
                            "price-to": "0.0",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "4"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                            "filter-by-basket-offer-price": "0",
                            "price-to": "-0.1",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "5"}],
                    ),
                    # price-to задан
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                            "filter-by-basket-offer-price": "0",
                            "price-to": "850",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "6"}],
                    ),
                    # basket-offer-price-threshold не задан
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "0",
                            "filter-by-basket-offer-price": "1",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "7"}],
                    ),
                    # basket-offer-price-threshold <= 0.0
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "0",
                            "filter-by-basket-offer-price": "1",
                            "basket-offer-price-threshold": "0.0",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "8"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "0",
                            "filter-by-basket-offer-price": "1",
                            "basket-offer-price-threshold": "-0.9",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "9"}],
                    ),
                    # basket-offer-price-threshold >= 1.0
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "0",
                            "filter-by-basket-offer-price": "1",
                            "basket-offer-price-threshold": "1.0",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "10"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "0",
                            "filter-by-basket-offer-price": "1",
                            "basket-offer-price-threshold": "100500.0",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "11"}],
                    ),
                    # 0.0 < basket-offer-price-threshold < 1.0
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "0",
                            "filter-by-basket-offer-price": "1",
                            "basket-offer-price-threshold": "0.8",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "12"}],
                    ),
                    # Включены все фильтры, один из которых сильно ограничительный,
                    # а второй наоборот
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                            "filter-by-basket-offer-price": "1",
                            "basket-offer-price-threshold": "0.61",
                            "price-to": "1150.0",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "13"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                            "filter-by-basket-offer-price": "1",
                            "basket-offer-price-threshold": "0.99",
                            "price-to": "50.0",
                            "version": "1",
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "1",
                        },
                        splits=[{"split": "14"}],
                    ),
                ],
            )
        ]

        # конфигурация внешнего сервиса для офферной формулы
        cls.recommender.on_request_accessory_offers(
            offer_id="EpnWVxDQxj4wg7vVI1ElnA", item_count=100, version="1"
        ).respond({"offers": ["pCl2on9YL4fCV8poq57hRg", "bpQ3a9LXZAl_Kz34vaOpSg"]})

    @staticmethod
    def make_query(split=None):
        rearr_param = "" if split is None else "&rearr-factors=split={split}".format(split=split)
        return "place=accessories&pp=143&offerid=EpnWVxDQxj4wg7vVI1ElnA&fesh=1010&hyperid=1&price=1000{rearr_param}".format(
            rearr_param=rearr_param
        )

    @staticmethod
    def all_ids():
        return list(range(2, 9))

    def test_filters_disabled(self):
        """
        Параметры ограничения цены не заданы в конфигурации
        проверяем значения по умолчанию: фильтры отключены
        Проверяем, что проходят все офферы
        """
        self.assertOffersInResponse(query=self.make_query(split=1), model_ids=T.all_ids())

        """
        Фильтры явно отключены флагами в конфигурации
        Проверяем, что проходят все офферы
        """
        self.assertEqualJsonResponses(request1=self.make_query(split=1), request2=self.make_query(split=2))

    def test_global_price_filter(self):
        """
        Тест общего ограничения цены

        Флаг общего фильтра взведён, но не задано значение
        price-to, что означает отключение фильтра.
        Проверяем, что проходят все офферы
        """
        self.assertOffersInResponse(query=self.make_query(split=3), model_ids=T.all_ids())
        """
        Флаг общего фильтра взведён, но price-to <= 0, что означает отключение фильтра
        Проверяем, что проходят все офферы
        """
        self.assertEqualJsonResponses(request1=self.make_query(split=4), request2=self.make_query(split=3))
        self.assertEqualJsonResponses(request1=self.make_query(split=5), request2=self.make_query(split=3))
        """
        Фильтр включен, проверяем, что дорогие офферы не прошли
        """
        self.assertOnlyOffersInResponse(query=self.make_query(split=6), model_ids=[2, 4, 6], all_model_ids=T.all_ids())

    def test_price_filter(self):
        """
        Тест ограничения цены аксессуаров по цене главного оффера
        Маргинальная конфигурация

        Флаг фильтра взведён, basket-offer-price-threshold == 1.0.
        Проверяем, что проходят только относительно дешевые аксессуары
        """
        self.assertOnlyOffersInResponse(
            query=self.make_query(split=10), model_ids=[2, 4, 6, 7], all_model_ids=T.all_ids()
        )
        """
        Флаг фильтра взведён, basket-offer-price-threshold > 1.0.
        Проверяем, что это равносильно basket-offer-price-threshold == 1.0
        """
        self.assertEqualJsonResponses(request1=self.make_query(split=11), request2=self.make_query(split=10))
        """
        Флаг фильтра взведён, basket-offer-price-threshold не задан.
        Проверяем, что это равносильно basket-offer-price-threshold == 1.0
        """
        self.assertEqualJsonResponses(request1=self.make_query(split=7), request2=self.make_query(split=10))
        """
        Флаг фильтра взведён, задан basket-offer-price-threshold <= 0.0
        Проверяем, что это равносильно отключению фильтра
        """
        self.assertOffersInResponse(query=self.make_query(split=8), model_ids=T.all_ids())
        self.assertEqualJsonResponses(request1=self.make_query(split=8), request2=self.make_query(split=9))

    def test_price_filter_2(self):
        """
        Тест ограничения цены аксессуаров по цене главного оффера
        Нормальная конфигурация.

        Флаг фильтра взведён, задан 0.0 < basket-offer-price-threshold < 1.0
        Проверяем, что для каждого офера работает порог вхождения:
        accessory < offer * basket-offer-threshold
        """
        self.assertOnlyOffersInResponse(query=self.make_query(split=12), model_ids=[2, 4, 6], all_model_ids=T.all_ids())

    def test_superfilter(self):
        """
        Тест двойного фильтра.
        Проверяем, что фильтры складываются:

        Для price-to: 1150 не попадает только #8, несмотря на малый относительный порог
        """
        self.assertOnlyOffersInResponse(
            self.make_query(split=13), model_ids=[2, 3, 4, 5, 6, 7], all_model_ids=T.all_ids()
        )
        """
        Относительный порог работает, так как позволяет пройти #7 и не пропускает оферы с ценой выше главного
        Сложение фильтров работает, так как обходится глобальное ограничение
        """
        self.assertOnlyOffersInResponse(self.make_query(split=14), model_ids=[2, 4, 6, 7], all_model_ids=T.all_ids())


if __name__ == '__main__':
    main()
