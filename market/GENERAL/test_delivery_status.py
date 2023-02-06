#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import NotEmpty

"""
Актуальный список всех ID на https://wiki.yandex-team.ru/users/tarnata/Treking-zakazov-na-Markete/

Проверяем только коды.
Здесь НЕ проверяются конкретные тексты описаний, т.к. они будут очень часто меняться в ближайшее время
"""


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    # Тесты на CGI

    def test_cgi_invalid(self):
        """
        Запрос без параметров ведёт к ошибке
        """
        response = self.report.request_json('place=delivery_status')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})

        self.error_log.expect(code=3043)

    def test_cgi_one_param(self):
        """
        Запрос одного delivery-status-id
        """
        response = self.report.request_json('place=delivery_status&delivery-status-id=0')
        self.assertFragmentIn(response, {"results": [{"id": 0}]})

    def test_cgi_one_param_several_values(self):
        """
        Запрос нескольких ID в одном параметре delivery-status-id
        """
        response = self.report.request_json('place=delivery_status&delivery-status-id=0,1,10')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 0},
                    {"id": 1},
                    {"id": 10},
                ]
            },
        )

    def test_cgi_several_params_one_value_per_param(self):
        """
        Запрос по одному ID в нескольких параметрах delivery-status-id
        """
        response = self.report.request_json(
            'place=delivery_status&delivery-status-id=0&delivery-status-id=1&delivery-status-id=10'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 0},
                    {"id": 1},
                    {"id": 10},
                ]
            },
        )

    def test_cgi_several_params_several_values(self):
        """
        Запрос по несколько ID в нескольких параметрах delivery-status-id
        """
        response = self.report.request_json('place=delivery_status&delivery-status-id=0,1&delivery-status-id=10,20,30')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 0},
                    {"id": 1},
                    {"id": 10},
                    {"id": 20},
                    {"id": 30},
                ]
            },
        )

    # Тесты на выхлоп

    def test_id_1(self):
        """
        Проверяем код с непустыми значениями, например 1
        """
        response = self.report.request_json('place=delivery_status&delivery-status-id=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryStatus",
                        "id": 1,
                        "code": "DELIVERY_LOADED",
                        "description": NotEmpty(),
                        "texts": {
                            "partnerInterface": NotEmpty(),
                            "desktop": NotEmpty(),
                            "mobile": NotEmpty(),
                        },
                    }
                ]
            },
        )

    def test_absent_id(self):
        """
        Проверка на ID, которого не существует (просто пустой результат)
        """
        response = self.report.request_json('place=delivery_status&delivery-status-id=100500')
        self.assertFragmentNotIn(response, {"results": [{"id": 100500}]})

    def test_delivery_status_with_delivery_type(self):
        """Проверяется, что для 48 статуса есть 2 варианта: для курьерки (0) и для самовывоза (1). Для других статусов есть только один вариант (0)"""
        for status, offer_shipping, delivery_type in [
            (48, "", 0),
            (48, "delivery", 0),
            (48, "pickup", 1),
            (49, "", 0),
            (49, "delivery", 0),
            (49, "pickup", 0),
        ]:
            response = self.report.request_json(
                'place=delivery_status&delivery-status-id={}&offer-shipping={}'.format(str(status), offer_shipping)
            )
            self.assertFragmentIn(response, {"results": [{"id": status, "deliveryType": delivery_type}]})


if __name__ == '__main__':
    main()
