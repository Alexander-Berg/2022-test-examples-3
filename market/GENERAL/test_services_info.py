#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.search.services_for_goods.mt.env as env


class T(env.ServicesForGoodsSuite):
    @classmethod
    def setup_market_access_resources(cls):
        cls.access_manager.add_services_info_resource({
            "services": [
                {
                    "service_supplier": {
                        "id": 1000,
                        "name": "JavaBusters"
                    },
                    "description": "Демонтаж + установка,  подключение, проверка, монтаж фасада",
                    "title": "Установка с демонтажом",
                    "price": {
                        "currency": "RUB",
                        "value": 4000
                    },
                    "ya_service_id": "{\"service_id\":[\"4021\",{\"tip_kholodilnika\":\"tip_kholodilnika_0\"}],\"additionals\":[\"demontazh_vstroennoi_tekhniki\",\"navesit_fasad_na_kholodilnik\"]}",
                    "filter": {
                        "sku": "6838903",
                        "hid": 14369615
                    },
                    "service_id": 214,
                    "rid": 213
                },
                {
                    "service_supplier": {
                        "id": 1000,
                        "name": "JavaBusters"
                    },
                    "description": "Проводка коммуникаций (до 5 м) + установка, монтаж фасада",
                    "title": "Комплексная установка",
                    "price": {
                        "currency": "RUB",
                        "value": 3800
                    },
                    "ya_service_id": "{\"service_id\":[\"4021\",{\"tip_kholodilnika\":\"tip_kholodilnika_0\"}],\"additionals\":[\"navesit_fasad_na_kholodilnik\",\"provesti_elektrokommunikatsii\"]}",
                    "filter": {
                        "sku": "539575033",
                        "hid": 14369615
                    },
                    "service_id": 5,
                    "rid": 213
                },
                {
                    "service_supplier": {
                        "id": 1002,
                        "name": "JavaBusters"
                    },
                    "description": "Услуга в московской области",
                    "title": "Комплексная установка",
                    "price": {
                        "currency": "RUB",
                        "value": 3800
                    },
                    "ya_service_id": "{\"service_id\":[\"4021\",{\"tip_kholodilnika\":\"tip_kholodilnika_0\"}],\"additionals\":[\"navesit_fasad_na_kholodilnik\",\"provesti_elektrokommunikatsii\"]}",
                    "filter": {
                        "sku": "539575044",
                        "hid": 14369616
                    },
                    "service_id": 7,
                    "rid": 1
                }
            ],
            "dsbs_shops_with_services": [1058670, 1073490, 1084891, 1096429, 1148950, 1256362],
            "skus_by_service_id": {"214": ["6838903"], "5": ["539575033"], "7": ["539575044"]}
        })
        cls.resources_manager.add_geo_info([
            {'id': 10000, 'parent': 0, 'name': "Земля", 'locative': "Земле", 'preposition': "на"},
            {'id': 11, 'parent': 10000, 'name': 'Евразия', 'locative': 'Евразии', 'preposition': 'в'},
            {'id': 111, 'parent': 11, 'name': 'СНГ', 'locative': 'СНГ', 'preposition': 'в'},
            {'id': 1111, 'parent': 111, 'name': 'Россия', 'locative': 'России', 'preposition': 'в'},
            {'id': 1, 'parent': 1111, 'name': 'Московская область', 'locative': 'Московская область', 'preposition': 'в'},
            {'id': 213, 'parent': 1, 'name': 'Москва', 'locative': 'Москва', 'preposition': 'в'},
            {'id': 214, 'parent': 1, 'name': 'Подольск', 'locative': 'Подольск', 'preposition': 'в'},
        ])

    def test_two_infos(self):
        """
        Проверяем что есть возможность полуить информацию по двум сервисам
        """
        response = self.services_for_goods.request_json('internal/services-for-goods/v1/services-info?ids=214;5')
        self.assertFragmentIn(response, {
            "services_info": [{
                "description": "Демонтаж + установка,  подключение, проверка, монтаж фасада",
                "price": {
                    "currency": "RUR",
                    "value": "4000"
                },
                "service_id": 214,
                "service_supplier": {
                    "id": 1000,
                    "name": "JavaBusters"
                },
                "title": "Установка с демонтажом",
                "ya_service_id": "{\"service_id\":[\"4021\",{\"tip_kholodilnika\":\"tip_kholodilnika_0\"}],\"additionals\":[\"demontazh_vstroennoi_tekhniki\",\"navesit_fasad_na_kholodilnik\"]}",
            },
            {
                "description": "Проводка коммуникаций (до 5 м) + установка, монтаж фасада",
                "price": {
                    "currency": "RUR",
                    "value": "3800"
                },
                "service_id": 5,
                "service_supplier": {
                    "id": 1000,
                    "name": "JavaBusters"
                },
                "title": "Комплексная установка",
                "ya_service_id": "{\"service_id\":[\"4021\",{\"tip_kholodilnika\":\"tip_kholodilnika_0\"}],\"additionals\":[\"navesit_fasad_na_kholodilnik\",\"provesti_elektrokommunikatsii\"]}",
            }]
        }, allow_different_len=False)

    def test_one_info(self):
        """
        Проверяем что есть возможность полуить информацию по одному сервису
        """
        response = self.services_for_goods.request_json('internal/services-for-goods/v1/services-info?ids=214')
        self.assertFragmentIn(response, {
            "services_info": [{
                "description": "Демонтаж + установка,  подключение, проверка, монтаж фасада",
                "price": {
                    "currency": "RUR",
                    "value": "4000"
                },
                "service_id": 214,
                "service_supplier": {
                    "id": 1000,
                    "name": "JavaBusters"
                },
                "title": "Установка с демонтажом",
                "ya_service_id": "{\"service_id\":[\"4021\",{\"tip_kholodilnika\":\"tip_kholodilnika_0\"}],\"additionals\":[\"demontazh_vstroennoi_tekhniki\",\"navesit_fasad_na_kholodilnik\"]}",
            }]
        }, allow_different_len=False)

    def test_available_services_in_user_region(self):
        """
        Проверяем, что услуги из региона пользователя доступны
        """
        print("test_available_services")
        body = {
            "filters": [{
                "sku": "6838903",
                "hid": 14369615
            },
            {
                "sku": "539575033",
                "hid": 14369615
            }]
        }
        response = self.services_for_goods.request_json('internal/services-for-goods/v1/available-services?rid=213', 'POST', body)
        self.assertFragmentIn(response, {
            "available_services": {
                "skus": {
                "539575033": {
                    "services": {
                    "ids": [
                        5
                    ]
                    }
                },
                "6838903": {
                    "services": {
                    "ids": [
                        214
                    ]
                    }
                }
                }
            }
        }, allow_different_len=False)

    def test_available_services_from_global_to_user_region(self):
        """
        Проверяем, что услуги из более глобального региона доступны пользователю из внутреннего региона
        """
        print("test_available_services")
        body = {
            "filters": [{
                "sku": "539575044",
                "hid": 14369616
            }]
        }
        response = self.services_for_goods.request_json('internal/services-for-goods/v1/available-services?rid=213', 'POST', body)
        self.assertFragmentIn(response, {
            "available_services": {
                "skus": {
                "539575044": {
                    "services": {
                    "ids": [
                        7
                    ]
                    }
                }
                }
            }
        }, allow_different_len=False)

    def test_skus_with_two_different_dbs_shop_ids(self):
        """
        Проверяем скю из разных магазинов оба доступных для услуг
        """
        print("test_available_services")
        body = {
            "filters": [{
                "sku": "6838903",
                "hid": 14369615,
                "dbsShopId": 1058670
            },
            {
                "sku": "539575033",
                "hid": 14369615,
                "dbsShopId": 1073490
            }]
        }
        response = self.services_for_goods.request_json('internal/services-for-goods/v1/available-services?rid=213', 'POST', body)
        self.assertFragmentIn(response, {
            "available_services": {
                "skus": {
                "539575033": {
                    "services": {
                    "ids": [
                        5
                    ]
                    }
                },
                "6838903": {
                    "services": {
                    "ids": [
                        214
                    ]
                    }
                }
                }
            }
        }, allow_different_len=False)

    def test_skus_with_one_bad_dbs_shop_id(self):
        """
        Проверяем скю из разных магазинов, для одного из них не доступны услуги
        """
        print("test_available_services")
        body = {
            "filters": [{
                "sku": "6838903",
                "hid": 14369615,
                "dbsShopId": 1058670
            },
            {
                "sku": "539575033",
                "hid": 14369615,
                "dbsShopId": 9999999
            }]
        }
        response = self.services_for_goods.request_json('internal/services-for-goods/v1/available-services?rid=213', 'POST', body)
        self.assertFragmentIn(response, {
            "available_services": {
                "skus": {
                "6838903": {
                    "services": {
                    "ids": [
                        214
                    ]
                    }
                }
                }
            }
        }, allow_different_len=False)
        self.assertFragmentNotIn(response, {
            "available_services": {
                "skus": {
                "539575033": {}
                }
            }
        })

if __name__ == '__main__':
    env.main()
