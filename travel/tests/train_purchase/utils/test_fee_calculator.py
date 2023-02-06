# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from collections import defaultdict
from decimal import Decimal, ROUND_CEILING
from math import floor

import pytest
from hamcrest import assert_that, has_properties

from common.apps.train_order.enums import CoachType
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_purchase.core.factories import ClientContractFactory
from travel.rasp.train_api.train_purchase.utils.fee_calculator import _get_experemental_fee, calculate_ticket_cost

pytestmark = [pytest.mark.mongouser]


def test_calculate_fee():
    contract = ClientContractFactory(partner_commission_sum=10)
    ticket_cost = calculate_ticket_cost(contract, CoachType.PLATZKARTE.value, Decimal(1000), service_amount=Decimal(0))
    assert ticket_cost.bedding_amount_without_fee == Decimal(0)
    assert_that(ticket_cost, has_properties(
        yandex_fee_percent=Decimal('0.11'),
        main_fee=Decimal(110),
        bedding_fee=Decimal(0),
    ))


def test_calculate_fee_on_small_summ():
    contract = ClientContractFactory(partner_commission_sum=30)
    ticket_cost = calculate_ticket_cost(contract, CoachType.PLATZKARTE.value, Decimal(100), service_amount=Decimal(0))
    assert ticket_cost.bedding_amount_without_fee == Decimal(0)
    assert_that(ticket_cost, has_properties(
        yandex_fee_percent=Decimal('0.11'),
        main_fee=Decimal(36),
        bedding_fee=Decimal(0),
    ))


def test_calculate_fee_with_bedding():
    contract = ClientContractFactory(partner_commission_sum=10)
    ticket_cost = calculate_ticket_cost(
        contract, CoachType.PLATZKARTE.value, Decimal(1000), service_amount=Decimal(100))
    assert ticket_cost.bedding_amount_without_fee == Decimal(100)
    assert_that(ticket_cost, has_properties(
        yandex_fee_percent=Decimal('0.11'),
        main_fee=Decimal(99),
        bedding_fee=Decimal(11),
    ))


def test_calculate_fee_with_bedding_on_small_summ():
    contract = ClientContractFactory(partner_commission_sum=30)
    ticket_cost = calculate_ticket_cost(contract, CoachType.PLATZKARTE.value, Decimal(100), service_amount=Decimal(10))
    assert ticket_cost.bedding_amount_without_fee == Decimal(10)
    assert_that(ticket_cost, has_properties(
        yandex_fee_percent=Decimal('0.11'),
        main_fee=Decimal('35.4'),
        bedding_fee=Decimal('1.1'),
    ))


def test_calculate_fee_without_contract():
    ticket_cost = calculate_ticket_cost(
        contract=None, coach_type=CoachType.PLATZKARTE.value, amount=Decimal(1000), service_amount=Decimal(100))
    assert ticket_cost.bedding_amount_without_fee == Decimal(100)
    assert_that(ticket_cost, has_properties(
        yandex_fee_percent=Decimal('0.11'),
        main_fee=Decimal(0),
        bedding_fee=Decimal(0),
    ))


@pytest.mark.parametrize('yandex_uid, delta_fee, expected_fee_percent, expected_main_fee, expected_bedding_fee', (
    ('fixed uid', '0.03', '0.1227', '110.43', '12.27'),
    (None, '0.03', '0.11', '99', '11'),
    ('fixed_uid', '0', '0.11', '99', '11'),
    ('fixed_uid', 'invalid delta', '0.11', '99', '11'),
    ('fixed uid', '0.03', '0.1227', '110.43', '12.27'),
))
def test_calculate_experiment_fee(yandex_uid, delta_fee, expected_fee_percent, expected_main_fee, expected_bedding_fee):
    contract = ClientContractFactory(partner_commission_sum=10)
    with replace_dynamic_setting('TRAIN_PURCHASE_EXPERIMENTAL_DELTA_FEE', delta_fee):
        ticket_cost = calculate_ticket_cost(
            contract, CoachType.PLATZKARTE.value, Decimal(1000), service_amount=Decimal(100),
            yandex_uid=yandex_uid,
        )
    assert ticket_cost.bedding_amount_without_fee == Decimal(100)
    assert_that(ticket_cost, has_properties(
        yandex_fee_percent=Decimal(expected_fee_percent),
        main_fee=Decimal(expected_main_fee),
        bedding_fee=Decimal(expected_bedding_fee),
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_EXPERIMENTAL_DELTA_FEE', '0.015')
def test_uniform_experiment_fee():
    """
    Проверка ряда наценок на равномерность распределения по критерию Пирсона: https://math.semestr.ru/group/xixi.php
    Значение хи-квадрат берем 0.5 для уровня свободы 30 - 1 = 29.
    """
    distribution = defaultdict(int)
    for i in range(1, 1000):
        fee = _get_experemental_fee(yandex_uid='uid_{}'.format(i)).quantize(Decimal('0.000'), rounding=ROUND_CEILING)
        distribution[fee] = distribution[fee] + 1
    distribution.pop(Decimal('0.095'), None)  # нижнее пограничное значение - побочный эффект округления
    assert len(distribution) == 30

    n = sum(v for k, v in distribution.items())
    e = floor(n // len(distribution))
    x = 0.
    chi_square_significant_29_dof = 28.34
    for k, v in distribution.items():
        x += pow(v - e, 2) / e
    assert x <= chi_square_significant_29_dof
