#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MnPlace, Model, ModelGroup, Offer, RegionalModel, Shop, YamarecPlace, YamarecSettingPartition
from core.testcase import TestCase, main
from core.dj import DjModel


class T(TestCase):
    """
    Набор тестов для проверки наличия дефолтного оффера
    в моделях выдачи для случаев, связанных с групповыми моделями
    и модификациями

    (Дублирует некоторые тесты, заимствованные из test_default_offer_in_models.py,
    todo: удалить дубли из test_default_offer_in_models.py)
    """

    @classmethod
    def register_groups_with_default_offer(
        cls, modification_sets, group_ids, analog_lists=None, prices=None, discounts=None, hids=None, shop_id=None
    ):
        """
        Добавляются в индекс группы и их модификации
        Для модификаций добавляется дефолтный оффер
        Несколько модификации групповой модели полезны для проверки выбора модификации
        при маппинге оффера к групповой модели
        Офферы модификаций для групп ранжируются с точки зрения ДО в порядке,
        в котором переданы их идентификаторы в modification_sets (за счет ts)
        """
        # данные по умолчанию
        fesh = shop_id if shop_id is not None else THE_SHOP_ID
        _prices = [100500] * len(group_ids) if prices is None else prices
        _hids = [None] * len(group_ids) if hids is None else hids
        _discounts = [None] * len(group_ids) if discounts is None else discounts
        _analog_lists = [[]] * len(group_ids) if analog_lists is None else analog_lists
        # Кажду каждую групповую модель и её модификации добавляем
        # в индекс вместе с офферами
        for modification_ids, group_id, analogs, hid, price, discount in zip(
            modification_sets, group_ids, _analog_lists, _hids, _prices, _discounts
        ):
            # model group
            cls.index.model_groups.append(ModelGroup(hyperid=group_id, hid=hid, analogs=analogs))
            # modifications with DO
            ids = [modification_ids] if type(modification_ids) == int else modification_ids
            cls.index.models += [
                Model(hyperid=modification_id, hid=hid, group_hyperid=group_id) for modification_id in ids
            ]
            cls.index.offers += [
                Offer(
                    ts=modification_id,
                    fesh=fesh,
                    hyperid=modification_id,
                    price=price,
                    waremd5=gen_waremd5(hyperid=modification_id, price=price),
                    discount=discount,
                )
                for modification_id in ids
            ]
            for i, hyperid in enumerate(ids):
                ts = hyperid
                mn_value = DEFAULT_MN_VALUE * (100 - i)
                cls.matrixnet.on_place(place=MnPlace.BASE_SEARCH, target=ts).respond(mn_value)

    @classmethod
    def _reg_ichwill_request(cls, user_id, models, item_count=40):
        cls.recommender.on_request_models_of_interest(
            user_id=user_id, item_count=item_count, with_timestamps=True, version=4
        ).respond({'models': map(str, models), 'timestamps': map(str, list(range(len(models), 0, -1)))})
        cls.bigb.on_request(yandexuid=user_id.replace('yandexuid:', ''), client='merch-machine').respond(counters=[])

    @classmethod
    def prepare(cls):
        """
        Данные для основных кейсов, общие для большинства плэйсов
        """

        # Общие данные
        cls.index.shops += [
            Shop(fesh=THE_SHOP_ID, regions=[THE_REGION_ID]),
        ]
        # Обычные модели с дефолтными офферами
        cls.index.models += [
            Model(hyperid=10, hid=102),
            Model(hyperid=11, hid=103),
        ]
        cls.index.offers += [
            Offer(waremd5=gen_waremd5(hyperid=10, price=400), fesh=THE_SHOP_ID, hyperid=10, price=400),
            Offer(waremd5=gen_waremd5(hyperid=11, price=400), fesh=THE_SHOP_ID, hyperid=11, price=400),
        ]
        # Групповые модели с дефолтными офферами
        cls.register_groups_with_default_offer(
            group_ids=[1, 6, 7], modification_sets=[[2, 3, 4, 5], [8], [9]], hids=[101, 102, 101], prices=[100] * 3
        )
        # Групповая модель (hyperid=6) со своим собственным оффером, менее релевантным, чем оффер модифифкации (hyperid=8)
        # (Специально для place=better_price делаем также цену точно такую же, как и в модификации,
        # чтоб выбор дефолтного оффера был с помощью релевантности
        cls.index.offers += [
            Offer(hyperid=6, waremd5=gen_waremd5(hyperid=6, price=100), fesh=THE_SHOP_ID, ts=6, price=100),
        ]
        low_mn_value = DEFAULT_MN_VALUE * 0.1
        cls.matrixnet.on_place(place=MnPlace.BASE_SEARCH, target=6).respond(low_mn_value)
        # Групповая модель со своим собственным оффером, более релевантным, чем оффер модифифкации
        cls.index.offers += [
            Offer(hyperid=7, waremd5=gen_waremd5(hyperid=7, price=90), fesh=THE_SHOP_ID, ts=7, price=90),
        ]
        high_mn_value = DEFAULT_MN_VALUE * 1000
        cls.matrixnet.on_place(place=MnPlace.BASE_SEARCH, target=7).respond(high_mn_value)

        # Конфигурация рекомендаций
        #
        # Проверяемые плэйсы похожи тем, что отправляют запрос на ДО, составляемый из моделей-рекомендаций
        # для каджого плэйса регистрируем даные для слудующих случаев:
        #
        # choose_modification_offer: в запрос на ДО отправляется группа с модификациями, но на первом месте групповая модель
        # share_default_offer: в запрос на ДО отправляется группа с модификациями, но на первом месте модификация
        # group_model_with_offer: у группы есть собственный релевантный оффер, а также офферы модификаций
        # multiple_group_models: есть группы с релевантным собственным оффером
        #   и есть группы с релевантным оффером модификации
        # mixed_output: в запросе на ДО присутствуют обычные модели вперемежку с групповой и модификациями
        # mixed_output_groups_last: в запросе на ДО присутствуют обычные модели и они - в начале запроса на ДО
        # mixed_output_groups_first: в запросе на ДО присутствуют обычные модели и они - в конце запроса на ДО

        # products_by_history
        cls.index.regional_models += [
            RegionalModel(hyperid=hyperid, offers=123, rids=[THE_REGION_ID]) for hyperid in [1, 2, 3, 4, 5, 7, 8, 10]
        ]
        cls._reg_ichwill_request('yandexuid:choose_modification_offer', [1, 10, 8, 7])
        cls._reg_ichwill_request('yandexuid:share_default_offer', [4, 3, 2, 1])
        cls._reg_ichwill_request('yandexuid:group_model_with_offer', [7, 10, 8, 5])
        cls._reg_ichwill_request('yandexuid:multiple_group_models', [6, 11, 7, 10])

        # return dj models
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='17001').respond(
            [DjModel(id='1'), DjModel(id='10'), DjModel(id='8'), DjModel(id='7')]
        )
        cls.dj.on_request(yandexuid='17002').respond(
            [DjModel(id='4'), DjModel(id='3'), DjModel(id='2'), DjModel(id='1')]
        )
        cls.dj.on_request(yandexuid='17003').respond(
            [DjModel(id='7'), DjModel(id='10'), DjModel(id='8'), DjModel(id='5')]
        )
        cls.dj.on_request(yandexuid='17004').respond(
            [DjModel(id='6'), DjModel(id='11'), DjModel(id='7'), DjModel(id='10')]
        )
        # also_viewed
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={'version': 'choose_modification_offer'}, splits=[{'split': 'choose_modification_offer'}]
                    ),
                    YamarecSettingPartition(
                        params={'version': 'share_default_offer'}, splits=[{'split': 'share_default_offer'}]
                    ),
                    YamarecSettingPartition(
                        params={'version': 'group_model_with_offer'}, splits=[{'split': 'group_model_with_offer'}]
                    ),
                    YamarecSettingPartition(
                        params={'version': 'multiple_group_models'}, splits=[{'split': 'multiple_group_models'}]
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(
            model_id=10, item_count=1000, version='choose_modification_offer'
        ).respond({'models': ['1', '2', '3', '4', '5']})
        cls.recommender.on_request_accessory_models(
            model_id=10, item_count=1000, version='share_default_offer'
        ).respond({'models': ['4', '2', '3', '1']})
        cls.recommender.on_request_accessory_models(
            model_id=10, item_count=1000, version='group_model_with_offer'
        ).respond({'models': ['7']})
        cls.recommender.on_request_accessory_models(
            model_id=10, item_count=1000, version='multiple_group_models'
        ).respond({'models': ['6', '7']})

        # product_accessories
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={'version': 'choose_modification_offer'}, splits=[{'split': 'choose_modification_offer'}]
                    ),
                    YamarecSettingPartition(
                        params={'version': 'share_default_offer'}, splits=[{'split': 'share_default_offer'}]
                    ),
                    YamarecSettingPartition(
                        params={'version': 'group_model_with_offer'}, splits=[{'split': 'group_model_with_offer'}]
                    ),
                    YamarecSettingPartition(
                        params={'version': 'multiple_group_models'}, splits=[{'split': 'multiple_group_models'}]
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(
            model_id=10, item_count=1000, version='choose_modification_offer'
        ).respond({'models': ['1', '2', '3', '4', '5']})
        cls.recommender.on_request_accessory_models(
            model_id=10, item_count=1000, version='share_default_offer'
        ).respond({'models': ['4', '2', '3', '1']})
        cls.recommender.on_request_accessory_models(
            model_id=10, item_count=1000, version='group_model_with_offer'
        ).respond({'models': ['7']})
        cls.recommender.on_request_accessory_models(
            model_id=10, item_count=1000, version='multiple_group_models'
        ).respond({'models': ['6', '7']})

        # better_price
        cls.index.yamarec_places += [
            YamarecPlace(
                name='better-price',
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'filter-by-price': '1',
                        },
                        splits=[
                            {'split': 'choose_modification_offer'},
                            {'split': 'share_default_offer'},
                            {'split': 'group_model_with_offer'},
                            {'split': 'multiple_group_models'},
                        ],
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_we_have_cheaper(
            user_id='yandexuid:choose_modification_offer', item_count=100
        ).respond(
            {
                'we_have_cheaper': [
                    {'model_id': hyperid, 'price': 550.0, 'success_requests_share': 0.1, 'timestamp': '1495206745'}
                    for hyperid in [1, 2, 3, 4, 5]
                ]
            }
        )
        cls.recommender.on_request_we_have_cheaper(user_id='yandexuid:share_default_offer', item_count=100).respond(
            {
                'we_have_cheaper': [
                    {'model_id': hyperid, 'price': 550.0, 'success_requests_share': 0.1, 'timestamp': '1495206745'}
                    for hyperid in [4, 3, 2, 1]
                ]
            }
        )
        cls.recommender.on_request_we_have_cheaper(user_id='yandexuid:group_model_with_offer', item_count=100).respond(
            {
                'we_have_cheaper': [
                    {'model_id': hyperid, 'price': 550.0, 'success_requests_share': 0.1, 'timestamp': '1495206745'}
                    for hyperid in [7]
                ]
            }
        )
        cls.recommender.on_request_we_have_cheaper(user_id='yandexuid:multiple_group_models', item_count=100).respond(
            {
                'we_have_cheaper': [
                    {'model_id': hyperid, 'price': 550.0, 'success_requests_share': 0.1, 'timestamp': '1495206745'}
                    for hyperid in [6, 7]
                ]
            }
        )

    def get_response(self, base_query):
        return self.report.request_json('{query}&rids={region_id}'.format(query=base_query, region_id=THE_REGION_ID))

    def check_default_offer(self, response, product_ids, product_types=None, prices=None, shop_id=None):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче
        """
        fesh = shop_id if shop_id is not None else THE_SHOP_ID
        types = product_types if product_types is not None else ['model'] * len(product_ids)
        _prices = [100500] * len(product_ids) if prices is None else prices
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'type': t,
                            'id': hyperid,
                            'offers': {
                                'items': [
                                    {  # default offer
                                        'entity': 'offer',
                                        'wareId': gen_waremd5(hyperid=hyperid, price=price),
                                        'prices': {'value': str(price)},
                                        'shop': {'id': fesh},
                                        'model': {'id': hyperid},
                                    },
                                ]
                            },
                        }
                        for hyperid, t, price in zip(product_ids, types, _prices)
                    ]
                }
            },
            preserve_order=False,
        )
        return response

    def check_default_offer_in_groups(self, response, group_ids, modification_ids, prices=None, shop_id=None):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче для группы
        """
        fesh = shop_id if shop_id is not None else THE_SHOP_ID
        _prices = [100500] * len(group_ids) if prices is None else prices
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'type': 'group',
                            'id': group_id,  # searched model
                            'offers': {
                                'items': [
                                    {  # default offer
                                        'entity': 'offer',
                                        'wareId': gen_waremd5(hyperid=modification_id, price=price),
                                        'prices': {'value': str(price)},
                                        'shop': {'id': fesh},
                                        'model': {
                                            'id': modification_id,  # actual modification model with default offer
                                            'parentId': group_id,
                                        },
                                    },
                                ]
                            },
                        }
                        for modification_id, group_id, price in zip(modification_ids, group_ids, _prices)
                    ]
                }
            },
            preserve_order=False,
        )
        return response

    def test_choose_modification_offer(self):
        """
        Проверяем, что для групповой модели находится дефолтный оффер подходящей модификации
        """
        testname = 'choose_modification_offer'
        split = testname
        yandexuid = testname
        for query in [
            'place=modelinfo&hyperid=1&use-default-offers=1',
            'place=products_by_history&rearr-factors=market_disable_dj_for_recent_findings%3D1',
            'place=better_price&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0',
            'place=product_accessories&hyperid=10&rearr-factors=market_disable_product_accessories=0',
            'place=also_viewed&hyperid=10',
        ]:
            response = self.get_response(
                base_query='{query}&rearr-factors=split={split}&yandexuid={yandexuid}'.format(
                    query=query, split=split, yandexuid=yandexuid
                )
            )
            self.check_default_offer_in_groups(response=response, group_ids=[1], modification_ids=[2], prices=[100])

        response = self.get_response(
            base_query='place=products_by_history&rearr-factors=split={split}&yandexuid=17001'.format(split=split)
        )
        self.check_default_offer_in_groups(response=response, group_ids=[1], modification_ids=[2], prices=[100])

    def test_group_model_offer(self):
        """
        Проверяем, что непосредственный оффер групповой модели может быть дефолтным
        при условии подходящей релевантности, даже если есть оффер модификации
        """
        testname = 'group_model_with_offer'
        split = testname
        yandexuid = testname
        for query in [
            'place=modelinfo&hyperid=7&use-default-offers=1',
            'place=products_by_history&rearr-factors=market_disable_dj_for_recent_findings%3D1',
            'place=better_price&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0',
            'place=product_accessories&hyperid=10&rearr-factors=market_disable_product_accessories=0',
            'place=also_viewed&hyperid=10',
        ]:
            response = self.get_response(
                base_query='{query}&rearr-factors=split={split}&yandexuid={yandexuid}'.format(
                    query=query, split=split, yandexuid=yandexuid
                )
            )
            # Релевантнее оффер самой групповой модели
            self.check_default_offer(response=response, product_ids=[7], product_types=['group'], prices=[90])

        response = self.get_response(
            base_query='place=products_by_history&rearr-factors=split={split}&yandexuid=17003'.format(split=split)
        )
        # Релевантнее оффер самой групповой модели
        self.check_default_offer(response=response, product_ids=[7], product_types=['group'], prices=[90])

    def test_multiple_group_models(self):
        """
        Проверяем, что корректность поиска дефолтных офферов для нескольких групп в запросе
        Есть группы с релевантным собственным оффером, есть группы - с релевавнтным оффером
        модификации
        """
        testname = 'multiple_group_models'
        split = testname
        yandexuid = testname
        for query in [
            'place=modelinfo&hyperid=6,7&use-default-offers=1',
            'place=products_by_history&rearr-factors=market_disable_dj_for_recent_findings%3D1',
            'place=better_price&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0',
            'place=product_accessories&hyperid=10&rearr-factors=market_disable_product_accessories=0',
            'place=also_viewed&hyperid=10',
        ]:
            response = self.get_response(
                base_query='{query}&rearr-factors=split={split}&yandexuid={yandexuid}'.format(
                    query=query, split=split, yandexuid=yandexuid
                )
            )
            # Релевантнее оффер модификации
            self.check_default_offer_in_groups(response=response, group_ids=[6], modification_ids=[8], prices=[100])
            # Релевантнее оффер самой групповой модели
            self.check_default_offer(response=response, product_ids=[7], product_types=['group'], prices=[90])

        response = self.get_response(
            base_query='place=products_by_history&rearr-factors=split={split}&yandexuid=17004'.format(split=split)
        )
        # Релевантнее оффер модификации
        self.check_default_offer_in_groups(response=response, group_ids=[6], modification_ids=[8], prices=[100])
        # Релевантнее оффер самой групповой модели
        self.check_default_offer(response=response, product_ids=[7], product_types=['group'], prices=[90])

    def test_share_default_offer(self):
        """
        Проверяем наличие вставки дефолтного оффера для групповой модели,
        когда подходящий дефолтный оффер есть дефолтный оффер модификации этой модели,
        также присутствующей в выдаче
        """
        testname = 'share_default_offer'
        split = testname
        yandexuid = testname
        for query in [
            'place=modelinfo&hyperid=1,2,3,4,5&use-default-offers=1',
            # Не проверяем 'place=popular_products_today' и 'place=popular_products', поскольку в этой ручке дедуплицирование категорий,
            # и поэтому кейс не повторяется
            'place=products_by_history&rearr-factors=market_disable_dj_for_recent_findings%3D1',
            'place=better_price&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0',
            'place=product_accessories&hyperid=10&rearr-factors=market_disable_product_accessories=0',
            'place=also_viewed&hyperid=10',
        ]:
            response = self.get_response(
                base_query='{query}&rearr-factors=split={split}&yandexuid={yandexuid}'.format(
                    query=query, split=split, yandexuid=yandexuid
                )
            )
            self.check_default_offer_in_groups(response=response, group_ids=[1], modification_ids=[2], prices=[100])
            self.check_default_offer(
                response=response, product_ids=[2, 3, 4, 5], product_types=['modification'] * 4, prices=[100]
            )
        response = self.get_response(
            base_query='place=products_by_history&rearr-factors=split={split}&yandexuid=17002'.format(split=split)
        )
        self.check_default_offer_in_groups(response=response, group_ids=[1], modification_ids=[2], prices=[100])
        self.check_default_offer(
            response=response, product_ids=[2, 3, 4, 5], product_types=['modification'] * 4, prices=[100]
        )


THE_SHOP_ID = 1
THE_REGION_ID = 1
DEFAULT_MN_VALUE = 0.0001


def gen_waremd5(hyperid, price):
    return (str(hyperid) + str(int(price * 100000)) + '1' * 21)[:21] + 'w'


if __name__ == '__main__':
    main()
