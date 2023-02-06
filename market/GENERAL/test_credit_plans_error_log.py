#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.logs import ErrorCodes
from core.types import CreditPlan, Currency

CREDIT_PLANS = [
    CreditPlan(
        plan_id='1', bank="Сбербанк", term=12, rate=12.3, initial_payment_percent=0, min_price=3500, max_price=10000
    ),
    CreditPlan(plan_id='2', bank="Альфа-банк", term=6, rate=10.5, initial_payment_percent=0, min_price=15000),
    CreditPlan(plan_id='3', bank="Райффайзен банк", term=24, rate=13, initial_payment_percent=0, max_price=40000),
]
CREDIT_PLANS_FRAGMENT = [plan.to_json(price_as_string=True) for plan in CREDIT_PLANS]
APPROVAL_RANGE_ERROR_MSG = "Invalid credit approval range: [{min_price} {currency}, {max_price} {currency}]".format(
    min_price=15000, max_price=10000, currency=Currency.RUR
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'

    @classmethod
    def prepare_credit_plans(cls):
        cls.index.credit_plans_container.credit_plans = CREDIT_PLANS

    def test_credit_plans_error(self):
        """
        В текущей реализации Калькулятора кредитов для Синего при чтении файла с кредитными программами
        'credit_plans.json' происходит проверка корректности заданных ограничений:
            - Никакое из минимальных ограничений  'restrictions':'minPrice' не превосходит никакое из
              максимальных  'restrictions':'maxPrice'
        В случае, если это условие не выполняется, происходит запись ошибки в  error.log Report'а
        https://st.yandex-team.ru/MARKETOUT-27727
        """
        request = 'place=prime' '&rgb=blue' '&pp=18' '&text=offer'
        self.report.request_json(request)
        self.error_log.expect(message=APPROVAL_RANGE_ERROR_MSG, code=ErrorCodes.FAILED_TO_PARSE_CREDIT_PLANS)
        self.base_logs_storage.error_log.expect(
            message=APPROVAL_RANGE_ERROR_MSG, code=ErrorCodes.FAILED_TO_PARSE_CREDIT_PLANS
        )


if __name__ == '__main__':
    main()
