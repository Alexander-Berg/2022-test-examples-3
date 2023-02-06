#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import CreditGlobalRestrictions, CreditPlan, Region, CreditTerm


MSK_RIDS = 213

GLOBAL_RESTRICTIONS = CreditGlobalRestrictions(
    min_price=2000, max_price=300000, category_blacklist=[654456, 321123], category_whitelist=[456654]
)

CREDIT_PLANS = [
    CreditPlan(
        plan_id='AD51BF786AA86B36BA57B8002FB4B474',
        bank="Сбербанк",
        term=12,
        rate=12.3,
        initial_payment_percent=0,
        min_price=3500,
        max_price=30000,
        category_blacklist=[123456, 654321],
        category_whitelist=[],
    ),
    CreditPlan(
        plan_id='C0AE65435E1D9065A64F1335B51C54AB',
        bank="Альфа-банк",
        term=6,
        rate=10.5,
        initial_payment_percent=0,
        min_price=2000,
        category_blacklist=[123321],
    ),
    CreditPlan(
        plan_id='0E966DEBAA73ABD8379FA316F8326B8D',
        bank="Райффайзен банк",
        terms=[
            CreditTerm(term=6, is_default=False),
            CreditTerm(term=12, is_default=False),
            CreditTerm(term=24, is_default=True),
        ],
        rate=13,
        initial_payment_percent=0,
        max_price=40000,
    ),
]

GLOBAL_RESTRICTIONS_FRAGMENT = GLOBAL_RESTRICTIONS.to_json(price_as_string=True)
CREDIT_PLANS_FRAGMENT = [plan.to_json(price_as_string=True, for_output_json=True) for plan in CREDIT_PLANS]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.index.regiontree += [Region(rid=MSK_RIDS, name="Москва")]

    @classmethod
    def prepare_credit_plans(cls):
        cls.index.credit_plans_container.global_restrictions = GLOBAL_RESTRICTIONS
        cls.index.credit_plans_container.credit_plans = CREDIT_PLANS

    def test_credit_plans(self):
        """
        Проверяем, что содержимое файла с кредитными программами (credit_plans.json) зачитывается корректно
        и присутствует в выдаче для синего и белого
        """
        request = 'place=prime' '&pp=18' '&debug=1' '&text=offer'
        test_fragment = [
            ('&rgb=blue', Absent(), CREDIT_PLANS_FRAGMENT),
            ('&rearr-factors=show_credits_on_white=1', Absent(), CREDIT_PLANS_FRAGMENT),
        ]
        for market_type, global_restrictions, credit_options in test_fragment:
            response = self.report.request_json(request + market_type)
            self.assertFragmentIn(
                response,
                {'search': {'globalRestrictions': global_restrictions, 'creditOptions': credit_options}},
                allow_different_len=False,
            )

    def test_credit_plans_place(self):
        """
        Проверяем, что условия кредитования от банков и глобальные ограничения корректно выдаются в place=credit_info
        """
        request = (
            'place=credit_info'
            '&offers-list=FakeSku-vm1Goleg:1;hid:1;p:1'
            '&total-price=0'
            '&currency=RUR'
            '&rids={rid}'
        )
        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(request.format(rid=MSK_RIDS) + market_type)
            self.assertFragmentIn(
                response,
                {'globalRestrictions': GLOBAL_RESTRICTIONS_FRAGMENT, 'creditOptions': CREDIT_PLANS_FRAGMENT},
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
